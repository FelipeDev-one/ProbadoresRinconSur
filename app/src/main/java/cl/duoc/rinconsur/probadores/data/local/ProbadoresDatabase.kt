package cl.duoc.rinconsur.probadores.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Base de datos local de la app (un archivo SQLite privado). Room genera la implementación.
@Database(
    entities = [
        ColaboradorEntity::class,
        ClienteEntity::class,
        ProbadorEntity::class,
        TurnoEntity::class,
        UsoEntity::class,
        CambioEstadoEntity::class,
        ParametrosEntity::class,
        CredencialEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ProbadoresDatabase : RoomDatabase() {
    abstract fun colaboradorDao(): ColaboradorDao
    abstract fun clienteDao(): ClienteDao
    abstract fun probadorDao(): ProbadorDao
    abstract fun turnoDao(): TurnoDao
    abstract fun usoDao(): UsoDao
    abstract fun cambioEstadoDao(): CambioEstadoDao
    abstract fun parametrosDao(): ParametrosDao
    abstract fun credencialDao(): CredencialDao

    companion object {
        const val NOMBRE = "probadores_rincon_sur.db"

        val TABLAS = arrayOf(
            "colaboradores", "clientes", "probadores", "turnos", "usos", "cambios_estado", "parametros"
        )

        fun crear(context: Context): ProbadoresDatabase =
            Room.databaseBuilder(context.applicationContext, ProbadoresDatabase::class.java, NOMBRE)
                // MVP con datos ficticios: si cambia el esquema, la base se recrea vacía
                // y la app vuelve a pedir la configuración inicial.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
