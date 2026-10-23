package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.viewmodel.ParametrosViewModel

// Parámetros de la sección que la jefatura ajusta sin depender de nadie (RD-05).
@Composable
fun ParametrosScreen(
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: ParametrosViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val e = estado.errores

    Scaffold(
        topBar = { BarraConVolver(stringResource(R.string.titulo_parametros), onVolver) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = Dimens.anchoMaximoFormulario), verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                CampoNumero("Máximo de prendas por ingreso", estado.maxPrendas, e.maxPrendas, Icons.Filled.Checkroom, "Entre 1 y 15", viewModel::onMaxPrendasChange)
                CampoNumero("Tiempo de referencia de uso (min)", estado.tiempoReferenciaUso, e.tiempoReferenciaUso, Icons.Filled.Timer, "Entre 1 y 60", viewModel::onTiempoReferenciaChange)
                CampoNumero("Umbral de uso prolongado (min)", estado.umbralProlongado, e.umbralProlongado, Icons.Filled.HourglassBottom, "Entre 5 y 60", viewModel::onUmbralProlongadoChange)
                CampoNumero("Umbral para ofrecer ayuda (min)", estado.umbralAyuda, e.umbralAyuda, Icons.Filled.SupportAgent, "Mayor que el umbral de uso prolongado", viewModel::onUmbralAyudaChange)
                CampoNumero("Tiempo para presentarse al ser llamado (min)", estado.minutosPresentarse, e.minutosPresentarse, Icons.Filled.Campaign, "Entre 1 y 10", viewModel::onMinutosPresentarseChange)
                CampoNumero("Ticket promedio de referencia ($)", estado.ticketPromedio, e.ticketPromedio, Icons.Filled.Payments, "Entre 1.000 y 1.000.000", viewModel::onTicketChange)

                MensajeError(e.general)

                Button(
                    onClick = { viewModel.guardar(onGuardado) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.alturaBoton)
                ) { Text("Guardar parámetros", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

@Composable
private fun CampoNumero(
    etiqueta: String,
    valor: String,
    error: String?,
    icono: ImageVector,
    ayuda: String,
    onChange: (String) -> Unit
) {
    CampoTexto(
        valor = valor,
        onCambio = onChange,
        etiqueta = etiqueta,
        icono = icono,
        error = error,
        ayuda = ayuda,
        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}
