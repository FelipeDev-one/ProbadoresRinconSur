package cl.duoc.rinconsur.probadores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cl.duoc.rinconsur.probadores.navigation.AppNavegacion
import cl.duoc.rinconsur.probadores.ui.theme.ProbadoresRinconSurTheme

// Única Activity de la app: todo lo demás son pantallas @Composable
// que cambian dentro de AppNavegacion (navigation/AppNavegacion.kt).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProbadoresRinconSurTheme {
                AppNavegacion()
            }
        }
    }
}
