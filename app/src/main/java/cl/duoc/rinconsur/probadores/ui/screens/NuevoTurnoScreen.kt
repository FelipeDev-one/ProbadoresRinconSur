package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.model.MotivoPreferente
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel
import cl.duoc.rinconsur.probadores.viewmodel.TurnoViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NuevoTurnoScreen(
    seccionViewModel: SeccionViewModel,
    onVolver: () -> Unit,
    onRegistrado: (codigo: String, minutos: Int?) -> Unit,
    viewModel: TurnoViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsState()
    val modoDemo by seccionViewModel.modoDemo.collectAsState()
    val panel by seccionViewModel.panel.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo turno") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(Modifier.widthIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                panel?.let {
                    Text(
                        "Hay ${it.personasEnFila} persona(s) en la fila y ${it.libres} probador(es) libre(s).",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                Text("Prendas que ingresa", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledIconButton(onClick = { viewModel.sumarPrenda(-1) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "Quitar una prenda")
                    }
                    OutlinedTextField(
                        value = estado.prendas,
                        onValueChange = viewModel::onPrendasChange,
                        label = { Text("Prendas") },
                        leadingIcon = { Icon(Icons.Filled.Checkroom, contentDescription = null) },
                        trailingIcon = if (estado.errores.prendas != null) {
                            { Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        isError = estado.errores.prendas != null,
                        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    FilledIconButton(onClick = { viewModel.sumarPrenda(1) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "Agregar una prenda")
                    }
                }
                MensajeError(estado.errores.prendas)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = estado.preferente, onCheckedChange = viewModel::onPreferenteChange)
                    Text("Atención preferente (prioridad para P-08)", style = MaterialTheme.typography.bodyLarge)
                }
                AnimatedVisibility(
                    visible = estado.preferente,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MotivoPreferente.entries.forEach { m ->
                                FilterChip(
                                    selected = estado.motivoPreferente == m,
                                    onClick = { viewModel.onMotivoPreferenteChange(m) },
                                    label = { Text(m.etiqueta) }
                                )
                            }
                        }
                        MensajeError(estado.errores.motivoPreferente)
                    }
                }

                MensajeError(estado.errores.general)

                Button(
                    onClick = {
                        viewModel.registrar(ignorarHorario = modoDemo, onExito = onRegistrado)
                    },
                    enabled = !estado.guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    if (estado.guardando) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Registrar en la fila", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Text(
                    "No se registran datos personales del cliente: solo su número de turno (RNF-06).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
