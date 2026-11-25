package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.viewmodel.ClimaUiState
import cl.duoc.rinconsur.probadores.viewmodel.ClimaViewModel

// Tarjeta con el clima actual de la tienda (API externa Open-Meteo consumida con Retrofit).
@Composable
fun ClimaCard(modifier: Modifier = Modifier, viewModel: ClimaViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (val e = estado) {
                ClimaUiState.Cargando -> {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Text("Consultando el clima…", style = MaterialTheme.typography.bodyMedium)
                }
                is ClimaUiState.Listo -> {
                    val c = e.clima
                    Icon(
                        if (c.descripcion == "Despejado") Icons.Filled.WbSunny else Icons.Filled.WbCloudy,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(Modifier.weight(1f)) {
                        Text("${c.temperatura} °C · ${c.descripcion}", style = MaterialTheme.typography.titleMedium)
                        Text("Sensación ${c.sensacion} °C en la tienda", style = MaterialTheme.typography.bodySmall)
                        Text(c.sugerencia, style = MaterialTheme.typography.bodySmall)
                    }
                }
                is ClimaUiState.Error -> {
                    Icon(Icons.Filled.WbCloudy, contentDescription = null, modifier = Modifier.size(36.dp))
                    Text(e.mensaje, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::actualizar) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Reintentar")
                    }
                }
            }
        }
    }
}
