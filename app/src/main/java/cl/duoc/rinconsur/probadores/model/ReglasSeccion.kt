package cl.duoc.rinconsur.probadores.model

import cl.duoc.rinconsur.probadores.utils.Tiempo
import java.util.UUID

object ReglasSeccion {
    const val MAX_MINUTOS_ATRASO = 15

    fun turnosDelDia(s: Seccion, fecha: String): List<Turno> = s.turnos.filter { it.fecha == fecha }

    fun filaDelDia(s: Seccion, fecha: String): List<Turno> =
        turnosDelDia(s, fecha)
            .filter { it.estado.abierto }
            .sortedWith(compareByDescending<Turno> { it.preferente }.thenBy { it.numero })

    fun usosCerradosDelDia(s: Seccion, fecha: String): List<Uso> =
        s.usos.filter { it.termino != null && Tiempo.fechaIso(it.termino) == fecha }

    fun usoActivo(s: Seccion, probador: Probador): Uso? =
        probador.usoActivoId?.let { id -> s.usos.find { it.id == id } }

    fun siguienteTurnoPara(probador: Probador, fila: List<Turno>, probadores: List<Probador>): Turno? {
        val esperando = fila.filter { it.estado.abierto }
        val preferentes = esperando.filter { it.preferente }
        if (probador.tipo == TipoProbador.ACCESIBLE) {
            return preferentes.firstOrNull() ?: esperando.minByOrNull { it.numero }
        }
        val accesibleLibre = probadores.any {
            it.tipo == TipoProbador.ACCESIBLE && it.estado == EstadoProbador.LIBRE
        }
        if (preferentes.isNotEmpty() && !accesibleLibre) return preferentes.first()
        return esperando.filter { !it.preferente }.minByOrNull { it.numero }
            ?: esperando.minByOrNull { it.numero }
    }

    fun motivoFilaCerrada(p: Parametros, ahora: Long): String? {
        val minuto = Tiempo.minutoDelDia(ahora)
        val limite = p.horaCierre * 60 - p.minutosCierreFila
        return when {
            minuto < p.horaApertura * 60 ->
                "La tienda abre a las %02d:00. Aún no se registran turnos.".format(p.horaApertura)
            minuto >= limite ->
                "Desde las %02d:%02d no se aceptan nuevos ingresos a la fila (RN-12).".format(limite / 60, limite % 60)
            else -> null
        }
    }

    fun colaboradorActivo(s: Seccion, rut: String): Colaborador {
        val colaborador = s.colaboradores.find { it.rut == rut }
            ?: throw ReglaNegocioException("El colaborador no está registrado.")
        if (!colaborador.activo) {
            throw ReglaNegocioException("Colaborador inactivo: no puede registrar acciones (RN-13).")
        }
        return colaborador
    }

    private fun requerirJefatura(colaborador: Colaborador) {
        if (!colaborador.perfil.esJefatura) {
            throw ReglaNegocioException("Solo la jefatura puede realizar esta acción (RN-11).")
        }
    }

    fun validarPrendas(prendas: Int, maximo: Int) {
        if (prendas !in 1..maximo) {
            throw ReglaNegocioException("Se permiten entre 1 y $maximo prendas por ingreso (RN-02).")
        }
    }

    private fun probador(s: Seccion, codigo: String): Probador =
        s.probadores.find { it.codigo == codigo }
            ?: throw ReglaNegocioException("No existe el probador $codigo.")

    private fun turno(s: Seccion, turnoId: String): Turno =
        s.turnos.find { it.id == turnoId }
            ?: throw ReglaNegocioException("El turno ya no existe.")

    private fun cambiarEstado(
        s: Seccion,
        actual: Probador,
        nuevo: Probador,
        rut: String,
        ahora: Long,
        motivo: String? = null
    ): Seccion {
        if (!actual.estado.puedePasarA(nuevo.estado)) {
            throw ReglaNegocioException(
                "${actual.codigo} no puede pasar de \"${actual.estado.etiqueta}\" a \"${nuevo.estado.etiqueta}\" (RN-01)."
            )
        }
        val cambio = CambioEstado(actual.codigo, actual.estado, nuevo.estado, ahora, rut, motivo)
        return s.copy(
            probadores = s.probadores.map { if (it.codigo == actual.codigo) nuevo else it },
            cambios = s.cambios + cambio
        )
    }

    fun registrarTurno(
        s: Seccion,
        prendas: Int,
        preferente: Boolean,
        rut: String,
        ahora: Long,
        ignorarHorario: Boolean
    ): Pair<Seccion, Turno> {
        colaboradorActivo(s, rut)
        validarPrendas(prendas, s.parametros.maxPrendas)
        if (!ignorarHorario) {
            motivoFilaCerrada(s.parametros, ahora)?.let { throw ReglaNegocioException(it) }
        }
        val fecha = Tiempo.fechaIso(ahora)
        val numero = (turnosDelDia(s, fecha).maxOfOrNull { it.numero } ?: 0) + 1
        val turno = Turno(
            id = UUID.randomUUID().toString(),
            numero = numero,
            fecha = fecha,
            horaIngreso = ahora,
            preferente = preferente,
            prendas = prendas,
            registradoPor = rut
        )
        return s.copy(turnos = s.turnos + turno) to turno
    }

    fun llamarTurno(s: Seccion, turnoId: String, rut: String, ahora: Long): Seccion {
        colaboradorActivo(s, rut)
        val t = turno(s, turnoId)
        if (t.estado != EstadoTurno.EN_ESPERA) {
            throw ReglaNegocioException("${t.codigo} no está en espera (${t.estado.etiqueta}).")
        }
        val llamado = t.copy(estado = EstadoTurno.LLAMADO, horaLlamado = ahora)
        return s.copy(turnos = s.turnos.map { if (it.id == turnoId) llamado else it })
    }

    fun registrarAbandono(
        s: Seccion,
        turnoId: String,
        motivo: MotivoAbandono,
        rut: String,
        ahora: Long,
        noSePresento: Boolean = false
    ): Seccion {
        colaboradorActivo(s, rut)
        val t = turno(s, turnoId)
        if (!t.estado.abierto) {
            throw ReglaNegocioException("${t.codigo} ya está cerrado (${t.estado.etiqueta}); un turno cerrado no se reabre (RN-03).")
        }
        if (noSePresento) {
            val llamado = t.horaLlamado
            val espera = s.parametros.minutosPresentarse
            if (t.estado != EstadoTurno.LLAMADO || llamado == null) {
                throw ReglaNegocioException("Primero debe llamar a ${t.codigo}.")
            }
            if (ahora - llamado < espera * 60_000L) {
                throw ReglaNegocioException("Aún no pasan $espera minutos desde el llamado (RN-07).")
            }
        }
        val cerrado = t.copy(
            estado = if (noSePresento) EstadoTurno.NO_SE_PRESENTO else EstadoTurno.ABANDONO,
            horaCierre = ahora,
            motivoAbandono = if (noSePresento) MotivoAbandono.NO_INFORMADO else motivo
        )
        return s.copy(turnos = s.turnos.map { if (it.id == turnoId) cerrado else it })
    }

    fun asignarProbador(
        s: Seccion,
        codigo: String,
        turnoId: String?,
        prendasSinFila: Int?,
        rut: String,
        ahora: Long,
        minutosAtraso: Int = 0
    ): Seccion {
        colaboradorActivo(s, rut)
        val p = probador(s, codigo)
        when (p.estado) {
            EstadoProbador.LIBRE -> Unit
            EstadoProbador.FUERA_DE_SERVICIO ->
                throw ReglaNegocioException("$codigo está fuera de servicio y no se puede asignar.")
            else -> throw ReglaNegocioException(
                "$codigo ya está \"${p.estado.etiqueta}\". Vale la primera asignación (Aclaración nº 7)."
            )
        }
        if (minutosAtraso !in 0..MAX_MINUTOS_ATRASO) {
            throw ReglaNegocioException("El inicio puede registrarse con hasta $MAX_MINUTOS_ATRASO minutos de atraso.")
        }

        var nueva = s
        val prendas: Int
        if (turnoId != null) {
            val t = turno(s, turnoId)
            if (!t.estado.abierto) {
                throw ReglaNegocioException("${t.codigo} ya fue cerrado (${t.estado.etiqueta}); no se puede atender dos veces.")
            }
            prendas = t.prendas
            val atendido = t.copy(estado = EstadoTurno.ATENDIDO, horaCierre = ahora)
            nueva = nueva.copy(turnos = nueva.turnos.map { if (it.id == turnoId) atendido else it })
        } else {
            prendas = prendasSinFila ?: throw ReglaNegocioException("Indique la cantidad de prendas.")
            validarPrendas(prendas, s.parametros.maxPrendas)
        }

        val inicio = ahora - minutosAtraso * 60_000L
        val uso = Uso(
            id = UUID.randomUUID().toString(),
            codigoProbador = codigo,
            turnoId = turnoId,
            inicio = inicio,
            prendasIngresadas = prendas,
            registroInicio = rut,
            conAtraso = minutosAtraso > 0
        )
        nueva = nueva.copy(usos = nueva.usos + uso)
        val ocupado = p.copy(estado = EstadoProbador.OCUPADO, desde = inicio, usoActivoId = uso.id)
        return cambiarEstado(nueva, p, ocupado, rut, ahora)
    }

    fun terminarUso(s: Seccion, codigo: String, devueltas: Int, rut: String, ahora: Long): Seccion {
        colaboradorActivo(s, rut)
        val p = probador(s, codigo)
        if (p.estado != EstadoProbador.OCUPADO) {
            throw ReglaNegocioException("$codigo no está ocupado.")
        }
        val uso = usoActivo(s, p) ?: throw ReglaNegocioException("$codigo no tiene un uso activo.")
        if (devueltas < 0 || devueltas > uso.prendasIngresadas) {
            throw ReglaNegocioException(
                "Las prendas devueltas no pueden ser más que las ingresadas (${uso.prendasIngresadas}) (RN-02)."
            )
        }
        val cerrado = uso.copy(termino = ahora, prendasDevueltas = devueltas, registroTermino = rut)
        val nueva = s.copy(usos = s.usos.map { if (it.id == uso.id) cerrado else it })
        val enOrden = p.copy(estado = EstadoProbador.EN_ORDENAMIENTO, desde = ahora, usoActivoId = null)
        return cambiarEstado(nueva, p, enOrden, rut, ahora)
    }

    fun finalizarOrdenamiento(s: Seccion, codigo: String, rut: String, ahora: Long): Seccion {
        colaboradorActivo(s, rut)
        val p = probador(s, codigo)
        if (p.estado != EstadoProbador.EN_ORDENAMIENTO) {
            throw ReglaNegocioException("$codigo no está en ordenamiento.")
        }
        return cambiarEstado(s, p, p.copy(estado = EstadoProbador.LIBRE, desde = ahora), rut, ahora)
    }

    fun marcarFueraDeServicio(
        s: Seccion,
        codigo: String,
        motivo: String,
        foto: String?,
        rut: String,
        ahora: Long
    ): Seccion {
        requerirJefatura(colaboradorActivo(s, rut))
        if (motivo.isBlank()) throw ReglaNegocioException("Debe indicar el motivo.")
        val p = probador(s, codigo)
        if (p.estado == EstadoProbador.OCUPADO) {
            throw ReglaNegocioException("$codigo está ocupado: registre primero el término del uso.")
        }
        if (p.estado == EstadoProbador.FUERA_DE_SERVICIO) {
            throw ReglaNegocioException("$codigo ya está fuera de servicio.")
        }
        val fuera = p.copy(
            estado = EstadoProbador.FUERA_DE_SERVICIO,
            desde = ahora,
            motivoFueraServicio = motivo.trim(),
            fotoEvidencia = foto
        )
        return cambiarEstado(s, p, fuera, rut, ahora, motivo.trim())
    }

    fun habilitar(s: Seccion, codigo: String, rut: String, ahora: Long): Seccion {
        requerirJefatura(colaboradorActivo(s, rut))
        val p = probador(s, codigo)
        val libre = p.copy(
            estado = EstadoProbador.LIBRE,
            desde = ahora,
            motivoFueraServicio = null,
            fotoEvidencia = null
        )
        return cambiarEstado(s, p, libre, rut, ahora, "Habilitado nuevamente")
    }

    fun cerrarJornada(s: Seccion, rut: String, ahora: Long): Pair<Seccion, Int> {
        requerirJefatura(colaboradorActivo(s, rut))
        var cerrados = 0
        val turnos = s.turnos.map {
            if (it.estado.abierto) {
                cerrados++
                it.copy(
                    estado = EstadoTurno.ABANDONO,
                    horaCierre = ahora,
                    motivoAbandono = MotivoAbandono.CIERRE_TIENDA
                )
            } else it
        }
        return s.copy(turnos = turnos) to cerrados
    }

    fun guardarColaborador(s: Seccion, colaborador: Colaborador, rutJefatura: String, esNuevo: Boolean): Seccion {
        requerirJefatura(colaboradorActivo(s, rutJefatura))
        val existe = s.colaboradores.any { it.rut == colaborador.rut }
        if (esNuevo && existe) throw ReglaNegocioException("Ya existe un colaborador con ese RUT.")
        if (!esNuevo && !existe) throw ReglaNegocioException("El colaborador no existe.")
        if (colaborador.rut == rutJefatura && !colaborador.activo) {
            throw ReglaNegocioException("No puede desactivarse a sí mismo.")
        }
        val lista = if (esNuevo) s.colaboradores + colaborador
        else s.colaboradores.map { if (it.rut == colaborador.rut) colaborador else it }
        return s.copy(colaboradores = lista)
    }

    fun guardarParametros(s: Seccion, parametros: Parametros, rut: String): Seccion {
        requerirJefatura(colaboradorActivo(s, rut))
        return s.copy(parametros = parametros)
    }
}
