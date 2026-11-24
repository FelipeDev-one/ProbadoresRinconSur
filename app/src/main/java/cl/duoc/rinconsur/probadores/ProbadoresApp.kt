package cl.duoc.rinconsur.probadores

import cl.duoc.rinconsur.probadores.utils.Notificaciones
import android.app.Application
import cl.duoc.rinconsur.probadores.data.PreferenciasDataStore
import cl.duoc.rinconsur.probadores.data.SeccionRepository
import cl.duoc.rinconsur.probadores.data.local.ProbadoresDatabase
import cl.duoc.rinconsur.probadores.data.remote.ConexionServidor

// Se crea una sola vez al abrir la app. Guarda las instancias únicas de la base de
// datos, el repositorio (con su conexión al servidor) y las preferencias, para que todos los ViewModels compartan las mismas.
class ProbadoresApp : Application() {
    private val baseDeDatos by lazy { ProbadoresDatabase.crear(this) }
    val preferencias by lazy { PreferenciasDataStore(this) }
    val repositorio by lazy { SeccionRepository(baseDeDatos, ConexionServidor(preferencias.obtenerUrlServidor())) }

    override fun onCreate() {
        super.onCreate()
        preferencias.borrarArchivoAntiguo()
        Notificaciones.crearCanales(this)
    }
}
