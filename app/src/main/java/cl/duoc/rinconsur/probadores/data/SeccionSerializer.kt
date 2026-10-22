package cl.duoc.rinconsur.probadores.data

import cl.duoc.rinconsur.probadores.model.CambioEstado
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.Parametros
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.model.Turno
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.model.Uso
import org.json.JSONArray
import org.json.JSONObject

object SeccionSerializer {
    private fun <T> lista(json: String?, leer: (JSONObject) -> T): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        val arreglo = JSONArray(json)
        return (0 until arreglo.length()).map { leer(arreglo.getJSONObject(it)) }
    }

    private fun <T> texto(items: List<T>, escribir: (T) -> JSONObject): String =
        JSONArray().apply { items.forEach { put(escribir(it)) } }.toString()

    private fun JSONObject.stringONull(clave: String): String? =
        if (has(clave) && !isNull(clave)) getString(clave) else null

    private fun JSONObject.longONull(clave: String): Long? =
        if (has(clave) && !isNull(clave)) getLong(clave) else null

    private fun JSONObject.intONull(clave: String): Int? =
        if (has(clave) && !isNull(clave)) getInt(clave) else null

    private fun JSONObject.ponerSiNoNulo(clave: String, valor: Any?): JSONObject =
        if (valor != null) put(clave, valor) else this

    fun probadoresATexto(items: List<Probador>) = texto(items) {
        JSONObject()
            .put("codigo", it.codigo)
            .put("tipo", it.tipo.name)
            .put("estado", it.estado.name)
            .put("desde", it.desde)
            .ponerSiNoNulo("usoActivoId", it.usoActivoId)
            .ponerSiNoNulo("motivo", it.motivoFueraServicio)
            .ponerSiNoNulo("foto", it.fotoEvidencia)
    }

    fun probadoresDesdeTexto(json: String?) = lista(json) {
        Probador(
            codigo = it.getString("codigo"),
            tipo = TipoProbador.valueOf(it.getString("tipo")),
            estado = EstadoProbador.valueOf(it.getString("estado")),
            desde = it.getLong("desde"),
            usoActivoId = it.stringONull("usoActivoId"),
            motivoFueraServicio = it.stringONull("motivo"),
            fotoEvidencia = it.stringONull("foto")
        )
    }

    fun colaboradoresATexto(items: List<Colaborador>) = texto(items) {
        JSONObject()
            .put("rut", it.rut)
            .put("nombre", it.nombre)
            .put("perfil", it.perfil.name)
            .put("turno", it.turno.name)
            .put("activo", it.activo)
    }

    fun colaboradoresDesdeTexto(json: String?) = lista(json) {
        Colaborador(
            rut = it.getString("rut"),
            nombre = it.getString("nombre"),
            perfil = Perfil.valueOf(it.getString("perfil")),
            turno = TurnoLaboral.valueOf(it.getString("turno")),
            activo = it.getBoolean("activo")
        )
    }

    fun turnosATexto(items: List<Turno>) = texto(items) {
        JSONObject()
            .put("id", it.id)
            .put("numero", it.numero)
            .put("fecha", it.fecha)
            .put("ingreso", it.horaIngreso)
            .put("preferente", it.preferente)
            .put("prendas", it.prendas)
            .put("estado", it.estado.name)
            .ponerSiNoNulo("llamado", it.horaLlamado)
            .ponerSiNoNulo("cierre", it.horaCierre)
            .ponerSiNoNulo("motivo", it.motivoAbandono?.name)
            .put("registro", it.registradoPor)
    }

    fun turnosDesdeTexto(json: String?) = lista(json) {
        Turno(
            id = it.getString("id"),
            numero = it.getInt("numero"),
            fecha = it.getString("fecha"),
            horaIngreso = it.getLong("ingreso"),
            preferente = it.getBoolean("preferente"),
            prendas = it.getInt("prendas"),
            estado = EstadoTurno.valueOf(it.getString("estado")),
            horaLlamado = it.longONull("llamado"),
            horaCierre = it.longONull("cierre"),
            motivoAbandono = it.stringONull("motivo")?.let { m -> MotivoAbandono.valueOf(m) },
            registradoPor = it.getString("registro")
        )
    }

    fun usosATexto(items: List<Uso>) = texto(items) {
        JSONObject()
            .put("id", it.id)
            .put("probador", it.codigoProbador)
            .ponerSiNoNulo("turnoId", it.turnoId)
            .put("inicio", it.inicio)
            .ponerSiNoNulo("termino", it.termino)
            .put("ingresadas", it.prendasIngresadas)
            .ponerSiNoNulo("devueltas", it.prendasDevueltas)
            .put("registroInicio", it.registroInicio)
            .ponerSiNoNulo("registroTermino", it.registroTermino)
            .put("conAtraso", it.conAtraso)
    }

    fun usosDesdeTexto(json: String?) = lista(json) {
        Uso(
            id = it.getString("id"),
            codigoProbador = it.getString("probador"),
            turnoId = it.stringONull("turnoId"),
            inicio = it.getLong("inicio"),
            termino = it.longONull("termino"),
            prendasIngresadas = it.getInt("ingresadas"),
            prendasDevueltas = it.intONull("devueltas"),
            registroInicio = it.getString("registroInicio"),
            registroTermino = it.stringONull("registroTermino"),
            conAtraso = it.optBoolean("conAtraso", false)
        )
    }

    fun cambiosATexto(items: List<CambioEstado>) = texto(items) {
        JSONObject()
            .put("probador", it.codigoProbador)
            .put("anterior", it.anterior.name)
            .put("nuevo", it.nuevo.name)
            .put("fechaHora", it.fechaHora)
            .put("colaborador", it.colaborador)
            .ponerSiNoNulo("motivo", it.motivo)
    }

    fun cambiosDesdeTexto(json: String?) = lista(json) {
        CambioEstado(
            codigoProbador = it.getString("probador"),
            anterior = EstadoProbador.valueOf(it.getString("anterior")),
            nuevo = EstadoProbador.valueOf(it.getString("nuevo")),
            fechaHora = it.getLong("fechaHora"),
            colaborador = it.getString("colaborador"),
            motivo = it.stringONull("motivo")
        )
    }

    fun parametrosATexto(p: Parametros): String = JSONObject()
        .put("maxPrendas", p.maxPrendas)
        .put("tiempoReferenciaUso", p.tiempoReferenciaUso)
        .put("minutosOrdenamiento", p.minutosOrdenamiento)
        .put("umbralProlongado", p.umbralProlongado)
        .put("umbralAyuda", p.umbralAyuda)
        .put("minutosPresentarse", p.minutosPresentarse)
        .put("ticketPromedio", p.ticketPromedio)
        .put("horaApertura", p.horaApertura)
        .put("horaCierre", p.horaCierre)
        .put("minutosCierreFila", p.minutosCierreFila)
        .toString()

    fun parametrosDesdeTexto(json: String?): Parametros {
        if (json.isNullOrBlank()) return Parametros()
        val o = JSONObject(json)
        val base = Parametros()
        return Parametros(
            maxPrendas = o.optInt("maxPrendas", base.maxPrendas),
            tiempoReferenciaUso = o.optInt("tiempoReferenciaUso", base.tiempoReferenciaUso),
            minutosOrdenamiento = o.optInt("minutosOrdenamiento", base.minutosOrdenamiento),
            umbralProlongado = o.optInt("umbralProlongado", base.umbralProlongado),
            umbralAyuda = o.optInt("umbralAyuda", base.umbralAyuda),
            minutosPresentarse = o.optInt("minutosPresentarse", base.minutosPresentarse),
            ticketPromedio = o.optInt("ticketPromedio", base.ticketPromedio),
            horaApertura = o.optInt("horaApertura", base.horaApertura),
            horaCierre = o.optInt("horaCierre", base.horaCierre),
            minutosCierreFila = o.optInt("minutosCierreFila", base.minutosCierreFila)
        )
    }
}
