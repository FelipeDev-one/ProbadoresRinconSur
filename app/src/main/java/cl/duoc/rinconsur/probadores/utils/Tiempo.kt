package cl.duoc.rinconsur.probadores.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Tiempo {
    private val zona: ZoneId = ZoneId.systemDefault()
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
    private val formatoFecha = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun aFecha(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()

    fun fechaIso(millis: Long): String = aFecha(millis).toString()

    fun hora(millis: Long): String = Instant.ofEpochMilli(millis).atZone(zona).format(formatoHora)

    fun horaDelDia(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(zona).hour

    fun minutoDelDia(millis: Long): Int {
        val t = Instant.ofEpochMilli(millis).atZone(zona)
        return t.hour * 60 + t.minute
    }

    fun fechaLegible(fechaIso: String): String = LocalDate.parse(fechaIso).format(formatoFecha)

    fun minutosEntre(desde: Long, hasta: Long): Int = ((hasta - desde) / 60_000L).toInt()

    fun millis(anio: Int, mes: Int, dia: Int, hora: Int, minuto: Int): Long =
        LocalDateTime.of(anio, mes, dia, hora, minuto).atZone(zona).toInstant().toEpochMilli()

    fun estimado(minutos: Int?): String = when (minutos) {
        null -> "—"
        0 -> "sin espera"
        else -> "~$minutos min"
    }

    fun duracion(minutos: Int): String = when {
        minutos < 60 -> "$minutos min"
        else -> "${minutos / 60} h %02d min".format(minutos % 60)
    }
}
