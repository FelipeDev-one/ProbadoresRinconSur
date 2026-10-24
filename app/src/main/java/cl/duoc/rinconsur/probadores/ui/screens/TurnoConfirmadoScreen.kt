package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.rinconsur.probadores.ui.theme.CifraCliente

@Composable
fun TurnoConfirmadoScreen(
    codigo: String,
    minutos: Int?,
    onVolverFila: () -> Unit,
    onOtroTurno: () -> Unit
) {
    val escala = remember { Animatable(0.3f) }
    LaunchedEffect(codigo) {
        escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primaryContainer) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Icon(Icons.Filled.CheckCircle, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Turno registrado", style = MaterialTheme.typography.titleLarge)
            Text(codigo, style = CifraCliente, modifier = Modifier.scale(escala.value))
            Text(
                when (minutos) {
                    null -> "Sin probadores habilitados: no se puede estimar la espera."
                    0 -> "Puede pasar de inmediato."
                    else -> "Espera estimada: ~$minutos minutos"
                },
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Indíquele al cliente su número. Puede seguir recorriendo la sección y consultar su turno en la pantalla de espera.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 480.dp)
            )
            Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onVolverFila, modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)) { Text("Volver a la fila") }
                OutlinedButton(onClick = onOtroTurno, modifier = Modifier.fillMaxWidth()) { Text("Registrar otro turno") }
            }
        }
    }
}
