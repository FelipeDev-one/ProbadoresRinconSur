package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.ProbadoresApp
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Credencial
import cl.duoc.rinconsur.probadores.model.LoginErrores
import cl.duoc.rinconsur.probadores.model.LoginUiState
import cl.duoc.rinconsur.probadores.model.PasoLogin
import cl.duoc.rinconsur.probadores.model.ReglasAcceso
import cl.duoc.rinconsur.probadores.utils.Contrasenas
import cl.duoc.rinconsur.probadores.utils.RutValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =====================================================================
//  LoginViewModel — ingreso de colaboradores (RF-10, RN-13)
//  ---------------------------------------------------------------------
//  Paso 1: RUT (todos). Asistentes y vendedores(as) entran directo.
//  Paso 2: solo jefatura → contraseña. Si aún no tiene, la crea y recibe
//  un código de recuperación que se muestra UNA vez. Con ese código puede
//  poner una contraseña nueva si la olvida. 5 intentos fallidos = 1 min bloqueado.
// =====================================================================

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = (application as ProbadoresApp).repositorio
    private val preferencias = (application as ProbadoresApp).preferencias

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado

    // Accesos recientes en este equipo (solo colaboradores que siguen activos).
    val recientes: StateFlow<List<Colaborador>> =
        combine(repositorio.obtenerSeccion(), preferencias.obtenerRecientes()) { s, ruts ->
            val colaboradores = s?.colaboradores ?: emptyList()
            ruts.mapNotNull { rut -> colaboradores.find { it.rut == rut && it.activo } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ------------------------------- CAMPOS -------------------------------

    // Se guarda solo dígitos y K; los puntos y el guion los dibuja la pantalla.
    fun onRutChange(valor: String) {
        val limpio = valor.uppercase().filter { it.isDigit() || it == 'K' }.take(9)
        _estado.update { it.copy(rut = limpio, errores = it.errores.copy(rut = null)) }
    }

    fun onContrasenaChange(valor: String) =
        _estado.update { it.copy(contrasena = valor.take(64), errores = it.errores.copy(contrasena = null)) }

    fun onConfirmacionChange(valor: String) =
        _estado.update { it.copy(confirmacion = valor.take(64), errores = it.errores.copy(confirmacion = null)) }

    fun onCodigoChange(valor: String) {
        val limpio = valor.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(9)
        _estado.update { it.copy(codigo = limpio, errores = it.errores.copy(codigo = null)) }
    }

    // ------------------------------ NAVEGACIÓN ------------------------------

    fun volverAlRut() {
        _estado.value = LoginUiState(rut = "")
    }

    fun irARecuperar() = _estado.update {
        it.copy(paso = PasoLogin.RECUPERAR, contrasena = "", confirmacion = "", codigo = "", errores = LoginErrores())
    }

    fun volverAContrasena() = _estado.update {
        it.copy(paso = PasoLogin.CONTRASENA, contrasena = "", confirmacion = "", codigo = "", errores = LoginErrores())
    }

    fun seleccionarReciente(colaborador: Colaborador, onExito: () -> Unit) {
        _estado.update { it.copy(rut = colaborador.rut.replace("-", ""), errores = LoginErrores()) }
        continuar(onExito)
    }

    fun quitarReciente(colaborador: Colaborador) {
        viewModelScope.launch { preferencias.quitarReciente(colaborador.rut) }
    }

    // -------------------------------- PASO 1 --------------------------------

    fun continuar(onExito: () -> Unit) {
        val actual = _estado.value
        if (actual.cargando) return
        val errorFormato = when {
            actual.rut.isBlank() -> "Campo obligatorio"
            !RutValidator.esValido(actual.rut) -> "RUT inválido: revise el dígito verificador"
            else -> null
        }
        if (errorFormato != null) {
            _estado.update { it.copy(errores = LoginErrores(rut = errorFormato)) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val rut = RutValidator.normalizar(actual.rut)
            val colaborador = repositorio.obtenerSeccion().first()?.colaboradores?.find { it.rut == rut }
            val error = when {
                colaborador == null -> "Este RUT no está registrado como colaborador"
                !colaborador.activo -> "Colaborador inactivo: consulte con la jefatura (RN-13)"
                else -> null
            }
            when {
                error != null || colaborador == null ->
                    _estado.update { it.copy(cargando = false, errores = LoginErrores(rut = error)) }
                !colaborador.perfil.esJefatura -> finalizar(rut, onExito)
                else -> {
                    val tieneContrasena = repositorio.obtenerCredencial(rut) != null
                    _estado.update {
                        it.copy(
                            cargando = false,
                            nombre = colaborador.nombre,
                            paso = if (tieneContrasena) PasoLogin.CONTRASENA else PasoLogin.CREAR_CONTRASENA,
                            errores = LoginErrores()
                        )
                    }
                }
            }
        }
    }

    // ------------------------------ CONTRASEÑA ------------------------------

    fun ingresarConContrasena(onExito: () -> Unit) {
        val actual = _estado.value
        if (actual.cargando) return
        if (actual.contrasena.isBlank()) {
            _estado.update { it.copy(errores = LoginErrores(contrasena = "Ingrese su contraseña")) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val rut = RutValidator.normalizar(actual.rut)
            val credencial = repositorio.obtenerCredencial(rut)
            if (credencial == null) {
                // Otra jefatura la restableció mientras tanto: se pide crear una nueva.
                _estado.update { it.copy(cargando = false, paso = PasoLogin.CREAR_CONTRASENA, contrasena = "") }
                return@launch
            }
            val error = verificarConBloqueo(credencial, "Contraseña incorrecta") { ReglasAcceso.contrasenaCorrecta(it, actual.contrasena) }
            if (error == null) finalizar(rut, onExito)
            else _estado.update { it.copy(cargando = false, contrasena = "", errores = LoginErrores(contrasena = error)) }
        }
    }

    fun crearContrasena() {
        val actual = _estado.value
        if (actual.cargando || !validarNueva()) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val rut = RutValidator.normalizar(actual.rut)
            // Si ya existe (p. ej. se creó desde otro intento), no se pisa: se pide ingresar.
            if (repositorio.obtenerCredencial(rut) != null) {
                _estado.update { it.copy(cargando = false, paso = PasoLogin.CONTRASENA, contrasena = "", confirmacion = "") }
                return@launch
            }
            guardarNueva(rut, actual.contrasena)
        }
    }

    fun recuperar() {
        val actual = _estado.value
        if (actual.cargando) return
        val codigoNormalizado = Contrasenas.normalizarCodigo(actual.codigo)
        val errorCodigo = if (codigoNormalizado.length != 8) "El código tiene 8 caracteres (XXXX-XXXX)" else null
        val nuevaValida = validarNueva()
        if (errorCodigo != null) {
            _estado.update { it.copy(errores = it.errores.copy(codigo = errorCodigo)) }
        }
        if (errorCodigo != null || !nuevaValida) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val rut = RutValidator.normalizar(actual.rut)
            val credencial = repositorio.obtenerCredencial(rut)
            if (credencial == null) {
                _estado.update { it.copy(cargando = false, paso = PasoLogin.CREAR_CONTRASENA, codigo = "") }
                return@launch
            }
            val error = verificarConBloqueo(credencial, "Código incorrecto") { ReglasAcceso.codigoCorrecto(it, actual.codigo) }
            if (error == null) guardarNueva(rut, actual.contrasena)
            else _estado.update { it.copy(cargando = false, errores = LoginErrores(codigo = error)) }
        }
    }

    fun confirmarCodigoGuardado(onExito: () -> Unit) {
        viewModelScope.launch { finalizar(RutValidator.normalizar(_estado.value.rut), onExito) }
    }

    // ------------------------------- INTERNOS -------------------------------

    private fun validarNueva(): Boolean {
        val actual = _estado.value
        val (error, errorConfirmacion) = Contrasenas.validarNueva(actual.contrasena, actual.confirmacion)
        _estado.update { it.copy(errores = it.errores.copy(contrasena = error, confirmacion = errorConfirmacion)) }
        return error == null && errorConfirmacion == null
    }

    // Revisa bloqueo, verifica (el hash es lento: va fuera del hilo principal) y guarda el resultado.
    private suspend fun verificarConBloqueo(
        credencial: Credencial,
        textoError: String,
        verificar: (Credencial) -> Boolean
    ): String? {
        val ahora = System.currentTimeMillis()
        val segundos = ReglasAcceso.segundosBloqueo(credencial, ahora)
        if (segundos > 0) return "Demasiados intentos. Espere $segundos segundos e intente de nuevo."

        val correcto = withContext(Dispatchers.Default) { verificar(credencial) }
        if (correcto) {
            repositorio.guardarCredencial(ReglasAcceso.registrarExito(credencial))
            return null
        }
        val actualizada = ReglasAcceso.registrarFallo(credencial, ahora)
        repositorio.guardarCredencial(actualizada)
        return if (actualizada.bloqueadaHasta > ahora) {
            "Demasiados intentos. Espere ${ReglasAcceso.MILIS_BLOQUEO / 1000} segundos e intente de nuevo."
        } else {
            val quedan = ReglasAcceso.intentosRestantes(actualizada)
            "$textoError. Le queda${if (quedan == 1) "" else "n"} $quedan intento${if (quedan == 1) "" else "s"}."
        }
    }

    private suspend fun guardarNueva(rut: String, contrasena: String) {
        val (credencial, codigo) = withContext(Dispatchers.Default) { ReglasAcceso.crear(rut, contrasena) }
        repositorio.guardarCredencial(credencial)
        _estado.update {
            it.copy(
                cargando = false,
                paso = PasoLogin.CODIGO_NUEVO,
                codigoGenerado = codigo,
                contrasena = "",
                confirmacion = "",
                codigo = "",
                errores = LoginErrores()
            )
        }
    }

    private suspend fun finalizar(rut: String, onExito: () -> Unit) {
        preferencias.guardarSesion(rut)
        preferencias.agregarReciente(rut)
        _estado.value = LoginUiState()
        onExito()
    }
}
