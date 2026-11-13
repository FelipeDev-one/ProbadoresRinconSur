package cl.duoc.rinconsur.probadores.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessible
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.ReglasSeccion
import cl.duoc.rinconsur.probadores.model.TurnoLaboral
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.BotonPrincipal
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.components.CamposNuevaContrasena
import cl.duoc.rinconsur.probadores.ui.components.MensajeError
import cl.duoc.rinconsur.probadores.ui.components.PanelCodigoRecuperacion
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.utils.RutVisualTransformation
import cl.duoc.rinconsur.probadores.viewmodel.ConfiguracionUiState
import cl.duoc.rinconsur.probadores.viewmodel.ConfiguracionViewModel
import cl.duoc.rinconsur.probadores.viewmodel.PasoConfiguracion

// =====================================================================
//  ConfiguracionInicialScreen — asistente de la primera vez
//  ---------------------------------------------------------------------
//  Paso 1 de 3: cuenta de jefatura   →   Paso 2 de 3: probadores
//                                     →   Paso 3 de 3: código de recuperación
//  Al confirmar el paso 3 se guarda todo y la app abre el panel.
// =====================================================================

@Composable
fun ConfiguracionInicialScreen(viewModel: ConfiguracionViewModel = viewModel()) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    val volver: (() -> Unit)? = when (estado.paso) {
        PasoConfiguracion.PROBADORES -> viewModel::volverAJefatura
        else -> null
    }
    BackHandler(enabled = estado.paso != PasoConfiguracion.JEFATURA) { volver?.invoke() }

    Scaffold(
        topBar = { BarraConVolver("Configuración inicial", onVolver = volver) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LinearProgressIndicator(
                progress = { (estado.paso.ordinal + 1) / 3f },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.espacioGrande),
                contentAlignment = Alignment.TopCenter
            ) {
                AnimatedContent(
                    targetState = estado.paso,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "pasoConfiguracion",
                    modifier = Modifier.widthIn(max = Dimens.anchoMaximoFormulario)
                ) { paso ->
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
                    ) {
                        when (paso) {
                            PasoConfiguracion.JEFATURA -> PasoJefatura(estado, viewModel)
                            PasoConfiguracion.PROBADORES -> PasoProbadores(estado, viewModel)
                            PasoConfiguracion.CODIGO -> PanelCodigoRecuperacion(
                                codigo = estado.codigoGenerado ?: "",
                                textoBoton = "Guardar y entrar",
                                cargando = estado.guardando,
                                onConfirmar = viewModel::finalizar
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Encabezado(paso: Int, titulo: String, detalle: String, icono: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icono, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
    Text("Paso $paso de 3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(titulo, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        detalle,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PasoJefatura(estado: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Encabezado(
        paso = 1,
        titulo = "Bienvenido(a) a ${stringResource(R.string.marca)}",
        detalle = "La app aún no tiene datos. Cree la cuenta de jefatura: con ella podrá agregar al resto del equipo.",
        icono = Icons.Filled.Checkroom
    )

    val rutValido = RutValidator.esValido(estado.rut)
    CampoTexto(
        valor = estado.rut,
        onCambio = viewModel::onRutChange,
        etiqueta = "RUT",
        icono = Icons.Filled.Badge,
        error = estado.errores.rut,
        ayuda = if (rutValido) "RUT válido" else "Los puntos y el guion se agregan solos",
        placeholder = "12.345.678-9",
        transformacionVisual = RutVisualTransformation,
        accionFinal = if (rutValido && estado.errores.rut == null) {
            { Icon(Icons.Filled.CheckCircle, contentDescription = "RUT válido", tint = MaterialTheme.colorScheme.primary) }
        } else null,
        opcionesTeclado = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Next
        )
    )
    CampoTexto(
        valor = estado.nombre,
        onCambio = viewModel::onNombreChange,
        etiqueta = "Nombre completo",
        icono = Icons.Filled.Person,
        error = estado.errores.nombre,
        ayuda = "Nombre y apellido",
        opcionesTeclado = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Next
        )
    )

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
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
        MensajeError(estado.errores.turno)
    }

    CamposNuevaContrasena(
        contrasena = estado.contrasena,
        confirmacion = estado.confirmacion,
        errorContrasena = estado.errores.contrasena,
        errorConfirmacion = estado.errores.confirmacion,
        onContrasena = viewModel::onContrasenaChange,
        onConfirmacion = viewModel::onConfirmacionChange,
        onDone = viewModel::continuarAProbadores,
        etiqueta = "Contraseña"
    )
    BotonPrincipal("Continuar", cargando = false, onClick = viewModel::continuarAProbadores)
}

@Composable
private fun PasoProbadores(estado: ConfiguracionUiState, viewModel: ConfiguracionViewModel) {
    Encabezado(
        paso = 2,
        titulo = "Probadores de la sección",
        detalle = "Indique cuántos probadores hay. Después podrá agregar, cambiar o quitar probadores desde Ajustes.",
        icono = Icons.Filled.MeetingRoom
    )
    CampoTexto(
        valor = estado.estandar,
        onCambio = viewModel::onEstandarChange,
        etiqueta = "Probadores estándar",
        icono = Icons.Filled.MeetingRoom,
        error = estado.errores.estandar,
        ayuda = "Entre 0 y ${ReglasSeccion.MAX_PROBADORES}",
        placeholder = "Ej: 7",
        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
    )
    CampoTexto(
        valor = estado.accesibles,
        onCambio = viewModel::onAccesiblesChange,
        etiqueta = "Probadores accesibles (opcional)",
        icono = Icons.Filled.Accessible,
        error = estado.errores.accesibles,
        ayuda = "Más amplios; tienen prioridad para la atención preferente",
        placeholder = "Opcional: vacío = ninguno",
        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        accionesTeclado = KeyboardActions(onDone = { viewModel.continuarACodigo() })
    )

    // Vista previa de lo que se va a crear.
    val estandar = estado.estandar.toIntOrNull() ?: 0
    val accesibles = estado.accesibles.toIntOrNull() ?: 0
    val total = estandar + accesibles
    if (total in 1..ReglasSeccion.MAX_PROBADORES) {
        val rango = if (total == 1) ReglasSeccion.codigoProbador(1)
        else "${ReglasSeccion.codigoProbador(1)} a ${ReglasSeccion.codigoProbador(total)}"
        val detalleAccesibles = when (accesibles) {
            0 -> ""
            1 -> " El accesible será ${ReglasSeccion.codigoProbador(total)}."
            else -> " Los accesibles serán los últimos $accesibles."
        }
        Text(
            "Se crearán $total probador(es): $rango.$detalleAccesibles",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
    }

    MensajeError(estado.errores.general, Modifier.fillMaxWidth())
    BotonPrincipal("Continuar", estado.guardando, onClick = viewModel::continuarACodigo)
}
