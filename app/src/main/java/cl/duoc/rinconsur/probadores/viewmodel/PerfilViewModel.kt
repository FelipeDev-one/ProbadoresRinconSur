package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.ProbadoresApp
import cl.duoc.rinconsur.probadores.model.Cuenta
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasAcceso
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.model.cuenta
import cl.duoc.rinconsur.probadores.utils.Contrasenas
import cl.duoc.rinconsur.probadores.utils.Tiempo
import cl.duoc.rinconsur.probadores.utils.Validaciones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =====================================================================
//  PerfilViewModel — datos propios y cambio de contraseña
//  ---------------------------------------------------------------------
//  Sirve para las dos clases de cuenta:
//   · Colaborador: edita su nombre y su turno habitual (el perfil y el estado
//     solo los cambia la jefatura).
//   · Cliente: edita su nombre.
//  Ambos cambian su contraseña indicando la actual; al cambiarla reciben un
//  código de recuperación nuevo.
// =====================================================================

data class PerfilUiState(
    val cuenta: Cuenta? = null,
    val nombre: String = "",
    val turno: TurnoLaboral? = null,
    val actividad: String = "",
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val errorNombre: String? = null,
    val errorGeneral: String? = null
) {
    // Hay cambios sin guardar respecto de lo que está en la base de datos.
    fun hayCambios(turnoOriginal: TurnoLaboral?): Boolean =
        cuenta != null && (Validaciones.limpiarNombre(nombre) != cuenta.nombre || turno != turnoOriginal)
}

data class ClaveUiState(
    val actual: String = "",
    val nueva: String = "",
    val confirmacion: String = "",
    val errorActual: String? = null,
    val errorNueva: String? = null,
    val errorConfirmacion: String? = null,
    val guardando: Boolean = false,
    val codigoGenerado: String? = null
)

class PerfilViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = (application as ProbadoresApp).repositorio
    private val preferencias = (application as ProbadoresApp).preferencias

    private val _perfil = MutableStateFlow(PerfilUiState())
    val perfil: StateFlow<PerfilUiState> = _perfil

    private val _clave = MutableStateFlow(ClaveUiState())
    val clave: StateFlow<ClaveUiState> = _clave

    var turnoOriginal: TurnoLaboral? = null
        private set

    init {
        viewModelScope.launch { cargar() }
    }

    private suspend fun cargar() {
        val rut = preferencias.obtenerSesion().first() ?: return
        val seccion = repositorio.obtenerSeccion().first() ?: return
        val cuenta = seccion.cuenta(rut) ?: return
        turnoOriginal = seccion.colaboradores.find { it.rut == rut }?.turno
        val hoy = Tiempo.fechaIso(System.currentTimeMillis())
        // Consulta con relaciones: lo que registró este colaborador (colaboradores → turnos y usos).
        val actividad = if (cuenta.esCliente) {
            val turnos = seccion.turnos.filter { it.clienteRut == rut }
            "Turnos tomados: ${turnos.count { it.fecha == hoy }} hoy · ${turnos.size} en total"
        } else {
            val registros = repositorio.registrosDe(rut)
            val turnosHoy = registros?.second?.count { it.fecha == hoy } ?: 0
            val usosHoy = registros?.third?.count { Tiempo.fechaIso(it.inicio) == hoy } ?: 0
            "Hoy: $turnosHoy turno(s) registrados · $usosHoy uso(s) de probador iniciados"
        }
        _perfil.value = PerfilUiState(cuenta = cuenta, nombre = cuenta.nombre, turno = turnoOriginal, actividad = actividad)
    }

    // ----------------------------- DATOS PROPIOS -----------------------------

    fun onNombreChange(valor: String) = _perfil.update {
        it.copy(nombre = valor.take(60), errorNombre = null, errorGeneral = null, guardado = false)
    }

    fun onTurnoChange(valor: TurnoLaboral) = _perfil.update { it.copy(turno = valor, guardado = false) }

    fun guardarDatos() {
        val e = _perfil.value
        val cuenta = e.cuenta ?: return
        if (e.guardando) return
        val error = Validaciones.nombrePersona(e.nombre)
        if (error != null) {
            _perfil.update { it.copy(errorNombre = error) }
            return
        }
        val nombre = Validaciones.limpiarNombre(e.nombre)
        viewModelScope.launch {
            _perfil.update { it.copy(guardando = true) }
            try {
                repositorio.actualizar { s ->
                    val turno = e.turno
                    val nueva = if (cuenta.esCliente || turno == null) {
                        ReglasSeccion.actualizarNombreCliente(s, cuenta.rut, nombre)
                    } else {
                        ReglasSeccion.actualizarDatosPropios(s, cuenta.rut, nombre, turno)
                    }
                    nueva to Unit
                }
                turnoOriginal = e.turno
                _perfil.update {
                    it.copy(guardando = false, guardado = true, nombre = nombre, cuenta = cuenta.copy(nombre = nombre))
                }
            } catch (ex: ReglaNegocioException) {
                _perfil.update { it.copy(guardando = false, errorGeneral = ex.message) }
            }
        }
    }

    // --------------------------- CAMBIAR CONTRASEÑA ---------------------------

    fun onActualChange(valor: String) = _clave.update { it.copy(actual = valor.take(64), errorActual = null) }

    fun onNuevaChange(valor: String) = _clave.update { it.copy(nueva = valor.take(64), errorNueva = null) }

    fun onConfirmacionChange(valor: String) =
        _clave.update { it.copy(confirmacion = valor.take(64), errorConfirmacion = null) }

    fun cambiarClave() {
        val e = _clave.value
        if (e.guardando) return
        val (errorNueva, errorConfirmacion) = Contrasenas.validarNueva(e.nueva, e.confirmacion)
        val errorActual = if (e.actual.isBlank()) "Ingrese su contraseña actual" else null
        val errorIgual = if (errorNueva == null && errorActual == null && e.nueva == e.actual) {
            "La nueva contraseña debe ser distinta de la actual"
        } else null
        if (listOfNotNull(errorActual, errorNueva, errorIgual, errorConfirmacion).isNotEmpty()) {
            _clave.update {
                it.copy(errorActual = errorActual, errorNueva = errorNueva ?: errorIgual, errorConfirmacion = errorConfirmacion)
            }
            return
        }
        viewModelScope.launch {
            _clave.update { it.copy(guardando = true) }
            val rut = preferencias.obtenerSesion().first()
            val credencial = rut?.let { repositorio.obtenerCredencial(it) }
            if (rut == null || credencial == null) {
                _clave.update { it.copy(guardando = false, errorActual = "Su sesión terminó. Ingrese nuevamente.") }
                return@launch
            }
            val ahora = System.currentTimeMillis()
            val segundos = ReglasAcceso.segundosBloqueo(credencial, ahora)
            if (segundos > 0) {
                _clave.update {
                    it.copy(guardando = false, errorActual = "Demasiados intentos. Espere $segundos segundos.")
                }
                return@launch
            }
            val correcta = withContext(Dispatchers.Default) { ReglasAcceso.contrasenaCorrecta(credencial, e.actual) }
            if (!correcta) {
                repositorio.guardarCredencial(ReglasAcceso.registrarFallo(credencial, ahora))
                _clave.update { it.copy(guardando = false, actual = "", errorActual = "La contraseña actual no es correcta") }
                return@launch
            }
            val (nueva, codigo) = withContext(Dispatchers.Default) { ReglasAcceso.crear(rut, e.nueva) }
            repositorio.guardarCredencial(nueva)
            _clave.value = ClaveUiState(codigoGenerado = codigo)
        }
    }
}
