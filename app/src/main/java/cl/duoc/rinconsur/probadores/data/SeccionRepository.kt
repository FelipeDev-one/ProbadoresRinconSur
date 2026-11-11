package cl.duoc.rinconsur.probadores.data

import androidx.room.withTransaction
import cl.duoc.rinconsur.probadores.data.local.ProbadoresDatabase
import cl.duoc.rinconsur.probadores.data.local.aEntidad
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Credencial
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.Uso
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// =====================================================================
//  SeccionRepository — la única puerta entre los ViewModels y la base de datos
//  ---------------------------------------------------------------------
//  Los ViewModels no conocen Room ni los DAOs: piden y guardan una "Seccion"
//  (el modelo completo). El repositorio la arma leyendo las tablas y, al
//  guardar, escribe solo las filas que cambiaron.
//
//     ViewModel ──▶ SeccionRepository ──▶ DAOs ──▶ SQLite (Room)
//
//  actualizar() lee, aplica la regla de negocio y guarda dentro de UNA
//  transacción: si la regla lanza un error, no se guarda nada.
// =====================================================================

class SeccionRepository(private val db: ProbadoresDatabase) {
    private val colaboradores = db.colaboradorDao()
    private val probadores = db.probadorDao()
    private val turnos = db.turnoDao()
    private val usos = db.usoDao()
    private val cambios = db.cambioEstadoDao()
    private val parametros = db.parametrosDao()
    private val credenciales = db.credencialDao()

    // Vuelve a emitir cada vez que cambia cualquiera de las tablas de la sección.
    // Emite null mientras la sección aún no tiene datos.
    fun obtenerSeccion(): Flow<Seccion?> =
        db.invalidationTracker.createFlow(*ProbadoresDatabase.TABLAS).map { db.withTransaction { leer() } }

    suspend fun inicializarSiFalta(crear: () -> Seccion) {
        db.withTransaction {
            if (leer() == null) guardarCambios(Seccion(), crear())
        }
    }

    suspend fun <T> actualizar(operacion: (Seccion) -> Pair<Seccion, T>): T =
        db.withTransaction {
            val actual = leer() ?: Seccion()
            val (nueva, valor) = operacion(actual)
            guardarCambios(actual, nueva)
            valor
        }

    // ----------------------------- CREDENCIALES -----------------------------

    fun obtenerRutsConContrasena(): Flow<Set<String>> = credenciales.observarRuts().map { it.toSet() }

    suspend fun obtenerCredencial(rut: String): Credencial? = credenciales.obtener(rut)?.aModelo()

    suspend fun guardarCredencial(credencial: Credencial) = credenciales.guardar(credencial.aEntidad())

    suspend fun eliminarCredencial(rut: String) = credenciales.eliminar(rut)

    // ------------------------ CONSULTAS CON RELACIONES ------------------------

    // Lo que registró un colaborador (colaboradores → turnos y usos).
    suspend fun registrosDe(rut: String): Triple<Colaborador, List<Turno>, List<Uso>>? =
        colaboradores.conRegistros(rut)?.let { r ->
            Triple(r.colaborador.aModelo(), r.turnos.map { it.aModelo() }, r.usos.map { it.aModelo() })
        }

    // Historial de usos de un probador (probadores → usos).
    suspend fun usosDelProbador(codigo: String): List<Uso> =
        probadores.conUsos(codigo)?.usos?.map { it.aModelo() } ?: emptyList()

    // Turnos de un día con el uso que los atendió, si lo hubo (turnos → usos).
    suspend fun turnosDelDiaConUso(fecha: String): List<Pair<Turno, Uso?>> =
        turnos.delDiaConUso(fecha).map { it.turno.aModelo() to it.uso?.aModelo() }

    // ------------------------------- INTERNOS -------------------------------

    private suspend fun leer(): Seccion? {
        val listaProbadores = probadores.todos()
        val listaColaboradores = colaboradores.todos()
        if (listaProbadores.isEmpty() && listaColaboradores.isEmpty()) return null
        return Seccion(
            probadores = listaProbadores.map { it.aModelo() },
            colaboradores = listaColaboradores.map { it.aModelo() },
            turnos = turnos.todos().map { it.aModelo() },
            usos = usos.todos().map { it.aModelo() },
            cambios = cambios.todos().map { it.aModelo() },
            parametros = parametros.obtener()?.aModelo() ?: Parametros()
        )
    }

    // Compara la sección anterior con la nueva y escribe solo las diferencias.
    // El orden importa por las claves foráneas: primero los "padres" (colaboradores,
    // probadores, turnos) y después los "hijos" (usos, cambios); al borrar, al revés.
    private suspend fun guardarCambios(antes: Seccion, despues: Seccion) {
        val dColaboradores = diferencias(antes.colaboradores, despues.colaboradores) { it.rut }
        val dProbadores = diferencias(antes.probadores, despues.probadores) { it.codigo }
        val dTurnos = diferencias(antes.turnos, despues.turnos) { it.id }
        val dUsos = diferencias(antes.usos, despues.usos) { it.id }
        val dCambios = diferencias(antes.cambios, despues.cambios) { Triple(it.codigoProbador, it.fechaHora, it.nuevo) }

        if (dColaboradores.guardar.isNotEmpty()) colaboradores.guardar(dColaboradores.guardar.map { it.aEntidad() })
        if (dProbadores.guardar.isNotEmpty()) probadores.guardar(dProbadores.guardar.map { it.aEntidad() })
        if (dTurnos.guardar.isNotEmpty()) turnos.guardar(dTurnos.guardar.map { it.aEntidad() })
        if (dUsos.guardar.isNotEmpty()) usos.guardar(dUsos.guardar.map { it.aEntidad() })
        if (dCambios.guardar.isNotEmpty()) cambios.guardar(dCambios.guardar.map { it.aEntidad() })
        if (antes.parametros != despues.parametros || parametros.obtener() == null) {
            parametros.guardar(despues.parametros.aEntidad())
        }

        if (dCambios.eliminar.isNotEmpty()) cambios.eliminar(dCambios.eliminar.map { it.aEntidad() })
        if (dUsos.eliminar.isNotEmpty()) usos.eliminar(dUsos.eliminar.map { it.aEntidad() })
        if (dTurnos.eliminar.isNotEmpty()) turnos.eliminar(dTurnos.eliminar.map { it.aEntidad() })
        if (dProbadores.eliminar.isNotEmpty()) probadores.eliminar(dProbadores.eliminar.map { it.aEntidad() })
        if (dColaboradores.eliminar.isNotEmpty()) colaboradores.eliminar(dColaboradores.eliminar.map { it.aEntidad() })
    }

    private class Diferencias<E>(val guardar: List<E>, val eliminar: List<E>)

    // guardar = filas nuevas o modificadas; eliminar = filas cuya clave ya no está.
    private fun <E, K> diferencias(antes: List<E>, despues: List<E>, clave: (E) -> K): Diferencias<E> {
        val anteriores = antes.toSet()
        val clavesNuevas = despues.map(clave).toSet()
        return Diferencias(
            guardar = despues.filter { it !in anteriores },
            eliminar = antes.filter { clave(it) !in clavesNuevas }
        )
    }
}
