package cl.duoc.rinconsur.probadores

import cl.duoc.rinconsur.probadores.data.DatosEjemplo
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.EstimadorEspera
import cl.duoc.rinconsur.probadores.model.FRANJAS
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.ResumenCalculator
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.utils.Tiempo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasNegocioTest {
    private val ahora = Tiempo.millis(2026, 10, 24, 12, 0)
    private val asistente = DatosEjemplo.RUT_FUENTES
    private val jefatura = DatosEjemplo.RUT_MORALES

    private fun seccion(): Seccion = DatosEjemplo.crear(ahora)

    @Test
    fun `los RUT del anexo 9 tienen digito verificador valido (RN-13)`() {
        listOf("16.842.311-K", "19.234.587-1", "17.550.982-8", "20.118.436-3",
            "15.763.204-3", "12.984.561-9", "18.447.093-4").forEach {
            assertTrue("RUT $it", RutValidator.esValido(it))
        }
        assertFalse(RutValidator.esValido("16.842.311-1"))
        assertEquals("16842311-K", RutValidator.normalizar("16.842.311-k"))
    }

    @Test
    fun `HU-01 CA-1 estimado con 4 personas y 7 probadores habilitados`() {
        assertEquals(7, EstimadorEspera.minutos(4, 7, 8.0, 1))
    }

    @Test
    fun `HU-01 CA-3 con menos de 10 usos se usa el tiempo de referencia`() {
        assertEquals(8.0, EstimadorEspera.promedioUso(emptyList(), 8), 0.0)
    }

    @Test
    fun `HU-01 CA-2 con probadores libres y sin fila no hay que esperar`() {
        assertEquals(0, EstimadorEspera.enSeccion(seccion(), 0, ahora))
    }

    @Test
    fun `HU-02 CA-2 no se registra un turno con 8 prendas (RN-02)`() {
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.registrarTurno(seccion(), 8, false, asistente, ahora, ignorarHorario = false)
        }
    }

    @Test
    fun `HU-02 CA-1 la numeracion del dia parte en T-001`() {
        val (_, turno) = ReglasSeccion.registrarTurno(seccion(), 3, false, asistente, ahora, false)
        assertEquals("T-001", turno.codigo)
        assertEquals(EstadoTurno.EN_ESPERA, turno.estado)
    }

    @Test
    fun `RN-12 no se aceptan ingresos desde las 20 45`() {
        val tarde = Tiempo.millis(2026, 10, 24, 20, 50)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.registrarTurno(seccion(), 2, false, asistente, tarde, ignorarHorario = false)
        }
    }

    @Test
    fun `HU-03 asignar ocupa el probador y atiende el turno, y no se puede asignar dos veces`() {
        val (s1, turno) = ReglasSeccion.registrarTurno(seccion(), 5, false, asistente, ahora, false)
        val s2 = ReglasSeccion.asignarProbador(s1, "P-03", turno.id, null, asistente, ahora + 60_000)
        assertEquals(EstadoProbador.OCUPADO, s2.probadores.first { it.codigo == "P-03" }.estado)
        assertEquals(EstadoTurno.ATENDIDO, s2.turnos.first { it.id == turno.id }.estado)

        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.asignarProbador(s2, "P-03", null, 2, asistente, ahora + 90_000)
        }
    }

    @Test
    fun `HU-03 CA-2 no se asigna un probador fuera de servicio`() {
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.asignarProbador(seccion(), "P-06", null, 2, asistente, ahora)
        }
    }

    @Test
    fun `HU-04 no se devuelven mas prendas de las ingresadas y el probador pasa a ordenamiento`() {
        val s1 = ReglasSeccion.asignarProbador(seccion(), "P-03", null, 5, asistente, ahora)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.terminarUso(s1, "P-03", 6, asistente, ahora + 600_000)
        }
        val s2 = ReglasSeccion.terminarUso(s1, "P-03", 3, asistente, ahora + 600_000)
        assertEquals(EstadoProbador.EN_ORDENAMIENTO, s2.probadores.first { it.codigo == "P-03" }.estado)
        val s3 = ReglasSeccion.finalizarOrdenamiento(s2, "P-03", asistente, ahora + 660_000)
        assertEquals(EstadoProbador.LIBRE, s3.probadores.first { it.codigo == "P-03" }.estado)
    }

    @Test
    fun `HU-08 CA-2 un asistente no puede marcar fuera de servicio`() {
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.marcarFueraDeServicio(seccion(), "P-02", "Cortina rota", null, asistente, ahora)
        }
        val s = ReglasSeccion.marcarFueraDeServicio(seccion(), "P-02", "Cortina rota", null, jefatura, ahora)
        assertEquals(EstadoProbador.FUERA_DE_SERVICIO, s.probadores.first { it.codigo == "P-02" }.estado)
    }

    @Test
    fun `RN-13 un colaborador inactivo no puede registrar acciones`() {
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.registrarTurno(seccion(), 2, false, DatosEjemplo.RUT_TORRES, ahora, false)
        }
    }

    @Test
    fun `HU-10 el turno preferente queda primero para P-08`() {
        val (s1, normal) = ReglasSeccion.registrarTurno(seccion(), 2, false, asistente, ahora, false)
        val (s2, preferente) = ReglasSeccion.registrarTurno(s1, 2, true, asistente, ahora + 1_000, false)
        val fila = ReglasSeccion.filaDelDia(s2, Tiempo.fechaIso(ahora))
        val p08 = s2.probadores.first { it.codigo == "P-08" }
        assertEquals(preferente.id, ReglasSeccion.siguienteTurnoPara(p08, fila, s2.probadores)?.id)

        val p01 = s2.probadores.first { it.codigo == "P-01" }
        assertEquals(normal.id, ReglasSeccion.siguienteTurnoPara(p01, fila, s2.probadores)?.id)
    }

    @Test
    fun `RN-07 no se presento solo despues de 2 minutos del llamado`() {
        val (s1, turno) = ReglasSeccion.registrarTurno(seccion(), 2, false, asistente, ahora, false)
        val s2 = ReglasSeccion.llamarTurno(s1, turno.id, asistente, ahora)
        assertThrows(ReglaNegocioException::class.java) {
            ReglasSeccion.registrarAbandono(s2, turno.id, MotivoAbandono.NO_INFORMADO, asistente, ahora + 60_000, noSePresento = true)
        }
        val s3 = ReglasSeccion.registrarAbandono(s2, turno.id, MotivoAbandono.NO_INFORMADO, asistente, ahora + 121_000, noSePresento = true)
        assertEquals(EstadoTurno.NO_SE_PRESENTO, s3.turnos.first { it.id == turno.id }.estado)
    }

    @Test
    fun `Anexo A resumen del 26-09 con 25 por ciento de abandono y conversion 6 de 9`() {
        val s = seccion()
        val r = ResumenCalculator.calcular(s.turnos, s.usos, "2026-09-26", FRANJAS[0], 27990)
        assertEquals(12, r.ingresos)
        assertEquals(3, r.abandonos)
        assertEquals(25.0, r.porcentajeAbandono!!, 0.01)
        assertEquals(6.0 / 9.0, r.conversionDia!!, 0.001)

        assertEquals(55980.0, r.ventaPerdida, 1.0)
    }

    @Test
    fun `RN-09 comprar sin probarse no cuenta como venta perdida`() {
        val s = seccion()
        val r = ResumenCalculator.calcular(s.turnos, s.usos, "2026-10-03", FRANJAS[0], 27990)
        assertEquals(10, r.ingresos)
        assertEquals(3, r.abandonos)
        assertEquals(2, r.abandonosVentaPerdida)
    }

    @Test
    fun `cerrar jornada deja los turnos en espera como cierre de tienda`() {
        val (s1, turno) = ReglasSeccion.registrarTurno(seccion(), 2, false, asistente, ahora, false)
        val (s2, cerrados) = ReglasSeccion.cerrarJornada(s1, jefatura, ahora)
        assertTrue(cerrados >= 1)
        val t = s2.turnos.first { it.id == turno.id }
        assertEquals(MotivoAbandono.CIERRE_TIENDA, t.motivoAbandono)
        assertNull(s2.turnos.firstOrNull { it.estado.abierto })
    }
}
