package cl.duoc.rinconsur.probadores.data.cuentas

import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Cuenta

data class CuentaEncontrada(val cuenta: Cuenta, val tieneContrasena: Boolean)

// =====================================================================
//  ServicioCuentas — ingreso y contraseñas
//  ---------------------------------------------------------------------
//  Dos implementaciones con las mismas reglas:
//   - CuentasRemotas: usuarios-service (BCrypt y bloqueo en el servidor).
//   - CuentasLocales: sin servidor configurado, en el teléfono (PBKDF2).
//  Los ViewModels no saben cuál se está usando.
//  Los errores llegan como ReglaNegocioException con el texto para el usuario.
// =====================================================================
interface ServicioCuentas {
    suspend fun buscar(rut: String): CuentaEncontrada?

    // SinContrasenaException si la cuenta todavía no tiene contraseña.
    suspend fun iniciarSesion(rut: String, contrasena: String)

    // Cada operación que crea una contraseña devuelve un código de recuperación nuevo.
    suspend fun crearContrasena(rut: String, contrasena: String): String

    suspend fun recuperar(rut: String, codigo: String, nueva: String): String

    suspend fun cambiarContrasena(rut: String, actual: String, nueva: String): String

    suspend fun restablecer(rut: String)

    // Para salir del modo punto de espera: true si la contraseña es correcta.
    suspend fun verificar(rut: String, contrasena: String): Boolean

    suspend fun registrarCliente(cliente: Cliente, contrasena: String): String

    suspend fun configuracionInicial(jefatura: Colaborador, contrasena: String): String
}
