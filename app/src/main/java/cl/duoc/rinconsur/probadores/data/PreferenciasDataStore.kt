package cl.duoc.rinconsur.probadores.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.preferencias by preferencesDataStore(name = "preferencias")

// =====================================================================
//  PreferenciasDataStore — solo preferencias del equipo y la sesión
//  ---------------------------------------------------------------------
//  Aquí NO van datos del negocio (eso vive en Room, ver SeccionRepository):
//  solo quién tiene la sesión abierta, el modo demostración y los accesos
//  recientes que muestra la pantalla de ingreso y si se quieren notificaciones.
// =====================================================================

class PreferenciasDataStore(private val context: Context) {
    private val SESION_RUT = stringPreferencesKey("sesion_rut")
    private val MODO_DEMO = booleanPreferencesKey("modo_demo")
    private val NOTIFICACIONES = booleanPreferencesKey("notificaciones")
    private val RECIENTES = stringPreferencesKey("accesos_recientes")
    private val MAX_RECIENTES = 4

    // ------------------------------- SESIÓN -------------------------------

    fun obtenerSesion(): Flow<String?> = context.preferencias.data.map { it[SESION_RUT] }

    suspend fun guardarSesion(rut: String?) {
        context.preferencias.edit { p ->
            if (rut == null) p.remove(SESION_RUT) else p[SESION_RUT] = rut
        }
    }

    // --------------------------- MODO DEMOSTRACIÓN ---------------------------

    fun obtenerModoDemo(): Flow<Boolean> = context.preferencias.data.map { it[MODO_DEMO] ?: false }

    suspend fun guardarModoDemo(valor: Boolean) {
        context.preferencias.edit { it[MODO_DEMO] = valor }
    }

    // ----------------------------- NOTIFICACIONES -----------------------------

    // Activadas por defecto; el colaborador las puede apagar en Ajustes.
    fun obtenerNotificaciones(): Flow<Boolean> = context.preferencias.data.map { it[NOTIFICACIONES] ?: true }

    suspend fun guardarNotificaciones(valor: Boolean) {
        context.preferencias.edit { it[NOTIFICACIONES] = valor }
    }

    // --------------------------- ACCESOS RECIENTES ---------------------------

    fun obtenerRecientes(): Flow<List<String>> = context.preferencias.data.map { p ->
        p[RECIENTES]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun agregarReciente(rut: String) {
        context.preferencias.edit { p ->
            val actuales = p[RECIENTES]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            p[RECIENTES] = (listOf(rut) + actuales.filter { it != rut }).take(MAX_RECIENTES).joinToString(",")
        }
    }

    suspend fun quitarReciente(rut: String) {
        context.preferencias.edit { p ->
            val actuales = p[RECIENTES]?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            p[RECIENTES] = actuales.filter { it != rut }.joinToString(",")
        }
    }

    // Versiones anteriores guardaban TODA la sección en DataStore. Ese archivo ya no se usa.
    fun borrarArchivoAntiguo() {
        File(context.filesDir, "datastore/rincon_sur_probadores.preferences_pb").delete()
    }
}
