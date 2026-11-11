package cl.duoc.rinconsur.probadores

import android.app.Application
import cl.duoc.rinconsur.probadores.data.PreferenciasDataStore
import cl.duoc.rinconsur.probadores.data.SeccionRepository
import cl.duoc.rinconsur.probadores.data.local.ProbadoresDatabase

// Se crea una sola vez al abrir la app. Guarda las instancias únicas de la base de
// datos, el repositorio y las preferencias, para que todos los ViewModels compartan las mismas.
class ProbadoresApp : Application() {
    private val baseDeDatos by lazy { ProbadoresDatabase.crear(this) }
    val repositorio by lazy { SeccionRepository(baseDeDatos) }
    val preferencias by lazy { PreferenciasDataStore(this) }

    override fun onCreate() {
        super.onCreate()
        preferencias.borrarArchivoAntiguo()
    }
}
