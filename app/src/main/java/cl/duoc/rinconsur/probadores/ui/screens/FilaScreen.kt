package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.navigation.Ruta
import cl.duoc.rinconsur.probadores.ui.components.PanelScaffold
import cl.duoc.rinconsur.probadores.utils.Tiempo
import cl.duoc.rinconsur.probadores.viewmodel.PanelUiState
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel
import cl.duoc.rinconsur.probadores.viewmodel.TurnoVista

@Composable
fun FilaScreen(navController: NavController, viewModel: SeccionViewModel) {
    val panel by viewModel.panel.collectAsState()
    val modoDemo by viewModel.modoDemo.collectAsState()
    var asignarId by rememberSaveable { mutableStateOf<String?>(null) }
    var abandonoId by rememberSaveable { mutableStateOf<String?>(null) }

    PanelScaffold(
        titulo = "Fila de espera",
        rutaActual = Ruta.Fila,
        navController = navController,
        viewModel = viewModel,
        floatingActionButton = {
            val cerrada = panel?.motivoFilaCerrada
            ExtendedFloatingActionButton(
                onClick = {
                    if (cerrada != null && !modoDemo) viewModel.mostrarMensaje(cerrada, true)
                    else navController.navigate(Ruta.NuevoTurno.ruta)
                },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Nuevo turno") }
            )
        }
    ) { padding ->
        val p = panel ?: return@PanelScaffold
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("${p.personasEnFila} esperando") })
                    AssistChip(onClick = {}, label = { Text("${p.libres} libres") })
                    AssistChip(onClick = {}, label = { Text("Nuevo: " + Tiempo.estimado(p.estimadoNuevo)) })
                }
                p.motivoFilaCerrada?.let {
                    Text(
                        if (modoDemo) "$it (modo demostración activo: se permite registrar)" else it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            if (p.fila.isEmpty()) {
                item { FilaVacia() }
            }
            items(p.fila, key = { it.turno.id }) { vista ->
                TurnoCard(
                    vista = vista,
                    minutosPresentarse = p.parametros.minutosPresentarse,
                    onLlamar = { viewModel.llamar(vista.turno) },
                    onAsignar = { asignarId = vista.turno.id },
                    onAbandono = { abandonoId = vista.turno.id },
                    onNoSePresento = { viewModel.noSePresento(vista.turno) },
                    modifier = Modifier.animateItem()
                )
            }
        }

        p.fila.find { it.turno.id == asignarId }?.let { vista ->
            AsignarDialog(vista, p, onAsignar = { codigo ->
                viewModel.asignar(vista.turno, codigo)
                asignarId = null
            }, onCerrar = { asignarId = null })
        }
        p.fila.find { it.turno.id == abandonoId }?.let { vista ->
            AbandonoDialog(vista, onConfirmar = { motivo ->
                viewModel.registrarAbandono(vista.turno, motivo)
                abandonoId = null
            }, onCerrar = { abandonoId = null })
        }
    }
}

@Composable
private fun FilaVacia() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.SentimentSatisfied, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Text("No hay personas esperando", style = MaterialTheme.typography.titleLarge)
        Text("Registre un turno solo si no hay probadores libres.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TurnoCard(
    vista: TurnoVista,
    minutosPresentarse: Int,
    onLlamar: () -> Unit,
    onAsignar: () -> Unit,
    onAbandono: () -> Unit,
    onNoSePresento: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = vista.turno
    val llamado = t.estado == EstadoTurno.LLAMADO

    val fondo by animateColorAsState(
        if (llamado) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        label = "fondoTurno"
    )
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = fondo)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${vista.posicion}.", style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.size(8.dp))
                Text(t.codigo, style = MaterialTheme.typography.headlineMedium)
                if (t.preferente) {
                    Spacer(Modifier.size(8.dp))
                    Icon(Icons.Filled.Accessibility, contentDescription = "Preferente", tint = MaterialTheme.colorScheme.secondary)
                    Text(" Preferente", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.weight(1f))
                if (llamado) EtiquetaLlamado()
            }
            Text(
                "Ingresó ${Tiempo.hora(t.horaIngreso)} · espera ${Tiempo.duracion(vista.minutosEsperando)} · " +
                    "${t.prendas} prenda(s) · estimado ${Tiempo.estimado(vista.estimado)}",
                style = MaterialTheme.typography.bodyMedium
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onAsignar) {
                    Icon(Icons.Filled.Checkroom, null, Modifier.size(18.dp)); Text("  Asignar")
                }
                if (!llamado) {
                    FilledTonalButton(onClick = onLlamar) {
                        Icon(Icons.Filled.Campaign, null, Modifier.size(18.dp)); Text("  Llamar")
                    }
                } else {
                    val puede = (vista.minutosDesdeLlamado ?: 0) >= minutosPresentarse
                    OutlinedButton(onClick = onNoSePresento, enabled = puede) {
                        Icon(Icons.Filled.PersonOff, null, Modifier.size(18.dp))
                        Text(if (puede) "  No se presentó" else "  Esperando (${minutosPresentarse} min)")
                    }
                }
                TextButton(onClick = onAbandono) {
                    Icon(Icons.Filled.Close, null, Modifier.size(18.dp)); Text(" Se retiró")
                }
            }
        }
    }
}

@Composable
private fun EtiquetaLlamado() {
    val parpadeo = rememberInfiniteTransition(label = "parpadeo")
    val alfa by parpadeo.animateFloat(0.4f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alfa")
    Text(
        "LLAMADO",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.alpha(alfa)
    )
}

@Composable
private fun AsignarDialog(vista: TurnoVista, panel: PanelUiState, onAsignar: (String) -> Unit, onCerrar: () -> Unit) {
    val libres = panel.probadores.filter { it.probador.estado == EstadoProbador.LIBRE }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Asignar probador a ${vista.turno.codigo}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (libres.isEmpty()) {
                    Text("No hay probadores libres en este momento. Revise los que están en ordenamiento.")
                }
                libres.forEach { libre ->
                    val sugerido = libre.siguienteTurno?.id == vista.turno.id
                    val accesible = libre.probador.tipo == TipoProbador.ACCESIBLE
                    Button(onClick = { onAsignar(libre.probador.codigo) }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            libre.probador.codigo +
                                (if (accesible) " · accesible" else "") +
                                (if (sugerido) " · sugerido" else ""),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}

@Composable
private fun AbandonoDialog(vista: TurnoVista, onConfirmar: (MotivoAbandono) -> Unit, onCerrar: () -> Unit) {
    var motivo by rememberSaveable { mutableStateOf(MotivoAbandono.NO_INFORMADO) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("${vista.turno.codigo} se retiró de la fila") },
        text = {
            Column {
                Text("Esperó ${Tiempo.duracion(vista.minutosEsperando)}. ¿Indicó el motivo?")
                MotivoAbandono.entries.filter { it.seleccionable }.forEach { m ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = motivo == m, onClick = { motivo = m }, role = Role.RadioButton)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = motivo == m, onClick = null)
                        Text(m.etiqueta, Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirmar(motivo) }) { Text("Registrar abandono") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
