package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.data.SeccionDataStore
import cl.duoc.rinconsur.probadores.model.LoginErrores
import cl.duoc.rinconsur.probadores.model.LoginUiState
import cl.duoc.rinconsur.probadores.utils.RutValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Ingreso de colaboradores con RUT (RF-10, RN-13).
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = SeccionDataStore(application)

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado

    fun onRutChange(valor: String) {
        val filtrado = valor.filter { it.isDigit() || it == '.' || it == '-' || it == 'k' || it == 'K' }.take(12)
        _estado.update { it.copy(rut = filtrado, errores = it.errores.copy(rut = null)) }
    }

    fun validarFormulario(): Boolean {
        val rut = _estado.value.rut
        val error = when {
            rut.isBlank() -> "Campo obligatorio"
            !RutValidator.esValido(rut) -> "RUT inválido: revise el dígito verificador"
            else -> null
        }
        _estado.update { it.copy(errores = LoginErrores(rut = error)) }
        return error == null
    }

    fun ingresar(onExito: () -> Unit) {
        if (_estado.value.cargando || !validarFormulario()) return
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true) }
            val rut = RutValidator.normalizar(_estado.value.rut)
            val colaborador = dataStore.obtenerSeccion().first()?.colaboradores?.find { it.rut == rut }
            val error = when {
                colaborador == null -> "Este RUT no está registrado como colaborador"
                !colaborador.activo -> "Colaborador inactivo: consulte con la jefatura (RN-13)"
                else -> null
            }
            if (error == null) {
                dataStore.guardarSesion(rut)
                _estado.value = LoginUiState()
                onExito()
            } else {
                _estado.update { it.copy(cargando = false, errores = LoginErrores(rut = error)) }
            }
        }
    }
}
