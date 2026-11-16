package cl.duoc.rinconsur.probadores.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.BotonPrincipal
import cl.duoc.rinconsur.probadores.ui.components.CampoContrasena
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.components.CamposNuevaContrasena
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.ui.components.PanelCodigoRecuperacion
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.viewmodel.PerfilViewModel

// =====================================================================
//  MiPerfilScreen — ver y editar los datos propios
//  ---------------------------------------------------------------------
//  Se puede editar: el nombre (todos) y el turno habitual (colaboradores).
//  Solo lectura: RUT y rol (el rol lo define la jefatura; el RUT identifica la cuenta).
//  Desde aquí se cambia la contraseña y se cierra la sesión.
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiPerfilScreen(
    onVolver: () -> Unit,
    onCambiarClave: () -> Unit,
    onCerrarSesion: () -> Unit,
    viewModel: PerfilViewModel = viewModel()
) {
    val estado by viewModel.perfil.collectAsStateWithLifecycle()
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    val cuenta = estado.cuenta

    Scaffold(topBar = { BarraConVolver("Mi perfil", onVolver) }) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            contentAlignment = Alignment.TopCenter
        ) {
            if (cuenta != null) Column(
                Modifier.widthIn(max = Dimens.anchoMaximoFormulario),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text(cuenta.iniciales, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Column {
                        Text(cuenta.nombre, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${cuenta.rol} · RUT ${RutValidator.formatear(cuenta.rut)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (estado.actividad.isNotBlank()) {
                    Text(estado.actividad, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                        Text("Mis datos", style = MaterialTheme.typography.titleMedium)
                        CampoTexto(
                            valor = estado.nombre,
                            onCambio = viewModel::onNombreChange,
                            etiqueta = "Nombre completo",
                            icono = Icons.Filled.Person,
                            error = estado.errorNombre,
                            ayuda = "Nombre y apellido",
                            opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done)
                        )
                        if (!cuenta.esCliente) {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
                                Text("Turno habitual", style = MaterialTheme.typography.titleSmall)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                                    TurnoLaboral.entries.forEach { turno ->
                                        FilterChip(
                                            selected = estado.turno == turno,
                                            onClick = { viewModel.onTurnoChange(turno) },
                                            label = { Text(turno.etiqueta) }
                                        )
                                    }
                                }
                            }
                        }
                        MensajeError(estado.errorGeneral)
                        BotonPrincipal(
                            texto = if (estado.guardado) "Cambios guardados" else "Guardar cambios",
                            cargando = estado.guardando,
                            habilitado = estado.hayCambios(viewModel.turnoOriginal),
                            onClick = viewModel::guardarDatos
                        )
                        Text(
                            if (cuenta.esCliente) "El RUT identifica su cuenta y no se puede cambiar."
                            else "El RUT y el perfil solo los puede cambiar la jefatura.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = onCambiarClave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.alturaBotonChico)
                ) {
                    Icon(Icons.Filled.Key, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Cambiar contraseña")
                }
                OutlinedButton(
                    onClick = { confirmarSalida = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.alturaBotonChico)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.btn_cerrar_sesion))
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrá que ingresar con su RUT y contraseña nuevamente.") },
            confirmButton = {
                Button(onClick = { confirmarSalida = false; onCerrarSesion() }) {
                    Text(stringResource(R.string.btn_cerrar_sesion))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text(stringResource(R.string.btn_cancelar)) }
            }
        )
    }
}

// =====================================================================
//  CambiarClaveScreen — contraseña actual + nueva (dos veces)
//  Al cambiarla se muestra un código de recuperación nuevo: el anterior deja de servir.
// =====================================================================

@Composable
fun CambiarClaveScreen(onVolver: () -> Unit, viewModel: PerfilViewModel = viewModel()) {
    val estado by viewModel.clave.collectAsStateWithLifecycle()
    val codigo = estado.codigoGenerado

    // Con el código en pantalla no se sale con "atrás": hay que confirmar que se anotó.
    BackHandler(enabled = codigo != null) {}

    Scaffold(
        topBar = { BarraConVolver("Cambiar contraseña", onVolver = onVolver.takeIf { codigo == null }) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioGrande),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier.widthIn(max = Dimens.anchoMaximoFormulario),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
            ) {
                if (codigo != null) {
                    PanelCodigoRecuperacion(
                        codigo = codigo,
                        textoBoton = "Listo",
                        cargando = false,
                        onConfirmar = onVolver
                    )
                } else {
                    CampoContrasena(
                        valor = estado.actual,
                        onCambio = viewModel::onActualChange,
                        etiqueta = "Contraseña actual",
                        error = estado.errorActual,
                        imeAction = ImeAction.Next
                    )
                    CamposNuevaContrasena(
                        contrasena = estado.nueva,
                        confirmacion = estado.confirmacion,
                        errorContrasena = estado.errorNueva,
                        errorConfirmacion = estado.errorConfirmacion,
                        onContrasena = viewModel::onNuevaChange,
                        onConfirmacion = viewModel::onConfirmacionChange,
                        onDone = viewModel::cambiarClave
                    )
                    BotonPrincipal("Cambiar contraseña", estado.guardando, onClick = viewModel::cambiarClave)
                }
            }
        }
    }
}
