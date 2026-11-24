package cl.duoc.rinconsur.probadores.data.cuentas

import cl.duoc.rinconsur.probadores.data.SeccionRepository
import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.Credencial
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasAcceso
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.SinContrasenaException
import cl.duoc.rinconsur.probadores.model.cuenta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// Ingreso y contraseñas sin servidor: hashes PBKDF2 en Room y bloqueo tras 5 intentos.
class CuentasLocales(
    private val repositorio: SeccionRepository,
    private val reloj: () -> Long = System::currentTimeMillis
) : ServicioCuentas {

    override suspend fun buscar(rut: String): CuentaEncontrada? {
        val cuenta = repositorio.obtenerSeccion().first()?.cuenta(rut) ?: return null
        return CuentaEncontrada(cuenta, repositorio.obtenerCredencial(rut) != null)
    }

    override suspend fun iniciarSesion(rut: String, contrasena: String) {
        val credencial = repositorio.obtenerCredencial(rut) ?: throw SinContrasenaException()
        verificarConBloqueo(credencial, "Contraseña incorrecta") { ReglasAcceso.contrasenaCorrecta(it, contrasena) }
    }

    override suspend fun crearContrasena(rut: String, contrasena: String): String {
        if (repositorio.obtenerCredencial(rut) != null) {
            throw ReglaNegocioException("La cuenta ya tiene contraseña: ingrese con ella.")
        }
        return guardarNueva(rut, contrasena)
    }

    override suspend fun recuperar(rut: String, codigo: String, nueva: String): String {
        val credencial = repositorio.obtenerCredencial(rut) ?: throw SinContrasenaException()
        verificarConBloqueo(credencial, "Código incorrecto") { ReglasAcceso.codigoCorrecto(it, codigo) }
        return guardarNueva(rut, nueva)
    }

    override suspend fun cambiarContrasena(rut: String, actual: String, nueva: String): String {
        val credencial = repositorio.obtenerCredencial(rut)
            ?: throw ReglaNegocioException("Su sesión terminó. Ingrese nuevamente.")
        val ahora = reloj()
        val segundos = ReglasAcceso.segundosBloqueo(credencial, ahora)
        if (segundos > 0) throw ReglaNegocioException("Demasiados intentos. Espere $segundos segundos.")
        val correcta = withContext(Dispatchers.Default) { ReglasAcceso.contrasenaCorrecta(credencial, actual) }
        if (!correcta) {
            repositorio.guardarCredencial(ReglasAcceso.registrarFallo(credencial, ahora))
            throw ReglaNegocioException("La contraseña actual no es correcta")
        }
        return guardarNueva(rut, nueva)
    }

    override suspend fun restablecer(rut: String) = repositorio.eliminarCredencial(rut)

    override suspend fun verificar(rut: String, contrasena: String): Boolean {
        val credencial = repositorio.obtenerCredencial(rut) ?: return false
        return withContext(Dispatchers.Default) { ReglasAcceso.contrasenaCorrecta(credencial, contrasena) }
    }

    override suspend fun registrarCliente(cliente: Cliente, contrasena: String): String {
        repositorio.actualizar { s -> ReglasSeccion.registrarCliente(s, cliente) to Unit }
        return guardarNueva(cliente.rut, contrasena)
    }

    // La jefatura se agrega a la sección al terminar el asistente (ReglasSeccion.configurarInicial).
    override suspend fun configuracionInicial(jefatura: Colaborador, contrasena: String): String =
        guardarNueva(jefatura.rut, contrasena)

    // Revisa el bloqueo, verifica (el hash es lento: va fuera del hilo principal) y guarda el resultado.
    private suspend fun verificarConBloqueo(
        credencial: Credencial,
        textoError: String,
        verificar: (Credencial) -> Boolean
    ) {
        val ahora = reloj()
        val segundos = ReglasAcceso.segundosBloqueo(credencial, ahora)
        if (segundos > 0) throw ReglaNegocioException("Demasiados intentos. Espere $segundos segundos e intente de nuevo.")

        val correcto = withContext(Dispatchers.Default) { verificar(credencial) }
        if (correcto) {
            repositorio.guardarCredencial(ReglasAcceso.registrarExito(credencial))
            return
        }
        val actualizada = ReglasAcceso.registrarFallo(credencial, ahora)
        repositorio.guardarCredencial(actualizada)
        throw ReglaNegocioException(
            if (actualizada.bloqueadaHasta > ahora) {
                "Demasiados intentos. Espere ${ReglasAcceso.MILIS_BLOQUEO / 1000} segundos e intente de nuevo."
            } else {
                val quedan = ReglasAcceso.intentosRestantes(actualizada)
                "$textoError. Le queda${if (quedan == 1) "" else "n"} $quedan intento${if (quedan == 1) "" else "s"}."
            }
        )
    }

    private suspend fun guardarNueva(rut: String, contrasena: String): String {
        val (credencial, codigo) = withContext(Dispatchers.Default) { ReglasAcceso.crear(rut, contrasena) }
        repositorio.guardarCredencial(credencial)
        return codigo
    }
}
