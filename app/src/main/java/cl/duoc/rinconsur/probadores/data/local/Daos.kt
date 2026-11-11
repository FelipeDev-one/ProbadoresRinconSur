package cl.duoc.rinconsur.probadores.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

// =====================================================================
//  DAOs — las consultas a cada tabla
//  ---------------------------------------------------------------------
//  @Upsert inserta la fila o, si ya existe esa clave, la actualiza.
//  Las funciones "suspend" corren fuera del hilo principal; las que
//  devuelven Flow vuelven a emitir solas cuando la tabla cambia.
// =====================================================================

@Dao
interface ColaboradorDao {
    @Query("SELECT * FROM colaboradores")
    suspend fun todos(): List<ColaboradorEntity>

    @Query("SELECT COUNT(*) FROM colaboradores")
    suspend fun cantidad(): Int

    @Upsert
    suspend fun guardar(items: List<ColaboradorEntity>)

    @Delete
    suspend fun eliminar(items: List<ColaboradorEntity>)

    @Transaction
    @Query("SELECT * FROM colaboradores WHERE rut = :rut")
    suspend fun conRegistros(rut: String): ColaboradorConRegistros?
}

@Dao
interface ProbadorDao {
    @Query("SELECT * FROM probadores ORDER BY codigo")
    suspend fun todos(): List<ProbadorEntity>

    @Query("SELECT COUNT(*) FROM probadores")
    suspend fun cantidad(): Int

    @Upsert
    suspend fun guardar(items: List<ProbadorEntity>)

    @Delete
    suspend fun eliminar(items: List<ProbadorEntity>)

    @Transaction
    @Query("SELECT * FROM probadores WHERE codigo = :codigo")
    suspend fun conUsos(codigo: String): ProbadorConUsos?
}

@Dao
interface TurnoDao {
    @Query("SELECT * FROM turnos ORDER BY hora_ingreso")
    suspend fun todos(): List<TurnoEntity>

    @Upsert
    suspend fun guardar(items: List<TurnoEntity>)

    @Delete
    suspend fun eliminar(items: List<TurnoEntity>)

    @Transaction
    @Query("SELECT * FROM turnos WHERE fecha = :fecha ORDER BY hora_ingreso")
    suspend fun delDiaConUso(fecha: String): List<TurnoConUso>
}

@Dao
interface UsoDao {
    @Query("SELECT * FROM usos ORDER BY inicio")
    suspend fun todos(): List<UsoEntity>

    @Upsert
    suspend fun guardar(items: List<UsoEntity>)

    @Delete
    suspend fun eliminar(items: List<UsoEntity>)
}

@Dao
interface CambioEstadoDao {
    @Query("SELECT * FROM cambios_estado ORDER BY fecha_hora")
    suspend fun todos(): List<CambioEstadoEntity>

    @Upsert
    suspend fun guardar(items: List<CambioEstadoEntity>)

    @Delete
    suspend fun eliminar(items: List<CambioEstadoEntity>)
}

@Dao
interface ParametrosDao {
    @Query("SELECT * FROM parametros WHERE id = ${ParametrosEntity.ID_UNICO}")
    suspend fun obtener(): ParametrosEntity?

    @Upsert
    suspend fun guardar(parametros: ParametrosEntity)
}

@Dao
interface CredencialDao {
    @Query("SELECT * FROM credenciales WHERE rut = :rut")
    suspend fun obtener(rut: String): CredencialEntity?

    @Query("SELECT rut FROM credenciales")
    fun observarRuts(): Flow<List<String>>

    @Upsert
    suspend fun guardar(credencial: CredencialEntity)

    @Query("DELETE FROM credenciales WHERE rut = :rut")
    suspend fun eliminar(rut: String)
}
