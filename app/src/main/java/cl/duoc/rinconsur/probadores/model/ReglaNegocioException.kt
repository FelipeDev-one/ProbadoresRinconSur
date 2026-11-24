package cl.duoc.rinconsur.probadores.model

// Error que se muestra tal cual al usuario (una regla de negocio no se cumple).
open class ReglaNegocioException(mensaje: String) : Exception(mensaje)

// El servidor no respondió (sin internet, dirección equivocada o microservicios apagados).
class SinConexionException : ReglaNegocioException(
    "No hay conexión con el servidor. Revise su internet o la dirección del servidor."
)

// El servidor respondió con un error (400, 401, 404, 409…). El mensaje viene del microservicio.
class ErrorServidorException(val codigo: Int, mensaje: String) : ReglaNegocioException(mensaje)

// La cuenta existe pero todavía no tiene contraseña (colaborador nuevo o restablecido).
class SinContrasenaException : ReglaNegocioException("La cuenta aún no tiene contraseña: créela ahora.")
