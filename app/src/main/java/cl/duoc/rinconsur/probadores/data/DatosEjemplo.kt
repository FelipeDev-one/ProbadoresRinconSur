package cl.duoc.rinconsur.probadores.data

import cl.duoc.rinconsur.probadores.model.CambioEstado
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.ReglaNegocioException
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.Seccion
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.model.Uso
import cl.duoc.rinconsur.probadores.utils.Tiempo
import java.util.UUID

object DatosEjemplo {
    const val RUT_FUENTES = "16842311-K"
    const val RUT_SOTO = "19234587-1"
    const val RUT_MUNOZ = "17550982-8"
    const val RUT_DIAZ = "20118436-3"
    const val RUT_REYES = "15763204-3"
    const val RUT_MORALES = "12984561-9"
    const val RUT_TORRES = "18447093-4"

    private val colaboradores = listOf(
        Colaborador(RUT_FUENTES, "Camila Andrea Fuentes Rojas", Perfil.ASISTENTE, TurnoLaboral.FIN_DE_SEMANA),
        Colaborador(RUT_SOTO, "Matías Ignacio Soto Pizarro", Perfil.ASISTENTE, TurnoLaboral.TARDE),
        Colaborador(RUT_MUNOZ, "Javiera Paz Muñoz Contreras", Perfil.VENDEDOR, TurnoLaboral.MANANA),
        Colaborador(RUT_DIAZ, "Benjamín Alonso Díaz Vera", Perfil.ASISTENTE, TurnoLaboral.FIN_DE_SEMANA),
        Colaborador(RUT_REYES, "Daniela Francisca Reyes Araya", Perfil.VENDEDOR, TurnoLaboral.TARDE),
        Colaborador(RUT_MORALES, "Patricia Elena Morales Castillo", Perfil.JEFATURA, TurnoLaboral.MANANA),
        Colaborador(RUT_TORRES, "Felipe Andrés Torres Godoy", Perfil.ASISTENTE, TurnoLaboral.TARDE, activo = false)
    )

    fun crear(ahora: Long): Seccion {
        val probadores = (1..8).map { n ->
            val codigo = "P-%02d".format(n)
            if (codigo == "P-06") {
                Probador(
                    codigo, TipoProbador.ESTANDAR, EstadoProbador.FUERA_DE_SERVICIO, ahora,
                    motivoFueraServicio = "Cortina rota"
                )
            } else {
                Probador(
                    codigo,
                    if (n == 8) TipoProbador.ACCESIBLE else TipoProbador.ESTANDAR,
                    EstadoProbador.LIBRE,
                    ahora
                )
            }
        }
        return Seccion(
            probadores = probadores,
            colaboradores = colaboradores,
            turnos = turnos03Oct() + turnos26Sep(),
            usos = usos03Oct() + usos26Sep(),
            cambios = cambios03Oct(),
            parametros = Parametros()
        )
    }

    private fun h03(hora: Int, minuto: Int) = Tiempo.millis(2026, 10, 3, hora, minuto)

    private fun turnos03Oct(): List<Turno> {
        data class Fila(
            val n: Int, val ingreso: String, val pref: Boolean, val prendas: Int,
            val estado: EstadoTurno, val cierre: String?, val motivo: MotivoAbandono?, val rut: String
        )
        val filas = listOf(
            Fila(87, "17:20", false, 4, EstadoTurno.ATENDIDO, "17:31", null, RUT_FUENTES),
            Fila(88, "17:24", true, 2, EstadoTurno.ATENDIDO, "17:39", null, RUT_FUENTES),
            Fila(89, "17:27", false, 6, EstadoTurno.ABANDONO, "17:36", MotivoAbandono.ESPERA_LARGA, RUT_FUENTES),
            Fila(90, "17:30", false, 3, EstadoTurno.ATENDIDO, "17:42", null, RUT_DIAZ),
            Fila(91, "17:33", false, 5, EstadoTurno.ATENDIDO, "17:48", null, RUT_DIAZ),
            Fila(92, "17:35", false, 1, EstadoTurno.ABANDONO, "17:40", MotivoAbandono.COMPRA_SIN_PROBARSE, RUT_FUENTES),
            Fila(93, "17:38", false, 3, EstadoTurno.ATENDIDO, "17:50", null, RUT_DIAZ),
            Fila(94, "17:41", false, 2, EstadoTurno.NO_SE_PRESENTO, "17:58", MotivoAbandono.NO_INFORMADO, RUT_FUENTES),
            Fila(95, "17:52", false, 4, EstadoTurno.EN_ESPERA, null, null, RUT_DIAZ),
            Fila(96, "17:54", false, 6, EstadoTurno.EN_ESPERA, null, null, RUT_FUENTES)
        )
        return filas.map { f ->
            Turno(
                id = "d0310-T${f.n}",
                numero = f.n,
                fecha = "2026-10-03",
                horaIngreso = hm(f.ingreso, ::h03),
                preferente = f.pref,
                prendas = f.prendas,
                estado = f.estado,
                horaCierre = f.cierre?.let { hm(it, ::h03) },
                motivoAbandono = f.motivo,
                registradoPor = f.rut
            )
        }
    }

    private fun usos03Oct(): List<Uso> {
        fun uso(id: Int, p: String, turno: Int?, ini: String, fin: String?, ing: Int, dev: Int?, rut: String) = Uso(
            id = "u0310-$id",
            codigoProbador = p,
            turnoId = turno?.let { "d0310-T$it" },
            inicio = hm(ini, ::h03),
            termino = fin?.let { hm(it, ::h03) },
            prendasIngresadas = ing,
            prendasDevueltas = dev,
            registroInicio = rut,
            registroTermino = if (fin != null) rut else null
        )
        return listOf(
            uso(1, "P-03", 84, "17:12", "17:24", 4, 2, RUT_FUENTES),
            uso(2, "P-05", 85, "17:15", "17:33", 5, 5, RUT_DIAZ),
            uso(3, "P-04", 87, "17:31", null, 4, null, RUT_FUENTES),
            uso(4, "P-08", 88, "17:39", null, 2, null, RUT_FUENTES),
            uso(5, "P-01", 90, "17:42", null, 3, null, RUT_DIAZ),
            uso(6, "P-03", null, "17:44", "17:54", 2, 0, RUT_DIAZ),
            uso(7, "P-07", 91, "17:48", null, 5, null, RUT_DIAZ),
            uso(8, "P-02", 93, "17:50", null, 3, null, RUT_FUENTES)
        )
    }

    private fun cambios03Oct(): List<CambioEstado> = listOf(
        CambioEstado("P-06", EstadoProbador.LIBRE, EstadoProbador.FUERA_DE_SERVICIO, h03(11, 10), RUT_MORALES, "Cortina rota"),
        CambioEstado("P-03", EstadoProbador.OCUPADO, EstadoProbador.EN_ORDENAMIENTO, h03(17, 24), RUT_FUENTES),
        CambioEstado("P-03", EstadoProbador.EN_ORDENAMIENTO, EstadoProbador.LIBRE, h03(17, 25), RUT_FUENTES),
        CambioEstado("P-05", EstadoProbador.OCUPADO, EstadoProbador.EN_ORDENAMIENTO, h03(17, 33), RUT_DIAZ),
        CambioEstado("P-03", EstadoProbador.OCUPADO, EstadoProbador.EN_ORDENAMIENTO, h03(17, 54), RUT_DIAZ),
        CambioEstado("P-05", EstadoProbador.EN_ORDENAMIENTO, EstadoProbador.LIBRE, h03(17, 56), RUT_DIAZ, "Se demoró: prendas olvidadas")
    )

    private fun h26(hora: Int, minuto: Int) = Tiempo.millis(2026, 9, 26, hora, minuto)

    private data class RegistroManual(
        val n: Int, val llego: String, val prendas: Int, val probador: String?,
        val entro: String?, val salio: String?, val devolvio: Int?,
        val seFue: String? = null, val motivo: MotivoAbandono? = null, val preferente: Boolean = false
    )

    private val registroManual = listOf(
        RegistroManual(1, "15:02", 3, "P-02", "15:02", "15:11", 1),
        RegistroManual(2, "15:04", 5, "P-05", "15:06", "15:19", 5),
        RegistroManual(3, "15:05", 2, null, null, null, null, "15:05", MotivoAbandono.ESPERA_LARGA),
        RegistroManual(4, "15:07", 6, "P-01", "15:12", "15:30", 4),
        RegistroManual(5, "15:09", 1, "P-03", "15:13", "15:17", 0),
        RegistroManual(6, "15:10", 4, null, null, null, null, "15:18", MotivoAbandono.NO_INFORMADO),
        RegistroManual(7, "15:14", 3, "P-07", "15:18", "15:26", 3),
        RegistroManual(8, "15:15", 2, "P-08", "15:16", "15:21", 0, preferente = true),
        RegistroManual(9, "15:20", 6, null, null, null, null, "15:20", MotivoAbandono.ESPERA_LARGA),
        RegistroManual(10, "15:22", 4, "P-02", "15:23", "15:35", 2),
        RegistroManual(11, "15:25", 3, "P-05", "15:27", "15:33", 3),
        RegistroManual(12, "15:31", 5, "P-03", "15:33", "15:47", 1)
    )

    private fun turnos26Sep(): List<Turno> = registroManual.map { r ->
        Turno(
            id = "d2609-T${r.n}",
            numero = r.n,
            fecha = "2026-09-26",
            horaIngreso = hm(r.llego, ::h26),
            preferente = r.preferente,
            prendas = r.prendas,
            estado = if (r.probador != null) EstadoTurno.ATENDIDO else EstadoTurno.ABANDONO,
            horaCierre = hm(r.entro ?: r.seFue!!, ::h26),
            motivoAbandono = r.motivo,
            registradoPor = RUT_MORALES
        )
    }

    private fun usos26Sep(): List<Uso> = registroManual.filter { it.probador != null }.map { r ->
        Uso(
            id = "u2609-${r.n}",
            codigoProbador = r.probador!!,
            turnoId = "d2609-T${r.n}",
            inicio = hm(r.entro!!, ::h26),
            termino = hm(r.salio!!, ::h26),
            prendasIngresadas = r.prendas,
            prendasDevueltas = r.devolvio,
            registroInicio = RUT_FUENTES,
            registroTermino = RUT_FUENTES
        )
    }

    private fun hm(texto: String, aMillis: (Int, Int) -> Long): Long {
        val (h, m) = texto.split(":").map { it.toInt() }
        return aMillis(h, m)
    }

    fun escenarioPeak(s: Seccion, ahora: Long, rut: String): Seccion {
        val ocupadoActual = s.probadores.any { it.estado == EstadoProbador.OCUPADO }
        if (ocupadoActual) {
            throw ReglaNegocioException("Hay probadores ocupados: termine esos usos antes de cargar la demostración.")
        }
        val fecha = Tiempo.fechaIso(ahora)
        var numero = ReglasSeccion.turnosDelDia(s, fecha).maxOfOrNull { it.numero } ?: 0
        fun minutosAtras(min: Int) = ahora - min * 60_000L

        val nuevosUsos = mutableListOf<Uso>()
        val nuevosTurnos = mutableListOf<Turno>()

        val ocupados = mapOf("P-01" to 14, "P-02" to 6, "P-04" to 26, "P-07" to 8, "P-08" to 17)

        val probadores = s.probadores.map { p ->
            when {
                p.codigo in ocupados -> {
                    val min = ocupados.getValue(p.codigo)
                    numero++
                    val turno = Turno(
                        id = UUID.randomUUID().toString(), numero = numero, fecha = fecha,
                        horaIngreso = minutosAtras(min + 7), preferente = p.codigo == "P-08",
                        prendas = 3, estado = EstadoTurno.ATENDIDO, horaCierre = minutosAtras(min),
                        registradoPor = rut
                    )
                    val uso = Uso(
                        id = UUID.randomUUID().toString(), codigoProbador = p.codigo, turnoId = turno.id,
                        inicio = minutosAtras(min), prendasIngresadas = 3, registroInicio = rut
                    )
                    nuevosTurnos += turno
                    nuevosUsos += uso
                    p.copy(estado = EstadoProbador.OCUPADO, desde = uso.inicio, usoActivoId = uso.id)
                }
                p.codigo == "P-03" -> p.copy(estado = EstadoProbador.EN_ORDENAMIENTO, desde = minutosAtras(1))
                p.codigo == "P-05" -> p.copy(estado = EstadoProbador.LIBRE, desde = minutosAtras(2))
                else -> p
            }
        }

        listOf(Triple(5, false, 4), Triple(3, true, 2), Triple(1, false, 6)).forEach { (min, pref, prendas) ->
            numero++
            nuevosTurnos += Turno(
                id = UUID.randomUUID().toString(), numero = numero, fecha = fecha,
                horaIngreso = minutosAtras(min), preferente = pref, prendas = prendas, registradoPor = rut
            )
        }
        return s.copy(
            probadores = probadores,
            turnos = s.turnos + nuevosTurnos,
            usos = s.usos + nuevosUsos
        )
    }
}
