package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Password
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import cl.duoc.rinconsur.probadores.utils.Contrasenas

// =====================================================================
//  Acceso — piezas compartidas por el ingreso y la configuración inicial
//  ---------------------------------------------------------------------
//  Campo de contraseña con "ojo", par de campos para una contraseña nueva,
//  panel del código de recuperación y botón principal con indicador de carga.
// =====================================================================

@Composable
fun CampoContrasena(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    imeAction: ImeAction,
    ayuda: String? = null,
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

// Contraseña nueva + repetición, con la regla escrita bajo el primer campo.
@Composable
fun CamposNuevaContrasena(
    contrasena: String,
    confirmacion: String,
    errorContrasena: String?,
    errorConfirmacion: String?,
    onContrasena: (String) -> Unit,
    onConfirmacion: (String) -> Unit,
    onDone: () -> Unit,
    etiqueta: String = "Nueva contraseña"
) {
    CampoContrasena(
        valor = contrasena,
        onCambio = onContrasena,
        etiqueta = etiqueta,
        error = errorContrasena,
        ayuda = "Mínimo ${Contrasenas.LARGO_MINIMO} caracteres, con letras y números",
        imeAction = ImeAction.Next
    )
    CampoContrasena(
        valor = confirmacion,
        onCambio = onConfirmacion,
        etiqueta = "Repita la contraseña",
        error = errorConfirmacion,
        imeAction = ImeAction.Done,
        onDone = onDone
    )
}

// El código se muestra UNA vez: hay que marcar que se anotó para poder continuar.
@Composable
fun PanelCodigoRecuperacion(
    codigo: String,
    textoBoton: String,
    cargando: Boolean,
    onConfirmar: () -> Unit
) {
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
            codigo,
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
    BotonPrincipal(textoBoton, cargando, habilitado = anotado, onClick = onConfirmar)
}

@Composable
fun BotonPrincipal(texto: String, cargando: Boolean, habilitado: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
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
