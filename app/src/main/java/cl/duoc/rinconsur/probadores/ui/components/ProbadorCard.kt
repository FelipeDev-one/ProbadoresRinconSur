package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.utils.Tiempo
import cl.duoc.rinconsur.probadores.viewmodel.ProbadorVista

// Borde que "late" para llamar la atención sobre un uso prolongado (HU-07).
@Composable
private fun bordePulsante(color: Color): BorderStroke {
    val pulso = rememberInfiniteTransition(label = "pulso")
    val alfa by pulso.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alfaBorde"
    )
    return BorderStroke(3.dp, color.copy(alpha = alfa))
}

// Tarjeta de un probador en la grilla del panel (HU-06): código, estado y hace cuánto.
@Composable
fun ProbadorCard(vista: ProbadorVista, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = vista.probador

    val fondo by animateColorAsState(fondoEstado(p.estado), animationSpec = tween(500), label = "fondo")

    val colorAlerta = if (vista.ofrecerAyuda) MaterialTheme.colorScheme.error else colorEstado(EstadoProbador.EN_ORDENAMIENTO)
    val borde = if (vista.prolongado) bordePulsante(colorAlerta) else null

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${p.codigo}, ${p.estado.etiqueta}, hace ${vista.minutosEnEstado} minutos"
            },
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = borde
    ) {
        Column(
            Modifier
                .padding(14.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.codigo,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                if (p.tipo == TipoProbador.ACCESIBLE) {
                    Icon(
                        Icons.Filled.Accessibility,
                        contentDescription = "Probador accesible",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            EstadoChip(p.estado)
            Text(
                "Desde ${Tiempo.hora(p.desde)} · ${Tiempo.duracion(vista.minutosEnEstado)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (p.estado) {
                EstadoProbador.OCUPADO -> vista.uso?.let { uso ->
                    Text(
                        "${uso.prendasIngresadas} prenda(s)" + if (uso.conAtraso) " · con atraso" else "",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                EstadoProbador.LIBRE -> vista.siguienteTurno?.let {
                    Text("Siguiente: ${it.codigo}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                EstadoProbador.FUERA_DE_SERVICIO -> p.motivoFueraServicio?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                EstadoProbador.EN_ORDENAMIENTO -> Unit
            }
            if (vista.prolongado) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = colorAlerta, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (vista.ofrecerAyuda) "Ofrecer ayuda" else "Uso prolongado",
                        color = if (vista.ofrecerAyuda) colorAlerta else Color.Unspecified,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
