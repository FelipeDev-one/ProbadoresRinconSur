package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.data.SeccionDataStore
import cl.duoc.rinconsur.probadores.model.EstimadorEspera
import cl.duoc.rinconsur.probadores.model.MotivoPreferente
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.TurnoErrores
import cl.duoc.rinconsur.probadores.model.TurnoUiState
import cl.duoc.rinconsur.probadores.utils.Tiempo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TurnoViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = SeccionDataStore(application)

    private val _estado = MutableStateFlow(TurnoUiState())
    val estado: StateFlow<TurnoUiState> = _estado

    private var maxPrendas = 6

    init {
        viewModelScope.launch {
            maxPrendas = dataStore.obtenerSeccion().first()?.parametros?.maxPrendas ?: 6
        }
    }

    fun onPrendasChange(valor: String) {
        _estado.update {
            it.copy(prendas = valor.filter(Char::isDigit).take(2), errores = it.errores.copy(prendas = null, general = null))
        }
    }

    fun sumarPrenda(delta: Int) {
        val actual = _estado.value.prendas.toIntOrNull() ?: 0
        onPrendasChange((actual + delta).coerceIn(1, 99).toString())
    }

    fun onPreferenteChange(valor: Boolean) {
        _estado.update {
            it.copy(
                preferente = valor,
                motivoPreferente = if (valor) it.motivoPreferente else null,
                errores = it.errores.copy(motivoPreferente = null)
            )
        }
    }

    fun onMotivoPreferenteChange(valor: MotivoPreferente) {
        _estado.update { it.copy(motivoPreferente = valor, errores = it.errores.copy(motivoPreferente = null)) }
    }

    fun validarFormulario(): Boolean {
        val e = _estado.value
        val prendas = e.prendas.toIntOrNull()
        val errores = TurnoErrores(
            prendas = when {
                prendas == null -> "Campo obligatorio"
                prendas < 1 -> "Debe ingresar al menos 1 prenda"
                prendas > maxPrendas -> "El máximo es $maxPrendas prendas por ingreso (RN-02)"
                else -> null
            },
            motivoPreferente = if (e.preferente && e.motivoPreferente == null)
                "Seleccione el motivo de la atención preferente" else null
        )
        _estado.update { it.copy(errores = errores) }
        return errores.prendas == null && errores.motivoPreferente == null
    }

    fun registrar(ignorarHorario: Boolean, onExito: (codigo: String, estimado: Int?) -> Unit) {
        if (!validarFormulario()) return
        viewModelScope.launch {
            val rut = dataStore.obtenerSesion().first()
            if (rut == null) {
                _estado.update { it.copy(errores = it.errores.copy(general = "Su sesión terminó")) }
                return@launch
            }
            _estado.update { it.copy(guardando = true) }
            try {
                val e = _estado.value
                val (codigo, estimado) = dataStore.actualizar { s ->
                    val ahora = System.currentTimeMillis()
                    val (nueva, turno) = ReglasSeccion.registrarTurno(
                        s, e.prendas.toInt(), e.preferente, rut, ahora, ignorarHorario
                    )
                    val fila = ReglasSeccion.filaDelDia(nueva, Tiempo.fechaIso(ahora))
                    val delante = fila.indexOfFirst { it.id == turno.id }
                    nueva to (turno.codigo to EstimadorEspera.enSeccion(nueva, delante, ahora))
                }
                _estado.value = TurnoUiState()
                onExito(codigo, estimado)
            } catch (ex: ReglaNegocioException) {
                _estado.update { it.copy(guardando = false, errores = it.errores.copy(general = ex.message)) }
            }
        }
    }
}
