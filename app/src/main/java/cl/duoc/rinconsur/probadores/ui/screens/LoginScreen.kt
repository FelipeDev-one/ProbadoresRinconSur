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
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.Spacer
import cl.duoc.rinconsur.probadores.ui.components.BotonPrincipal
import cl.duoc.rinconsur.probadores.ui.components.CampoContrasena
import cl.duoc.rinconsur.probadores.ui.components.CamposNuevaContrasena
import cl.duoc.rinconsur.probadores.ui.components.PanelCodigoRecuperacion
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.model.Cuenta
import cl.duoc.rinconsur.probadores.model.Perfil
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
    onIngreso: (Perfil?) -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val recientes by viewModel.recientes.collectAsStateWithLifecycle()

    // Atrás retrocede un paso; en el código nuevo no se puede salir sin confirmarlo.
    val volverPaso: () -> Unit = {
        when (estado.paso) {
            PasoLogin.RUT -> Unit
            PasoLogin.COLABORADOR -> viewModel.irAClientes()
            PasoLogin.RECUPERAR -> viewModel.volverAContrasena()
            PasoLogin.REGISTRO -> viewModel.volverAlRut()
            PasoLogin.CODIGO_NUEVO -> Unit
            else -> viewModel.volverAlRut()
        }
    }
    BackHandler(enabled = estado.paso != PasoLogin.RUT, onBack = volverPaso)

    Scaffold(
        // Es la primera pantalla de la app: en el paso del RUT no hay a dónde volver.
        topBar = {
            BarraConVolver(
                if (estado.modoColaborador) "Ingreso de colaboradores" else stringResource(R.string.titulo_login),
                onVolver = volverPaso.takeIf { estado.paso != PasoLogin.RUT && estado.paso != PasoLogin.CODIGO_NUEVO }
            )
        }
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
                        PasoLogin.RUT -> PasoRut(estado, recientes.filter { it.esCliente }, viewModel)
                        PasoLogin.COLABORADOR -> PasoColaborador(estado, recientes.filter { !it.esCliente }, viewModel)
                        PasoLogin.CONTRASENA -> PasoContrasena(estado, viewModel, onIngreso)
                        PasoLogin.CREAR_CONTRASENA -> PasoCrear(estado, viewModel)
                        PasoLogin.RECUPERAR -> PasoRecuperar(estado, viewModel)
                        PasoLogin.REGISTRO -> PasoRegistro(estado, viewModel)
                        PasoLogin.CODIGO_NUEVO -> PasoCodigoNuevo(estado, viewModel, onIngreso)
                    }
                }
            }
        }
    }
}

// --------------------------------- PASO 1 ---------------------------------

// Pantalla inicial de la app: pensada para el CLIENTE. El personal tiene su propio ingreso.
@Composable
private fun PasoRut(
    estado: LoginUiState,
    recientes: List<Cuenta>,
    viewModel: LoginViewModel
) {
    Icon(Icons.Filled.Checkroom, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.marca), style = MaterialTheme.typography.headlineMedium)
    Text(
        "Probadores de Vestuario",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        "Tome su turno y siga su espera desde el teléfono, sin hacer fila.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )

    BotonPrincipal("Crear mi cuenta", estado.cargando) { viewModel.irARegistro() }

    HorizontalDivider()
    Text("¿Ya tiene cuenta?", style = MaterialTheme.typography.titleMedium)
    AccesosRecientes(recientes, estado.cargando, viewModel)
    CampoRut(estado.rut, viewModel::onRutChange, estado.errores.rut, ImeAction.Done) { viewModel.continuar() }
    OutlinedButton(
        onClick = viewModel::continuar,
        enabled = !estado.cargando,
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.alturaBoton)
    ) {
        Text("Ingresar", style = MaterialTheme.typography.titleMedium)
    }
    TextButton(onClick = viewModel::olvideContrasena, enabled = !estado.cargando) {
        Text("¿Olvidó su contraseña?")
    }

    HorizontalDivider()
    TextButton(onClick = viewModel::irAColaboradores, enabled = !estado.cargando) {
        Icon(Icons.Filled.Badge, contentDescription = null)
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text("¿Trabaja en la tienda? Ingreso de colaboradores")
    }
}

// Ingreso del personal: asistentes, vendedores(as) y jefatura. Sus cuentas las crea la jefatura.
@Composable
private fun PasoColaborador(
    estado: LoginUiState,
    recientes: List<Cuenta>,
    viewModel: LoginViewModel
) {
    Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
    Text("Ingreso de colaboradores", style = MaterialTheme.typography.headlineSmall)
    Text(
        stringResource(R.string.sucursal),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    Text(
        if (recientes.isEmpty()) "Ingrese con su RUT y contraseña" else "Toque su nombre o ingrese su RUT",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
    AccesosRecientes(recientes, estado.cargando, viewModel)
    CampoRut(estado.rut, viewModel::onRutChange, estado.errores.rut, ImeAction.Done) { viewModel.continuar() }
    BotonPrincipal("Continuar", estado.cargando) { viewModel.continuar() }
    TextButton(onClick = viewModel::olvideContrasena, enabled = !estado.cargando) {
        Text("¿Olvidó su contraseña?")
    }
    Text(
        "Su cuenta la crea la jefatura en Ajustes → Colaboradores. En su primer ingreso elegirá su contraseña.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccesosRecientes(recientes: List<Cuenta>, cargando: Boolean, viewModel: LoginViewModel) {
    if (recientes.isEmpty()) return
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
                            enabled = !cargando,
                            onClickLabel = "Ingresar como ${c.nombreCorto}",
                            onLongClickLabel = "Quitar de accesos recientes",
                            onClick = { viewModel.seleccionarReciente(c) },
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

// ---------------------------- REGISTRO DE CLIENTE ----------------------------

@Composable
private fun PasoRegistro(estado: LoginUiState, viewModel: LoginViewModel) {
    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
    Text("Crear cuenta de cliente", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        "Con su cuenta podrá tomar un turno para los probadores y seguir su espera desde el teléfono.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
    CampoRut(estado.rut, viewModel::onRutChange, estado.errores.rut, ImeAction.Next) {}
    CampoTexto(
        valor = estado.nombre,
        onCambio = viewModel::onNombreChange,
        etiqueta = "Nombre completo",
        icono = Icons.Filled.Person,
        error = estado.errores.nombre,
        ayuda = "Nombre y apellido",
        opcionesTeclado = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next)
    )
    CamposNuevaContrasena(
        contrasena = estado.contrasena,
        confirmacion = estado.confirmacion,
        errorContrasena = estado.errores.contrasena,
        errorConfirmacion = estado.errores.confirmacion,
        onContrasena = viewModel::onContrasenaChange,
        onConfirmacion = viewModel::onConfirmacionChange,
        onDone = viewModel::registrarCliente,
        etiqueta = "Contraseña"
    )
    BotonPrincipal("Crear mi cuenta", estado.cargando) { viewModel.registrarCliente() }
    Text(
        "Solo guardamos su RUT y su nombre para identificar su turno.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun CampoRut(
    valor: String,
    onCambio: (String) -> Unit,
    error: String?,
    imeAction: ImeAction,
    onDone: () -> Unit
) {
    val rutValido = RutValidator.esValido(valor)
    CampoTexto(
        valor = valor,
        onCambio = onCambio,
        etiqueta = "RUT",
        icono = Icons.Filled.Badge,
        error = error,
        ayuda = if (rutValido) "RUT válido" else "Los puntos y el guion se agregan solos",
        placeholder = "12.345.678-9",
        transformacionVisual = RutVisualTransformation,
        accionFinal = if (rutValido && error == null) {
            { Icon(Icons.Filled.CheckCircle, contentDescription = "RUT válido", tint = MaterialTheme.colorScheme.primary) }
        } else null,
        opcionesTeclado = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            capitalization = KeyboardCapitalization.Characters,
            imeAction = imeAction
        ),
        accionesTeclado = KeyboardActions(onDone = { onDone() })
    )
}

// ------------------------------- CONTRASEÑA -------------------------------

@Composable
private fun PasoContrasena(estado: LoginUiState, viewModel: LoginViewModel, onIngreso: (Perfil?) -> Unit) {
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
        "Primer ingreso",
        "La jefatura creó su cuenta. Elija ahora su contraseña: la pediremos cada vez que ingrese."
    )
    NuevaContrasena(estado, viewModel) { viewModel.crearContrasena() }
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
    NuevaContrasena(estado, viewModel) { viewModel.recuperar() }
    BotonPrincipal("Guardar contraseña", estado.cargando) { viewModel.recuperar() }
    Text(
        "¿No tiene el código? Si es colaborador, pida a la jefatura que restablezca su contraseña en Ajustes → Colaboradores.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun PasoCodigoNuevo(estado: LoginUiState, viewModel: LoginViewModel, onIngreso: (Perfil?) -> Unit) {
    PanelCodigoRecuperacion(
        codigo = estado.codigoGenerado ?: "",
        textoBoton = "Ingresar",
        cargando = estado.cargando,
        onConfirmar = { viewModel.confirmarCodigoGuardado(onIngreso) }
    )
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
                "${estado.rol} · RUT ${RutValidator.formatear(RutValidator.normalizar(estado.rut))}",
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
private fun NuevaContrasena(estado: LoginUiState, viewModel: LoginViewModel, onDone: () -> Unit) {
    CamposNuevaContrasena(
        contrasena = estado.contrasena,
        confirmacion = estado.confirmacion,
        errorContrasena = estado.errores.contrasena,
        errorConfirmacion = estado.errores.confirmacion,
        onContrasena = viewModel::onContrasenaChange,
        onConfirmacion = viewModel::onConfirmacionChange,
        onDone = onDone
    )
}
