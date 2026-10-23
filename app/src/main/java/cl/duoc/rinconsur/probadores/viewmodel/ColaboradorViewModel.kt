package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.data.SeccionDataStore
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.ColaboradorErrores
import cl.duoc.rinconsur.probadores.model.ColaboradorUiState
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.utils.RutValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Mantenedor de colaboradores: solo jefatura (RN-11).
class ColaboradorViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = SeccionDataStore(application)

    private val _estado = MutableStateFlow(ColaboradorUiState())
    val estado: StateFlow<ColaboradorUiState> = _estado

    // El ViewModel sobrevive a la rotación de pantalla, pero la pantalla vuelve a llamar
    // a iniciar(). Con esta marca no se borra lo que el usuario ya escribió.
    private var iniciado = false

    fun iniciar(rut: String?) {
        if (iniciado) return
        iniciado = true
        if (rut == null) {
            _estado.value = ColaboradorUiState()
            return
        }
        viewModelScope.launch {
            val c = dataStore.obtenerSeccion().first()?.colaboradores?.find { it.rut == rut } ?: return@launch
            _estado.value = ColaboradorUiState(
                esNuevo = false,
                rut = RutValidator.formatear(c.rut),
                nombre = c.nombre,
                perfil = c.perfil,
                turno = c.turno,
                activo = c.activo
            )
        }
    }

    fun onRutChange(valor: String) {
        val filtrado = valor.filter { it.isDigit() || it == '.' || it == '-' || it == 'k' || it == 'K' }.take(12)
        _estado.update { it.copy(rut = filtrado, errores = it.errores.copy(rut = null, general = null)) }
    }

    fun onNombreChange(valor: String) {
        _estado.update { it.copy(nombre = valor.take(60), errores = it.errores.copy(nombre = null)) }
    }

    fun onPerfilChange(valor: Perfil) {
        _estado.update { it.copy(perfil = valor, errores = it.errores.copy(perfil = null)) }
    }

    fun onTurnoChange(valor: TurnoLaboral) {
        _estado.update { it.copy(turno = valor, errores = it.errores.copy(turno = null)) }
    }

    fun onActivoChange(valor: Boolean) {
        _estado.update { it.copy(activo = valor, errores = it.errores.copy(general = null)) }
    }

    fun validarFormulario(): Boolean {
        val e = _estado.value
        val nombre = e.nombre.trim()
        val soloLetras = nombre.all { it.isLetter() || it == ' ' || it == '\'' || it == '-' }
        val errores = ColaboradorErrores(
            rut = when {
                e.rut.isBlank() -> "Campo obligatorio"
                !RutValidator.esValido(e.rut) -> "RUT inválido: revise el dígito verificador"
                else -> null
            },
            nombre = when {
                nombre.isBlank() -> "Campo obligatorio"
                !soloLetras -> "Use solo letras"
                nombre.split(" ").count { it.isNotBlank() } < 2 -> "Ingrese al menos nombre y apellido"
                else -> null
            },
            perfil = if (e.perfil == null) "Seleccione un perfil" else null,
            turno = if (e.turno == null) "Seleccione el turno habitual" else null
        )
        _estado.update { it.copy(errores = errores) }
        return listOfNotNull(errores.rut, errores.nombre, errores.perfil, errores.turno).isEmpty()
    }

    fun guardar(onExito: () -> Unit) {
        if (!validarFormulario()) return
        val e = _estado.value
        val perfil = e.perfil ?: return
        val turno = e.turno ?: return
        viewModelScope.launch {
            try {
                val rutJefatura = dataStore.obtenerSesion().first() ?: throw ReglaNegocioException("Su sesión terminó")
                val colaborador = Colaborador(
                    rut = RutValidator.normalizar(e.rut),
                    nombre = e.nombre.trim().split(" ").filter { it.isNotBlank() }.joinToString(" "),
                    perfil = perfil,
                    turno = turno,
                    activo = e.activo
                )
                dataStore.actualizar { s ->
                    ReglasSeccion.guardarColaborador(s, colaborador, rutJefatura, e.esNuevo) to Unit
                }
                onExito()
            } catch (ex: ReglaNegocioException) {
                _estado.update { it.copy(errores = it.errores.copy(general = ex.message)) }
            }
        }
    }
}
