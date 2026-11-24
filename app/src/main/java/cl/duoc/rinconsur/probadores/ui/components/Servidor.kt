package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.data.remote.EstadoConexion
import cl.duoc.rinconsur.probadores.viewmodel.ServidorViewModel

// Botón con el estado de la conexión; al tocarlo se edita la dirección del servidor.
@Composable
fun BotonServidor(
    modifier: Modifier = Modifier,
    viewModel: ServidorViewModel = viewModel()
) {
    val url by viewModel.urlActual.collectAsStateWithLifecycle()
    val conexion by viewModel.estadoConexion.collectAsStateWithLifecycle()
    TextButton(onClick = viewModel::abrir, modifier = modifier) {
        Icon(iconoConexion(conexion), contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(
            if (url.isBlank()) "Sin servidor" else "Servidor: ${conexion.etiqueta.substringBefore(':')}",
            style = MaterialTheme.typography.labelLarge
        )
    }
    DialogoServidor(viewModel)
}

fun iconoConexion(conexion: EstadoConexion) = when (conexion) {
    EstadoConexion.SIN_SERVIDOR -> Icons.Filled.PhoneAndroid
    EstadoConexion.SIN_CONEXION -> Icons.Filled.CloudOff
    else -> Icons.Filled.Cloud
}

@Composable
fun DialogoServidor(viewModel: ServidorViewModel) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val conexion by viewModel.estadoConexion.collectAsStateWithLifecycle()
    if (!estado.editando) return
    AlertDialog(
        onDismissRequest = { if (!estado.probando) viewModel.cerrar() },
        icon = { Icon(iconoConexion(conexion), contentDescription = null) },
        title = { Text("Servidor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(conexion.etiqueta, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Dirección del API Gateway de los microservicios. En el emulador es http://10.0.2.2:8080/; " +
                        "en un teléfono, la URL pública que entrega VS Code (puerto 8080). " +
                        "Déjela vacía para trabajar sin servidor.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = estado.url,
                    onValueChange = viewModel::onUrlChange,
                    label = { Text("Dirección") },
                    placeholder = { Text("https://…-8080.brs.devtunnels.ms/") },
                    singleLine = true,
                    isError = estado.error != null,
                    supportingText = estado.error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    enabled = !estado.probando,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::usarEmulador, enabled = !estado.probando) { Text("Emulador") }
                    TextButton(onClick = viewModel::sinServidor, enabled = !estado.probando) { Text("Sin servidor") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = viewModel::guardar, enabled = !estado.probando) {
                if (estado.probando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Guardar y probar")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::cerrar, enabled = !estado.probando) { Text("Cancelar") }
        }
    )
}
