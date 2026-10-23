package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.rinconsur.probadores.model.Franja
import cl.duoc.rinconsur.probadores.ui.theme.Dimens

// =====================================================================
//  GraficoFranjas — ingresos y abandonos por franja horaria (RF-09, HU-09)
//  ---------------------------------------------------------------------
//  Barras agrupadas dibujadas a mano con Canvas: por cada franja, una barra
//  de ingresos y otra de abandonos. La franja con más ingresos ocupa todo
//  el alto y el resto se escala con regla de tres (valor / máximo).
//  Cada barra lleva su número encima: no se depende solo del color (RNF-07).
// =====================================================================

@Composable
fun GraficoFranjas(datos: List<Triple<Franja, Int, Int>>, modifier: Modifier = Modifier) {
    val colorIngresos = MaterialTheme.colorScheme.primary
    val colorAbandonos = MaterialTheme.colorScheme.secondary
    val colorTexto = MaterialTheme.colorScheme.onSurface
    val colorEje = MaterialTheme.colorScheme.outline
    val medidorTexto = rememberTextMeasurer()

    // Animación: las barras crecen de 0 a su alto cada vez que cambian los datos.
    val progreso = remember(datos) { Animatable(0f) }
    LaunchedEffect(datos) { progreso.animateTo(1f, animationSpec = tween(durationMillis = 800)) }

    // Lectura para TalkBack: "10–12: 5 ingresos, 1 abandonos; ..."
    val resumenAccesible = datos.joinToString("; ") { (franja, ingresos, abandonos) ->
        "${franja.etiqueta}: $ingresos ingresos, $abandonos abandonos"
    }

    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ingresos y abandonos por franja", style = MaterialTheme.typography.titleMedium)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .semantics { contentDescription = resumenAccesible }
            ) {
                if (datos.isEmpty()) return@Canvas

                val margenSuperior = 20.dp.toPx()   // espacio para el número sobre la barra
                val margenInferior = 24.dp.toPx()   // espacio para la etiqueta de la franja
                val altoDisponible = size.height - margenSuperior - margenInferior
                val baseY = size.height - margenInferior

                val maximo = datos.maxOf { it.second }.coerceAtLeast(1)
                val anchoColumna = size.width / datos.size
                val anchoBarra = anchoColumna * 0.3f
                val separacion = 3.dp.toPx()
                val estilo = TextStyle(fontSize = 12.sp, color = colorTexto)

                drawLine(colorEje, Offset(0f, baseY), Offset(size.width, baseY), strokeWidth = 1.dp.toPx())

                datos.forEachIndexed { indice, (franja, ingresos, abandonos) ->
                    val inicio = indice * anchoColumna
                    val xIngresos = inicio + anchoColumna / 2 - anchoBarra - separacion / 2
                    val xAbandonos = inicio + anchoColumna / 2 + separacion / 2

                    listOf(xIngresos to ingresos, xAbandonos to abandonos).forEachIndexed { serie, (x, valor) ->
                        val alto = altoDisponible * (valor.toFloat() / maximo) * progreso.value
                        drawRoundRect(
                            color = if (serie == 0) colorIngresos else colorAbandonos,
                            topLeft = Offset(x, baseY - alto),
                            size = Size(anchoBarra, alto),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                        val texto = medidorTexto.measure(valor.toString(), estilo)
                        drawText(
                            textLayoutResult = texto,
                            topLeft = Offset(
                                x = x + (anchoBarra - texto.size.width) / 2,
                                y = baseY - alto - texto.size.height - 2.dp.toPx()
                            )
                        )
                    }

                    val etiqueta = medidorTexto.measure(franja.etiqueta, estilo)
                    drawText(
                        textLayoutResult = etiqueta,
                        topLeft = Offset(
                            x = inicio + (anchoColumna - etiqueta.size.width) / 2,
                            y = baseY + 4.dp.toPx()
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio), verticalAlignment = Alignment.CenterVertically) {
                Leyenda(colorIngresos, "Ingresos")
                Leyenda(colorAbandonos, "Abandonos")
            }
        }
    }
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}
