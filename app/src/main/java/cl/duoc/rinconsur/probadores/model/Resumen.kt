package cl.duoc.rinconsur.probadores.model

import cl.duoc.rinconsur.probadores.utils.Tiempo

data class Franja(val etiqueta: String, val desde: Int, val hasta: Int)

val FRANJAS = listOf(
    Franja("Todo el día", 0, 24),
    Franja("10–12", 10, 12),
    Franja("12–14", 12, 14),
    Franja("14–16", 14, 16),
    Franja("16–18", 16, 18),
    Franja("18–21", 18, 21)
)

data class Resumen(
    val ingresos: Int,
    val atendidos: Int,
    val abandonos: Int,
    val abandonosVentaPerdida: Int,
    val esperaPromedio: Double?,
    val usoPromedio: Double?,
    val usosCerrados: Int,
    val conversionDia: Double?,
    val ventaPerdida: Double
) {
    val porcentajeAbandono: Double?
        get() = if (ingresos == 0) null else abandonos * 100.0 / ingresos
}

object ResumenCalculator {
    private fun enFranja(millis: Long, franja: Franja): Boolean =
        Tiempo.horaDelDia(millis) in franja.desde until franja.hasta

    fun conversion(usos: List<Uso>): Double? {
        val cerrados = usos.filter { it.cerrado }
        if (cerrados.isEmpty()) return null
        return cerrados.count { it.huboConversion }.toDouble() / cerrados.size
    }

    fun calcular(
        turnos: List<Turno>,
        usos: List<Uso>,
        fecha: String,
        franja: Franja,
        ticketPromedio: Int
    ): Resumen {
        val usosDia = usos.filter { Tiempo.fechaIso(it.inicio) == fecha }
        val turnosFranja = turnos.filter { it.fecha == fecha && enFranja(it.horaIngreso, franja) }
        val usosFranja = usosDia.filter { enFranja(it.inicio, franja) }

        val atendidos = turnosFranja.filter { it.estado == EstadoTurno.ATENDIDO }
        val abandonos = turnosFranja.filter {
            it.estado == EstadoTurno.ABANDONO || it.estado == EstadoTurno.NO_SE_PRESENTO
        }
        val abandonosVp = abandonos.count { it.motivoAbandono?.cuentaComoVentaPerdida ?: true }

        val esperas = atendidos.mapNotNull { t -> t.horaCierre?.let { (it - t.horaIngreso) / 60_000.0 } }
        val usosCerrados = usosFranja.filter { it.cerrado }
        val duraciones = usosCerrados.map { (it.termino!! - it.inicio) / 60_000.0 }

        val conversionDia = conversion(usosDia)
        val ventaPerdida = abandonosVp * (conversionDia ?: 0.0) * ticketPromedio

        return Resumen(
            ingresos = turnosFranja.size,
            atendidos = atendidos.size,
            abandonos = abandonos.size,
            abandonosVentaPerdida = abandonosVp,
            esperaPromedio = esperas.takeIf { it.isNotEmpty() }?.average(),
            usoPromedio = duraciones.takeIf { it.isNotEmpty() }?.average(),
            usosCerrados = usosCerrados.size,
            conversionDia = conversionDia,
            ventaPerdida = ventaPerdida
        )
    }

    fun porFranja(turnos: List<Turno>, fecha: String): List<Triple<Franja, Int, Int>> =
        FRANJAS.drop(1).map { franja ->
            val enLaFranja = turnos.filter { it.fecha == fecha && enFranja(it.horaIngreso, franja) }
            val abandonos = enLaFranja.count {
                it.estado == EstadoTurno.ABANDONO || it.estado == EstadoTurno.NO_SE_PRESENTO
            }
            Triple(franja, enLaFranja.size, abandonos)
        }
}
