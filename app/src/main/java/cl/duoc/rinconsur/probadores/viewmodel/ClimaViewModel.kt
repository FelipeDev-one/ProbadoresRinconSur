package cl.duoc.rinconsur.probadores.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.data.externa.Clima
import cl.duoc.rinconsur.probadores.data.externa.ClimaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ClimaUiState {
    data object Cargando : ClimaUiState
    data class Listo(val clima: Clima) : ClimaUiState
    data class Error(val mensaje: String) : ClimaUiState
}

// Consume la API externa de clima (Open-Meteo) para la tarjeta del cliente y del resumen.
class ClimaViewModel(private val repositorio: ClimaRepository) : ViewModel() {

    // viewModel() de Compose usa este constructor; las pruebas pasan un repositorio falso.
    constructor() : this(ClimaRepository.crear())

    private val _estado = MutableStateFlow<ClimaUiState>(ClimaUiState.Cargando)
    val estado: StateFlow<ClimaUiState> = _estado

    init {
        actualizar()
    }

    fun actualizar() {
        _estado.value = ClimaUiState.Cargando
        viewModelScope.launch {
            _estado.value = repositorio.climaTienda().fold(
                onSuccess = { ClimaUiState.Listo(it) },
                onFailure = { ClimaUiState.Error(it.message ?: "No se pudo obtener el clima") }
            )
        }
    }
}
