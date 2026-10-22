package cl.duoc.rinconsur.probadores.model

enum class EstadoProbador(val etiqueta: String) {
    LIBRE("Libre"),
    OCUPADO("Ocupado"),
    EN_ORDENAMIENTO("En ordenamiento"),
    FUERA_DE_SERVICIO("Fuera de servicio");

    fun puedePasarA(nuevo: EstadoProbador): Boolean = when (this) {
        LIBRE -> nuevo == OCUPADO || nuevo == FUERA_DE_SERVICIO
        OCUPADO -> nuevo == EN_ORDENAMIENTO || nuevo == FUERA_DE_SERVICIO
        EN_ORDENAMIENTO -> nuevo == LIBRE || nuevo == FUERA_DE_SERVICIO
        FUERA_DE_SERVICIO -> nuevo == LIBRE
    }
}

enum class TipoProbador(val etiqueta: String) {
    ESTANDAR("Estándar"),
    ACCESIBLE("Accesible")
}

enum class EstadoTurno(val etiqueta: String) {
    EN_ESPERA("En espera"),
    LLAMADO("Llamado"),
    ATENDIDO("Atendido"),
    ABANDONO("Abandonó"),
    NO_SE_PRESENTO("No se presentó");

    val abierto: Boolean get() = this == EN_ESPERA || this == LLAMADO
}

enum class Perfil(val etiqueta: String) {
    ASISTENTE("Asistente de probadores"),
    VENDEDOR("Vendedor(a)"),
    JEFATURA("Jefatura");

    val esJefatura: Boolean get() = this == JEFATURA
}

enum class TurnoLaboral(val etiqueta: String) {
    MANANA("Mañana"),
    TARDE("Tarde"),
    FIN_DE_SEMANA("Fin de semana")
}

enum class MotivoAbandono(
    val etiqueta: String,
    val cuentaComoVentaPerdida: Boolean,
    val seleccionable: Boolean = true
) {
    ESPERA_LARGA("Espera muy larga", true),
    SE_HIZO_TARDE("Se le hizo tarde", true),
    COMPRA_SIN_PROBARSE("Decidió comprar sin probarse", false),
    OTRO("Otro", true),
    NO_INFORMADO("No informado", true),
    CIERRE_TIENDA("Cierre de tienda", false, seleccionable = false)
}
