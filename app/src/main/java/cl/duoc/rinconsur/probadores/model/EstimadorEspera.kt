package cl.duoc.rinconsur.probadores.model

import cl.duoc.rinconsur.probadores.utils.Tiempo
import kotlin.math.ceil

object EstimadorEspera {
    const val MINIMO_USOS_DEL_DIA = 10

    fun promedioUso(usosCerrados: List<Uso>, referencia: Int): Double {
        val duraciones = usosCerrados.mapNotNull { uso ->
            uso.termino?.let { (it - uso.inicio) / 60_000.0 }
        }
        return if (duraciones.size < MINIMO_USOS_DEL_DIA) referencia.toDouble() else duraciones.average()
    }

    fun minutos(
        personasAntes: Int,
        habilitados: Int,
        promedioUso: Double,
        minutosOrdenamiento: Int
    ): Int? {
        if (habilitados <= 0) return null
        val valor = (personasAntes + 1).toDouble() / habilitados * (promedioUso + minutosOrdenamiento)
        return ceil(valor - 1e-9).toInt()
    }

    fun enSeccion(s: Seccion, personasAntes: Int, ahora: Long): Int? {
        val libres = s.probadores.count { it.estado == EstadoProbador.LIBRE }
        if (libres > 0 && personasAntes == 0) return 0
        val fecha = Tiempo.fechaIso(ahora)
        val promedio = promedioUso(ReglasSeccion.usosCerradosDelDia(s, fecha), s.parametros.tiempoReferenciaUso)
        return minutos(
            personasAntes = personasAntes,
            habilitados = s.probadores.count { it.habilitado },
            promedioUso = promedio,
            minutosOrdenamiento = s.parametros.minutosOrdenamiento
        )
    }
}
