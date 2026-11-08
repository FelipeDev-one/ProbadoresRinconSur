package cl.duoc.rinconsur.probadores.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

// =====================================================================
//  Contrasenas — hash y validación de contraseñas de jefatura
//  ---------------------------------------------------------------------
//  La contraseña NUNCA se guarda tal cual: se guarda un hash PBKDF2 con
//  una "sal" aleatoria por usuario. Para verificar se vuelve a calcular el
//  hash con la misma sal y se comparan. Lo mismo para el código de recuperación.
// =====================================================================

object Contrasenas {
    const val LARGO_MINIMO = 6
    private const val ITERACIONES = 60_000
    private const val LARGO_HASH_BITS = 256
    private const val LARGO_SAL_BYTES = 16

    // Sin 0/O ni 1/I/L para que el código sea fácil de anotar y leer.
    private const val ALFABETO_CODIGO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    private val aleatorio = SecureRandom()

    data class Hash(val hash: String, val sal: String)

    fun generarSal(): String {
        val bytes = ByteArray(LARGO_SAL_BYTES).also { aleatorio.nextBytes(it) }
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun hashear(texto: String, sal: String = generarSal()): Hash {
        val spec = PBEKeySpec(texto.toCharArray(), Base64.getDecoder().decode(sal), ITERACIONES, LARGO_HASH_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            return Hash(Base64.getEncoder().encodeToString(bytes), sal)
        } finally {
            spec.clearPassword()
        }
    }

    fun verificar(texto: String, hash: String, sal: String): Boolean {
        val calculado = Base64.getDecoder().decode(hashear(texto, sal).hash)
        val guardado = Base64.getDecoder().decode(hash)
        return MessageDigest.isEqual(calculado, guardado)
    }

    // Formato XXXX-XXXX. Se muestra una sola vez al crear o cambiar la contraseña.
    fun generarCodigoRecuperacion(): String {
        val letras = (1..8).map { ALFABETO_CODIGO[aleatorio.nextInt(ALFABETO_CODIGO.length)] }
        return letras.take(4).joinToString("") + "-" + letras.drop(4).joinToString("")
    }

    // Acepta el código con o sin guion, en minúsculas o con espacios.
    fun normalizarCodigo(codigo: String): String =
        codigo.uppercase().filter { it.isLetterOrDigit() }

    // Devuelve el mensaje de error, o null si la contraseña sirve.
    fun validarNueva(contrasena: String, confirmacion: String): Pair<String?, String?> {
        val error = when {
            contrasena.isBlank() -> "Campo obligatorio"
            contrasena.length < LARGO_MINIMO -> "Debe tener al menos $LARGO_MINIMO caracteres"
            contrasena.none { it.isLetter() } || contrasena.none { it.isDigit() } ->
                "Debe tener al menos una letra y un número"
            else -> null
        }
        val errorConfirmacion = when {
            error != null -> null
            confirmacion != contrasena -> "Las contraseñas no coinciden"
            else -> null
        }
        return error to errorConfirmacion
    }
}
