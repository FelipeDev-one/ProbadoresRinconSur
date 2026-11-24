package cl.duoc.rinconsur.probadores.data.remote

import cl.duoc.rinconsur.probadores.model.ErrorServidorException
import cl.duoc.rinconsur.probadores.model.SinConexionException
import retrofit2.HttpException
import java.io.IOException

// Ejecuta una llamada al servidor y traduce los errores de red a mensajes para el usuario:
//  - sin respuesta (IOException)        → SinConexionException
//  - respuesta con error (HttpException) → ErrorServidorException con el mensaje del microservicio
suspend fun <T> llamar(bloque: suspend () -> T): T =
    try {
        bloque()
    } catch (e: HttpException) {
        throw ErrorServidorException(e.code(), mensajeDeError(e.code(), e.response()?.errorBody()?.string()))
    } catch (e: IOException) {
        // Queda en Logcat (System.err) para diagnosticar la dirección o la red.
        System.err.println("Servidor sin respuesta: $e")
        throw SinConexionException()
    }

// {"mensaje":"Datos no válidos","campos":{"rut":"…"}} → "Datos no válidos: rut …"
fun mensajeDeError(codigo: Int, cuerpo: String?): String {
    val error = try {
        RetrofitClient.gson.fromJson(cuerpo, ErrorDto::class.java)
    } catch (e: Exception) {
        null
    }
    val mensaje = error?.mensaje?.takeIf { it.isNotBlank() } ?: when (codigo) {
        503 -> "El servidor está iniciando o un servicio no responde. Intente en unos segundos."
        else -> "El servidor respondió con un error ($codigo)."
    }
    val campo = error?.campos?.entries?.firstOrNull() ?: return mensaje
    return "$mensaje: ${campo.key} ${campo.value}"
}
