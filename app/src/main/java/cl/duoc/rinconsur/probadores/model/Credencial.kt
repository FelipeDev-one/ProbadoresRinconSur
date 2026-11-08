package cl.duoc.rinconsur.probadores.model

import cl.duoc.rinconsur.probadores.utils.Contrasenas

// Contraseña de una jefatura (solo hashes, nunca el texto) y su control de intentos.
data class Credencial(
    val rut: String,
    val hash: String,
    val sal: String,
    val hashRecuperacion: String,
    val salRecuperacion: String,
    val intentosFallidos: Int = 0,
    val bloqueadaHasta: Long = 0
)

object ReglasAcceso {
    const val MAX_INTENTOS = 5
    const val MILIS_BLOQUEO = 60_000L

    // Crea la credencial y devuelve también el código de recuperación en texto (para mostrarlo una vez).
    fun crear(rut: String, contrasena: String): Pair<Credencial, String> {
        val codigo = Contrasenas.generarCodigoRecuperacion()
        val hash = Contrasenas.hashear(contrasena)
        val hashCodigo = Contrasenas.hashear(Contrasenas.normalizarCodigo(codigo))
        return Credencial(rut, hash.hash, hash.sal, hashCodigo.hash, hashCodigo.sal) to codigo
    }

    fun segundosBloqueo(c: Credencial, ahora: Long): Int =
        if (c.bloqueadaHasta > ahora) ((c.bloqueadaHasta - ahora + 999) / 1000).toInt() else 0

    fun contrasenaCorrecta(c: Credencial, contrasena: String): Boolean =
        Contrasenas.verificar(contrasena, c.hash, c.sal)

    fun codigoCorrecto(c: Credencial, codigo: String): Boolean =
        Contrasenas.verificar(Contrasenas.normalizarCodigo(codigo), c.hashRecuperacion, c.salRecuperacion)

    // Suma un intento fallido; al llegar al máximo bloquea un minuto y reinicia el contador.
    fun registrarFallo(c: Credencial, ahora: Long): Credencial {
        val intentos = c.intentosFallidos + 1
        return if (intentos >= MAX_INTENTOS) c.copy(intentosFallidos = 0, bloqueadaHasta = ahora + MILIS_BLOQUEO)
        else c.copy(intentosFallidos = intentos)
    }

    fun intentosRestantes(c: Credencial): Int = MAX_INTENTOS - c.intentosFallidos

    fun registrarExito(c: Credencial): Credencial = c.copy(intentosFallidos = 0, bloqueadaHasta = 0)
}
