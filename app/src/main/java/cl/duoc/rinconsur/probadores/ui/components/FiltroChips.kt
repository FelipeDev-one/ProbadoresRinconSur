package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duoc.rinconsur.probadores.ui.theme.Dimens

// Una opción de filtro: su texto y cuántos elementos quedan si se elige.
data class OpcionFiltro<T>(val valor: T, val etiqueta: String, val cantidad: Int? = null)

// Fila de chips de filtro con desplazamiento horizontal: se elige una opción a la vez.
// La cantidad entre paréntesis ayuda a decidir sin tener que abrir cada filtro.
@Composable
fun <T> FiltroChips(
    opciones: List<OpcionFiltro<T>>,
    seleccionado: T,
    onSeleccion: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.espacioChico),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        opciones.forEach { opcion ->
            val elegido = opcion.valor == seleccionado
            FilterChip(
                selected = elegido,
                onClick = { onSeleccion(opcion.valor) },
                label = { Text(opcion.etiqueta + (opcion.cantidad?.let { " ($it)" } ?: "")) },
                leadingIcon = if (elegido) {
                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.then(Modifier)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors()
            )
        }
    }
}
