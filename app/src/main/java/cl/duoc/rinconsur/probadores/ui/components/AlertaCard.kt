package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.rinconsur.probadores.model.Alerta

// Tarjeta para cada alerta de GeneradorAlertas (RF-07, Aclaración nº 8).
// Las urgentes usan el color de error y un ícono distinto (no solo color: RNF-07).
@Composable
fun AlertaCard(alerta: Alerta, modifier: Modifier = Modifier) {
    val colores = if (alerta.urgente) {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    Card(modifier = modifier.fillMaxWidth(), colors = colores) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (alerta.urgente) Icons.Filled.NotificationsActive else Icons.Filled.Info,
                contentDescription = if (alerta.urgente) "Alerta urgente" else "Aviso"
            )
            Column {
                Text(alerta.titulo, style = MaterialTheme.typography.titleMedium)
                Text(alerta.detalle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
