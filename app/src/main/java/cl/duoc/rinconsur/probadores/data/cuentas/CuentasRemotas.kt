package cl.duoc.rinconsur.probadores.data.cuentas

import cl.duoc.rinconsur.probadores.data.remote.CambiarContrasenaDto
import cl.duoc.rinconsur.probadores.data.remote.ConfiguracionInicialDto
import cl.duoc.rinconsur.probadores.data.remote.ContrasenaDto
import cl.duoc.rinconsur.probadores.data.remote.CuentaDto
import cl.duoc.rinconsur.probadores.data.remote.LoginDto
import cl.duoc.rinconsur.probadores.data.remote.RecuperarDto
import cl.duoc.rinconsur.probadores.data.remote.RegistroDto
import cl.duoc.rinconsur.probadores.data.remote.UsuarioApi
import cl.duoc.rinconsur.probadores.data.remote.aCuenta
import cl.duoc.rinconsur.probadores.data.remote.llamar
import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.ErrorServidorException
import cl.duoc.rinconsur.probadores.model.SinContrasenaException

// Ingreso y contraseñas contra usuarios-service (a través del API Gateway).
class CuentasRemotas(private val api: UsuarioApi) : ServicioCuentas {

    override suspend fun buscar(rut: String): CuentaEncontrada? =
        try {
            val u = llamar { api.obtener(rut) }
            CuentaEncontrada(u.aCuenta(), u.tieneContrasena)
        } catch (e: ErrorServidorException) {
            if (e.codigo == 404) null else throw e
        }

    override suspend fun iniciarSesion(rut: String, contrasena: String) {
        sinContrasenaSi409 { llamar { api.iniciarSesion(LoginDto(rut, contrasena)) } }
    }

    override suspend fun crearContrasena(rut: String, contrasena: String): String =
        codigo(llamar { api.crearContrasena(rut, ContrasenaDto(contrasena)) })

    override suspend fun recuperar(rut: String, codigo: String, nueva: String): String =
        sinContrasenaSi409 { codigo(llamar { api.recuperar(rut, RecuperarDto(codigo, nueva)) }) }

    override suspend fun cambiarContrasena(rut: String, actual: String, nueva: String): String =
        codigo(llamar { api.cambiarContrasena(rut, CambiarContrasenaDto(actual, nueva)) })

    override suspend fun restablecer(rut: String) {
        llamar { api.restablecerContrasena(rut) }
    }

    override suspend fun verificar(rut: String, contrasena: String): Boolean =
        try {
            llamar { api.iniciarSesion(LoginDto(rut, contrasena)) }
            true
        } catch (e: ErrorServidorException) {
            false
        }

    override suspend fun registrarCliente(cliente: Cliente, contrasena: String): String =
        codigo(llamar { api.registrarCliente(RegistroDto(cliente.rut, cliente.nombre, contrasena)) })

    override suspend fun configuracionInicial(jefatura: Colaborador, contrasena: String): String =
        codigo(llamar {
            api.configuracionInicial(ConfiguracionInicialDto(jefatura.rut, jefatura.nombre, jefatura.turno.name, contrasena))
        })

    private fun codigo(cuenta: CuentaDto): String = cuenta.codigoRecuperacion ?: ""

    // usuarios-service responde 409 cuando la cuenta aún no tiene contraseña.
    private suspend fun <T> sinContrasenaSi409(bloque: suspend () -> T): T =
        try {
            bloque()
        } catch (e: ErrorServidorException) {
            if (e.codigo == 409) throw SinContrasenaException() else throw e
        }
}
