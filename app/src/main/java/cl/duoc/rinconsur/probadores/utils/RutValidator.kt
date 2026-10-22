package cl.duoc.rinconsur.probadores.utils

object RutValidator {
    fun normalizar(rut: String): String {
        val limpio = rut.replace(".", "").replace("-", "").replace(" ", "").uppercase()
        if (limpio.length < 2) return limpio
        return limpio.dropLast(1) + "-" + limpio.last()
    }

    fun calcularDv(cuerpo: Int): Char {
        var suma = 0
        var multiplicador = 2
        var numero = cuerpo
        while (numero > 0) {
            suma += (numero % 10) * multiplicador
            numero /= 10
            multiplicador = if (multiplicador == 7) 2 else multiplicador + 1
        }
        return when (val resto = 11 - (suma % 11)) {
            11 -> '0'
            10 -> 'K'
            else -> ('0' + resto)
        }
    }

    fun esValido(rut: String): Boolean {
        val normalizado = normalizar(rut)
        val partes = normalizado.split("-")
        if (partes.size != 2 || partes[1].length != 1) return false
        val cuerpo = partes[0].toIntOrNull() ?: return false
        if (cuerpo < 1_000_000) return false
        return calcularDv(cuerpo) == partes[1][0]
    }

    fun formatear(rut: String): String {
        val normalizado = normalizar(rut)
        val partes = normalizado.split("-")
        if (partes.size != 2) return rut
        val cuerpo = partes[0].reversed().chunked(3).joinToString(".").reversed()
        return "$cuerpo-${partes[1]}"
    }
}
