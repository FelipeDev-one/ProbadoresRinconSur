package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.viewmodel.Mensaje

// Aviso flotante de éxito o error (lo publica SeccionViewModel.mostrarMensaje).
// liveRegion = el lector de pantalla lo lee en voz alta apenas aparece.
@Composable
fun MensajeFlotante(mensaje: Mensaje?, modifier: Modifier = Modifier) {
    var ultimo by remember { mutableStateOf(mensaje) }
    LaunchedEffect(mensaje) { if (mensaje != null) ultimo = mensaje }

    AnimatedVisibility(
        visible = mensaje != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        val m = mensaje ?: ultimo ?: return@AnimatedVisibility
        Surface(
            color = if (m.esError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface,
            contentColor = if (m.esError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface,
            shape = MaterialTheme.shapes.large,
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.espacioMedio)
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                Modifier.padding(Dimens.espacioMedio),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(if (m.esError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle, contentDescription = null)
                Text(m.texto, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
