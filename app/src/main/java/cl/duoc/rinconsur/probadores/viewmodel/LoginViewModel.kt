package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.ProbadoresApp
import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Credencial
import cl.duoc.rinconsur.probadores.model.Cuenta
import cl.duoc.rinconsur.probadores.model.LoginErrores
import cl.duoc.rinconsur.probadores.model.LoginUiState
import cl.duoc.rinconsur.probadores.model.PasoLogin
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasAcceso
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.cuenta
import cl.duoc.rinconsur.probadores.utils.Contrasenas
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.utils.Validaciones
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
//  LoginViewModel — ingreso con RUT y contraseña para todas las cuentas
//  ---------------------------------------------------------------------
//  RUT ─┬─ cuenta con contraseña ─────▶ CONTRASEÑA ─────────────▶ su vista
//       │                                 └─ ¿la olvidó? ─▶ RECUPERAR ─┐
//       ├─ colaborador sin contraseña ─▶ CREAR (primer ingreso) ───────┤
//       └─ "Crear cuenta de cliente" ──▶ REGISTRO ─────────────────────┴▶ CÓDIGO NUEVO ─▶ su vista
//
//  La cuenta de cliente la crea el propio cliente. Las de colaborador solo las crea
//  la jefatura (Ajustes → Colaboradores); el colaborador elige su contraseña al ingresar
//  por primera vez. 5 intentos fallidos = 1 minuto de bloqueo.
//  Al terminar se avisa el perfil de la cuenta (null = cliente) para abrir su vista.
// =====================================================================

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = (application as ProbadoresApp).repositorio
    private val preferencias = (application as ProbadoresApp).preferencias

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado

    // Perfil de la cuenta que está ingresando (null = cliente).
    private var perfilActual: Perfil? = null

    // Accesos recientes en este equipo (solo cuentas que siguen activas).
    val recientes: StateFlow<List<Cuenta>> =
        combine(repositorio.obtenerSeccion(), preferencias.obtenerRecientes()) { s, ruts ->
            ruts.mapNotNull { rut -> s?.cuenta(rut)?.takeIf { it.activa } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ------------------------------- CAMPOS -------------------------------

    // Se guarda solo dígitos y K; los puntos y el guion los dibuja la pantalla.
    fun onRutChange(valor: String) {
        val limpio = valor.uppercase().filter { it.isDigit() || it == 'K' }.take(9)
        _estado.update { it.copy(rut = limpio, errores = it.errores.copy(rut = null)) }
    }

    fun onNombreChange(valor: String) =
        _estado.update { it.copy(nombre = valor.take(60), errores = it.errores.copy(nombre = null)) }

    fun onContrasenaChange(valor: String) =
        _estado.update { it.copy(contrasena = valor.take(64), errores = it.errores.copy(contrasena = null)) }

    fun onConfirmacionChange(valor: String) =
        _estado.update { it.copy(confirmacion = valor.take(64), errores = it.errores.copy(confirmacion = null)) }

    fun onCodigoChange(valor: String) {
        val limpio = valor.uppercase().filter { it.isLetterOrDigit() || it == '-' }.take(9)
        _estado.update { it.copy(codigo = limpio, errores = it.errores.copy(codigo = null)) }
    }

    // ------------------------------ NAVEGACIÓN ------------------------------

    // Vuelve al inicio del modo actual: clientes o colaboradores.
    fun volverAlRut() {
        val colaborador = _estado.value.modoColaborador
        _estado.value = LoginUiState(
            paso = if (colaborador) PasoLogin.COLABORADOR else PasoLogin.RUT,
            modoColaborador = colaborador
        )
    }

    fun irAColaboradores() {
        _estado.value = LoginUiState(paso = PasoLogin.COLABORADOR, modoColaborador = true)
    }

    fun irAClientes() {
        _estado.value = LoginUiState()
    }

    fun irARegistro() = _estado.update {
        LoginUiState(paso = PasoLogin.REGISTRO, rut = it.rut)
    }

    fun irARecuperar() = _estado.update {
        it.copy(paso = PasoLogin.RECUPERAR, contrasena = "", confirmacion = "", codigo = "", errores = LoginErrores())
    }

    fun volverAContrasena() = _estado.update {
        it.copy(paso = PasoLogin.CONTRASENA, contrasena = "", confirmacion = "", codigo = "", errores = LoginErrores())
    }

    fun seleccionarReciente(cuenta: Cuenta) {
        _estado.update { it.copy(rut = cuenta.rut.replace("-", ""), errores = LoginErrores()) }
        continuar()
    }

    fun quitarReciente(cuenta: Cuenta) {
        viewModelScope.launch { preferencias.quitarReciente(cuenta.rut) }
    }

    // -------------------------------- PASO 1 --------------------------------

    fun continuar() = identificar(recuperar = false)

    // "¿Olvidó su contraseña?" desde la primera pantalla: pide el RUT y salta directo a recuperar.
    fun olvideContrasena() = identificar(recuperar = true)

    private fun identificar(recuperar: Boolean) {
        val actual = _estado.value
        if (actual.cargando) return
        val errorFormato = when {
            actual.rut.isBlank() && recuperar -> "Ingrese su RUT para recuperar su contraseña"
            else -> Validaciones.rut(actual.rut)
        }
        if (errorFormato != null) {
            _estado.update { it.copy(errores = LoginErrores(rut = errorFormato)) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val rut = RutValidator.normalizar(actual.rut)
            val cuenta = repositorio.obtenerSeccion().first()?.cuenta(rut)
            val error = when {
                cuenta == null && actual.modoColaborador ->
                    "Este RUT no está registrado como colaborador. Consulte con la jefatura."
                cuenta == null -> "No hay una cuenta con ese RUT. Puede crear una arriba."
                // Cada tipo de cuenta ingresa por su propia pantalla.
                actual.modoColaborador && cuenta.esCliente ->
                    "Es una cuenta de cliente: ingrese desde la pantalla de clientes."
                !actual.modoColaborador && !cuenta.esCliente ->
                    "Es una cuenta de colaborador: use el ingreso de colaboradores."
                !cuenta.activa -> "Colaborador inactivo: consulte con la jefatura (RN-13)"
                else -> null
            }
            if (error != null || cuenta == null) {
                _estado.update { it.copy(cargando = false, errores = LoginErrores(rut = error)) }
                return@launch
            }
            perfilActual = cuenta.perfil
            val tieneContrasena = repositorio.obtenerCredencial(rut) != null
            _estado.update {
                it.copy(
                    cargando = false,
                    nombre = cuenta.nombre,
                    rol = cuenta.rol,
                    paso = when {
                        !tieneContrasena -> PasoLogin.CREAR_CONTRASENA
                        recuperar -> PasoLogin.RECUPERAR
                        else -> PasoLogin.CONTRASENA
                    },
                    errores = LoginErrores()
                )
            }
        }
    }

    // ------------------------------ CONTRASEÑA ------------------------------

    fun ingresarConContrasena(onExito: (Perfil?) -> Unit) {
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
                // La jefatura la restableció mientras tanto: se pide crear una nueva.
                _estado.update { it.copy(cargando = false, paso = PasoLogin.CREAR_CONTRASENA, contrasena = "") }
                return@launch
            }
            val error = verificarConBloqueo(credencial, "Contraseña incorrecta") {
                ReglasAcceso.contrasenaCorrecta(it, actual.contrasena)
            }
            if (error == null) finalizar(rut, onExito)
            else _estado.update { it.copy(cargando = false, contrasena = "", errores = LoginErrores(contrasena = error)) }
        }
    }

    // Primer ingreso de un colaborador creado por la jefatura (o tras un restablecimiento).
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
            val error = verificarConBloqueo(credencial, "Código incorrecto") {
                ReglasAcceso.codigoCorrecto(it, actual.codigo)
            }
            if (error == null) guardarNueva(rut, actual.contrasena)
            else _estado.update { it.copy(cargando = false, errores = LoginErrores(codigo = error)) }
        }
    }

    // ---------------------------- REGISTRO DE CLIENTE ----------------------------

    fun registrarCliente() {
        val actual = _estado.value
        if (actual.cargando) return
        val (errorClave, errorConfirmacion) = Contrasenas.validarNueva(actual.contrasena, actual.confirmacion)
        val errores = LoginErrores(
            rut = Validaciones.rut(actual.rut),
            nombre = Validaciones.nombrePersona(actual.nombre),
            contrasena = errorClave,
            confirmacion = errorConfirmacion
        )
        if (listOfNotNull(errores.rut, errores.nombre, errores.contrasena, errores.confirmacion).isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, errores = LoginErrores()) }
            val rut = RutValidator.normalizar(actual.rut)
            val cliente = Cliente(rut, Validaciones.limpiarNombre(actual.nombre), System.currentTimeMillis())
            try {
                repositorio.actualizar { s -> ReglasSeccion.registrarCliente(s, cliente) to Unit }
                perfilActual = null
                _estado.update { it.copy(nombre = cliente.nombre, rol = "Cliente") }
                guardarNueva(rut, actual.contrasena)
            } catch (ex: ReglaNegocioException) {
                _estado.update { it.copy(cargando = false, errores = LoginErrores(rut = ex.message)) }
            }
        }
    }

    fun confirmarCodigoGuardado(onExito: (Perfil?) -> Unit) {
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

    private suspend fun finalizar(rut: String, onExito: (Perfil?) -> Unit) {
        preferencias.guardarSesion(rut)
        preferencias.agregarReciente(rut)
        _estado.value = LoginUiState()
        onExito(perfilActual)
    }
}
