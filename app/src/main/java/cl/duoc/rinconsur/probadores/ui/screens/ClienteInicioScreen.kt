package cl.duoc.rinconsur.probadores.ui.screens

import cl.duoc.rinconsur.probadores.ui.components.ClimaCard
import cl.duoc.rinconsur.probadores.utils.Notificaciones
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.EstadoTurno
import cl.duoc.rinconsur.probadores.model.MotivoAbandono
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.BotonPrincipal
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.Tiempo
import cl.duoc.rinconsur.probadores.viewmodel.PanelUiState
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel
import cl.duoc.rinconsur.probadores.viewmodel.TurnoClienteVista

// =====================================================================
//  ClienteInicioScreen — la vista propia de la cuenta de cliente
//  ---------------------------------------------------------------------
//  Sin turno:  estado de los probadores + formulario "Tomar un turno".
//  Con turno:  su número en grande, cuántas personas tiene antes, la espera
//              estimada (en vivo) y el aviso cuando lo llaman. Puede retirarse.
//  El cliente solo ve y maneja SU turno: no ve la fila de los demás.
// =====================================================================

@Composable
fun ClienteInicioScreen(viewModel: SeccionViewModel, onPerfil: () -> Unit) {
    val cliente by viewModel.sesionCliente.collectAsStateWithLifecycle()
    val panel by viewModel.panel.collectAsStateWithLifecycle()
    val turno by viewModel.turnoCliente.collectAsStateWithLifecycle()

    // El aviso de "turno llamado" lo envía SeccionViewModel, aunque la app esté en segundo plano.
    val contexto = LocalContext.current
    // El permiso se pide en el momento en que tiene sentido: al tomar el turno.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val tomar: (String, Boolean) -> String? = { prendas, preferente ->
        val error = viewModel.tomarTurnoCliente(prendas, preferente)
        if (error == null && Notificaciones.requierePermiso && !Notificaciones.tienePermiso(contexto)) {
            pedirPermiso.launch(Notificaciones.PERMISO)
        }
        error
    }

    Scaffold(
        topBar = {
            BarraConVolver(
                titulo = cliente?.let { "Hola, ${it.primerNombre}" } ?: stringResource(R.string.marca),
                onVolver = null,
                acciones = {
                    IconButton(onClick = onPerfil) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Mi perfil")
                    }
                }
            )
        }
    ) { padding ->
        val p = panel
        if (p == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioPantalla),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                Modifier.widthIn(max = Dimens.anchoMaximoFormulario),
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
            ) {
                ClimaCard()
                val t = turno
                if (t != null) {
                    MiTurno(t, onRetirar = viewModel::retirarTurnoCliente)
                } else {
                    EstadoSeccion(p)
                    TomarTurno(p, onTomar = tomar)
                }
            }
        }
    }
}

// ------------------------------- CON TURNO -------------------------------

@Composable
private fun MiTurno(vista: TurnoClienteVista, onRetirar: (MotivoAbandono) -> Unit) {
    var retirando by rememberSaveable { mutableStateOf(false) }
    val turno = vista.turno

    if (turno.estado == EstadoTurno.LLAMADO) {
        TurnoLlamado(turno.codigo)
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(Dimens.espacioGrande),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
            ) {
                Text("Su turno", style = MaterialTheme.typography.titleMedium)
                Text(turno.codigo, style = MaterialTheme.typography.displayLarge)
                Text(
                    if (vista.personasAntes == 0) "Usted es el siguiente"
                    else "${vista.personasAntes} persona(s) antes que usted",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    when (val e = vista.estimado) {
                        null -> "Espera estimada no disponible"
                        0 -> "Sin espera: acérquese al mesón"
                        else -> "Espera aproximada: $e min"
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    Text(
        "Tomado a las ${Tiempo.hora(turno.horaIngreso)} · ${turno.prendas} prenda(s)" +
            if (turno.preferente) " · atención preferente" else "",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (turno.estado != EstadoTurno.LLAMADO) Text(
        "Esta pantalla se actualiza sola. Cuando sea su turno le llegará una notificación.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OutlinedButton(
        onClick = { retirando = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.alturaBotonChico)
    ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("Retirarme de la fila")
    }

    if (retirando) {
        val motivos = MotivoAbandono.entries.filter { it.seleccionable && it != MotivoAbandono.NO_INFORMADO }
        var motivo by rememberSaveable { mutableStateOf<MotivoAbandono?>(null) }
        AlertDialog(
            onDismissRequest = { retirando = false },
            title = { Text("¿Retirarse de la fila?") },
            text = {
                Column(Modifier.selectableGroup()) {
                    Text("Perderá su turno ${turno.codigo}. ¿Nos cuenta por qué? (opcional)")
                    motivos.forEach { opcion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = motivo == opcion,
                                    onClick = { motivo = opcion },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = Dimens.espacioMini)
                        ) {
                            RadioButton(selected = motivo == opcion, onClick = null)
                            Spacer(Modifier.width(Dimens.espacioChico))
                            Text(opcion.etiqueta)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRetirar(motivo ?: MotivoAbandono.NO_INFORMADO)
                    retirando = false
                }) { Text("Retirarme") }
            },
            dismissButton = {
                TextButton(onClick = { retirando = false }) { Text("Seguir esperando") }
            }
        )
    }
}

// ------------------------------- SIN TURNO -------------------------------

@Composable
private fun EstadoSeccion(p: PanelUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
                Icon(Icons.Filled.Checkroom, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Probadores de Vestuario", style = MaterialTheme.typography.titleMedium)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Dato("${p.libres}", "libres")
                Dato("${p.personasEnFila}", "en la fila")
                Dato(
                    when (val e = p.estimadoNuevo) {
                        null -> "—"
                        0 -> "0"
                        else -> "~$e"
                    },
                    "min de espera"
                )
            }
        }
    }
}

@Composable
private fun Dato(valor: String, etiqueta: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// Formulario del cliente: solo prendas y si necesita atención preferente (RN-01, RN-02).
@Composable
private fun TomarTurno(p: PanelUiState, onTomar: (prendas: String, preferente: Boolean) -> String?) {
    var prendas by rememberSaveable { mutableStateOf("1") }
    var preferente by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val maximo = p.parametros.maxPrendas
    val cerrada = p.motivoFilaCerrada

    fun cambiar(delta: Int) {
        prendas = ((prendas.toIntOrNull() ?: 0) + delta).coerceIn(1, maximo).toString()
        error = null
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(Dimens.espacioMedio), verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
            Text("Tomar un turno", style = MaterialTheme.typography.titleLarge)
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
            ) {
                FilledTonalIconButton(onClick = { cambiar(-1) }, modifier = Modifier.padding(top = Dimens.espacioChico)) {
                    Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.cd_quitar_prenda))
                }
                CampoTexto(
                    valor = prendas,
                    onCambio = { prendas = it.filter(Char::isDigit).take(2); error = null },
                    etiqueta = "Prendas que va a probarse",
                    icono = Icons.Filled.ConfirmationNumber,
                    error = error,
                    ayuda = "Entre 1 y $maximo",
                    modifier = Modifier.weight(1f),
                    opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )
                FilledTonalIconButton(onClick = { cambiar(1) }, modifier = Modifier.padding(top = Dimens.espacioChico)) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_agregar_prenda))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Necesito atención preferente", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Movilidad reducida, embarazo, adulto mayor o niño pequeño",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = preferente, onCheckedChange = { preferente = it })
            }
            MensajeError(cerrada)
            BotonPrincipal("Tomar turno", cargando = false, habilitado = cerrada == null) {
                error = onTomar(prendas, preferente)
            }
        }
    }
}
