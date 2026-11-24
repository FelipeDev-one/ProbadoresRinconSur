package cl.duoc.rinconsur.probadores.data.remote

import cl.duoc.rinconsur.probadores.model.CambioEstado
import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.ErrorServidorException
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.Uso

// =====================================================================
//  Sincronización con los microservicios
//  ---------------------------------------------------------------------
//  ENVIAR: cada acción de la app produce una Seccion nueva. PlanSincronizacion
//  compara la anterior con la nueva y arma la lista de llamadas al servidor
//  (solo lo que cambió). EnviadorRemoto las ejecuta en orden; si una falla,
//  la acción se cancela y no se guarda nada en el teléfono.
//
//  DESCARGAR: DescargaRemota trae el estado completo de los tres servicios y lo
//  arma como una Seccion, que el repositorio guarda en Room (caché local).
// =====================================================================

sealed interface OperacionRemota {
    data class CrearColaborador(val colaborador: Colaborador) : OperacionRemota
    data class ActualizarColaborador(val colaborador: Colaborador) : OperacionRemota
    data class ActualizarCliente(val cliente: Cliente) : OperacionRemota
    data class GuardarProbador(val probador: Probador) : OperacionRemota
    data class EliminarProbador(val codigo: String) : OperacionRemota
    data class GuardarTurno(val turno: Turno) : OperacionRemota
    data class GuardarUso(val uso: Uso) : OperacionRemota
    data class RegistrarCambio(val cambio: CambioEstado) : OperacionRemota
    data class GuardarParametros(val parametros: Parametros) : OperacionRemota
}

object PlanSincronizacion {

    // El orden respeta las dependencias: primero cuentas y probadores, después turnos,
    // usos e historial; los probadores eliminados al final.
    // Las cuentas de cliente NUEVAS no van aquí: se crean con su contraseña en usuarios-service.
    fun operaciones(antes: Seccion, despues: Seccion): List<OperacionRemota> {
        val ops = mutableListOf<OperacionRemota>()
        val colaboradoresAntes = antes.colaboradores.associateBy { it.rut }
        despues.colaboradores.forEach { c ->
            val previo = colaboradoresAntes[c.rut]
            when {
                previo == null -> ops += OperacionRemota.CrearColaborador(c)
                previo != c -> ops += OperacionRemota.ActualizarColaborador(c)
            }
        }
        val clientesAntes = antes.clientes.associateBy { it.rut }
        despues.clientes.forEach { c ->
            val previo = clientesAntes[c.rut]
            if (previo != null && previo.nombre != c.nombre) ops += OperacionRemota.ActualizarCliente(c)
        }
        nuevosOCambiados(antes.probadores, despues.probadores).forEach { ops += OperacionRemota.GuardarProbador(it) }
        nuevosOCambiados(antes.turnos, despues.turnos).forEach { ops += OperacionRemota.GuardarTurno(it) }
        nuevosOCambiados(antes.usos, despues.usos).forEach { ops += OperacionRemota.GuardarUso(it) }
        nuevosOCambiados(antes.cambios, despues.cambios).forEach { ops += OperacionRemota.RegistrarCambio(it) }
        if (antes.parametros != despues.parametros) ops += OperacionRemota.GuardarParametros(despues.parametros)
        val codigosNuevos = despues.probadores.map { it.codigo }.toSet()
        antes.probadores.filter { it.codigo !in codigosNuevos }.forEach { ops += OperacionRemota.EliminarProbador(it.codigo) }
        return ops
    }

    private fun <E> nuevosOCambiados(antes: List<E>, despues: List<E>): List<E> {
        val anteriores = antes.toSet()
        return despues.filter { it !in anteriores }
    }
}

class EnviadorRemoto(private val api: ApiServidor) {

    suspend fun enviar(operaciones: List<OperacionRemota>) {
        operaciones.forEach { llamar { ejecutar(it) } }
    }

    private suspend fun ejecutar(op: OperacionRemota) {
        when (op) {
            is OperacionRemota.CrearColaborador -> crearColaborador(op.colaborador)
            is OperacionRemota.ActualizarColaborador ->
                api.usuarios.actualizar(op.colaborador.rut, op.colaborador.aActualizarDto())
            is OperacionRemota.ActualizarCliente ->
                api.usuarios.actualizar(op.cliente.rut, ActualizarUsuarioDto(nombre = op.cliente.nombre))
            is OperacionRemota.GuardarProbador -> api.probadores.guardar(op.probador.codigo, op.probador.aDto())
            is OperacionRemota.EliminarProbador -> api.probadores.eliminar(op.codigo)
            is OperacionRemota.GuardarTurno -> api.turnos.guardar(op.turno.id, op.turno.aDto())
            is OperacionRemota.GuardarUso -> api.probadores.guardarUso(op.uso.id, op.uso.aDto())
            is OperacionRemota.RegistrarCambio -> api.probadores.registrarCambio(op.cambio.aDto())
            is OperacionRemota.GuardarParametros -> api.turnos.guardarParametros(op.parametros.aDto())
        }
    }

    // La jefatura de la configuración inicial ya existe en el servidor (se creó con su contraseña):
    // si responde 409, se actualizan sus datos en vez de crearla.
    private suspend fun crearColaborador(c: Colaborador) {
        try {
            llamar { api.usuarios.crearColaborador(c.aNuevoDto()) }
        } catch (e: ErrorServidorException) {
            if (e.codigo != 409) throw e
            api.usuarios.actualizar(c.rut, c.aActualizarDto())
        }
    }
}

// Estado completo del servidor + los RUT que ya tienen contraseña (para la pantalla de colaboradores).
data class EstadoRemoto(val seccion: Seccion, val rutsConContrasena: Set<String>)

class DescargaRemota(private val api: ApiServidor) {

    // creadoLocal: fecha de creación de los clientes que ya estaban en el teléfono (el servidor no la envía).
    suspend fun descargar(creadoLocal: Map<String, Long> = emptyMap()): EstadoRemoto = llamar {
        val usuarios = api.usuarios.listar()
        val seccion = Seccion(
            probadores = api.probadores.listar().map { it.aModelo() },
            colaboradores = usuarios.filterNot { it.esCliente() }.map { it.aColaborador() },
            clientes = usuarios.filter { it.esCliente() }.map { it.aCliente(creadoLocal[it.rut] ?: 0L) },
            turnos = api.turnos.listar().map { it.aModelo() },
            usos = api.probadores.usos().map { it.aModelo() },
            cambios = api.probadores.cambios().map { it.aModelo() },
            parametros = api.turnos.parametros().aModelo()
        )
        EstadoRemoto(seccion, usuarios.filter { it.tieneContrasena }.map { it.rut }.toSet())
    }
}
