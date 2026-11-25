package cl.duoc.rinconsur.probadores

import cl.duoc.rinconsur.probadores.model.Cliente
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasAcceso
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.model.cuenta
import cl.duoc.rinconsur.probadores.navigation.Ruta
import cl.duoc.rinconsur.probadores.utils.Contrasenas
import cl.duoc.rinconsur.probadores.utils.RutVisualTransformation
import cl.duoc.rinconsur.probadores.utils.Tiempo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// Cuentas de cliente (las crea el cliente), turno tomado por el cliente y permisos por rol.
class CuentasYRolesTest {
    private val ahora = Tiempo.millis(2026, 11, 12, 12, 0)
    private val fecha = Tiempo.fechaIso(ahora)
    private val jefa = Colaborador("12984561-9", "Patricia Morales", Perfil.JEFATURA, TurnoLaboral.MANANA)
    private val asistente = Colaborador("16842311-K", "Camila Fuentes", Perfil.ASISTENTE, TurnoLaboral.TARDE)
    private val vendedor = Colaborador("17550982-8", "Javiera Muñoz", Perfil.VENDEDOR, TurnoLaboral.TARDE)
    private val ana = Cliente("19234587-1", "Ana Pérez", ahora)
    private val luis = Cliente("20118436-3", "Luis Soto", ahora)

    private fun seccion(): Seccion {
        var s = ReglasSeccion.configurarInicial(Seccion(), jefa, 2, 1, ahora)
        s = ReglasSeccion.guardarColaborador(s, asistente, jefa.rut, esNuevo = true)
        s = ReglasSeccion.guardarColaborador(s, vendedor, jefa.rut, esNuevo = true)
        s = ReglasSeccion.registrarCliente(s, ana)
        return ReglasSeccion.registrarCliente(s, luis)
    }

    // ------------------------------ CUENTAS ------------------------------

    @Test
    fun `el cliente crea su cuenta y queda como cliente, no como colaborador`() {
        val s = seccion()
        assertEquals(listOf(ana, luis), s.clientes)
        assertTrue(s.cuenta(ana.rut)!!.esCliente)
        assertEquals("Cliente", s.cuenta(ana.rut)!!.rol)
        assertEquals(Perfil.VENDEDOR, s.cuenta(vendedor.rut)!!.perfil)
        assertNull(s.cuenta("11111111-1"))
    }

    @Test
    fun `un RUT no puede tener cuenta de cliente y de colaborador a la vez`() {
        val s = seccion()
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.registrarCliente(s, Cliente(vendedor.rut, "Otra Persona", ahora))
        }
        assertThrows(ReglaNegocioException::class.java) { ReglasSeccion.registrarCliente(s, ana) }
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.guardarColaborador(
                s, Colaborador(ana.rut, "Ana Pérez", Perfil.ASISTENTE, TurnoLaboral.TARDE), jefa.rut, esNuevo = true
            )
        }
    }

    @Test
    fun `no se crean cuentas de cliente antes de configurar la seccion`() {
        assertThrows(ReglaNegocioException::class.java) { ReglasSeccion.registrarCliente(Seccion(), ana) }
    }

    @Test
    fun `cada cuenta edita solo sus datos propios`() {
        var s = ReglasSeccion.actualizarNombreCliente(seccion(), ana.rut, "Ana María Pérez")
        assertEquals("Ana María Pérez", s.clientes.first { it.rut == ana.rut }.nombre)

        s = ReglasSeccion.actualizarDatosPropios(s, vendedor.rut, "Javiera Paz Muñoz", TurnoLaboral.MANANA)
        val editado = s.colaboradores.first { it.rut == vendedor.rut }
        assertEquals("Javiera Paz Muñoz", editado.nombre)
        assertEquals(TurnoLaboral.MANANA, editado.turno)
        // El perfil no cambia al editar los datos propios.
        assertEquals(Perfil.VENDEDOR, editado.perfil)
    }

    // ------------------------- TURNO DEL CLIENTE -------------------------

    @Test
    fun `el cliente toma su turno y queda en la fila a su nombre`() {
        val (s, turno) = ReglasSeccion.tomarTurnoCliente(seccion(), ana.rut, 3, false, ahora, false)
        assertEquals(1, turno.numero)
        assertEquals(ana.rut, turno.clienteRut)
        assertNull(turno.registradoPor)
        assertEquals(turno, ReglasSeccion.turnoAbiertoDeCliente(s, ana.rut, fecha))
        assertEquals(listOf(turno), ReglasSeccion.filaDelDia(s, fecha))
    }

    @Test
    fun `el cliente solo puede tener un turno abierto y respeta el maximo de prendas`() {
        val (s, _) = ReglasSeccion.tomarTurnoCliente(seccion(), ana.rut, 3, false, ahora, false)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.tomarTurnoCliente(s, ana.rut, 2, false, ahora, false)
        }
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.tomarTurnoCliente(seccion(), ana.rut, 7, false, ahora, false)
        }
        // Otro cliente sí puede tomar el suyo, y un colaborador no puede usar esta vía.
        assertEquals(2, ReglasSeccion.tomarTurnoCliente(s, luis.rut, 1, false, ahora, false).second.numero)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.tomarTurnoCliente(s, vendedor.rut, 1, false, ahora, false)
        }
    }

    @Test
    fun `el cliente no toma turno fuera del horario de la fila`() {
        val noche = Tiempo.millis(2026, 11, 12, 22, 0)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.tomarTurnoCliente(seccion(), ana.rut, 2, false, noche, false)
        }
        // En modo demostración se ignora el horario.
        assertEquals(1, ReglasSeccion.tomarTurnoCliente(seccion(), ana.rut, 2, false, noche, true).second.numero)
    }

    @Test
    fun `el cliente se retira y puede volver a tomar turno`() {
        val (conTurno, turno) = ReglasSeccion.tomarTurnoCliente(seccion(), ana.rut, 3, false, ahora, false)
        val s = ReglasSeccion.retirarTurnoCliente(conTurno, ana.rut, MotivoAbandono.ESPERA_LARGA, ahora + 60_000)
        val cerrado = s.turnos.first { it.id == turno.id }
        assertEquals(EstadoTurno.ABANDONO, cerrado.estado)
        assertEquals(MotivoAbandono.ESPERA_LARGA, cerrado.motivoAbandono)
        assertNull(ReglasSeccion.turnoAbiertoDeCliente(s, ana.rut, fecha))
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.retirarTurnoCliente(s, ana.rut, MotivoAbandono.OTRO, ahora + 120_000)
        }
        assertEquals(2, ReglasSeccion.tomarTurnoCliente(s, ana.rut, 1, false, ahora + 180_000, false).second.numero)
    }

    @Test
    fun `el personal atiende el turno que tomo el cliente`() {
        val (conTurno, turno) = ReglasSeccion.tomarTurnoCliente(seccion(), ana.rut, 2, false, ahora, false)
        val llamado = ReglasSeccion.llamarTurno(conTurno, turno.id, asistente.rut, ahora + 60_000)
        val asignado = ReglasSeccion.asignarProbador(llamado, "P-01", turno.id, null, asistente.rut, ahora + 120_000)
        assertEquals(EstadoTurno.ATENDIDO, asignado.turnos.first { it.id == turno.id }.estado)
        assertNull(ReglasSeccion.turnoAbiertoDeCliente(asignado, ana.rut, fecha))
    }

    // ------------------------------- ROLES -------------------------------

    @Test
    fun `el vendedor registra turnos y retiros pero no opera probadores`() {
        val (s, turno) = ReglasSeccion.registrarTurno(seccion(), 2, false, vendedor.rut, ahora, false)
        assertEquals(vendedor.rut, turno.registradoPor)

        assertThrows(ReglaNegocioException::class.java) { ReglasSeccion.llamarTurno(s, turno.id, vendedor.rut, ahora) }
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.asignarProbador(s, "P-01", turno.id, null, vendedor.rut, ahora)
        }
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.asignarProbador(s, "P-01", null, 2, vendedor.rut, ahora)
        }
        val retirado = ReglasSeccion.registrarAbandono(s, turno.id, MotivoAbandono.SE_HIZO_TARDE, vendedor.rut, ahora)
        assertEquals(EstadoTurno.ABANDONO, retirado.turnos.first { it.id == turno.id }.estado)
    }

    @Test
    fun `el asistente opera probadores pero no hace acciones de jefatura`() {
        val conUso = ReglasSeccion.asignarProbador(seccion(), "P-01", null, 2, asistente.rut, ahora)
        val terminado = ReglasSeccion.terminarUso(conUso, "P-01", 1, asistente.rut, ahora + 60_000)
        ReglasSeccion.finalizarOrdenamiento(terminado, "P-01", asistente.rut, ahora + 120_000)

        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.marcarFueraDeServicio(seccion(), "P-02", "Cortina rota", null, asistente.rut, ahora)
        }
        assertThrows(ReglaNegocioException::class.java) { ReglasSeccion.cerrarJornada(seccion(), asistente.rut, ahora) }
    }

    @Test
    fun `cada tipo de cuenta abre en su propia vista`() {
        assertEquals(Ruta.ClienteInicio, Ruta.inicioPara(null))
        assertEquals(Ruta.Fila, Ruta.inicioPara(Perfil.VENDEDOR))
        assertEquals(Ruta.Probadores, Ruta.inicioPara(Perfil.ASISTENTE))
        assertEquals(Ruta.Resumen, Ruta.inicioPara(Perfil.JEFATURA))
    }

    // ------------------------------ CONTRASEÑAS Y BLOQUEO ------------------------------

    private val rutAcceso = "12984561-9"
    private val ahoraAcceso = 1_000_000L

    @Test
    fun `la contrasena se guarda como hash y se verifica`() {
        val (c, _) = ReglasAcceso.crear(rutAcceso, "clave123")
        assertNotEquals("clave123", c.hash)
        assertTrue(ReglasAcceso.contrasenaCorrecta(c, "clave123"))
        assertFalse(ReglasAcceso.contrasenaCorrecta(c, "clave124"))
    }

    @Test
    fun `la misma contrasena genera hashes distintos por la sal`() {
        val (a, _) = ReglasAcceso.crear(rutAcceso, "clave123")
        val (b, _) = ReglasAcceso.crear(rutAcceso, "clave123")
        assertNotEquals(a.hash, b.hash)
    }

    @Test
    fun `el codigo de recuperacion sirve con o sin guion y en minusculas`() {
        val (c, codigo) = ReglasAcceso.crear(rutAcceso, "clave123")
        assertTrue(Regex("[A-Z2-9]{4}-[A-Z2-9]{4}").matches(codigo))
        assertTrue(ReglasAcceso.codigoCorrecto(c, codigo))
        assertTrue(ReglasAcceso.codigoCorrecto(c, codigo.replace("-", "").lowercase()))
        assertFalse(ReglasAcceso.codigoCorrecto(c, "AAAA-AAAA"))
    }

    @Test
    fun `cinco fallos bloquean un minuto y luego se libera`() {
        var c = ReglasAcceso.crear(rutAcceso, "clave123").first
        repeat(4) { c = ReglasAcceso.registrarFallo(c, ahoraAcceso) }
        assertEquals(1, ReglasAcceso.intentosRestantes(c))
        assertEquals(0, ReglasAcceso.segundosBloqueo(c, ahoraAcceso))

        c = ReglasAcceso.registrarFallo(c, ahoraAcceso)
        assertEquals(60, ReglasAcceso.segundosBloqueo(c, ahoraAcceso))
        assertEquals(0, ReglasAcceso.segundosBloqueo(c, ahoraAcceso + ReglasAcceso.MILIS_BLOQUEO))
    }

    @Test
    fun `un ingreso correcto reinicia los intentos`() {
        var c = ReglasAcceso.crear(rutAcceso, "clave123").first
        repeat(3) { c = ReglasAcceso.registrarFallo(c, ahoraAcceso) }
        c = ReglasAcceso.registrarExito(c)
        assertEquals(ReglasAcceso.MAX_INTENTOS, ReglasAcceso.intentosRestantes(c))
    }

    @Test
    fun `reglas para una contrasena nueva`() {
        assertEquals("Campo obligatorio", Contrasenas.validarNueva("", "").first)
        assertEquals("Debe tener al menos 6 caracteres", Contrasenas.validarNueva("ab1", "ab1").first)
        assertEquals("Debe tener al menos una letra y un número", Contrasenas.validarNueva("abcdefg", "abcdefg").first)
        assertEquals("Las contraseñas no coinciden", Contrasenas.validarNueva("clave123", "clave12").second)
        assertEquals(null to null, Contrasenas.validarNueva("clave123", "clave123"))
    }

    @Test
    fun `el RUT se dibuja con puntos y guion mientras se escribe`() {
        assertEquals("1", RutVisualTransformation.formatear("1").first)
        assertEquals("12-9", RutVisualTransformation.formatear("129").first)
        assertEquals("1.298-4", RutVisualTransformation.formatear("12984").first)
        assertEquals("12.984.561-9", RutVisualTransformation.formatear("129845619").first)
        assertEquals("16.842.311-K", RutVisualTransformation.formatear("16842311K").first)
        // El cursor al final del texto crudo queda al final del texto con formato.
        val (texto, mapa) = RutVisualTransformation.formatear("129845619")
        assertEquals(texto.length, mapa[9])
    }
}
