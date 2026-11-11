package cl.duoc.rinconsur.probadores.data.local

import androidx.room.Embedded
import androidx.room.Relation

// =====================================================================
//  Relaciones — Room arma estos objetos juntando dos tablas
//  ---------------------------------------------------------------------
//  @Embedded es la fila "padre"; @Relation trae las filas "hijas" cuya
//  columna entityColumn coincide con parentColumn del padre.
// =====================================================================

// Un probador con todos sus usos (1 a muchos).
data class ProbadorConUsos(
    @Embedded val probador: ProbadorEntity,
    @Relation(parentColumn = "codigo", entityColumn = "codigo_probador")
    val usos: List<UsoEntity>
)

// Un turno con el uso que lo atendió, si llegó a entrar a un probador (1 a 0..1).
data class TurnoConUso(
    @Embedded val turno: TurnoEntity,
    @Relation(parentColumn = "id", entityColumn = "turno_id")
    val uso: UsoEntity?
)

// Un colaborador con lo que registró: turnos en la fila y usos iniciados (1 a muchos).
data class ColaboradorConRegistros(
    @Embedded val colaborador: ColaboradorEntity,
    @Relation(parentColumn = "rut", entityColumn = "registrado_por")
    val turnos: List<TurnoEntity>,
    @Relation(parentColumn = "rut", entityColumn = "registro_inicio")
    val usos: List<UsoEntity>
)
