package cl.duoc.rinconsur.probadores.data

import androidx.room.withTransaction
import cl.duoc.rinconsur.probadores.data.cuentas.CuentasLocales
import cl.duoc.rinconsur.probadores.data.cuentas.CuentasRemotas
import cl.duoc.rinconsur.probadores.data.cuentas.ServicioCuentas
import cl.duoc.rinconsur.probadores.data.local.ProbadoresDatabase
import cl.duoc.rinconsur.probadores.data.local.aEntidad
import cl.duoc.rinconsur.probadores.data.remote.ConexionServidor
import cl.duoc.rinconsur.probadores.data.remote.DescargaRemota
import cl.duoc.rinconsur.probadores.data.remote.EnviadorRemoto
import cl.duoc.rinconsur.probadores.data.remote.EstadoConexion
import cl.duoc.rinconsur.probadores.data.remote.PlanSincronizacion
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Credencial
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.SinConexionException
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.Uso
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// =====================================================================
//  SeccionRepository — la única puerta entre los ViewModels y los datos
//  ---------------------------------------------------------------------
//  Los ViewModels piden y guardan una "Seccion" (el modelo completo) y no
//  saben de dónde viene. Con servidor configurado, los microservicios son la
//  fuente de verdad y Room es la copia local (caché) que lee la pantalla:
//
//     ViewModel ──▶ SeccionRepository ──▶ Retrofit ──▶ API Gateway ──▶ microservicios
//                          │
//                          └──▶ DAOs ──▶ SQLite (Room, caché)
//
//  actualizar(): aplica la regla de negocio, ENVÍA los cambios al servidor y,
//  solo si el servidor los aceptó, los guarda en Room. Si algo falla, no se guarda nada.
//  sincronizar(): DESCARGA el estado del servidor y lo deja en Room (lo llama
//  SeccionViewModel cada pocos segundos, para ver lo que hacen otros equipos).
//  Sin servidor (dirección vacía en Ajustes) todo queda solo en Room, como antes.
// =====================================================================

class SeccionRepository(
    private val db: ProbadoresDatabase,
    private val conexion: ConexionServidor
) {
    private val colaboradores = db.colaboradorDao()
    private val clientes = db.clienteDao()
    private val probadores = db.probadorDao()
    private val turnos = db.turnoDao()
    private val usos = db.usoDao()
    private val cambios = db.cambioEstadoDao()
    private val parametros = db.parametrosDao()
    private val credenciales = db.credencialDao()

    // Una sola escritura a la vez (acción del usuario o sincronización).
    private val candado = Mutex()

    private val _estadoConexion = MutableStateFlow(EstadoConexion.CONECTANDO)
    val estadoConexion: StateFlow<EstadoConexion> = _estadoConexion

    // false hasta el primer intento de sincronizar: evita mostrar el asistente de
    // configuración mientras todavía no se sabe si el servidor ya tiene datos.
    private val _primeraSincronizacion = MutableStateFlow(false)
    val primeraSincronizacion: StateFlow<Boolean> = _primeraSincronizacion

    private val rutsConContrasenaRemotos = MutableStateFlow<Set<String>>(emptySet())

    val urlServidor: Flow<String> = conexion.url

    // Vuelve a emitir cada vez que cambia cualquiera de las tablas de la sección.
    // Emite null mientras la sección aún no tiene datos.
    fun obtenerSeccion(): Flow<Seccion?> =
        db.invalidationTracker.createFlow(*ProbadoresDatabase.TABLAS).map { db.withTransaction { leer() } }

    suspend fun <T> actualizar(operacion: (Seccion) -> Pair<Seccion, T>): T = candado.withLock {
        val api = conexion.api()
        if (api == null) {
            return@withLock db.withTransaction {
                val actual = leer() ?: Seccion()
                val (nueva, valor) = operacion(actual)
                guardarCambios(actual, nueva)
                valor
            }
        }
        val actual = db.withTransaction { leer() } ?: Seccion()
        val (nueva, valor) = operacion(actual)
        try {
            EnviadorRemoto(api).enviar(PlanSincronizacion.operaciones(actual, nueva))
        } catch (e: SinConexionException) {
            _estadoConexion.value = EstadoConexion.SIN_CONEXION
            throw e
        }
        _estadoConexion.value = EstadoConexion.CONECTADO
        db.withTransaction { guardarCambios(actual, nueva) }
        valor
    }

    // Descarga todo desde los microservicios y actualiza la copia local.
    // Devuelve false si no hay servidor configurado. Lanza SinConexionException si no responde.
    suspend fun sincronizar(): Boolean = candado.withLock {
        val api = conexion.api()
        if (api == null) {
            _estadoConexion.value = EstadoConexion.SIN_SERVIDOR
            _primeraSincronizacion.value = true
            return@withLock false
        }
        try {
            val creados = clientes.todos().associate { it.rut to it.aModelo().creado }
            val remoto = DescargaRemota(api).descargar(creados)
            db.withTransaction { guardarCambios(leer() ?: Seccion(), remoto.seccion) }
            rutsConContrasenaRemotos.value = remoto.rutsConContrasena
            _estadoConexion.value = EstadoConexion.CONECTADO
            true
        } catch (e: SinConexionException) {
            _estadoConexion.value = EstadoConexion.SIN_CONEXION
            throw e
        } finally {
            _primeraSincronizacion.value = true
        }
    }

    // Ingreso y contraseñas: en el servidor si hay uno configurado; si no, en el teléfono.
    suspend fun cuentas(): ServicioCuentas =
        conexion.api()?.let { CuentasRemotas(it.usuarios) } ?: CuentasLocales(this)

    // ----------------------------- CREDENCIALES -----------------------------
    // (solo se usan sin servidor; con servidor las contraseñas viven en usuarios-service)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun obtenerRutsConContrasena(): Flow<Set<String>> = conexion.url.flatMapLatest { url ->
        if (url.isBlank()) credenciales.observarRuts().map { it.toSet() } else rutsConContrasenaRemotos
    }

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
            clientes = clientes.todos().map { it.aModelo() },
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
        val dClientes = diferencias(antes.clientes, despues.clientes) { it.rut }
        val dProbadores = diferencias(antes.probadores, despues.probadores) { it.codigo }
        val dTurnos = diferencias(antes.turnos, despues.turnos) { it.id }
        val dUsos = diferencias(antes.usos, despues.usos) { it.id }
        val dCambios = diferencias(antes.cambios, despues.cambios) { Triple(it.codigoProbador, it.fechaHora, it.nuevo) }

        if (dColaboradores.guardar.isNotEmpty()) colaboradores.guardar(dColaboradores.guardar.map { it.aEntidad() })
        if (dClientes.guardar.isNotEmpty()) clientes.guardar(dClientes.guardar.map { it.aEntidad() })
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
        if (dClientes.eliminar.isNotEmpty()) clientes.eliminar(dClientes.eliminar.map { it.aEntidad() })
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
