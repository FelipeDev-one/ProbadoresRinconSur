package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.ProbadoresApp
import cl.duoc.rinconsur.probadores.data.remote.EstadoConexion
import cl.duoc.rinconsur.probadores.data.remote.RetrofitClient
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServidorUiState(
    val editando: Boolean = false,
    val url: String = "",
    val error: String? = null,
    val probando: Boolean = false
)

// Dirección del API Gateway (se guarda en DataStore) y estado de la conexión.
class ServidorViewModel(application: Application) : AndroidViewModel(application) {
    private val repositorio = (application as ProbadoresApp).repositorio
    private val preferencias = (application as ProbadoresApp).preferencias

    val urlActual: StateFlow<String> = repositorio.urlServidor
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    val estadoConexion: StateFlow<EstadoConexion> = repositorio.estadoConexion

    private val _estado = MutableStateFlow(ServidorUiState())
    val estado: StateFlow<ServidorUiState> = _estado

    fun abrir() {
        viewModelScope.launch {
            _estado.value = ServidorUiState(editando = true, url = repositorio.urlServidor.first())
        }
    }

    fun cerrar() = _estado.update { ServidorUiState() }

    fun onUrlChange(valor: String) = _estado.update { it.copy(url = valor.trim().take(200), error = null) }

    fun usarEmulador() = onUrlChange(RetrofitClient.URL_EMULADOR)

    fun sinServidor() = onUrlChange("")

    // Guarda la dirección y prueba la conexión descargando los datos del servidor.
    fun guardar() {
        val e = _estado.value
        RetrofitClient.validarUrl(e.url)?.let { error ->
            _estado.update { it.copy(error = error) }
            return
        }
        viewModelScope.launch {
            _estado.update { it.copy(probando = true) }
            preferencias.guardarUrlServidor(e.url)
            try {
                repositorio.sincronizar()
                _estado.value = ServidorUiState()
            } catch (ex: ReglaNegocioException) {
                _estado.update { it.copy(probando = false, error = ex.message) }
            }
        }
    }
}
