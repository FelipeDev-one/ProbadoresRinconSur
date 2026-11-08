package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
// En las jefaturas se muestra si ya tienen contraseña y se puede restablecer la de otra.
@Composable
fun ColaboradoresScreen(navController: NavController, viewModel: SeccionViewModel) {
    val seccion by viewModel.seccion.collectAsStateWithLifecycle()
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    val conContrasena by viewModel.rutsConContrasena.collectAsStateWithLifecycle()
    var porRestablecer by remember { mutableStateOf<Colaborador?>(null) }

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
                val tieneContrasena = c.rut in conContrasena
                Card(onClick = { navController.navigate(Ruta.ColaboradorForm.crear(c.rut)) }) {
                    ListItem(
                        headlineContent = { Text(c.nombre) },
                        supportingContent = {
                            Column {
                                Text("${c.perfil.etiqueta} · ${c.turno.etiqueta} · ${RutValidator.formatear(c.rut)}")
                                if (c.perfil.esJefatura) {
                                    Text(
                                        if (tieneContrasena) "Contraseña configurada" else "Contraseña pendiente: la crea al ingresar",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        trailingContent = {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
                                SuggestionChip(
                                    onClick = { navController.navigate(Ruta.ColaboradorForm.crear(c.rut)) },
                                    label = { Text(if (c.activo) "Activo" else "Inactivo") },
                                    icon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                                )
                                if (c.perfil.esJefatura && tieneContrasena && c.rut != sesion?.rut) {
                                    SuggestionChip(
                                        onClick = { porRestablecer = c },
                                        label = { Text("Restablecer") },
                                        icon = { Icon(Icons.Filled.LockReset, contentDescription = null) }
                                    )
                                }
                            }
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

    porRestablecer?.let { c ->
        AlertDialog(
            onDismissRequest = { porRestablecer = null },
            icon = { Icon(Icons.Filled.LockReset, contentDescription = null) },
            title = { Text("¿Restablecer contraseña?") },
            text = {
                Text("Se borrará la contraseña de ${c.nombre}. La próxima vez que ingrese deberá crear una nueva y recibirá otro código de recuperación.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.restablecerContrasena(c)
                    porRestablecer = null
                }) { Text("Restablecer") }
            },
            dismissButton = {
                TextButton(onClick = { porRestablecer = null }) { Text("Cancelar") }
            }
        )
    }
}
