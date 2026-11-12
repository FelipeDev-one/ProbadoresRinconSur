package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.ProbadoresApp
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Credencial
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasAcceso
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.utils.Contrasenas
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.utils.Validaciones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// =====================================================================
//  ConfiguracionViewModel — primera vez que se abre la app
//  ---------------------------------------------------------------------
//  La app parte sin datos. Quien la abre por primera vez:
//    1. crea la cuenta de jefatura (RUT, nombre, turno y contraseña),
//    2. indica cuántos probadores tiene la sección,
//    3. anota su código de recuperación.
//  Nada se guarda hasta confirmar el paso 3: si se cierra antes, se empieza de nuevo.
// =====================================================================

enum class PasoConfiguracion { JEFATURA, PROBADORES, CODIGO }

data class ConfiguracionUiState(
    val paso: PasoConfiguracion = PasoConfiguracion.JEFATURA,
    val rut: String = "",
    val nombre: String = "",
    val turno: TurnoLaboral? = null,
    val contrasena: String = "",
    val confirmacion: String = "",
    val estandar: String = "",
    val accesibles: String = "",
    val codigoGenerado: String? = null,
    val guardando: Boolean = false,
    val errores: ConfiguracionErrores = ConfiguracionErrores()
)

data class ConfiguracionErrores(
    val rut: String? = null,
    val nombre: String? = null,
    val turno: String? = null,
    val contrasena: String? = null,
    val confirmacion: String? = null,
    val estandar: String? = null,
    val accesibles: String? = null,
    val general: String? = null
)

class ConfiguracionViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = (application as ProbadoresApp).repositorio
    private val preferencias = (application as ProbadoresApp).preferencias

    private val _estado = MutableStateFlow(ConfiguracionUiState())
    val estado: StateFlow<ConfiguracionUiState> = _estado

    // Se arma en el paso 2 y se guarda recién al confirmar el código.
    private var credencialPendiente: Credencial? = null

    // ------------------------------- CAMPOS -------------------------------

    fun onRutChange(valor: String) {
        val limpio = valor.uppercase().filter { it.isDigit() || it == 'K' }.take(9)
        _estado.update { it.copy(rut = limpio, errores = it.errores.copy(rut = null)) }
    }

    fun onNombreChange(valor: String) =
        _estado.update { it.copy(nombre = valor.take(60), errores = it.errores.copy(nombre = null)) }

    fun onTurnoChange(valor: TurnoLaboral) =
        _estado.update { it.copy(turno = valor, errores = it.errores.copy(turno = null)) }

    fun onContrasenaChange(valor: String) =
        _estado.update { it.copy(contrasena = valor.take(64), errores = it.errores.copy(contrasena = null)) }

    fun onConfirmacionChange(valor: String) =
        _estado.update { it.copy(confirmacion = valor.take(64), errores = it.errores.copy(confirmacion = null)) }

    fun onEstandarChange(valor: String) = _estado.update {
        it.copy(estandar = valor.filter(Char::isDigit).take(2), errores = it.errores.copy(estandar = null, general = null))
    }

    fun onAccesiblesChange(valor: String) = _estado.update {
        it.copy(accesibles = valor.filter(Char::isDigit).take(2), errores = it.errores.copy(accesibles = null, general = null))
    }

    // -------------------------------- PASOS --------------------------------

    fun continuarAProbadores() {
        val e = _estado.value
        val (errorClave, errorConfirmacion) = Contrasenas.validarNueva(e.contrasena, e.confirmacion)
        val errores = ConfiguracionErrores(
            rut = Validaciones.rut(e.rut),
            nombre = Validaciones.nombrePersona(e.nombre),
            turno = if (e.turno == null) "Seleccione su turno habitual" else null,
            contrasena = errorClave,
            confirmacion = errorConfirmacion
        )
        val valido = listOfNotNull(
            errores.rut, errores.nombre, errores.turno, errores.contrasena, errores.confirmacion
        ).isEmpty()
        _estado.update {
            it.copy(errores = errores, paso = if (valido) PasoConfiguracion.PROBADORES else it.paso)
        }
    }

    fun volverAJefatura() = _estado.update {
        it.copy(paso = PasoConfiguracion.JEFATURA, errores = ConfiguracionErrores())
    }

    // Valida los probadores y prepara la contraseña; muestra el código de recuperación.
    fun continuarACodigo() {
        val e = _estado.value
        if (e.guardando) return
        val estandar = e.estandar.toIntOrNull()
        val accesibles = e.accesibles.toIntOrNull()
        val errores = ConfiguracionErrores(
            estandar = when {
                e.estandar.isBlank() -> "Campo obligatorio (puede ser 0)"
                estandar == null || estandar > ReglasSeccion.MAX_PROBADORES -> "Entre 0 y ${ReglasSeccion.MAX_PROBADORES}"
                else -> null
            },
            accesibles = when {
                e.accesibles.isBlank() -> "Campo obligatorio (puede ser 0)"
                accesibles == null || accesibles > ReglasSeccion.MAX_PROBADORES -> "Entre 0 y ${ReglasSeccion.MAX_PROBADORES}"
                else -> null
            },
            general = when {
                estandar == null || accesibles == null -> null
                estandar + accesibles < 1 -> "Registre al menos un probador."
                estandar + accesibles > ReglasSeccion.MAX_PROBADORES ->
                    "El máximo es ${ReglasSeccion.MAX_PROBADORES} probadores en total."
                else -> null
            }
        )
        if (listOfNotNull(errores.estandar, errores.accesibles, errores.general).isNotEmpty()) {
            _estado.update { it.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errores = ConfiguracionErrores()) }
            val rut = RutValidator.normalizar(e.rut)
            val (credencial, codigo) = withContext(Dispatchers.Default) { ReglasAcceso.crear(rut, e.contrasena) }
            credencialPendiente = credencial
            _estado.update {
                it.copy(guardando = false, paso = PasoConfiguracion.CODIGO, codigoGenerado = codigo)
            }
        }
    }

    // Guarda todo de una vez. La sesión va primero para que la app abra directo en el panel.
    fun finalizar() {
        val e = _estado.value
        val credencial = credencialPendiente ?: return
        val turno = e.turno ?: return
        if (e.guardando) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true) }
            val jefatura = Colaborador(
                rut = credencial.rut,
                nombre = Validaciones.limpiarNombre(e.nombre),
                perfil = Perfil.JEFATURA,
                turno = turno
            )
            try {
                preferencias.guardarSesion(jefatura.rut)
                repositorio.actualizar { s ->
                    ReglasSeccion.configurarInicial(
                        s, jefatura, e.estandar.toInt(), e.accesibles.toInt(), System.currentTimeMillis()
                    ) to Unit
                }
                repositorio.guardarCredencial(credencial)
                preferencias.agregarReciente(jefatura.rut)
            } catch (ex: ReglaNegocioException) {
                preferencias.guardarSesion(null)
                _estado.update {
                    it.copy(
                        guardando = false,
                        paso = PasoConfiguracion.PROBADORES,
                        errores = ConfiguracionErrores(general = ex.message)
                    )
                }
            }
        }
    }
}
