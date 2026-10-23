package cl.duoc.rinconsur.probadores.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.navigation.Ruta
import cl.duoc.rinconsur.probadores.utils.Vibracion
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel

// =====================================================================
//  PanelScaffold — esqueleto común del panel de colaboradores
//  ---------------------------------------------------------------------
//   ┌ TopAppBar ─ título + "nombre · perfil" de la sesión
//   │ contenido de cada pantalla
//   └ NavigationBar ─ Probadores · Fila · Resumen (solo jefatura) · Ajustes
//
//  Las pestañas usan saveState/restoreState: al volver a una pestaña,
//  su scroll y estado siguen donde estaban.
// =====================================================================

private data class Pestana(
    val ruta: Ruta,
    @StringRes val titulo: Int,
    val icono: ImageVector
)

private val pestanas = listOf(
    Pestana(Ruta.Probadores, R.string.nav_probadores, Icons.Filled.Checkroom),
    Pestana(Ruta.Fila, R.string.nav_fila, Icons.Filled.People),
    Pestana(Ruta.Resumen, R.string.nav_resumen, Icons.Filled.BarChart),
    Pestana(Ruta.Ajustes, R.string.nav_ajustes, Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelScaffold(
    titulo: String,
    rutaActual: Ruta,
    navController: NavController,
    viewModel: SeccionViewModel,
    acciones: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    val panel by viewModel.panel.collectAsStateWithLifecycle()
    val urgentes = panel?.alertas?.count { it.urgente } ?: 0
    val enFila = panel?.personasEnFila ?: 0
    val esJefatura = sesion?.perfil?.esJefatura == true

    // Vibra solo cuando AUMENTAN las alertas urgentes (no cada segundo).
    val contexto = LocalContext.current
    var urgentesPrevias by remember { mutableIntStateOf(urgentes) }
    LaunchedEffect(urgentes) {
        if (urgentes > urgentesPrevias) Vibracion.avisar(contexto)
        urgentesPrevias = urgentes
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titulo)
                        sesion?.let {
                            Text(
                                "${it.nombreCorto} · ${it.perfil.etiqueta}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                },
                actions = acciones,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar {
                pestanas
                    .filter { !it.ruta.soloJefatura || esJefatura }
                    .forEach { pestana ->
                        val contador = when (pestana.ruta) {
                            Ruta.Probadores -> urgentes
                            Ruta.Fila -> enFila
                            else -> 0
                        }
                        NavigationBarItem(
                            selected = rutaActual == pestana.ruta,
                            onClick = {
                                if (rutaActual != pestana.ruta) {
                                    navController.navigate(pestana.ruta.ruta) {
                                        popUpTo(Ruta.Probadores.ruta) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                if (contador > 0) {
                                    BadgedBox(badge = { Badge { Text("$contador") } }) {
                                        Icon(pestana.icono, contentDescription = null)
                                    }
                                } else {
                                    Icon(pestana.icono, contentDescription = null)
                                }
                            },
                            label = { Text(stringResource(pestana.titulo)) }
                        )
                    }
            }
        },
        floatingActionButton = floatingActionButton,
        content = content
    )
}
