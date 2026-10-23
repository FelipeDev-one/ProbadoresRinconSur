package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.theme.CifraCliente
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.viewmodel.ConsultaTurno
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel

// =====================================================================
//  ClienteScreen — pantalla para el punto de espera (RF-06, HU-01)
//  ---------------------------------------------------------------------
//  Solo información AGREGADA: libres, personas en la fila y espera (RN-11).
//  Letra muy grande, alto contraste e íconos además del color (RNF-05, RNF-07).
//  En pantallas anchas (tablet en el mesón) los indicadores van en fila.
// =====================================================================

@Composable
fun ClienteScreen(viewModel: SeccionViewModel, onVolver: () -> Unit) {
    val panel by viewModel.panel.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { BarraConVolver(stringResource(R.string.titulo_cliente), onVolver) }
    ) { padding ->
        val p = panel
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioPantalla)
        ) {
            MensajePrincipal(p.libres, p.personasEnFila, p.estimadoNuevo)

            BoxWithConstraints {
                val ancho = maxWidth > 600.dp
                val textoEspera = when (val e = p.estimadoNuevo) {
                    null -> "—"
                    0 -> "0"
                    else -> "~$e"
                }
                if (ancho) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                        Indicador(Icons.Filled.Checkroom, "Probadores libres", "${p.libres}", Modifier.weight(1f))
                        Indicador(Icons.Filled.Groups, "Personas esperando", "${p.personasEnFila}", Modifier.weight(1f))
                        Indicador(Icons.Filled.Timer, "Espera estimada", textoEspera, Modifier.weight(1f), sufijo = "min")
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                        Indicador(Icons.Filled.Checkroom, "Probadores libres", "${p.libres}", Modifier.fillMaxWidth())
                        Indicador(Icons.Filled.Groups, "Personas esperando", "${p.personasEnFila}", Modifier.fillMaxWidth())
                        Indicador(Icons.Filled.Timer, "Espera estimada", textoEspera, Modifier.fillMaxWidth(), sufijo = "min")
                    }
                }
            }

            // HU-01 CA-3: si aún no hay datos del día, se avisa que es un estimado de referencia.
            if (p.usandoReferencia && p.personasEnFila > 0) {
                Text(
                    "Estimado calculado con el tiempo de referencia de la sección.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ConsultaMiTurno(viewModel)
        }
    }
}

@Composable
private fun MensajePrincipal(libres: Int, personas: Int, estimado: Int?) {
    val sinEspera = libres > 0 && personas == 0
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (sinEspera) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(Dimens.espacioPantalla),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            Icon(
                if (sinEspera) Icons.Filled.CheckCircle else Icons.Filled.Timer,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Text(
                when {
                    sinEspera -> "¡No necesita esperar! Acérquese al mesón."
                    estimado == null -> "Por ahora no hay probadores disponibles."
                    else -> "Espera aproximada: $estimado minutos. Gracias por su paciencia."
                },
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}

@Composable
private fun Indicador(icono: ImageVector, titulo: String, valor: String, modifier: Modifier, sufijo: String? = null) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(Dimens.espacioPantalla),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                Icon(icono, contentDescription = null, modifier = Modifier.size(32.dp))
                Text(titulo, style = MaterialTheme.typography.titleLarge)
            }
            // La cifra "rueda" hacia arriba cuando cambia, para que se note el cambio.
            AnimatedContent(
                targetState = valor,
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                },
                label = "cifra"
            ) { texto ->
                Text(texto, style = CifraCliente, color = MaterialTheme.colorScheme.primary)
            }
            sufijo?.let { Text(it, style = MaterialTheme.typography.titleLarge) }
        }
    }
}

// RD-01: el cliente consulta su propio turno por número, sin dar datos personales.
@Composable
private fun ConsultaMiTurno(viewModel: SeccionViewModel) {
    var numero by rememberSaveable { mutableStateOf("") }
    var resultado by remember { mutableStateOf<ConsultaTurno?>(null) }
    val foco = LocalFocusManager.current
    val consultar = {
        resultado = viewModel.consultarTurno(numero)
        foco.clearFocus()
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.espacioPantalla), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("¿Ya tiene número de turno?", style = MaterialTheme.typography.titleLarge)
            val error = (resultado as? ConsultaTurno.Invalido)?.error
            CampoTexto(
                valor = numero,
                onCambio = { numero = it.filter(Char::isDigit).take(3); resultado = null },
                etiqueta = "Número de turno",
                icono = Icons.Filled.ConfirmationNumber,
                error = error,
                ayuda = "El número que le entregaron en el mesón",
                placeholder = "Ej: 24",
                prefijo = "T-",
                opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                accionesTeclado = KeyboardActions(onSearch = { consultar() })
            )
            Button(
                onClick = consultar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.alturaBoton)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                Text("Consultar mi turno", style = MaterialTheme.typography.titleMedium)
            }

            AnimatedVisibility(
                visible = resultado != null && resultado !is ConsultaTurno.Invalido,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                when (val r = resultado) {
                    is ConsultaTurno.EnEspera -> Text(
                        "${r.codigo}: " +
                            (if (r.personasAntes == 0) "usted es el siguiente." else "hay ${r.personasAntes} persona(s) antes que usted.") +
                            (r.estimado?.let { " Espera aproximada: $it min." } ?: ""),
                        style = MaterialTheme.typography.titleLarge
                    )
                    is ConsultaTurno.Llamado -> TurnoLlamado(r.codigo)
                    is ConsultaTurno.Cerrado -> Text("${r.codigo} ya no está en la fila (${r.estado}).", style = MaterialTheme.typography.titleLarge)
                    ConsultaTurno.NoEncontrado -> Text("No encontramos ese turno hoy. Consulte en el mesón.", style = MaterialTheme.typography.titleLarge)
                    is ConsultaTurno.Invalido, null -> Unit
                }
            }
        }
    }
}

@Composable
private fun TurnoLlamado(codigo: String) {
    val latido = rememberInfiniteTransition(label = "latido")
    val escala by latido.animateFloat(1f, 1.06f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "escala")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .scale(escala)
    ) {
        Row(Modifier.padding(Dimens.espacioPantalla), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Dimens.tamanoIconoGrande)
            )
            Text(
                "¡Es su turno, $codigo! Acérquese al mesón.",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
