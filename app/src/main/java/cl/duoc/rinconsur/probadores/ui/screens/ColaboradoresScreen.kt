package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.navigation.Ruta
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel

// Mantenedor de colaboradores (RN-11, solo jefatura): activos primero, por nombre.
@Composable
fun ColaboradoresScreen(navController: NavController, viewModel: SeccionViewModel) {
    val seccion by viewModel.seccion.collectAsStateWithLifecycle()

    val colaboradores = seccion?.colaboradores
        ?.sortedWith(compareByDescending<Colaborador> { it.activo }.thenBy { it.nombre })
        ?: emptyList()

    Scaffold(
        topBar = { BarraConVolver(stringResource(R.string.titulo_colaboradores), onVolver = { navController.navigateUp() }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Ruta.ColaboradorForm.crear()) },
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
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
            items(colaboradores, key = { it.rut }) { c ->
                Card(onClick = { navController.navigate(Ruta.ColaboradorForm.crear(c.rut)) }) {
                    ListItem(
                        headlineContent = { Text(c.nombre) },
                        supportingContent = {
                            Text("${c.perfil.etiqueta} · ${c.turno.etiqueta} · ${RutValidator.formatear(c.rut)}")
                        },
                        trailingContent = {
                            SuggestionChip(
                                onClick = { navController.navigate(Ruta.ColaboradorForm.crear(c.rut)) },
                                label = { Text(if (c.activo) "Activo" else "Inactivo") },
                                icon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                            )
                        }
                    )
                }
            }
            item {
                Text(
                    "Solo se guardan los datos necesarios para identificar al colaborador (RNF-06).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
