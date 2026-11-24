package cl.duoc.rinconsur.probadores.data.remote

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

enum class EstadoConexion(val etiqueta: String) {
    SIN_SERVIDOR("Sin servidor: los datos quedan solo en este equipo"),
    CONECTANDO("Conectando…"),
    CONECTADO("Conectado"),
    SIN_CONEXION("Sin conexión: se muestran los últimos datos descargados")
}

// Entrega las APIs de Retrofit para la dirección guardada en Ajustes (null = sin servidor).
// Solo vuelve a crear Retrofit cuando la dirección cambia.
class ConexionServidor(val url: Flow<String>) {
    private var actual: Pair<String, ApiServidor>? = null

    suspend fun api(): ApiServidor? {
        val direccion = url.first()
        if (direccion.isBlank()) return null
        actual?.let { (u, api) -> if (u == direccion) return api }
        return RetrofitClient.crear(direccion).also { actual = direccion to it }
    }
}
