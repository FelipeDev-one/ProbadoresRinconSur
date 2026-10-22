package cl.duoc.rinconsur.probadores.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.duoc.rinconsur.probadores.model.Seccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "rincon_sur_probadores")

class SeccionDataStore(private val context: Context) {
    private val PROBADORES = stringPreferencesKey("probadores")
    private val COLABORADORES = stringPreferencesKey("colaboradores")
    private val TURNOS = stringPreferencesKey("turnos")
    private val USOS = stringPreferencesKey("usos")
    private val CAMBIOS = stringPreferencesKey("cambios_estado")
    private val PARAMETROS = stringPreferencesKey("parametros")
    private val SESION_RUT = stringPreferencesKey("sesion_rut")
    private val MODO_DEMO = booleanPreferencesKey("modo_demo")

    private fun leer(p: Preferences): Seccion? {
        if (p[PROBADORES] == null) return null
        return Seccion(
            probadores = SeccionSerializer.probadoresDesdeTexto(p[PROBADORES]),
            colaboradores = SeccionSerializer.colaboradoresDesdeTexto(p[COLABORADORES]),
            turnos = SeccionSerializer.turnosDesdeTexto(p[TURNOS]),
            usos = SeccionSerializer.usosDesdeTexto(p[USOS]),
            cambios = SeccionSerializer.cambiosDesdeTexto(p[CAMBIOS]),
            parametros = SeccionSerializer.parametrosDesdeTexto(p[PARAMETROS])
        )
    }

    private fun escribir(p: MutablePreferences, s: Seccion) {
        p[PROBADORES] = SeccionSerializer.probadoresATexto(s.probadores)
        p[COLABORADORES] = SeccionSerializer.colaboradoresATexto(s.colaboradores)
        p[TURNOS] = SeccionSerializer.turnosATexto(s.turnos)
        p[USOS] = SeccionSerializer.usosATexto(s.usos)
        p[CAMBIOS] = SeccionSerializer.cambiosATexto(s.cambios)
        p[PARAMETROS] = SeccionSerializer.parametrosATexto(s.parametros)
    }

    fun obtenerSeccion(): Flow<Seccion?> = context.dataStore.data.map { leer(it) }

    fun obtenerSesion(): Flow<String?> = context.dataStore.data.map { it[SESION_RUT] }

    fun obtenerModoDemo(): Flow<Boolean> = context.dataStore.data.map { it[MODO_DEMO] ?: false }

    suspend fun inicializarSiFalta(crear: () -> Seccion) {
        context.dataStore.edit { p ->
            if (p[PROBADORES] == null) escribir(p, crear())
        }
    }

    suspend fun <T> actualizar(operacion: (Seccion) -> Pair<Seccion, T>): T {
        var resultado: T? = null
        context.dataStore.edit { p ->
            val actual = leer(p) ?: Seccion()
            val (nueva, valor) = operacion(actual)
            escribir(p, nueva)
            resultado = valor
        }
        @Suppress("UNCHECKED_CAST")
        return resultado as T
    }

    suspend fun guardarSesion(rut: String?) {
        context.dataStore.edit { p ->
            if (rut == null) p.remove(SESION_RUT) else p[SESION_RUT] = rut
        }
    }

    suspend fun guardarModoDemo(valor: Boolean) {
        context.dataStore.edit { it[MODO_DEMO] = valor }
    }
}
