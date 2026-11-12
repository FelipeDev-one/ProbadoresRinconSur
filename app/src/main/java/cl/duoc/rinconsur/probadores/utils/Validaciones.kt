package cl.duoc.rinconsur.probadores.utils

// Validaciones de formulario compartidas. Cada función devuelve el mensaje
// de error para mostrar bajo el campo, o null si el valor sirve.
object Validaciones {
    fun rut(valor: String): String? = when {
        valor.isBlank() -> "Campo obligatorio"
        !RutValidator.esValido(valor) -> "RUT inválido: revise el dígito verificador"
        else -> null
    }

    fun nombrePersona(valor: String): String? {
        val nombre = valor.trim()
        val soloLetras = nombre.all { it.isLetter() || it == ' ' || it == '\'' || it == '-' }
        return when {
            nombre.isBlank() -> "Campo obligatorio"
            !soloLetras -> "Use solo letras"
            nombre.split(" ").count { it.isNotBlank() } < 2 -> "Ingrese al menos nombre y apellido"
            else -> null
        }
    }

    // Quita espacios repetidos: "  Ana   Pérez " → "Ana Pérez".
    fun limpiarNombre(valor: String): String =
        valor.trim().split(" ").filter { it.isNotBlank() }.joinToString(" ")
}
