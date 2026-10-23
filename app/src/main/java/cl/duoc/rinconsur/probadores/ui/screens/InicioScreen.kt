package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.ui.theme.ProbadoresRinconSurTheme

// Pantalla de entrada: separa al CLIENTE (sin sesión) del COLABORADOR (con RUT).
@Composable
fun InicioScreen(haySesion: Boolean, onCliente: () -> Unit, onColaborador: () -> Unit) {
    var visible by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioGrande),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.espacioPantalla, Alignment.CenterVertically)
        ) {
            Icon(
                Icons.Filled.Checkroom,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(stringResource(R.string.marca), style = MaterialTheme.typography.headlineLarge)
            Text(
                stringResource(R.string.sucursal),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
            ) {
                Column(
                    Modifier.widthIn(max = Dimens.anchoMaximoFormulario),
                    verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
                ) {
                    OpcionInicio(
                        icono = Icons.Filled.Timer,
                        titulo = stringResource(R.string.inicio_cliente_titulo),
                        detalle = stringResource(R.string.inicio_cliente_detalle),
                        onClick = onCliente
                    )
                    OpcionInicio(
                        icono = Icons.Filled.Badge,
                        titulo = stringResource(
                            if (haySesion) R.string.inicio_continuar_titulo else R.string.inicio_colaborador_titulo
                        ),
                        detalle = stringResource(R.string.inicio_colaborador_detalle),
                        onClick = onColaborador
                    )
                }
            }
        }
    }
}

@Composable
private fun OpcionInicio(icono: ImageVector, titulo: String, detalle: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(Dimens.espacioPantalla),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(Dimens.tamanoIconoGrande), tint = MaterialTheme.colorScheme.secondary)
            Column {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InicioPreview() {
    ProbadoresRinconSurTheme {
        InicioScreen(haySesion = false, onCliente = {}, onColaborador = {})
    }
}
