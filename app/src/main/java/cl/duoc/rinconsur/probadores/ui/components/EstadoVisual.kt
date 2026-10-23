package cl.duoc.rinconsur.probadores.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.duoc.rinconsur.probadores.model.EstadoProbador
import cl.duoc.rinconsur.probadores.ui.theme.EstadoFuera
import cl.duoc.rinconsur.probadores.ui.theme.EstadoFueraFondo
import cl.duoc.rinconsur.probadores.ui.theme.EstadoFueraFondoOscuro
import cl.duoc.rinconsur.probadores.ui.theme.EstadoFueraOscuro
import cl.duoc.rinconsur.probadores.ui.theme.EstadoLibre
import cl.duoc.rinconsur.probadores.ui.theme.EstadoLibreFondo
import cl.duoc.rinconsur.probadores.ui.theme.EstadoLibreFondoOscuro
import cl.duoc.rinconsur.probadores.ui.theme.EstadoLibreOscuro
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOcupado
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOcupadoFondo
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOcupadoFondoOscuro
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOcupadoOscuro
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOrden
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOrdenFondo
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOrdenFondoOscuro
import cl.duoc.rinconsur.probadores.ui.theme.EstadoOrdenOscuro

// Cada estado del probador (RN-01) tiene ícono + texto + color: nunca solo color (RNF-07).

fun iconoEstado(estado: EstadoProbador): ImageVector = when (estado) {
    EstadoProbador.LIBRE -> Icons.Filled.CheckCircle
    EstadoProbador.OCUPADO -> Icons.Filled.Person
    EstadoProbador.EN_ORDENAMIENTO -> Icons.Filled.CleaningServices
    EstadoProbador.FUERA_DE_SERVICIO -> Icons.Filled.Block
}

@Composable
fun colorEstado(estado: EstadoProbador): Color {
    val oscuro = isSystemInDarkTheme()
    return when (estado) {
        EstadoProbador.LIBRE -> if (oscuro) EstadoLibreOscuro else EstadoLibre
        EstadoProbador.OCUPADO -> if (oscuro) EstadoOcupadoOscuro else EstadoOcupado
        EstadoProbador.EN_ORDENAMIENTO -> if (oscuro) EstadoOrdenOscuro else EstadoOrden
        EstadoProbador.FUERA_DE_SERVICIO -> if (oscuro) EstadoFueraOscuro else EstadoFuera
    }
}

@Composable
fun fondoEstado(estado: EstadoProbador): Color {
    val oscuro = isSystemInDarkTheme()
    return when (estado) {
        EstadoProbador.LIBRE -> if (oscuro) EstadoLibreFondoOscuro else EstadoLibreFondo
        EstadoProbador.OCUPADO -> if (oscuro) EstadoOcupadoFondoOscuro else EstadoOcupadoFondo
        EstadoProbador.EN_ORDENAMIENTO -> if (oscuro) EstadoOrdenFondoOscuro else EstadoOrdenFondo
        EstadoProbador.FUERA_DE_SERVICIO -> if (oscuro) EstadoFueraFondoOscuro else EstadoFueraFondo
    }
}

@Composable
fun EstadoChip(estado: EstadoProbador, modifier: Modifier = Modifier) {
    val color = colorEstado(estado)
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(iconoEstado(estado), contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(estado.etiqueta, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
