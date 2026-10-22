package cl.duoc.rinconsur.probadores.model

data class Probador(
    val codigo: String,
    val tipo: TipoProbador,
    val estado: EstadoProbador,
    val desde: Long,
    val usoActivoId: String? = null,
    val motivoFueraServicio: String? = null,
    val fotoEvidencia: String? = null
) {
    val habilitado: Boolean get() = estado != EstadoProbador.FUERA_DE_SERVICIO
}

data class Colaborador(
    val rut: String,
    val nombre: String,
    val perfil: Perfil,
    val turno: TurnoLaboral,
    val activo: Boolean = true
) {
    val nombreCorto: String
        get() {
            val partes = nombre.trim().split(" ").filter { it.isNotBlank() }
            return when {
                partes.size >= 3 -> "${partes[0].first()}. ${partes[partes.size - 2]}"
                partes.size == 2 -> "${partes[0].first()}. ${partes[1]}"
                else -> nombre
            }
        }
}

data class Turno(
    val id: String,
    val numero: Int,
    val fecha: String,
    val horaIngreso: Long,
    val preferente: Boolean,
    val prendas: Int,
    val estado: EstadoTurno = EstadoTurno.EN_ESPERA,
    val horaLlamado: Long? = null,
    val horaCierre: Long? = null,
    val motivoAbandono: MotivoAbandono? = null,
    val registradoPor: String
) {
    val codigo: String get() = "T-%03d".format(numero)
}

data class Uso(
    val id: String,
    val codigoProbador: String,
    val turnoId: String?,
    val inicio: Long,
    val termino: Long? = null,
    val prendasIngresadas: Int,
    val prendasDevueltas: Int? = null,
    val registroInicio: String,
    val registroTermino: String? = null,
    val conAtraso: Boolean = false
) {
    val cerrado: Boolean get() = termino != null

    val huboConversion: Boolean
        get() = prendasDevueltas != null && prendasDevueltas < prendasIngresadas
}

data class CambioEstado(
    val codigoProbador: String,
    val anterior: EstadoProbador,
    val nuevo: EstadoProbador,
    val fechaHora: Long,
    val colaborador: String,
    val motivo: String? = null
)

data class Parametros(
    val maxPrendas: Int = 6,
    val tiempoReferenciaUso: Int = 8,
    val minutosOrdenamiento: Int = 1,
    val umbralProlongado: Int = 15,
    val umbralAyuda: Int = 25,
    val minutosPresentarse: Int = 2,
    val ticketPromedio: Int = 27990,
    val horaApertura: Int = 10,
    val horaCierre: Int = 21,
    val minutosCierreFila: Int = 15
)

data class Seccion(
    val probadores: List<Probador> = emptyList(),
    val colaboradores: List<Colaborador> = emptyList(),
    val turnos: List<Turno> = emptyList(),
    val usos: List<Uso> = emptyList(),
    val cambios: List<CambioEstado> = emptyList(),
    val parametros: Parametros = Parametros()
)
