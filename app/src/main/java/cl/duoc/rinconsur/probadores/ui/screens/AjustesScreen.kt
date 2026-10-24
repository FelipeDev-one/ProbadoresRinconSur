package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.navigation.Ruta
import cl.duoc.rinconsur.probadores.ui.components.PanelScaffold
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel


@Composable
fun AjustesScreen(navController: NavController, viewModel: SeccionViewModel) {
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    val modoDemo by viewModel.modoDemo.collectAsStateWithLifecycle()
    var confirmarCierre by rememberSaveable { mutableStateOf(false) }
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    val esJefatura = sesion?.perfil?.esJefatura == true

    val textoModo = if (modoDemo) "Activado: se ignora el horario de la tienda"
    else "Desactivado: rige el horario 10:00–21:00"
    val colorModo by animateColorAsState(
        if (modoDemo) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(500),
        label = "colorModo"
    )

    PanelScaffold(
        titulo = stringResource(R.string.titulo_ajustes),
        rutaActual = Ruta.Ajustes,
        navController = navController,
        viewModel = viewModel
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioMedio),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            sesion?.let { c ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
                        Text(c.nombre, style = MaterialTheme.typography.titleLarge)
                        Text("${c.perfil.etiqueta} · Turno ${c.turno.etiqueta.lowercase()}")
                        Text("RUT ${RutValidator.formatear(c.rut)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                OpcionAjuste(Icons.Filled.Visibility, "Pantalla para clientes", "Abrir la vista del punto de espera") {
                    navController.navigate(Ruta.Cliente.ruta)
                }
            }

            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colorModo)) {
                Row(Modifier.padding(Dimens.espacioMedio), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Modo demostración", style = MaterialTheme.typography.titleMedium)
                        Text(textoModo, style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(checked = modoDemo, onCheckedChange = viewModel::cambiarModoDemo)
                }
            }


            if (esJefatura) {
                Text("Jefatura", style = MaterialTheme.typography.titleMedium)
                Card(Modifier.fillMaxWidth()) {
                    OpcionAjuste(Icons.Filled.Badge, "Colaboradores", "Altas, perfiles y activación") {
                        navController.navigate(Ruta.Colaboradores.ruta)
                    }
                    HorizontalDivider()
                    OpcionAjuste(Icons.Filled.Tune, "Parámetros de la sección", "Tiempos de referencia, umbrales y ticket") {
                        navController.navigate(Ruta.Parametros.ruta)
                    }

                    HorizontalDivider()
                    OpcionAjuste(Icons.Filled.Lock, "Cerrar jornada", "Los turnos que siguen en espera quedan como \"Cierre de tienda\"") {
                        confirmarCierre = true
                    }
                }
            }

            OutlinedButton(
                onClick = { confirmarSalida = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.alturaBotonChico)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.btn_cerrar_sesion))
            }
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("¿Cerrar la jornada?") },
            text = { Text("Todos los turnos que siguen en la fila quedarán como abandono por \"Cierre de tienda\" (RN-12). No cuentan como venta perdida.") },
            confirmButton = {
                Button(onClick = { viewModel.cerrarJornada(); confirmarCierre = false }) { Text("Cerrar jornada") }
            },
            dismissButton = { TextButton(onClick = { confirmarCierre = false }) { Text(stringResource(R.string.btn_cancelar)) } }
        )
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrá que ingresar su RUT nuevamente para registrar acciones.") },
            confirmButton = {
                Button(onClick = { confirmarSalida = false; viewModel.cerrarSesion() }) {
                    Text(stringResource(R.string.btn_cerrar_sesion))
                }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text(stringResource(R.string.btn_cancelar)) } }
        )
    }
}

@Composable
private fun OpcionAjuste(icono: ImageVector, titulo: String, detalle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(detalle) },
        leadingContent = { Icon(icono, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
