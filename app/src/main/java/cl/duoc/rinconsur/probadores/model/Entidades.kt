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
    val nombreCorto: String get() = Nombres.corto(nombre)
    val primerNombre: String get() = Nombres.primero(nombre)
    val iniciales: String get() = Nombres.iniciales(nombre)
}

// Cuenta de cliente: la crea el propio cliente. Solo RUT y nombre (su clave va aparte, como hash).
data class Cliente(
    val rut: String,
    val nombre: String,
    val creado: Long
) {
    val nombreCorto: String get() = Nombres.corto(nombre)
    val primerNombre: String get() = Nombres.primero(nombre)
    val iniciales: String get() = Nombres.iniciales(nombre)
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
    // Colaborador que registró el turno en el mesón; null si lo tomó el propio cliente.
    val registradoPor: String?,
    // Cliente dueño del turno, si lo tomó desde su cuenta.
    val clienteRut: String? = null
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
    val clientes: List<Cliente> = emptyList(),
    val turnos: List<Turno> = emptyList(),
    val usos: List<Uso> = emptyList(),
    val cambios: List<CambioEstado> = emptyList(),
    val parametros: Parametros = Parametros()
)
