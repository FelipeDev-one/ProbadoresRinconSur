package cl.duoc.rinconsur.probadores.data.remote

import cl.duoc.rinconsur.probadores.model.CambioEstado
import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Cuenta
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.model.Uso

// Conversión entre el modelo de la app y los DTOs del servidor (ida y vuelta).

const val ROL_CLIENTE = "CLIENTE"

fun UsuarioDto.esCliente() = rol == ROL_CLIENTE

fun UsuarioDto.aColaborador() = Colaborador(
    rut = rut,
    nombre = nombre,
    perfil = Perfil.valueOf(rol),
    turno = turnoLaboral?.let { TurnoLaboral.valueOf(it) } ?: TurnoLaboral.MANANA,
    activo = activo
)

fun UsuarioDto.aCliente(creado: Long) = Cliente(rut = rut, nombre = nombre, creado = creado)

fun UsuarioDto.aCuenta() = Cuenta(
    rut = rut,
    nombre = nombre,
    perfil = if (esCliente()) null else Perfil.valueOf(rol),
    activa = activo
)

fun Colaborador.aNuevoDto() = ColaboradorNuevoDto(rut, nombre, perfil.name, turno.name)

fun Colaborador.aActualizarDto() = ActualizarUsuarioDto(nombre, perfil.name, turno.name, activo)

fun Probador.aDto() = ProbadorDto(codigo, tipo.name, estado.name, desde, usoActivoId, motivoFueraServicio, fotoEvidencia)

fun ProbadorDto.aModelo() = Probador(
    codigo = codigo,
    tipo = TipoProbador.valueOf(tipo),
    estado = EstadoProbador.valueOf(estado),
    desde = desde,
    usoActivoId = usoActivoId,
    motivoFueraServicio = motivoFueraServicio,
    fotoEvidencia = fotoEvidencia
)

fun Uso.aDto() = UsoDto(
    id, codigoProbador, turnoId, inicio, termino, prendasIngresadas, prendasDevueltas,
    registroInicio, registroTermino, conAtraso
)

fun UsoDto.aModelo() = Uso(
    id = id,
    codigoProbador = codigoProbador,
    turnoId = turnoId,
    inicio = inicio,
    termino = termino,
    prendasIngresadas = prendasIngresadas,
    prendasDevueltas = prendasDevueltas,
    registroInicio = registroInicio,
    registroTermino = registroTermino,
    conAtraso = conAtraso == true
)

fun CambioEstado.aDto() = CambioEstadoDto(codigoProbador, anterior.name, nuevo.name, fechaHora, colaborador, motivo)

fun CambioEstadoDto.aModelo() = CambioEstado(
    codigoProbador = codigoProbador,
    anterior = EstadoProbador.valueOf(anterior),
    nuevo = EstadoProbador.valueOf(nuevo),
    fechaHora = fechaHora,
    colaborador = colaborador,
    motivo = motivo
)

fun Turno.aDto() = TurnoDto(
    id, numero, fecha, horaIngreso, preferente, prendas, estado.name,
    horaLlamado, horaCierre, motivoAbandono?.name, registradoPor, clienteRut
)

fun TurnoDto.aModelo() = Turno(
    id = id,
    numero = numero,
    fecha = fecha,
    horaIngreso = horaIngreso,
    preferente = preferente == true,
    prendas = prendas,
    estado = EstadoTurno.valueOf(estado),
    horaLlamado = horaLlamado,
    horaCierre = horaCierre,
    motivoAbandono = motivoAbandono?.let { MotivoAbandono.valueOf(it) },
    registradoPor = registradoPor,
    clienteRut = clienteRut
)

fun Parametros.aDto() = ParametrosDto(
    maxPrendas, tiempoReferenciaUso, minutosOrdenamiento, umbralProlongado, umbralAyuda,
    minutosPresentarse, ticketPromedio, horaApertura, horaCierre, minutosCierreFila
)

fun ParametrosDto.aModelo() = Parametros(
    maxPrendas, tiempoReferenciaUso, minutosOrdenamiento, umbralProlongado, umbralAyuda,
    minutosPresentarse, ticketPromedio, horaApertura, horaCierre, minutosCierreFila
)
