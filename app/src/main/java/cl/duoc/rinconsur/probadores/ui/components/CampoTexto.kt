package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import cl.duoc.rinconsur.probadores.ui.theme.Dimens

// =====================================================================
//  CampoTexto — campo de formulario reutilizable
//  ---------------------------------------------------------------------
//  El campo NO guarda su texto: "valor" viene del estado del ViewModel y
//  "onCambio" le avisa cada letra escrita (estado elevado / state hoisting).
//  Si hay "error", el borde se pone rojo y el mensaje reemplaza a la ayuda.
// =====================================================================

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    error: String?,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
    placeholder: String? = null,
    prefijo: String? = null,
    habilitado: Boolean = true,
    unaLinea: Boolean = true,
    lineasMinimas: Int = 1,
    estiloTexto: TextStyle? = null,
    opcionesTeclado: KeyboardOptions = KeyboardOptions.Default,
    accionesTeclado: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        placeholder = placeholder?.let { texto -> { Text(texto) } },
        prefix = prefijo?.let { texto -> { Text(texto) } },
        leadingIcon = { Icon(icono, contentDescription = null) },
        trailingIcon = if (error != null) {
            { Icon(Icons.Filled.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error) }
        } else null,
        isError = error != null,
        supportingText = (error ?: ayuda)?.let { texto ->
            {
                Text(
                    texto,
                    color = if (error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        enabled = habilitado,
        singleLine = unaLinea,
        minLines = lineasMinimas,
        textStyle = estiloTexto ?: MaterialTheme.typography.bodyLarge,
        keyboardOptions = opcionesTeclado,
        keyboardActions = accionesTeclado,
        modifier = modifier.fillMaxWidth()
    )
}

// Mensaje de error que aparece y desaparece con animación (para errores sin campo propio).
@Composable
fun MensajeError(texto: String?, modifier: Modifier = Modifier) {
    // Se recuerda el último texto para que no quede vacío mientras se anima la salida.
    var ultimo by remember { mutableStateOf(texto ?: "") }
    LaunchedEffect(texto) { if (texto != null) ultimo = texto }

    AnimatedVisibility(
        visible = texto != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico)
        ) {
            Icon(
                Icons.Filled.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimens.tamanoIcono)
            )
            Text(texto ?: ultimo, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
