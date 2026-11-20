package cl.duoc.rinconsur.probadores

import cl.duoc.rinconsur.probadores.model.Alerta
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.GeneradorAlertas
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.utils.Tiempo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Alertas agrupadas: una sola alerta para todos los probadores libres mientras hay fila.
class AlertasTest {
    private val ahora = Tiempo.millis(2026, 11, 19, 12, 0)
    private val jefa = Colaborador("12984561-9", "Patricia Morales", Perfil.JEFATURA, TurnoLaboral.MANANA)

    private fun conFila(): Seccion {
        val s = ReglasSeccion.configurarInicial(Seccion(), jefa, 3, 0, ahora)
        return ReglasSeccion.registrarTurno(s, 2, false, jefa.rut, ahora, false).first
    }

    @Test
    fun `los probadores libres con fila generan una sola alerta agrupada`() {
        val s = conFila()
        val fila = ReglasSeccion.filaDelDia(s, Tiempo.fechaIso(ahora))
        val alertas = GeneradorAlertas.generar(s, fila, ahora + 5 * 60_000)

        val libres = alertas.filterIsInstance<Alerta.ProbadoresSinAsignar>()
        assertEquals(1, libres.size)
        assertEquals("3 probadores libres sin asignar", libres.first().titulo)
        assertTrue(libres.first().detalle.startsWith("P-01, P-02, P-03"))
        assertTrue(libres.first().urgente)
    }

    @Test
    fun `sin fila no hay alerta de probadores libres`() {
        val s = ReglasSeccion.configurarInicial(Seccion(), jefa, 3, 0, ahora)
        val alertas = GeneradorAlertas.generar(s, emptyList(), ahora + 5 * 60_000)
        assertTrue(alertas.none { it is Alerta.ProbadoresSinAsignar })
    }

    @Test
    fun `un solo probador libre se nombra por su codigo`() {
        var s = conFila()
        s = ReglasSeccion.asignarProbador(s, "P-01", null, 1, jefa.rut, ahora)
        s = ReglasSeccion.asignarProbador(s, "P-02", null, 1, jefa.rut, ahora)
        val fila = ReglasSeccion.filaDelDia(s, Tiempo.fechaIso(ahora))
        val alerta = GeneradorAlertas.generar(s, fila, ahora + 5 * 60_000).filterIsInstance<Alerta.ProbadoresSinAsignar>().single()
        assertEquals("P-03 libre sin asignar", alerta.titulo)
    }
}
