package cl.duoc.rinconsur.probadores

import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.utils.Tiempo
import cl.duoc.rinconsur.probadores.utils.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// La app parte vacía: estas pruebas cubren la configuración inicial y el mantenedor de probadores.
class ConfiguracionSeccionTest {
    private val ahora = Tiempo.millis(2026, 11, 12, 12, 0)
    private val jefa = Colaborador("12984561-9", "Patricia Morales", Perfil.JEFATURA, TurnoLaboral.MANANA)
    private val vendedor = Colaborador("17550982-8", "Javiera Muñoz", Perfil.VENDEDOR, TurnoLaboral.TARDE)

    private fun configurada(estandar: Int = 3, accesibles: Int = 1): Seccion =
        ReglasSeccion.configurarInicial(Seccion(), jefa, estandar, accesibles, ahora)

    @Test
    fun `una seccion vacia no esta configurada`() {
        assertFalse(ReglasSeccion.estaConfigurada(null))
        assertFalse(ReglasSeccion.estaConfigurada(Seccion()))
        assertTrue(ReglasSeccion.estaConfigurada(configurada()))
    }

    @Test
    fun `la configuracion inicial crea la jefatura y los probadores libres`() {
        val s = configurada(estandar = 3, accesibles = 1)
        assertEquals(listOf(jefa), s.colaboradores)
        assertEquals(listOf("P-01", "P-02", "P-03", "P-04"), s.probadores.map { it.codigo })
        assertTrue(s.probadores.all { it.estado == EstadoProbador.LIBRE && it.desde == ahora })
        // Los accesibles quedan al final de la numeración.
        assertEquals(listOf("P-04"), s.probadores.filter { it.tipo == TipoProbador.ACCESIBLE }.map { it.codigo })
        assertTrue(s.turnos.isEmpty() && s.usos.isEmpty())
    }

    @Test
    fun `la configuracion inicial exige jefatura y al menos un probador`() {
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.configurarInicial(Seccion(), vendedor, 3, 1, ahora)
        }
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.configurarInicial(Seccion(), jefa, 0, 0, ahora)
        }
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.configurarInicial(Seccion(), jefa, ReglasSeccion.MAX_PROBADORES, 1, ahora)
        }
    }

    @Test
    fun `no se puede configurar dos veces`() {
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.configurarInicial(configurada(), jefa, 2, 0, ahora)
        }
    }

    @Test
    fun `agregar un probador usa el primer codigo libre`() {
        val (s, nuevo) = ReglasSeccion.agregarProbador(configurada(2, 0), TipoProbador.ACCESIBLE, jefa.rut, ahora)
        assertEquals("P-03", nuevo.codigo)
        assertEquals(TipoProbador.ACCESIBLE, nuevo.tipo)

        // Si se elimina P-01, el siguiente que se agregue reutiliza ese código.
        val sinPrimero = ReglasSeccion.eliminarProbador(s, "P-01", jefa.rut)
        assertEquals("P-01", ReglasSeccion.agregarProbador(sinPrimero, TipoProbador.ESTANDAR, jefa.rut, ahora).second.codigo)
    }

    @Test
    fun `solo la jefatura administra probadores (RN-11)`() {
        val s = ReglasSeccion.guardarColaborador(configurada(), vendedor, jefa.rut, esNuevo = true)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.agregarProbador(s, TipoProbador.ESTANDAR, vendedor.rut, ahora)
        }
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.eliminarProbador(s, "P-01", vendedor.rut)
        }
    }

    @Test
    fun `cambiar el tipo de un probador`() {
        val s = ReglasSeccion.cambiarTipoProbador(configurada(2, 0), "P-02", TipoProbador.ACCESIBLE, jefa.rut)
        assertEquals(TipoProbador.ACCESIBLE, s.probadores.first { it.codigo == "P-02" }.tipo)
    }

    @Test
    fun `no se elimina un probador con usos ni el ultimo que queda`() {
        val conUso = ReglasSeccion.asignarProbador(configurada(2, 0), "P-01", null, 2, jefa.rut, ahora, 0)
        val terminado = ReglasSeccion.terminarUso(conUso, "P-01", 2, jefa.rut, ahora + 60_000)
        val libre = ReglasSeccion.finalizarOrdenamiento(terminado, "P-01", jefa.rut, ahora + 120_000)
        assertThrows(ReglaNegocioException::class.java) { ReglasSeccion.eliminarProbador(libre, "P-01", jefa.rut) }

        val unoSolo = configurada(1, 0)
        assertThrows(ReglaNegocioException::class.java) { ReglasSeccion.eliminarProbador(unoSolo, "P-01", jefa.rut) }
    }

    @Test
    fun `debe quedar al menos una jefatura activa`() {
        val degradada = jefa.copy(perfil = Perfil.VENDEDOR)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.guardarColaborador(configurada(), degradada, jefa.rut, esNuevo = false)
        }
    }

    @Test
    fun `validaciones compartidas de RUT y nombre`() {
        assertNull(Validaciones.rut("12.984.561-9"))
        assertEquals("Campo obligatorio", Validaciones.rut(""))
        assertEquals("RUT inválido: revise el dígito verificador", Validaciones.rut("12.984.561-0"))
        assertNull(Validaciones.nombrePersona("Ana Pérez"))
        assertEquals("Ingrese al menos nombre y apellido", Validaciones.nombrePersona("Ana"))
        assertEquals("Use solo letras", Validaciones.nombrePersona("Ana 123"))
        assertEquals("Ana Pérez", Validaciones.limpiarNombre("  Ana   Pérez "))
    }
}
