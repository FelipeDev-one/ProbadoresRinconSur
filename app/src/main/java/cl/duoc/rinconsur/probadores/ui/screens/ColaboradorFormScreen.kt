package cl.duoc.rinconsur.probadores.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.Perfil
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.viewmodel.ColaboradorViewModel

// Alta o edición de un colaborador (rut = null -> nuevo).
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColaboradorFormScreen(
    rut: String?,
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: ColaboradorViewModel = viewModel()
) {
    LaunchedEffect(rut) { viewModel.iniciar(rut) }
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BarraConVolver(
                stringResource(if (estado.esNuevo) R.string.titulo_colaborador_nuevo else R.string.titulo_colaborador_editar),
                onVolver
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = Dimens.anchoMaximoFormulario), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = estado.rut,
                    onCambio = viewModel::onRutChange,
                    etiqueta = "RUT",
                    icono = Icons.Filled.Badge,
                    error = estado.errores.rut,
                    ayuda = if (estado.esNuevo) "Con dígito verificador" else "El RUT no se puede modificar",
                    habilitado = estado.esNuevo,
                    opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Ascii, capitalization = KeyboardCapitalization.Characters)
                )
                CampoTexto(
                    valor = estado.nombre,
                    onCambio = viewModel::onNombreChange,
                    etiqueta = "Nombre completo",
                    icono = Icons.Filled.Person,
                    error = estado.errores.nombre,
                    ayuda = "Nombre y apellido",
                    opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Text("Perfil", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                    Perfil.entries.forEach { p ->
                        FilterChip(
                            selected = estado.perfil == p,
                            onClick = { viewModel.onPerfilChange(p) },
                            label = { Text(p.etiqueta) },
                            leadingIcon = if (estado.perfil == p) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
                MensajeError(estado.errores.perfil)

                Text("Turno habitual", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                    TurnoLaboral.entries.forEach { t ->
                        FilterChip(
                            selected = estado.turno == t,
                            onClick = { viewModel.onTurnoChange(t) },
                            label = { Text(t.etiqueta) },
                            leadingIcon = if (estado.turno == t) {
                                { Icon(Icons.Filled.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
                MensajeError(estado.errores.turno)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Activo", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Un colaborador inactivo no puede registrar acciones (RN-13)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(checked = estado.activo, onCheckedChange = viewModel::onActivoChange)
                }

                MensajeError(estado.errores.general)

                Button(
                    onClick = { viewModel.guardar(onGuardado) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.alturaBoton)
                ) { Text(stringResource(R.string.btn_guardar), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}
