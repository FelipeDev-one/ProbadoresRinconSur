package cl.duoc.rinconsur.probadores.model

import cl.duoc.rinconsur.probadores.utils.Tiempo

sealed class Alerta(val titulo: String, val detalle: String, val urgente: Boolean) {
    class UsoProlongado(codigo: String, minutos: Int, ofrecerAyuda: Boolean) : Alerta(
        titulo = if (ofrecerAyuda) "$codigo: ofrecer ayuda" else "$codigo: uso prolongado",
        detalle = if (ofrecerAyuda) "Lleva $minutos min y hay personas esperando. Acérquese cordialmente."
        else "Lleva $minutos min ocupado.",
        urgente = ofrecerAyuda
    )

    // Agrupada: una sola alerta para todos los probadores libres mientras hay fila,
    // en vez de una tarjeta por probador (menos información repetida en pantalla).
    class ProbadoresSinAsignar(codigos: List<String>, personas: Int) : Alerta(
        titulo = if (codigos.size == 1) "${codigos.first()} libre sin asignar"
        else "${codigos.size} probadores libres sin asignar",
        detalle = "${codigos.joinToString(", ")} · hay $personas en la fila.",
        urgente = true
    )

    class TurnoSinPresentarse(codigoTurno: String, minutos: Int) : Alerta(
        titulo = "$codigoTurno no se presenta",
        detalle = "Llamado hace $minutos min. Puede marcarlo como \"No se presentó\".",
        urgente = false
    )

    class CierreFilaProximo(minutosRestantes: Int) : Alerta(
        titulo = "Cierre de la fila en $minutosRestantes min",
        detalle = "Después no se aceptan nuevos ingresos (RN-12).",
        urgente = false
    )
}

object GeneradorAlertas {
    fun generar(s: Seccion, fila: List<Turno>, ahora: Long): List<Alerta> {
        val p = s.parametros
        val alertas = mutableListOf<Alerta>()
        val enEspera = fila.count { it.estado == EstadoTurno.EN_ESPERA }
        val libresSinAsignar = mutableListOf<String>()

        s.probadores.forEach { probador ->
            val minutos = Tiempo.minutosEntre(probador.desde, ahora)
            when (probador.estado) {
                EstadoProbador.OCUPADO -> if (minutos > p.umbralProlongado) {
                    val ayuda = minutos > p.umbralAyuda && fila.isNotEmpty()
                    alertas += Alerta.UsoProlongado(probador.codigo, minutos, ayuda)
                }
                EstadoProbador.LIBRE -> if (enEspera > 0 && minutos >= 1) {
                    libresSinAsignar += probador.codigo
                }
                else -> Unit
            }
        }

        if (libresSinAsignar.isNotEmpty()) {
            alertas += Alerta.ProbadoresSinAsignar(libresSinAsignar, enEspera)
        }

        fila.filter { it.estado == EstadoTurno.LLAMADO }.forEach { turno ->
            val minutos = Tiempo.minutosEntre(turno.horaLlamado ?: ahora, ahora)
            if (minutos >= p.minutosPresentarse) {
                alertas += Alerta.TurnoSinPresentarse(turno.codigo, minutos)
            }
        }

        val limite = p.horaCierre * 60 - p.minutosCierreFila
        val restantes = limite - Tiempo.minutoDelDia(ahora)
        if (restantes in 1..15) alertas += Alerta.CierreFilaProximo(restantes)

        return alertas.sortedByDescending { it.urgente }
    }
}
