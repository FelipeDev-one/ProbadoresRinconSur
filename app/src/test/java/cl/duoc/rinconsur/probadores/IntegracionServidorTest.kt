package cl.duoc.rinconsur.probadores

import cl.duoc.rinconsur.probadores.data.cuentas.CuentasRemotas
import cl.duoc.rinconsur.probadores.data.remote.ActualizarUsuarioDto
import cl.duoc.rinconsur.probadores.data.remote.ApiServidor
import cl.duoc.rinconsur.probadores.data.remote.CambiarContrasenaDto
import cl.duoc.rinconsur.probadores.data.remote.CambioEstadoDto
import cl.duoc.rinconsur.probadores.data.remote.ColaboradorNuevoDto
import cl.duoc.rinconsur.probadores.data.remote.ConfiguracionInicialDto
import cl.duoc.rinconsur.probadores.data.remote.ContrasenaDto
import cl.duoc.rinconsur.probadores.data.remote.CuentaDto
import cl.duoc.rinconsur.probadores.data.remote.DescargaRemota
import cl.duoc.rinconsur.probadores.data.remote.EnviadorRemoto
import cl.duoc.rinconsur.probadores.data.remote.EstadoInicialDto
import cl.duoc.rinconsur.probadores.data.remote.LoginDto
import cl.duoc.rinconsur.probadores.data.remote.OperacionRemota
import cl.duoc.rinconsur.probadores.data.remote.ParametrosDto
import cl.duoc.rinconsur.probadores.data.remote.PlanSincronizacion
import cl.duoc.rinconsur.probadores.data.remote.ProbadorApi
import cl.duoc.rinconsur.probadores.data.remote.ProbadorDto
import cl.duoc.rinconsur.probadores.data.remote.RecuperarDto
import cl.duoc.rinconsur.probadores.data.remote.RegistroDto
import cl.duoc.rinconsur.probadores.data.remote.RetrofitClient
import cl.duoc.rinconsur.probadores.data.remote.TurnoApi
import cl.duoc.rinconsur.probadores.data.remote.TurnoDto
import cl.duoc.rinconsur.probadores.data.remote.UsoDto
import cl.duoc.rinconsur.probadores.data.remote.UsuarioApi
import cl.duoc.rinconsur.probadores.data.remote.UsuarioDto
import cl.duoc.rinconsur.probadores.data.remote.aDto
import cl.duoc.rinconsur.probadores.data.remote.aModelo
import cl.duoc.rinconsur.probadores.data.remote.mensajeDeError
import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.ErrorServidorException
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.SinConexionException
import cl.duoc.rinconsur.probadores.model.SinContrasenaException
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

// =====================================================================
//  Pruebas de la integración con los microservicios, SIN servidor real:
//  las APIs de Retrofit se reemplazan por "Fake" en memoria (mismo patrón
//  que FakeUsuarioApi del curso). sinConexion = true simula que el servidor
//  no responde (IOException).
// =====================================================================
class IntegracionServidorTest {

    // ----------------------------------------------------------- FAKES

    private fun errorHttp(codigo: Int, mensaje: String): HttpException {
        val cuerpo = ResponseBody.create(MediaType.parse("application/json"), """{"estado":$codigo,"mensaje":"$mensaje"}""")
        return HttpException(Response.error<Any>(codigo, cuerpo))
    }

    private inner class FakeUsuarioApi : UsuarioApi {
        var sinConexion = false
        val usuarios = mutableMapOf<String, UsuarioDto>()
        val llamadas = mutableListOf<String>()
        var claveCorrecta = "clave123"

        private fun revisar(llamada: String) {
            llamadas += llamada
            if (sinConexion) throw IOException("sin red")
        }

        override suspend fun listar() = usuarios.values.toList().also { revisar("listar") }
        override suspend fun obtener(rut: String): UsuarioDto {
            revisar("obtener")
            return usuarios[rut] ?: throw errorHttp(404, "No existe")
        }
        override suspend fun estadoInicial() = EstadoInicialDto(usuarios.isNotEmpty())
        override suspend fun configuracionInicial(datos: ConfiguracionInicialDto): CuentaDto {
            revisar("configuracion")
            val u = UsuarioDto(1, datos.rut, datos.nombre, "JEFATURA", datos.turnoLaboral, true, true)
            usuarios[datos.rut] = u
            return CuentaDto(u, "AAAA-BBBB")
        }
        override suspend fun registrarCliente(datos: RegistroDto): CuentaDto {
            revisar("registro")
            if (datos.rut in usuarios) throw errorHttp(409, "Ya existe una cuenta con el RUT ${datos.rut}")
            val u = UsuarioDto(2, datos.rut, datos.nombre, "CLIENTE", null, true, true)
            usuarios[datos.rut] = u
            return CuentaDto(u, "CCCC-DDDD")
        }
        override suspend fun crearColaborador(datos: ColaboradorNuevoDto): UsuarioDto {
            revisar("crearColaborador")
            if (datos.rut in usuarios) throw errorHttp(409, "Ya existe")
            return UsuarioDto(3, datos.rut, datos.nombre, datos.rol, datos.turnoLaboral).also { usuarios[datos.rut] = it }
        }
        override suspend fun actualizar(rut: String, datos: ActualizarUsuarioDto): UsuarioDto {
            revisar("actualizar")
            val u = usuarios.getValue(rut)
            return u.copy(nombre = datos.nombre ?: u.nombre).also { usuarios[rut] = it }
        }
        override suspend fun iniciarSesion(datos: LoginDto): UsuarioDto {
            revisar("login")
            val u = usuarios[datos.rut] ?: throw errorHttp(401, "RUT o contraseña incorrectos")
            if (!u.tieneContrasena) throw errorHttp(409, "La cuenta aún no tiene contraseña")
            if (datos.contrasena != claveCorrecta) throw errorHttp(401, "Contraseña incorrecta. Le quedan 4 intentos.")
            return u
        }
        override suspend fun crearContrasena(rut: String, datos: ContrasenaDto) =
            CuentaDto(usuarios.getValue(rut), "EEEE-FFFF").also { revisar("crearContrasena") }
        override suspend fun cambiarContrasena(rut: String, datos: CambiarContrasenaDto) =
            CuentaDto(usuarios.getValue(rut), "GGGG-HHHH").also { revisar("cambiarContrasena") }
        override suspend fun recuperar(rut: String, datos: RecuperarDto): CuentaDto {
            revisar("recuperar")
            if (usuarios[rut]?.tieneContrasena != true) throw errorHttp(409, "Sin contraseña")
            return CuentaDto(usuarios.getValue(rut), "JJJJ-KKKK")
        }
        override suspend fun restablecerContrasena(rut: String) = revisar("restablecer")
    }

    private inner class FakeProbadorApi : ProbadorApi {
        var sinConexion = false
        val probadores = mutableMapOf<String, ProbadorDto>()
        val usos = mutableListOf<UsoDto>()
        val cambios = mutableListOf<CambioEstadoDto>()
        val llamadas = mutableListOf<String>()

        private fun revisar(llamada: String) {
            llamadas += llamada
            if (sinConexion) throw IOException("sin red")
        }

        override suspend fun listar() = probadores.values.sortedBy { it.codigo }.also { revisar("listar") }
        override suspend fun guardar(codigo: String, datos: ProbadorDto) = datos.also { revisar("guardar $codigo"); probadores[codigo] = it }
        override suspend fun eliminar(codigo: String) { revisar("eliminar $codigo"); probadores.remove(codigo) }
        override suspend fun usos() = usos.toList().also { revisar("usos") }
        override suspend fun guardarUso(id: String, datos: UsoDto) = datos.also { revisar("uso $id"); usos += it }
        override suspend fun cambios() = cambios.toList().also { revisar("cambios") }
        override suspend fun registrarCambio(datos: CambioEstadoDto) = datos.also { revisar("cambio"); cambios += it }
    }

    private inner class FakeTurnoApi : TurnoApi {
        val turnos = mutableMapOf<String, TurnoDto>()
        var parametros = Parametros().aDto()
        val llamadas = mutableListOf<String>()

        override suspend fun listar() = turnos.values.toList()
        override suspend fun guardar(id: String, datos: TurnoDto) = datos.also { llamadas += "turno $id"; turnos[id] = it }
        override suspend fun parametros() = parametros
        override suspend fun guardarParametros(datos: ParametrosDto) = datos.also { llamadas += "parametros"; parametros = it }
    }

    private val usuariosApi = FakeUsuarioApi()
    private val probadoresApi = FakeProbadorApi()
    private val turnosApi = FakeTurnoApi()
    private val api = ApiServidor(usuariosApi, probadoresApi, turnosApi)

    // ----------------------------------------------------------- DATOS

    private val jefa = Colaborador("12984561-9", "Patricia Morales", Perfil.JEFATURA, TurnoLaboral.MANANA)
    private val asistente = Colaborador("16842311-K", "Camila Fuentes", Perfil.ASISTENTE, TurnoLaboral.TARDE)
    private val p01 = Probador("P-01", TipoProbador.ESTANDAR, EstadoProbador.LIBRE, 0L)
    private val p02 = Probador("P-02", TipoProbador.ACCESIBLE, EstadoProbador.LIBRE, 0L)
    private val turno = Turno("t1", 1, "2026-11-24", 10L, false, 3, registradoPor = asistente.rut)
    private val base = Seccion(probadores = listOf(p01, p02), colaboradores = listOf(jefa))

    // ----------------------------------------------------------- PLAN

    @Test
    fun sinCambiosNoSeLlamaAlServidor() {
        assertTrue(PlanSincronizacion.operaciones(base, base).isEmpty())
    }

    @Test
    fun elPlanEnviaSoloLoQueCambioYEnOrden() {
        val ocupado = p01.copy(estado = EstadoProbador.OCUPADO, desde = 5L, usoActivoId = "u1")
        val despues = base.copy(
            colaboradores = listOf(jefa.copy(nombre = "Patricia Elena Morales"), asistente),
            probadores = listOf(ocupado),
            turnos = listOf(turno),
            parametros = Parametros(maxPrendas = 8)
        )
        val ops = PlanSincronizacion.operaciones(base, despues)
        assertEquals(
            listOf(
                OperacionRemota.ActualizarColaborador(despues.colaboradores[0]),
                OperacionRemota.CrearColaborador(asistente),
                OperacionRemota.GuardarProbador(ocupado),
                OperacionRemota.GuardarTurno(turno),
                OperacionRemota.GuardarParametros(Parametros(maxPrendas = 8)),
                OperacionRemota.EliminarProbador("P-02")
            ),
            ops
        )
    }

    @Test
    fun losClientesNuevosNoVanEnElPlanPeroSuNombreSi() {
        val cliente = Cliente("19234587-1", "Ana Soto", 1L)
        val conCliente = base.copy(clientes = listOf(cliente))
        assertTrue(PlanSincronizacion.operaciones(base, conCliente).isEmpty())

        val renombrado = conCliente.copy(clientes = listOf(cliente.copy(nombre = "Ana María Soto")))
        assertEquals(
            listOf(OperacionRemota.ActualizarCliente(renombrado.clientes[0])),
            PlanSincronizacion.operaciones(conCliente, renombrado)
        )
    }

    // ----------------------------------------------------------- ENVIAR

    @Test
    fun elEnviadorLlamaALosTresServicios() = runTest {
        val despues = base.copy(colaboradores = listOf(jefa, asistente), turnos = listOf(turno))
        EnviadorRemoto(api).enviar(PlanSincronizacion.operaciones(base, despues))

        assertEquals(listOf("crearColaborador"), usuariosApi.llamadas)
        assertEquals(listOf("turno t1"), turnosApi.llamadas)
        assertEquals(asistente.rut, usuariosApi.usuarios.getValue(asistente.rut).rut)
    }

    @Test
    fun siLaJefaturaYaExisteSeActualizaEnVezDeCrearla() = runTest {
        usuariosApi.usuarios[jefa.rut] = UsuarioDto(1, jefa.rut, jefa.nombre, "JEFATURA", "MANANA", true, true)
        EnviadorRemoto(api).enviar(listOf(OperacionRemota.CrearColaborador(jefa)))
        assertEquals(listOf("crearColaborador", "actualizar"), usuariosApi.llamadas)
    }

    @Test
    fun sinConexionElEnvioFallaConUnMensajeClaro() = runTest {
        probadoresApi.sinConexion = true
        try {
            EnviadorRemoto(api).enviar(listOf(OperacionRemota.GuardarProbador(p01)))
            fail("Debía fallar sin conexión")
        } catch (e: SinConexionException) {
            assertTrue(e.message!!.contains("conexión"))
        }
    }

    @Test
    fun unErrorDelServidorLlegaConSuMensaje() = runTest {
        usuariosApi.usuarios[asistente.rut] = UsuarioDto(3, asistente.rut, asistente.nombre, "ASISTENTE")
        val otro = asistente.copy(nombre = "Otra Persona")
        // El servidor rechaza el colaborador repetido (409) y luego se actualiza: no falla.
        EnviadorRemoto(api).enviar(listOf(OperacionRemota.CrearColaborador(otro)))
        assertEquals("Otra Persona", usuariosApi.usuarios.getValue(asistente.rut).nombre)
    }

    // ----------------------------------------------------------- DESCARGAR

    @Test
    fun laDescargaArmaLaSeccionCompleta() = runTest {
        usuariosApi.usuarios[jefa.rut] = UsuarioDto(1, jefa.rut, jefa.nombre, "JEFATURA", "MANANA", true, true)
        usuariosApi.usuarios["19234587-1"] = UsuarioDto(2, "19234587-1", "Ana Soto", "CLIENTE", null, true, true)
        usuariosApi.usuarios[asistente.rut] = UsuarioDto(3, asistente.rut, asistente.nombre, "ASISTENTE", "TARDE", true, false)
        probadoresApi.probadores["P-01"] = p01.aDto()
        turnosApi.turnos["t1"] = turno.aDto()
        turnosApi.parametros = Parametros(maxPrendas = 5).aDto()

        val remoto = DescargaRemota(api).descargar(mapOf("19234587-1" to 99L))

        assertEquals(listOf(jefa.rut, asistente.rut), remoto.seccion.colaboradores.map { it.rut })
        assertEquals(99L, remoto.seccion.clientes.single().creado)
        assertEquals(listOf(p01), remoto.seccion.probadores)
        assertEquals(listOf(turno), remoto.seccion.turnos)
        assertEquals(5, remoto.seccion.parametros.maxPrendas)
        assertEquals(setOf(jefa.rut, "19234587-1"), remoto.rutsConContrasena)
    }

    @Test(expected = SinConexionException::class)
    fun laDescargaSinConexionAvisa() = runTest {
        usuariosApi.sinConexion = true
        DescargaRemota(api).descargar()
    }

    // ----------------------------------------------------------- CUENTAS

    @Test
    fun cuentasRemotasBuscanYRegistran() = runTest {
        val cuentas = CuentasRemotas(usuariosApi)
        assertNull(cuentas.buscar("19234587-1"))

        val codigo = cuentas.registrarCliente(Cliente("19234587-1", "Ana Soto", 0L), "clave123")
        assertEquals("CCCC-DDDD", codigo)
        val encontrada = cuentas.buscar("19234587-1")!!
        assertTrue(encontrada.cuenta.esCliente)
        assertTrue(encontrada.tieneContrasena)

        try {
            cuentas.registrarCliente(Cliente("19234587-1", "Ana Soto", 0L), "clave123")
            fail("RUT repetido")
        } catch (e: ErrorServidorException) {
            assertEquals(409, e.codigo)
            assertEquals("Ya existe una cuenta con el RUT 19234587-1", e.message)
        }
    }

    @Test
    fun ingresoRemotoConClaveIncorrectaSinClaveYCorrecta() = runTest {
        val cuentas = CuentasRemotas(usuariosApi)
        assertEquals("AAAA-BBBB", cuentas.configuracionInicial(jefa, "clave123"))

        try {
            cuentas.iniciarSesion(jefa.rut, "mala")
            fail("Clave incorrecta")
        } catch (e: ErrorServidorException) {
            assertEquals("Contraseña incorrecta. Le quedan 4 intentos.", e.message)
        }
        cuentas.iniciarSesion(jefa.rut, "clave123")
        assertTrue(cuentas.verificar(jefa.rut, "clave123"))
        assertFalse(cuentas.verificar(jefa.rut, "mala"))

        usuariosApi.usuarios[asistente.rut] = UsuarioDto(3, asistente.rut, asistente.nombre, "ASISTENTE", "TARDE", true, false)
        try {
            cuentas.iniciarSesion(asistente.rut, "x")
            fail("Sin contraseña")
        } catch (e: SinContrasenaException) {
            // Se le pide crear su contraseña.
        }
        assertEquals("EEEE-FFFF", cuentas.crearContrasena(asistente.rut, "nueva123"))
        assertEquals("GGGG-HHHH", cuentas.cambiarContrasena(jefa.rut, "clave123", "otra1234"))
        assertEquals("JJJJ-KKKK", cuentas.recuperar(jefa.rut, "AAAA-BBBB", "otra1234"))
        cuentas.restablecer(asistente.rut)
        assertTrue("restablecer" in usuariosApi.llamadas)
    }

    @Test(expected = SinConexionException::class)
    fun ingresoSinConexionAvisa() = runTest {
        usuariosApi.sinConexion = true
        CuentasRemotas(usuariosApi).buscar(jefa.rut)
    }

    // ----------------------------------------------------------- UTILIDADES

    @Test
    fun losErroresDeValidacionIncluyenElCampo() {
        assertEquals(
            "Datos no válidos: prendas no puede ser más de 6",
            mensajeDeError(400, """{"estado":400,"mensaje":"Datos no válidos","campos":{"prendas":"no puede ser más de 6"}}""")
        )
        assertTrue(mensajeDeError(503, "<html>").contains("unos segundos"))
        assertEquals("El servidor respondió con un error (500).", mensajeDeError(500, null))
    }

    @Test
    fun laDireccionDelServidorSeNormalizaYValida() {
        assertEquals("http://10.0.2.2:8080/", RetrofitClient.normalizarUrl("10.0.2.2:8080"))
        assertEquals("https://abc-8080.brs.devtunnels.ms/", RetrofitClient.normalizarUrl(" https://abc-8080.brs.devtunnels.ms "))
        assertEquals("", RetrofitClient.normalizarUrl("  "))
        assertNull(RetrofitClient.validarUrl("https://abc-8080.brs.devtunnels.ms/"))
        assertNull(RetrofitClient.validarUrl(""))
        assertTrue(RetrofitClient.validarUrl("no es una url") != null)
    }

    @Test
    fun losTurnosVanYVuelvenSinPerderDatos() {
        val cerrado = turno.copy(
            estado = EstadoTurno.ABANDONO, horaCierre = 20L, motivoAbandono = MotivoAbandono.CIERRE_TIENDA, clienteRut = "19234587-1"
        )
        assertEquals(cerrado, cerrado.aDto().aModelo())
        assertEquals(Parametros(maxPrendas = 7), Parametros(maxPrendas = 7).aDto().aModelo())
    }
}
