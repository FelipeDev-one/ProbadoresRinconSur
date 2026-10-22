package cl.duoc.rinconsur.probadores.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.rinconsur.probadores.data.DatosEjemplo
import cl.duoc.rinconsur.probadores.data.SeccionDataStore
import cl.duoc.rinconsur.probadores.model.Alerta
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.EstimadorEspera
import cl.duoc.rinconsur.probadores.model.Franja
import cl.duoc.rinconsur.probadores.model.GeneradorAlertas
import cl.duoc.rinconsur.probadores.model.Resumen
import cl.duoc.rinconsur.probadores.model.ResumenCalculator
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.Uso
import cl.duoc.rinconsur.probadores.model.UsoSinFilaErrores
import cl.duoc.rinconsur.probadores.utils.Tiempo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProbadorVista(
    val probador: Probador,
    val minutosEnEstado: Int,
    val uso: Uso?,
    val prolongado: Boolean,
    val ofrecerAyuda: Boolean,
    val siguienteTurno: Turno?
)

data class TurnoVista(
    val turno: Turno,
    val posicion: Int,
    val minutosEsperando: Int,
    val estimado: Int?,
    val minutosDesdeLlamado: Int?
)

data class PanelUiState(
    val probadores: List<ProbadorVista>,
    val fila: List<TurnoVista>,
    val libres: Int,
    val habilitados: Int,
    val estimadoNuevo: Int?,
    val usandoReferencia: Boolean,
    val alertas: List<Alerta>,
    val motivoFilaCerrada: String?,
    val parametros: Parametros,
    val ahora: Long
) {
    val personasEnFila: Int get() = fila.size
}

data class Mensaje(val texto: String, val esError: Boolean)

class SeccionViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = SeccionDataStore(application)

    val ahora: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1_000)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, System.currentTimeMillis())

    val seccion: StateFlow<Seccion?> = dataStore.obtenerSeccion()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val modoDemo: StateFlow<Boolean> = dataStore.obtenerModoDemo()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val sesion: StateFlow<Colaborador?> = combine(seccion, dataStore.obtenerSesion()) { s, rut ->
        s?.colaboradores?.find { it.rut == rut && it.activo }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensaje = MutableStateFlow<Mensaje?>(null)
    val mensaje: StateFlow<Mensaje?> = _mensaje
    private var trabajoMensaje: Job? = null

    val panel: StateFlow<PanelUiState?> = combine(seccion, ahora) { s, ahora ->
        s?.let { construirPanel(it, ahora) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            dataStore.inicializarSiFalta { DatosEjemplo.crear(System.currentTimeMillis()) }
            seccion.filterNotNull().first()
            delay(1_200)
            _cargando.value = false
        }
    }

    private fun construirPanel(s: Seccion, ahora: Long): PanelUiState {
        val fecha = Tiempo.fechaIso(ahora)
        val fila = ReglasSeccion.filaDelDia(s, fecha)
        val usosCerrados = ReglasSeccion.usosCerradosDelDia(s, fecha)
        val p = s.parametros

        val probadores = s.probadores.map { probador ->
            val minutos = Tiempo.minutosEntre(probador.desde, ahora)
            val ocupado = probador.estado == EstadoProbador.OCUPADO
            ProbadorVista(
                probador = probador,
                minutosEnEstado = minutos,
                uso = ReglasSeccion.usoActivo(s, probador),
                prolongado = ocupado && minutos > p.umbralProlongado,
                ofrecerAyuda = ocupado && minutos > p.umbralAyuda && fila.isNotEmpty(),
                siguienteTurno = if (probador.estado == EstadoProbador.LIBRE)
                    ReglasSeccion.siguienteTurnoPara(probador, fila, s.probadores) else null
            )
        }

        val turnos = fila.mapIndexed { indice, turno ->
            TurnoVista(
                turno = turno,
                posicion = indice + 1,
                minutosEsperando = Tiempo.minutosEntre(turno.horaIngreso, ahora),
                estimado = EstimadorEspera.enSeccion(s, indice, ahora),
                minutosDesdeLlamado = turno.horaLlamado?.let { Tiempo.minutosEntre(it, ahora) }
            )
        }

        return PanelUiState(
            probadores = probadores,
            fila = turnos,
            libres = s.probadores.count { it.estado == EstadoProbador.LIBRE },
            habilitados = s.probadores.count { it.habilitado },
            estimadoNuevo = EstimadorEspera.enSeccion(s, fila.size, ahora),
            usandoReferencia = usosCerrados.size < EstimadorEspera.MINIMO_USOS_DEL_DIA,
            alertas = GeneradorAlertas.generar(s, fila, ahora),
            motivoFilaCerrada = if (modoDemo.value) null else ReglasSeccion.motivoFilaCerrada(p, ahora),
            parametros = p,
            ahora = ahora
        )
    }

    fun mostrarMensaje(texto: String, esError: Boolean = false) {
        trabajoMensaje?.cancel()
        trabajoMensaje = viewModelScope.launch {
            _mensaje.value = Mensaje(texto, esError)
            delay(if (esError) 4_000 else 2_500)
            _mensaje.value = null
        }
    }

    private fun ejecutar(textoExito: String?, operacion: (Seccion, String, Long) -> Seccion) {
        val rut = sesion.value?.rut
        if (rut == null) {
            mostrarMensaje("Su sesión terminó. Ingrese nuevamente.", true)
            return
        }
        viewModelScope.launch {
            try {
                dataStore.actualizar { s -> operacion(s, rut, System.currentTimeMillis()) to Unit }
                textoExito?.let { mostrarMensaje(it) }
            } catch (e: ReglaNegocioException) {
                mostrarMensaje(e.message ?: "No se pudo completar la acción.", true)
            }
        }
    }

    fun llamar(turno: Turno) = ejecutar("${turno.codigo} llamado al mesón") { s, rut, t ->
        ReglasSeccion.llamarTurno(s, turno.id, rut, t)
    }

    fun asignar(turno: Turno, codigoProbador: String) =
        ejecutar("$codigoProbador asignado a ${turno.codigo}") { s, rut, t ->
            ReglasSeccion.asignarProbador(s, codigoProbador, turno.id, null, rut, t)
        }

    fun registrarAbandono(turno: Turno, motivo: MotivoAbandono) =
        ejecutar("${turno.codigo} registrado como abandono") { s, rut, t ->
            ReglasSeccion.registrarAbandono(s, turno.id, motivo, rut, t)
        }

    fun noSePresento(turno: Turno) = ejecutar("${turno.codigo}: no se presentó") { s, rut, t ->
        ReglasSeccion.registrarAbandono(s, turno.id, MotivoAbandono.NO_INFORMADO, rut, t, noSePresento = true)
    }

    fun usoSinFila(codigo: String, prendasTexto: String, atrasoTexto: String): UsoSinFilaErrores {
        val max = seccion.value?.parametros?.maxPrendas ?: 6
        val prendas = prendasTexto.trim().toIntOrNull()
        val atraso = if (atrasoTexto.isBlank()) 0 else atrasoTexto.trim().toIntOrNull()
        val errores = UsoSinFilaErrores(
            prendas = when {
                prendas == null -> "Campo obligatorio"
                prendas !in 1..max -> "Entre 1 y $max prendas"
                else -> null
            },
            atraso = if (atraso == null || atraso !in 0..ReglasSeccion.MAX_MINUTOS_ATRASO)
                "Debe estar entre 0 y ${ReglasSeccion.MAX_MINUTOS_ATRASO} minutos" else null
        )
        if (errores.hayErrores || prendas == null || atraso == null) return errores
        val sufijo = if (atraso > 0) " (registrado con atraso)" else ""
        ejecutar("Uso iniciado en $codigo$sufijo") { s, rut, t ->
            ReglasSeccion.asignarProbador(s, codigo, null, prendas, rut, t, atraso)
        }
        return errores
    }

    fun terminarUso(codigo: String, devueltasTexto: String, ingresadas: Int): String? {
        val devueltas = devueltasTexto.trim().toIntOrNull()
        when {
            devueltas == null -> return "Seleccione cuántas prendas devolvió (0 si se llevó todo)"
            devueltas < 0 -> return "No puede ser negativo"
            devueltas > ingresadas -> return "No puede devolver más de las $ingresadas prendas que ingresó"
        }
        ejecutar("$codigo pasó a ordenamiento") { s, rut, t ->
            ReglasSeccion.terminarUso(s, codigo, devueltas, rut, t)
        }
        return null
    }

    fun finalizarOrdenamiento(codigo: String) = ejecutar("$codigo quedó libre") { s, rut, t ->
        ReglasSeccion.finalizarOrdenamiento(s, codigo, rut, t)
    }

    fun habilitar(codigo: String) = ejecutar("$codigo habilitado nuevamente") { s, rut, t ->
        ReglasSeccion.habilitar(s, codigo, rut, t)
    }

    fun cerrarJornada() {
        val rut = sesion.value?.rut ?: return
        viewModelScope.launch {
            try {
                val cerrados = dataStore.actualizar { s ->
                    ReglasSeccion.cerrarJornada(s, rut, System.currentTimeMillis())
                }
                mostrarMensaje("Jornada cerrada: $cerrados turno(s) quedaron como \"Cierre de tienda\"")
            } catch (e: ReglaNegocioException) {
                mostrarMensaje(e.message ?: "No se pudo cerrar la jornada.", true)
            }
        }
    }

    fun cargarEscenarioDemo() = ejecutar("Escenario de hora peak cargado") { s, rut, t ->
        DatosEjemplo.escenarioPeak(s, t, rut)
    }

    fun cambiarModoDemo(valor: Boolean) {
        viewModelScope.launch {
            dataStore.guardarModoDemo(valor)
            mostrarMensaje(if (valor) "Modo demostración activado" else "Modo demostración desactivado")
        }
    }

    fun cerrarSesion() {
        viewModelScope.launch { dataStore.guardarSesion(null) }
    }

    fun consultarTurno(numeroTexto: String): ConsultaTurno {
        val numero = numeroTexto.trim().removePrefix("T-").removePrefix("t-").toIntOrNull()
            ?: return ConsultaTurno.Invalido("Ingrese solo el número de su turno, por ejemplo 24")
        val s = seccion.value ?: return ConsultaTurno.NoEncontrado
        val ahora = System.currentTimeMillis()
        val turno = ReglasSeccion.turnosDelDia(s, Tiempo.fechaIso(ahora)).find { it.numero == numero }
            ?: return ConsultaTurno.NoEncontrado
        return when (turno.estado) {
            EstadoTurno.LLAMADO -> ConsultaTurno.Llamado(turno.codigo)
            EstadoTurno.EN_ESPERA -> {
                val fila = ReglasSeccion.filaDelDia(s, Tiempo.fechaIso(ahora))
                val delante = fila.indexOfFirst { it.id == turno.id }
                ConsultaTurno.EnEspera(turno.codigo, delante, EstimadorEspera.enSeccion(s, delante, ahora))
            }
            else -> ConsultaTurno.Cerrado(turno.codigo, turno.estado.etiqueta)
        }
    }

    fun fechasConRegistros(): List<String> {
        val hoy = Tiempo.fechaIso(System.currentTimeMillis())
        val fechas = seccion.value?.turnos?.map { it.fecha }?.toSet() ?: emptySet()
        return (fechas + hoy).sortedDescending()
    }

    fun resumen(fecha: String, franja: Franja): Resumen? {
        val s = seccion.value ?: return null
        return ResumenCalculator.calcular(s.turnos, s.usos, fecha, franja, s.parametros.ticketPromedio)
    }

    fun resumenPorFranja(fecha: String): List<Triple<Franja, Int, Int>> =
        seccion.value?.let { ResumenCalculator.porFranja(it.turnos, fecha) } ?: emptyList()
}

sealed class ConsultaTurno {
    data class Invalido(val error: String) : ConsultaTurno()
    data object NoEncontrado : ConsultaTurno()
    data class EnEspera(val codigo: String, val personasAntes: Int, val estimado: Int?) : ConsultaTurno()
    data class Llamado(val codigo: String) : ConsultaTurno()
    data class Cerrado(val codigo: String, val estado: String) : ConsultaTurno()
}
