package cl.duoc.rinconsur.probadores.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.Colaborador
import cl.duoc.rinconsur.probadores.model.LoginUiState
import cl.duoc.rinconsur.probadores.model.PasoLogin
import cl.duoc.rinconsur.probadores.ui.components.BarraConVolver
import cl.duoc.rinconsur.probadores.ui.components.CampoTexto
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.Contrasenas
import cl.duoc.rinconsur.probadores.utils.RutValidator
import cl.duoc.rinconsur.probadores.utils.RutVisualTransformation
import cl.duoc.rinconsur.probadores.viewmodel.LoginViewModel

// =====================================================================
//  LoginScreen — ingreso en pasos
//  ---------------------------------------------------------------------
//  RUT ─┬─ asistente / vendedor(a) ─────────────────────────▶ panel
//       └─ jefatura ─┬─ tiene contraseña ─▶ CONTRASEÑA ──────▶ panel
//                    │                        └─ ¿la olvidó? ─▶ RECUPERAR ─┐
//                    └─ primera vez ───────▶ CREAR ───────────────────────┴▶ CÓDIGO NUEVO ─▶ panel
// =====================================================================

@Composable
fun LoginScreen(
    onVolver: () -> Unit,
    onIngreso: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val recientes by viewModel.recientes.collectAsStateWithLifecycle()

    // Atrás retrocede un paso; en el código nuevo no se puede salir sin confirmarlo.
    val volverPaso: () -> Unit = {
        when (estado.paso) {
            PasoLogin.RUT -> onVolver()
            PasoLogin.RECUPERAR -> viewModel.volverAContrasena()
            PasoLogin.CODIGO_NUEVO -> Unit
            else -> viewModel.volverAlRut()
        }
    }
    BackHandler(enabled = estado.paso != PasoLogin.RUT, onBack = volverPaso)

    Scaffold(
        topBar = { BarraConVolver(stringResource(R.string.titulo_login), volverPaso) }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.espacioGrande),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = estado.paso,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "pasoLogin",
                modifier = Modifier.widthIn(max = Dimens.anchoMaximoFormulario)
            ) { paso ->
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
                ) {
                    when (paso) {
                        PasoLogin.RUT -> PasoRut(estado, recientes, viewModel, onIngreso)
                        PasoLogin.CONTRASENA -> PasoContrasena(estado, viewModel, onIngreso)
                        PasoLogin.CREAR_CONTRASENA -> PasoCrear(estado, viewModel)
                        PasoLogin.RECUPERAR -> PasoRecuperar(estado, viewModel)
                        PasoLogin.CODIGO_NUEVO -> PasoCodigoNuevo(estado, viewModel, onIngreso)
                    }
                }
            }
        }
    }
}

// --------------------------------- PASO 1 ---------------------------------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PasoRut(
    estado: LoginUiState,
    recientes: List<Colaborador>,
    viewModel: LoginViewModel,
    onIngreso: () -> Unit
) {
    Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    Text("Hola, bienvenido(a)", style = MaterialTheme.typography.headlineSmall)
    Text(
        if (recientes.isEmpty()) "Ingrese su RUT para registrar acciones en la sección"
        else "Toque su nombre o ingrese su RUT",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    if (recientes.isNotEmpty()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.espacioChico)) {
            Text("Accesos recientes", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
                recientes.forEach { c ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(76.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .combinedClickable(
                                enabled = !estado.cargando,
                                onClickLabel = "Ingresar como ${c.nombreCorto}",
                                onLongClickLabel = "Quitar de accesos recientes",
                                onClick = { viewModel.seleccionarReciente(c, onIngreso) },
                                onLongClick = { viewModel.quitarReciente(c) }
                            )
                            .padding(vertical = Dimens.espacioMini)
                    ) {
                        Avatar(c.iniciales, tamano = 52)
                        Text(c.nombreCorto, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
            Text(
                "Mantenga presionado un nombre para quitarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val rutValido = RutValidator.esValido(estado.rut)
    CampoTexto(
        valor = estado.rut,
        onCambio = viewModel::onRutChange,
        etiqueta = "RUT",
        icono = Icons.Filled.Person,
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
            imeAction = ImeAction.Done
        ),
        accionesTeclado = KeyboardActions(onDone = { viewModel.continuar(onIngreso) })
    )
    BotonPrincipal("Continuar", estado.cargando) { viewModel.continuar(onIngreso) }
    TextButton(onClick = viewModel::olvideContrasena, enabled = !estado.cargando) {
        Text("¿Olvidó su contraseña?")
    }
}

// ------------------------------- CONTRASEÑA -------------------------------

@Composable
private fun PasoContrasena(estado: LoginUiState, viewModel: LoginViewModel, onIngreso: () -> Unit) {
    CabeceraUsuario(estado, onNoSoyYo = viewModel::volverAlRut)
    CampoContrasena(
        valor = estado.contrasena,
        onCambio = viewModel::onContrasenaChange,
        etiqueta = "Contraseña",
        error = estado.errores.contrasena,
        imeAction = ImeAction.Done,
        onDone = { viewModel.ingresarConContrasena(onIngreso) }
    )
    BotonPrincipal("Ingresar", estado.cargando) { viewModel.ingresarConContrasena(onIngreso) }
    TextButton(onClick = viewModel::irARecuperar, enabled = !estado.cargando) {
        Text("¿Olvidó su contraseña?")
    }
}

@Composable
private fun PasoCrear(estado: LoginUiState, viewModel: LoginViewModel) {
    CabeceraUsuario(estado, onNoSoyYo = viewModel::volverAlRut)
    Aviso(
        Icons.Filled.Lock,
        "Primera vez en este equipo",
        "Cree una contraseña para proteger las acciones de jefatura. La pediremos cada vez que ingrese."
    )
    CamposNuevaContrasena(estado, viewModel, etiqueta = "Nueva contraseña") { viewModel.crearContrasena() }
    BotonPrincipal("Crear contraseña", estado.cargando) { viewModel.crearContrasena() }
}

@Composable
private fun PasoRecuperar(estado: LoginUiState, viewModel: LoginViewModel) {
    CabeceraUsuario(estado, onNoSoyYo = viewModel::volverAlRut)
    Aviso(
        Icons.Filled.LockReset,
        "Recupere su acceso",
        "Ingrese el código de recuperación que anotó al crear su contraseña y elija una nueva."
    )
    CampoTexto(
        valor = estado.codigo,
        onCambio = viewModel::onCodigoChange,
        etiqueta = "Código de recuperación",
        icono = Icons.Filled.Key,
        error = estado.errores.codigo,
        placeholder = "XXXX-XXXX",
        estiloTexto = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
        opcionesTeclado = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            capitalization = KeyboardCapitalization.Characters,
            imeAction = ImeAction.Next
        )
    )
    CamposNuevaContrasena(estado, viewModel, etiqueta = "Nueva contraseña") { viewModel.recuperar() }
    BotonPrincipal("Guardar contraseña", estado.cargando) { viewModel.recuperar() }
    Text(
        "¿No tiene el código? Pida a otra jefatura que restablezca su contraseña en Ajustes → Colaboradores.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PasoCodigoNuevo(estado: LoginUiState, viewModel: LoginViewModel, onIngreso: () -> Unit) {
    var anotado by rememberSaveable { mutableStateOf(false) }

    Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
    Text("Guarde su código de recuperación", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        "Si olvida su contraseña, lo necesitará para crear una nueva. Anótelo en un lugar seguro: no se volverá a mostrar.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            estado.codigoGenerado ?: "",
            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.espacioGrande)
        )
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { anotado = !anotado }
    ) {
        Checkbox(checked = anotado, onCheckedChange = { anotado = it })
        Text("Ya anoté el código", style = MaterialTheme.typography.bodyLarge)
    }
    Button(
        onClick = { viewModel.confirmarCodigoGuardado(onIngreso) },
        enabled = anotado,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.alturaBoton)
    ) {
        Text("Ingresar al panel", style = MaterialTheme.typography.titleMedium)
    }
}

// ------------------------------- COMPONENTES -------------------------------

@Composable
private fun Avatar(iniciales: String, tamano: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(tamano.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            iniciales,
            style = if (tamano >= 52) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// "Hola, Patricia · Jefatura · No soy yo"
@Composable
private fun CabeceraUsuario(estado: LoginUiState, onNoSoyYo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)
    ) {
        Avatar(estado.iniciales, tamano = 56)
        Column(Modifier.weight(1f)) {
            Text("Hola, ${estado.primerNombre}", style = MaterialTheme.typography.titleLarge)
            Text(
                "Jefatura · RUT ${RutValidator.formatear(RutValidator.normalizar(estado.rut))}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onNoSoyYo, enabled = !estado.cargando) { Text("No soy yo") }
    }
}

@Composable
private fun Aviso(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, texto: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(Dimens.espacioMedio), horizontalArrangement = Arrangement.spacedBy(Dimens.espacioMedio)) {
            Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.espacioMini)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun CampoContrasena(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    ayuda: String? = null,
    imeAction: ImeAction,
    onDone: () -> Unit = {}
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    CampoTexto(
        valor = valor,
        onCambio = onCambio,
        etiqueta = etiqueta,
        icono = Icons.Filled.Password,
        error = error,
        ayuda = ayuda,
        transformacionVisual = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        accionFinal = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        opcionesTeclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        accionesTeclado = KeyboardActions(onDone = { onDone() })
    )
}

@Composable
private fun CamposNuevaContrasena(
    estado: LoginUiState,
    viewModel: LoginViewModel,
    etiqueta: String,
    onDone: () -> Unit
) {
    CampoContrasena(
        valor = estado.contrasena,
        onCambio = viewModel::onContrasenaChange,
        etiqueta = etiqueta,
        error = estado.errores.contrasena,
        ayuda = "Mínimo ${Contrasenas.LARGO_MINIMO} caracteres, con letras y números",
        imeAction = ImeAction.Next
    )
    CampoContrasena(
        valor = estado.confirmacion,
        onCambio = viewModel::onConfirmacionChange,
        etiqueta = "Repita la contraseña",
        error = estado.errores.confirmacion,
        imeAction = ImeAction.Done,
        onDone = onDone
    )
}

@Composable
private fun BotonPrincipal(texto: String, cargando: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.alturaBoton)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 3.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}
