package cl.duoc.rinconsur.probadores.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cl.duoc.rinconsur.probadores.model.CambioEstado
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Credencial
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.model.Uso

// =====================================================================
//  Entidades de Room — una clase por tabla
//  ---------------------------------------------------------------------
//   colaboradores ──┬──< turnos        (quién registró el turno)
//        │          ├──< usos          (quién registró el inicio del uso)
//        │          ├──< cambios_estado
//        │          └──1 credenciales  (se borra junto con el colaborador)
//   probadores ─────┬──< usos          (en qué probador fue el uso)
//                   └──< cambios_estado
//   turnos ─────────────0..1 usos      (el uso que atendió ese turno)
//
//  Cada entidad sabe convertirse a su clase del modelo (aModelo) y de vuelta
//  (aEntidad). Las reglas de negocio solo conocen el modelo, nunca la base de datos.
//  Los enums se guardan por su nombre (Room lo hace solo).
// =====================================================================

@Entity(tableName = "colaboradores")
data class ColaboradorEntity(
    @PrimaryKey val rut: String,
    val nombre: String,
    val perfil: Perfil,
    val turno: TurnoLaboral,
    val activo: Boolean
) {
    fun aModelo() = Colaborador(rut, nombre, perfil, turno, activo)
}

fun Colaborador.aEntidad() = ColaboradorEntity(rut, nombre, perfil, turno, activo)

@Entity(tableName = "probadores")
data class ProbadorEntity(
    @PrimaryKey val codigo: String,
    val tipo: TipoProbador,
    val estado: EstadoProbador,
    val desde: Long,
    // Sin clave foránea: probador y uso se apuntan entre sí, y el uso se inserta después.
    @ColumnInfo(name = "uso_activo_id") val usoActivoId: String?,
    @ColumnInfo(name = "motivo_fuera_servicio") val motivoFueraServicio: String?,
    @ColumnInfo(name = "foto_evidencia") val fotoEvidencia: String?
) {
    fun aModelo() = Probador(codigo, tipo, estado, desde, usoActivoId, motivoFueraServicio, fotoEvidencia)
}

fun Probador.aEntidad() =
    ProbadorEntity(codigo, tipo, estado, desde, usoActivoId, motivoFueraServicio, fotoEvidencia)

@Entity(
    tableName = "turnos",
    foreignKeys = [
        ForeignKey(
            entity = ColaboradorEntity::class,
            parentColumns = ["rut"],
            childColumns = ["registrado_por"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("registrado_por"), Index("fecha")]
)
data class TurnoEntity(
    @PrimaryKey val id: String,
    val numero: Int,
    val fecha: String,
    @ColumnInfo(name = "hora_ingreso") val horaIngreso: Long,
    val preferente: Boolean,
    val prendas: Int,
    val estado: EstadoTurno,
    @ColumnInfo(name = "hora_llamado") val horaLlamado: Long?,
    @ColumnInfo(name = "hora_cierre") val horaCierre: Long?,
    @ColumnInfo(name = "motivo_abandono") val motivoAbandono: MotivoAbandono?,
    @ColumnInfo(name = "registrado_por") val registradoPor: String
) {
    fun aModelo() = Turno(
        id, numero, fecha, horaIngreso, preferente, prendas, estado,
        horaLlamado, horaCierre, motivoAbandono, registradoPor
    )
}

fun Turno.aEntidad() = TurnoEntity(
    id, numero, fecha, horaIngreso, preferente, prendas, estado,
    horaLlamado, horaCierre, motivoAbandono, registradoPor
)

@Entity(
    tableName = "usos",
    foreignKeys = [
        ForeignKey(
            entity = ProbadorEntity::class,
            parentColumns = ["codigo"],
            childColumns = ["codigo_probador"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TurnoEntity::class,
            parentColumns = ["id"],
            childColumns = ["turno_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ColaboradorEntity::class,
            parentColumns = ["rut"],
            childColumns = ["registro_inicio"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("codigo_probador"), Index("turno_id"), Index("registro_inicio")]
)
data class UsoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "codigo_probador") val codigoProbador: String,
    @ColumnInfo(name = "turno_id") val turnoId: String?,
    val inicio: Long,
    val termino: Long?,
    @ColumnInfo(name = "prendas_ingresadas") val prendasIngresadas: Int,
    @ColumnInfo(name = "prendas_devueltas") val prendasDevueltas: Int?,
    @ColumnInfo(name = "registro_inicio") val registroInicio: String,
    @ColumnInfo(name = "registro_termino") val registroTermino: String?,
    @ColumnInfo(name = "con_atraso") val conAtraso: Boolean
) {
    fun aModelo() = Uso(
        id, codigoProbador, turnoId, inicio, termino, prendasIngresadas,
        prendasDevueltas, registroInicio, registroTermino, conAtraso
    )
}

fun Uso.aEntidad() = UsoEntity(
    id, codigoProbador, turnoId, inicio, termino, prendasIngresadas,
    prendasDevueltas, registroInicio, registroTermino, conAtraso
)

// Historial: quién cambió el estado de qué probador y cuándo.
// Un probador no cambia dos veces al mismo estado en el mismo milisegundo: eso identifica la fila.
@Entity(
    tableName = "cambios_estado",
    primaryKeys = ["codigo_probador", "fecha_hora", "nuevo"],
    foreignKeys = [
        ForeignKey(
            entity = ProbadorEntity::class,
            parentColumns = ["codigo"],
            childColumns = ["codigo_probador"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ColaboradorEntity::class,
            parentColumns = ["rut"],
            childColumns = ["colaborador"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("colaborador")]
)
data class CambioEstadoEntity(
    @ColumnInfo(name = "codigo_probador") val codigoProbador: String,
    val anterior: EstadoProbador,
    val nuevo: EstadoProbador,
    @ColumnInfo(name = "fecha_hora") val fechaHora: Long,
    val colaborador: String,
    val motivo: String?
) {
    fun aModelo() = CambioEstado(codigoProbador, anterior, nuevo, fechaHora, colaborador, motivo)
}

fun CambioEstado.aEntidad() =
    CambioEstadoEntity(codigoProbador, anterior, nuevo, fechaHora, colaborador, motivo)

// Una sola fila (id = 1) con los parámetros de la sección.
@Entity(tableName = "parametros")
data class ParametrosEntity(
    @PrimaryKey val id: Int = ID_UNICO,
    @ColumnInfo(name = "max_prendas") val maxPrendas: Int,
    @ColumnInfo(name = "tiempo_referencia_uso") val tiempoReferenciaUso: Int,
    @ColumnInfo(name = "minutos_ordenamiento") val minutosOrdenamiento: Int,
    @ColumnInfo(name = "umbral_prolongado") val umbralProlongado: Int,
    @ColumnInfo(name = "umbral_ayuda") val umbralAyuda: Int,
    @ColumnInfo(name = "minutos_presentarse") val minutosPresentarse: Int,
    @ColumnInfo(name = "ticket_promedio") val ticketPromedio: Int,
    @ColumnInfo(name = "hora_apertura") val horaApertura: Int,
    @ColumnInfo(name = "hora_cierre") val horaCierre: Int,
    @ColumnInfo(name = "minutos_cierre_fila") val minutosCierreFila: Int
) {
    fun aModelo() = Parametros(
        maxPrendas, tiempoReferenciaUso, minutosOrdenamiento, umbralProlongado, umbralAyuda,
        minutosPresentarse, ticketPromedio, horaApertura, horaCierre, minutosCierreFila
    )

    companion object {
        const val ID_UNICO = 1
    }
}

fun Parametros.aEntidad() = ParametrosEntity(
    ParametrosEntity.ID_UNICO, maxPrendas, tiempoReferenciaUso, minutosOrdenamiento, umbralProlongado,
    umbralAyuda, minutosPresentarse, ticketPromedio, horaApertura, horaCierre, minutosCierreFila
)

// Clave de ingreso (solo hashes). Si se elimina el colaborador, se elimina su credencial.
@Entity(
    tableName = "credenciales",
    foreignKeys = [
        ForeignKey(
            entity = ColaboradorEntity::class,
            parentColumns = ["rut"],
            childColumns = ["rut"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CredencialEntity(
    @PrimaryKey val rut: String,
    val hash: String,
    val sal: String,
    @ColumnInfo(name = "hash_recuperacion") val hashRecuperacion: String,
    @ColumnInfo(name = "sal_recuperacion") val salRecuperacion: String,
    @ColumnInfo(name = "intentos_fallidos") val intentosFallidos: Int,
    @ColumnInfo(name = "bloqueada_hasta") val bloqueadaHasta: Long
) {
    fun aModelo() =
        Credencial(rut, hash, sal, hashRecuperacion, salRecuperacion, intentosFallidos, bloqueadaHasta)
}

fun Credencial.aEntidad() =
    CredencialEntity(rut, hash, sal, hashRecuperacion, salRecuperacion, intentosFallidos, bloqueadaHasta)
