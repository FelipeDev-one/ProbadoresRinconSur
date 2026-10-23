package cl.duoc.rinconsur.probadores.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EsquemaClaro = lightColorScheme(
    primary = Petroleo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4E8F2),
    onPrimaryContainer = Color(0xFF001F27),
    secondary = Terracota,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCC),
    onSecondaryContainer = Color(0xFF380D00),
    tertiaryContainer = Color(0xFFE6DEFF),
    onTertiaryContainer = Color(0xFF1D1148),
    background = Color(0xFFFBF8F4),
    onBackground = Grafito,
    surface = Color(0xFFFBF8F4),
    onSurface = Grafito,
    surfaceVariant = Arena,
    onSurfaceVariant = Color(0xFF45474A),
    surfaceContainer = Color(0xFFF2EEE8),
    surfaceContainerHigh = Color(0xFFECE7E1),
    error = Color(0xFFBA1A1A)
)

private val EsquemaOscuro = darkColorScheme(
    primary = PetroleoClaro,
    onPrimary = Color(0xFF003543),
    primaryContainer = Color(0xFF004E60),
    onPrimaryContainer = Color(0xFFC4E8F2),
    secondary = TerracotaClaro,
    onSecondary = Color(0xFF5A1C00),
    secondaryContainer = Color(0xFF7F2C00),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiaryContainer = Color(0xFF3E3470),
    onTertiaryContainer = Color(0xFFE6DEFF),
    background = Color(0xFF111416),
    onBackground = Color(0xFFE2E2E5),
    surface = Color(0xFF111416),
    onSurface = Color(0xFFE2E2E5),
    surfaceVariant = Color(0xFF3F4447),
    onSurfaceVariant = Color(0xFFC3C7CA),
    surfaceContainer = Color(0xFF1C2023),
    surfaceContainerHigh = Color(0xFF262B2E)
)

@Composable
fun ProbadoresRinconSurTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content
    )
}
