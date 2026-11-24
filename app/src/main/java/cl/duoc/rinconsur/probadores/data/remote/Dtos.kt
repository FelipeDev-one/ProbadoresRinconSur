package cl.duoc.rinconsur.probadores.data.remote

// =====================================================================
//  DTOs: la forma exacta del JSON que envían y reciben los microservicios.
//  Gson los convierte desde y hacia JSON. Se mantienen separados del modelo
//  de la app (model/) para que un cambio en el servidor no toque las reglas.
// =====================================================================

// ------------------------------ usuarios-service ------------------------------

data class UsuarioDto(
    val id: Long? = null,
    val rut: String,
    val nombre: String,
    val rol: String,
    val turnoLaboral: String? = null,
    val activo: Boolean = true,
    val tieneContrasena: Boolean = false
)

data class CuentaDto(val usuario: UsuarioDto, val codigoRecuperacion: String?)

data class EstadoInicialDto(val configurado: Boolean)

data class ConfiguracionInicialDto(val rut: String, val nombre: String, val turnoLaboral: String, val contrasena: String)

data class RegistroDto(val rut: String, val nombre: String, val contrasena: String)

data class ColaboradorNuevoDto(val rut: String, val nombre: String, val rol: String, val turnoLaboral: String)

data class ActualizarUsuarioDto(
    val nombre: String? = null,
    val rol: String? = null,
    val turnoLaboral: String? = null,
    val activo: Boolean? = null
)

data class LoginDto(val rut: String, val contrasena: String)

data class ContrasenaDto(val contrasena: String)

data class CambiarContrasenaDto(val actual: String, val nueva: String)

data class RecuperarDto(val codigo: String, val nueva: String)

// ----------------------------- probadores-service -----------------------------

data class ProbadorDto(
    val codigo: String,
    val tipo: String,
    val estado: String,
    val desde: Long,
    val usoActivoId: String? = null,
    val motivoFueraServicio: String? = null,
    val fotoEvidencia: String? = null
)

data class UsoDto(
    val id: String,
    val codigoProbador: String,
    val turnoId: String?,
    val inicio: Long,
    val termino: Long?,
    val prendasIngresadas: Int,
    val prendasDevueltas: Int?,
    val registroInicio: String,
    val registroTermino: String?,
    val conAtraso: Boolean?
)

data class CambioEstadoDto(
    val codigoProbador: String,
    val anterior: String,
    val nuevo: String,
    val fechaHora: Long,
    val colaborador: String,
    val motivo: String?
)

// ------------------------------- turnos-service -------------------------------

data class TurnoDto(
    val id: String,
    val numero: Int,
    val fecha: String,
    val horaIngreso: Long,
    val preferente: Boolean?,
    val prendas: Int,
    val estado: String,
    val horaLlamado: Long?,
    val horaCierre: Long?,
    val motivoAbandono: String?,
    val registradoPor: String?,
    val clienteRut: String?
)

data class ParametrosDto(
    val maxPrendas: Int,
    val tiempoReferenciaUso: Int,
    val minutosOrdenamiento: Int,
    val umbralProlongado: Int,
    val umbralAyuda: Int,
    val minutosPresentarse: Int,
    val ticketPromedio: Int,
    val horaApertura: Int,
    val horaCierre: Int,
    val minutosCierreFila: Int
)

// Cuerpo de los errores de los microservicios: {"estado":409,"mensaje":"…","campos":{…}}
data class ErrorDto(val estado: Int?, val mensaje: String?, val campos: Map<String, String>?)
