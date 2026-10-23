package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.TipoProbador
import cl.duoc.rinconsur.probadores.model.UsoSinFilaErrores
import cl.duoc.rinconsur.probadores.navigation.Ruta
import cl.duoc.rinconsur.probadores.ui.components.AlertaCard
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.components.EstadoChip
import cl.duoc.rinconsur.probadores.ui.components.FotoEvidencia
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.ui.components.PanelScaffold
import cl.duoc.rinconsur.probadores.ui.components.ProbadorCard
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.Tiempo
import cl.duoc.rinconsur.probadores.viewmodel.ProbadorVista
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel

// =====================================================================
//  ProbadoresScreen — estado de los 8 probadores (HU-06, RF-02)
//  ---------------------------------------------------------------------
//  Grilla adaptable: 2 columnas en teléfono, más en tablet.
//  Al tocar un probador se abre un diálogo con SOLO las acciones válidas
//  para su estado (RN-01): así es difícil equivocarse (RNF-03).
// =====================================================================

@Composable
fun ProbadoresScreen(navController: NavController, viewModel: SeccionViewModel) {
    val panel by viewModel.panel.collectAsStateWithLifecycle()
    val sesion by viewModel.sesion.collectAsStateWithLifecycle()
    var seleccionado by rememberSaveable { mutableStateOf<String?>(null) }

    PanelScaffold(
        titulo = stringResource(R.string.titulo_probadores),
        rutaActual = Ruta.Probadores,
        navController = navController,
        viewModel = viewModel,
        acciones = {
            IconButton(onClick = { navController.navigate(Ruta.Cliente.ruta) }) {
                Icon(Icons.Filled.Visibility, contentDescription = stringResource(R.string.cd_ver_clientes))
            }
        }
    ) { padding ->
        val p = panel
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@PanelScaffold
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = Dimens.anchoMinimoTarjeta),
            contentPadding = PaddingValues(Dimens.espacioMedio),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ResumenRapido(libres = p.libres, habilitados = p.habilitados, personas = p.personasEnFila, estimado = p.estimadoNuevo)
            }

            // Alertas (RF-07): aparecen con animación para que se noten.
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                    p.alertas.forEach { alerta ->
                        key(alerta.titulo) {
                            val aparicion = remember { MutableTransitionState(false).apply { targetState = true } }
                            AnimatedVisibility(
                                visibleState = aparicion,
                                enter = expandVertically() + fadeIn()
                            ) { AlertaCard(alerta) }
                        }
                    }
                }
            }

            items(p.probadores, key = { it.probador.codigo }) { vista ->
                ProbadorCard(vista, onClick = { seleccionado = vista.probador.codigo })
            }

            // Ubicación física de cada probador (Aclaración nº 2)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "P-01 a P-04 lado mesón · P-05 a P-07 lado espejo · P-08 al fondo (accesible)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 80.dp)
                )
            }
        }

        val vista = p.probadores.find { it.probador.codigo == seleccionado }
        if (vista != null) {
            AccionesProbadorDialog(
                vista = vista,
                esJefatura = sesion?.perfil?.esJefatura == true,
                maxPrendas = p.parametros.maxPrendas,
                viewModel = viewModel,
                onFueraServicio = {
                    seleccionado = null
                    navController.navigate(Ruta.FueraServicio.crear(vista.probador.codigo))
                },
                onCerrar = { seleccionado = null }
            )
        }
    }
}

@Composable
private fun ResumenRapido(libres: Int, habilitados: Int, personas: Int, estimado: Int?) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico), modifier = Modifier.fillMaxWidth()) {
        AssistChip(onClick = {}, label = { Text("$libres de $habilitados libres") })
        AssistChip(onClick = {}, label = { Text("$personas en fila") }, leadingIcon = { Icon(Icons.Filled.Groups, null) })
        AssistChip(onClick = {}, label = { Text(Tiempo.estimado(estimado)) }, leadingIcon = { Icon(Icons.Filled.Timer, null) })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccionesProbadorDialog(
    vista: ProbadorVista,
    esJefatura: Boolean,
    maxPrendas: Int,
    viewModel: SeccionViewModel,
    onFueraServicio: () -> Unit,
    onCerrar: () -> Unit
) {
    val p = vista.probador
    var prendas by rememberSaveable(p.codigo) { mutableStateOf("") }
    var atraso by rememberSaveable(p.codigo) { mutableStateOf("") }
    var devueltas by rememberSaveable(p.codigo) { mutableStateOf("") }
    var error by rememberSaveable(p.codigo) { mutableStateOf<String?>(null) }
    var erroresSinFila by remember(p.codigo) { mutableStateOf(UsoSinFilaErrores()) }

    AlertDialog(
        onDismissRequest = onCerrar,
        title = {
            Column {
                Text(p.codigo + if (p.tipo == TipoProbador.ACCESIBLE) " · accesible" else "")
                EstadoChip(p.estado, Modifier.padding(top = 6.dp))
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Desde las ${Tiempo.hora(p.desde)} (${Tiempo.duracion(vista.minutosEnEstado)})")

                when (p.estado) {
                    // HU-03: asignar al siguiente turno, o registrar un cliente sin fila (Aclaración nº 1 y 3)
                    EstadoProbador.LIBRE -> {
                        vista.siguienteTurno?.let { turno ->
                            Button(
                                onClick = { viewModel.asignar(turno, p.codigo); onCerrar() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(Dimens.alturaBoton)
                            ) {
                                Text(
                                    "Asignar a ${turno.codigo}" + if (turno.preferente) " (preferente)" else "",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            HorizontalDivider()
                        }
                        Text("Cliente sin fila", style = MaterialTheme.typography.titleMedium)
                        CampoTexto(
                            valor = prendas,
                            onCambio = {
                                prendas = it.filter(Char::isDigit).take(2)
                                erroresSinFila = erroresSinFila.copy(prendas = null)
                            },
                            etiqueta = "Prendas",
                            icono = Icons.Filled.Checkroom,
                            error = erroresSinFila.prendas,
                            ayuda = "Entre 1 y $maxPrendas",
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        CampoTexto(
                            valor = atraso,
                            onCambio = {
                                atraso = it.filter(Char::isDigit).take(2)
                                erroresSinFila = erroresSinFila.copy(atraso = null)
                            },
                            etiqueta = "Entró hace (min, opcional)",
                            icono = Icons.Filled.History,
                            error = erroresSinFila.atraso,
                            ayuda = "Máximo ${ReglasSeccion.MAX_MINUTOS_ATRASO} min; queda marcado \"con atraso\"",
                            opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedButton(
                            onClick = {
                                erroresSinFila = viewModel.usoSinFila(p.codigo, prendas, atraso)
                                if (!erroresSinFila.hayErrores) onCerrar()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Iniciar uso sin fila") }
                    }

                    // HU-04: término del uso con prendas devueltas
                    EstadoProbador.OCUPADO -> {
                        val ingresadas = vista.uso?.prendasIngresadas ?: 0
                        Text("Ingresó $ingresadas prenda(s). ¿Cuántas devolvió en el mesón?")

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico),
                            verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
                        ) {
                            (0..ingresadas).forEach { n ->
                                FilterChip(
                                    selected = devueltas == n.toString(),
                                    onClick = { devueltas = n.toString(); error = null },
                                    label = { Text("$n", style = MaterialTheme.typography.titleMedium) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                error = viewModel.terminarUso(p.codigo, devueltas, ingresadas)
                                if (error == null) onCerrar()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.alturaBoton)
                        ) { Text("Registrar término del uso", style = MaterialTheme.typography.titleMedium) }
                        if (vista.ofrecerAyuda) {
                            Text(
                                "Hay personas esperando: puede acercarse cordialmente a ofrecer ayuda. Nunca se le pide salir (RN-08).",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    // HU-04 CA-2: fin del ordenamiento
                    EstadoProbador.EN_ORDENAMIENTO -> {
                        Text("Retire ganchos y prendas olvidadas antes de dejarlo libre.")
                        Button(
                            onClick = { viewModel.finalizarOrdenamiento(p.codigo); onCerrar() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.alturaBoton)
                        ) { Text("Probador listo", style = MaterialTheme.typography.titleMedium) }
                    }

                    // HU-08: fuera de servicio, con la foto de evidencia si la hay
                    EstadoProbador.FUERA_DE_SERVICIO -> {
                        Text("Motivo: ${p.motivoFueraServicio ?: "No indicado"}")
                        if (p.fotoEvidencia != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                FotoEvidencia(
                                    ruta = p.fotoEvidencia,
                                    descripcion = stringResource(R.string.cd_foto_evidencia, p.codigo),
                                    tamano = 160.dp
                                )
                            }
                        }
                        if (esJefatura) {
                            Button(
                                onClick = { viewModel.habilitar(p.codigo); onCerrar() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Habilitar probador") }
                        } else {
                            Text(
                                "Solo la jefatura puede volver a habilitarlo (RN-11).",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                MensajeError(error)

                // RN-11: solo la jefatura ve la opción de marcar fuera de servicio.
                // RN-01: un probador ocupado debe terminar su uso primero.
                if (esJefatura && p.estado != EstadoProbador.FUERA_DE_SERVICIO && p.estado != EstadoProbador.OCUPADO) {
                    HorizontalDivider()
                    TextButton(onClick = onFueraServicio, modifier = Modifier.align(Alignment.End)) {
                        Text("Marcar fuera de servicio", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onCerrar) { Text(stringResource(R.string.btn_cerrar)) } }
    )
}
