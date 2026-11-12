package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.Probador
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel

// =====================================================================
//  GestionProbadoresScreen — mantenedor de probadores (solo jefatura, RN-11)
//  ---------------------------------------------------------------------
//  Agregar un probador (toma el primer código libre: P-01, P-02…), cambiar
//  su tipo entre estándar y accesible, o eliminarlo si nunca se usó.
//  Las reglas viven en ReglasSeccion; aquí solo se muestran y confirman.
// =====================================================================

@Composable
fun GestionProbadoresScreen(viewModel: SeccionViewModel, onVolver: () -> Unit) {
    val seccion by viewModel.seccion.collectAsStateWithLifecycle()
    val probadores = seccion?.probadores?.sortedBy { it.codigo } ?: emptyList()
    val conUsos = remember(seccion) { seccion?.usos?.map { it.codigoProbador }?.toSet() ?: emptySet() }

    var agregando by rememberSaveable { mutableStateOf(false) }
    var porEliminar by remember { mutableStateOf<Probador?>(null) }

    Scaffold(
        topBar = { BarraConVolver("Probadores de la sección", onVolver) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { agregando = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Agregar") }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = Dimens.espacioMedio, end = Dimens.espacioMedio,
                top = Dimens.espacioChico, bottom = Dimens.espacioBajoLista
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    "${probadores.size} probador(es) · ${probadores.count { it.tipo == TipoProbador.ACCESIBLE }} accesible(s)",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(probadores, key = { it.codigo }) { p ->
                val tieneUsos = p.codigo in conUsos
                Card(Modifier.fillMaxWidth()) {
                    ListItem(
                        leadingContent = {
                            Icon(
                                if (p.tipo == TipoProbador.ACCESIBLE) Icons.Filled.Accessible else Icons.Filled.MeetingRoom,
                                contentDescription = null
                            )
                        },
                        headlineContent = { Text(p.codigo, style = MaterialTheme.typography.titleLarge) },
                        supportingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
                                Text("${p.estado.etiqueta}${if (tieneUsos) " · con historial de usos" else ""}")
                                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                                    TipoProbador.entries.forEach { tipo ->
                                        FilterChip(
                                            selected = p.tipo == tipo,
                                            onClick = { if (p.tipo != tipo) viewModel.cambiarTipoProbador(p.codigo, tipo) },
                                            label = { Text(tipo.etiqueta) }
                                        )
                                    }
                                }
                            }
                        },
                        trailingContent = {
                            IconButton(onClick = { porEliminar = p }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Eliminar ${p.codigo}")
                            }
                        }
                    )
                }
            }
            item {
                Text(
                    "Un probador con historial de usos no se elimina: márquelo fuera de servicio desde la pestaña Probadores.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (agregando) {
        var tipo by rememberSaveable { mutableStateOf(TipoProbador.ESTANDAR) }
        AlertDialog(
            onDismissRequest = { agregando = false },
            icon = { Icon(Icons.Filled.MeetingRoom, contentDescription = null) },
            title = { Text("Agregar probador") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                    Text("Se le asignará el primer código libre. ¿De qué tipo es?")
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                        TipoProbador.entries.forEach { opcion ->
                            FilterChip(
                                selected = tipo == opcion,
                                onClick = { tipo = opcion },
                                label = { Text(opcion.etiqueta) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.agregarProbador(tipo); agregando = false }) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(onClick = { agregando = false }) { Text(stringResource(R.string.btn_cancelar)) }
            }
        )
    }

    porEliminar?.let { p ->
        AlertDialog(
            onDismissRequest = { porEliminar = null },
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text("¿Eliminar ${p.codigo}?") },
            text = { Text("Dejará de aparecer en la sección. Solo se puede eliminar si está libre y nunca se usó.") },
            confirmButton = {
                Button(onClick = { viewModel.eliminarProbador(p.codigo); porEliminar = null }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { porEliminar = null }) { Text(stringResource(R.string.btn_cancelar)) }
            }
        )
    }
}
