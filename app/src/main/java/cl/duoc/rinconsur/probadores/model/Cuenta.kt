package cl.duoc.rinconsur.probadores.model

// Una cuenta con la que se puede ingresar: de colaborador (con su perfil) o de cliente (perfil == null).
// Sirve para tratar igual el ingreso, el perfil propio y el cambio de clave de ambos tipos.
data class Cuenta(
    val rut: String,
    val nombre: String,
    val perfil: Perfil?,
    val activa: Boolean = true
) {
    val esCliente: Boolean get() = perfil == null
    val rol: String get() = perfil?.etiqueta ?: "Cliente"
    val nombreCorto: String get() = Nombres.corto(nombre)
    val primerNombre: String get() = Nombres.primero(nombre)
    val iniciales: String get() = Nombres.iniciales(nombre)
}

// Busca el RUT primero entre los colaboradores y después entre los clientes
// (las reglas impiden que un mismo RUT esté en ambos).
fun Seccion.cuenta(rut: String?): Cuenta? {
    if (rut == null) return null
    colaboradores.find { it.rut == rut }?.let { return Cuenta(it.rut, it.nombre, it.perfil, it.activo) }
    return clientes.find { it.rut == rut }?.let { Cuenta(it.rut, it.nombre, null) }
}
