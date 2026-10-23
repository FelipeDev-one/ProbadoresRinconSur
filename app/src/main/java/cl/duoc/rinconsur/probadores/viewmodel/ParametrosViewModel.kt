package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.data.SeccionDataStore
import cl.duoc.rinconsur.probadores.model.ParametrosErrores
import cl.duoc.rinconsur.probadores.model.ParametrosUiState
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Ajuste de parámetros de la sección por la jefatura (RD-05).
class ParametrosViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = SeccionDataStore(application)

    private val _estado = MutableStateFlow(ParametrosUiState())
    val estado: StateFlow<ParametrosUiState> = _estado

    init {
        viewModelScope.launch {
            val p = dataStore.obtenerSeccion().first()?.parametros ?: return@launch
            _estado.value = ParametrosUiState(
                maxPrendas = p.maxPrendas.toString(),
                tiempoReferenciaUso = p.tiempoReferenciaUso.toString(),
                umbralProlongado = p.umbralProlongado.toString(),
                umbralAyuda = p.umbralAyuda.toString(),
                minutosPresentarse = p.minutosPresentarse.toString(),
                ticketPromedio = p.ticketPromedio.toString()
            )
        }
    }

    private fun soloDigitos(valor: String, largo: Int = 3) = valor.filter(Char::isDigit).take(largo)

    fun onMaxPrendasChange(v: String) = _estado.update {
        it.copy(maxPrendas = soloDigitos(v, 2), errores = it.errores.copy(maxPrendas = null, general = null))
    }

    fun onTiempoReferenciaChange(v: String) = _estado.update {
        it.copy(tiempoReferenciaUso = soloDigitos(v), errores = it.errores.copy(tiempoReferenciaUso = null, general = null))
    }

    fun onUmbralProlongadoChange(v: String) = _estado.update {
        it.copy(umbralProlongado = soloDigitos(v), errores = it.errores.copy(umbralProlongado = null, general = null))
    }

    fun onUmbralAyudaChange(v: String) = _estado.update {
        it.copy(umbralAyuda = soloDigitos(v), errores = it.errores.copy(umbralAyuda = null, general = null))
    }

    fun onMinutosPresentarseChange(v: String) = _estado.update {
        it.copy(minutosPresentarse = soloDigitos(v, 2), errores = it.errores.copy(minutosPresentarse = null, general = null))
    }

    fun onTicketChange(v: String) = _estado.update {
        it.copy(ticketPromedio = soloDigitos(v, 7), errores = it.errores.copy(ticketPromedio = null, general = null))
    }

    private fun rango(texto: String, min: Int, max: Int): String? {
        val n = texto.toIntOrNull() ?: return "Campo obligatorio"
        return if (n !in min..max) "Debe estar entre $min y $max" else null
    }

    fun validarFormulario(): Boolean {
        val e = _estado.value
        val prolongado = e.umbralProlongado.toIntOrNull()
        val ayuda = e.umbralAyuda.toIntOrNull()
        val errores = ParametrosErrores(
            maxPrendas = rango(e.maxPrendas, 1, 15),
            tiempoReferenciaUso = rango(e.tiempoReferenciaUso, 1, 60),
            umbralProlongado = rango(e.umbralProlongado, 5, 60),
            umbralAyuda = rango(e.umbralAyuda, 5, 90)
                ?: if (prolongado != null && ayuda != null && ayuda <= prolongado)
                    "Debe ser mayor que el umbral de uso prolongado" else null,
            minutosPresentarse = rango(e.minutosPresentarse, 1, 10),
            ticketPromedio = rango(e.ticketPromedio, 1000, 1_000_000)
        )
        _estado.update { it.copy(errores = errores) }
        return listOfNotNull(
            errores.maxPrendas, errores.tiempoReferenciaUso, errores.umbralProlongado,
            errores.umbralAyuda, errores.minutosPresentarse, errores.ticketPromedio
        ).isEmpty()
    }

    fun guardar(onExito: () -> Unit) {
        if (!validarFormulario()) return
        viewModelScope.launch {
            try {
                val rut = dataStore.obtenerSesion().first() ?: throw ReglaNegocioException("Su sesión terminó")
                val e = _estado.value
                dataStore.actualizar { s ->
                    val nuevos = s.parametros.copy(
                        maxPrendas = e.maxPrendas.toInt(),
                        tiempoReferenciaUso = e.tiempoReferenciaUso.toInt(),
                        umbralProlongado = e.umbralProlongado.toInt(),
                        umbralAyuda = e.umbralAyuda.toInt(),
                        minutosPresentarse = e.minutosPresentarse.toInt(),
                        ticketPromedio = e.ticketPromedio.toInt()
                    )
                    ReglasSeccion.guardarParametros(s, nuevos, rut) to Unit
                }
                onExito()
            } catch (ex: ReglaNegocioException) {
                _estado.update { it.copy(errores = it.errores.copy(general = ex.message)) }
            }
        }
    }
}
