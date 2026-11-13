package cl.duoc.rinconsur.probadores.model

// Formas cortas de un nombre completo, compartidas por colaboradores y clientes.
object Nombres {
    private fun partes(nombre: String) = nombre.trim().split(" ").filter { it.isNotBlank() }

    // "Patricia Elena Morales Castillo" -> "P. Morales"
    fun corto(nombre: String): String {
        val p = partes(nombre)
        return when {
            p.size >= 3 -> "${p[0].first()}. ${p[p.size - 2]}"
            p.size == 2 -> "${p[0].first()}. ${p[1]}"
            else -> nombre
        }
    }

    fun primero(nombre: String): String = partes(nombre).firstOrNull() ?: nombre

    // "Patricia Elena Morales Castillo" -> "PM" (nombre + apellido paterno).
    fun iniciales(nombre: String): String {
        val p = partes(nombre)
        val apellido = when {
            p.size >= 3 -> p[p.size - 2]
            p.size == 2 -> p[1]
            else -> null
        }
        return listOfNotNull(p.firstOrNull(), apellido).joinToString("") { it.first().uppercase() }
    }
}
