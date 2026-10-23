package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.data.SeccionDataStore
import cl.duoc.rinconsur.probadores.model.FueraServicioUiState
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.utils.ImagenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// =====================================================================
//  FueraServicioViewModel — marcar un probador fuera de servicio (HU-08)
//  ---------------------------------------------------------------------
//  Mientras la app de cámara está abierta, Android puede cerrar nuestra app
//  para liberar memoria. Por eso el motivo, la foto y el archivo pendiente
//  se guardan también en SavedStateHandle: al volver, nada se pierde.
// =====================================================================

class FueraServicioViewModel(
    application: Application,
    private val guardado: SavedStateHandle
) : AndroidViewModel(application) {
    private val dataStore = SeccionDataStore(application)

    private val _estado = MutableStateFlow(
        FueraServicioUiState(
            codigo = guardado.get<String>(CLAVE_CODIGO) ?: "",
            motivo = guardado.get<String>(CLAVE_MOTIVO) ?: "",
            fotoRuta = guardado.get<String>(CLAVE_FOTO)
        )
    )
    val estado: StateFlow<FueraServicioUiState> = _estado

    val motivosRapidos = listOf("Cortina rota", "Sin iluminación", "Espejo dañado", "Limpieza profunda")

    fun iniciar(codigo: String) {
        if (_estado.value.codigo == codigo) return
        _estado.value = FueraServicioUiState(codigo = codigo)
        guardado[CLAVE_CODIGO] = codigo
        recordar()
    }

    private fun recordar() {
        val e = _estado.value
        guardado[CLAVE_MOTIVO] = e.motivo
        guardado[CLAVE_FOTO] = e.fotoRuta
    }

    fun onMotivoChange(valor: String) {
        _estado.update { it.copy(motivo = valor.take(120), errores = it.errores.copy(motivo = null, general = null)) }
        recordar()
    }

    // ------------------------------- CÁMARA -------------------------------

    // Crea el archivo donde la app de cámara dejará la foto y devuelve su Uri.
    fun prepararFoto(): Uri {
        val (archivo, uri) = ImagenUtils.crearArchivoFoto(getApplication())
        guardado[CLAVE_PENDIENTE] = archivo.absolutePath
        return uri
    }

    // La cámara respondió: exito = true si el usuario sacó y aceptó la foto.
    fun onFotoTomada(exito: Boolean) {
        val ruta = guardado.get<String>(CLAVE_PENDIENTE)
        guardado.remove<String>(CLAVE_PENDIENTE)
        val archivo = ruta?.let { File(it) } ?: return
        if (exito && archivo.length() > 0) reemplazarFoto(archivo.absolutePath) else archivo.delete()
    }

    // La cámara no se pudo abrir (sin app de cámara): se borra el archivo vacío.
    fun onCamaraNoDisponible(mensaje: String) {
        ImagenUtils.borrar(guardado.get<String>(CLAVE_PENDIENTE))
        guardado.remove<String>(CLAVE_PENDIENTE)
        mostrarErrorFoto(mensaje)
    }

    // ------------------------------- GALERÍA ------------------------------

    fun onImagenGaleria(uri: Uri?) {
        if (uri == null) return   // el usuario cerró el selector sin elegir
        viewModelScope.launch {
            val copia = withContext(Dispatchers.IO) { ImagenUtils.copiarDesdeGaleria(getApplication(), uri) }
            if (copia == null) mostrarErrorFoto("No se pudo leer la imagen seleccionada")
            else reemplazarFoto(copia.absolutePath)
        }
    }

    fun mostrarErrorFoto(mensaje: String) {
        _estado.update { it.copy(errores = it.errores.copy(foto = mensaje)) }
    }

    // Una foto nueva reemplaza a la anterior: la anterior se borra para no llenar el teléfono.
    private fun reemplazarFoto(nueva: String) {
        val anterior = _estado.value.fotoRuta
        _estado.update { it.copy(fotoRuta = nueva, errores = it.errores.copy(foto = null)) }
        recordar()
        if (anterior != nueva) ImagenUtils.borrar(anterior)
    }

    fun quitarFoto() {
        val anterior = _estado.value.fotoRuta
        _estado.update { it.copy(fotoRuta = null, errores = it.errores.copy(foto = null)) }
        recordar()
        ImagenUtils.borrar(anterior)
    }

    // ----------------------------- VALIDACIÓN -----------------------------

    fun validarFormulario(): Boolean {
        val motivo = _estado.value.motivo.trim()
        val error = when {
            motivo.isBlank() -> "Campo obligatorio"
            motivo.length < 5 -> "Describa el motivo (mínimo 5 caracteres)"
            else -> null
        }
        _estado.update { it.copy(errores = it.errores.copy(motivo = error)) }
        return error == null
    }

    fun guardar(onExito: () -> Unit) {
        if (_estado.value.guardando || !validarFormulario()) return
        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errores = it.errores.copy(general = null)) }
            try {
                val rut = dataStore.obtenerSesion().first() ?: throw ReglaNegocioException("Su sesión terminó")
                val e = _estado.value
                dataStore.actualizar { s ->
                    ReglasSeccion.marcarFueraDeServicio(
                        s, e.codigo, e.motivo, e.fotoRuta, rut, System.currentTimeMillis()
                    ) to Unit
                }
                // La foto ya quedó guardada en el probador: el formulario se limpia SIN borrarla.
                _estado.value = FueraServicioUiState()
                guardado.remove<String>(CLAVE_CODIGO)
                guardado.remove<String>(CLAVE_MOTIVO)
                guardado.remove<String>(CLAVE_FOTO)
                onExito()
            } catch (ex: ReglaNegocioException) {
                _estado.update { it.copy(guardando = false, errores = it.errores.copy(general = ex.message)) }
            }
        }
    }

    // Se llama al salir de la pantalla sin guardar: la foto tomada no se usará.
    override fun onCleared() {
        ImagenUtils.borrar(_estado.value.fotoRuta)
        ImagenUtils.borrar(guardado.get<String>(CLAVE_PENDIENTE))
        super.onCleared()
    }

    private companion object {
        const val CLAVE_CODIGO = "fuera_servicio_codigo"
        const val CLAVE_MOTIVO = "fuera_servicio_motivo"
        const val CLAVE_FOTO = "fuera_servicio_foto"
        const val CLAVE_PENDIENTE = "fuera_servicio_pendiente"
    }
}
