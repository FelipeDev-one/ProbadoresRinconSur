package cl.duoc.rinconsur.probadores.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.ui.components.MensajeFlotante
import cl.duoc.rinconsur.probadores.ui.screens.AjustesScreen
import cl.duoc.rinconsur.probadores.ui.screens.ClienteScreen
import cl.duoc.rinconsur.probadores.ui.screens.ColaboradorFormScreen
import cl.duoc.rinconsur.probadores.ui.screens.ColaboradoresScreen
import cl.duoc.rinconsur.probadores.ui.screens.FilaScreen
import cl.duoc.rinconsur.probadores.ui.screens.FueraServicioScreen
import cl.duoc.rinconsur.probadores.ui.screens.InicioScreen
import cl.duoc.rinconsur.probadores.ui.screens.LoginScreen
import cl.duoc.rinconsur.probadores.ui.screens.NuevoTurnoScreen
import cl.duoc.rinconsur.probadores.ui.screens.ParametrosScreen
import cl.duoc.rinconsur.probadores.ui.screens.ProbadoresScreen
import cl.duoc.rinconsur.probadores.ui.screens.ResumenScreen
import cl.duoc.rinconsur.probadores.ui.screens.TurnoConfirmadoScreen
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel
import kotlinx.coroutines.delay

// =====================================================================
//  AppNavegacion — mapa de pantallas de la app
//  ---------------------------------------------------------------------
//               ┌─ Cliente (sin sesión: espera y "consultar mi turno")
//   Inicio ─────┤
//               └─ Login ─▶ Probadores ⇄ Fila ⇄ Resumen ⇄ Ajustes   (barra inferior)
//                              │          │                  │
//                     FueraServicio   NuevoTurno ─▶      Colaboradores ─▶ ColaboradorForm
//                       (cámara)      TurnoConfirmado    Parámetros
//
//  SeccionViewModel se crea UNA vez aquí y se comparte con todas las pantallas
//  del panel: así todos ven la misma información en tiempo real (Aclaración nº 7).
// =====================================================================

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    val seccionViewModel: SeccionViewModel = viewModel()
    val cargando by seccionViewModel.cargando.collectAsStateWithLifecycle()
    val sesion by seccionViewModel.sesion.collectAsStateWithLifecycle()
    val mensaje by seccionViewModel.mensaje.collectAsStateWithLifecycle()

    // Pantalla de carga mientras se leen (o crean) los datos de ejemplo.
    if (cargando) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.marca), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimary)
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                Text(stringResource(R.string.cargando_seccion), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        return
    }

    // ------------------------- GUARDIAS DE ACCESO -------------------------
    // RF-10 / RN-11: sin sesión no se entra al panel; sin perfil de jefatura
    // no se entra a las pantallas de jefatura (aunque alguien llegue por error).
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = Ruta.desdePatron(entradaActual?.destination?.route)
    LaunchedEffect(sesion, rutaActual) {
        val ruta = rutaActual ?: return@LaunchedEffect
        val actual = sesion
        if (ruta.requiereSesion && actual == null) {
            // Se espera un instante: justo después de ingresar, la sesión tarda unos
            // milisegundos en llegar desde DataStore. Si llega, este efecto se cancela solo.
            delay(400)
            navController.navigate(Ruta.Login.ruta) {
                popUpTo(Ruta.Inicio.ruta)
                launchSingleTop = true
            }
        } else if (ruta.soloJefatura && actual != null && !actual.perfil.esJefatura) {
            seccionViewModel.mostrarMensaje("Solo la jefatura puede entrar aquí (RN-11)", true)
            navController.popBackStack()
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Ruta.Inicio.ruta,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) + fadeIn(tween(300))
            },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) + fadeOut(tween(300))
            }
        ) {
            // ----------------------------- SIN SESIÓN -----------------------------
            composable(Ruta.Inicio.ruta) {
                InicioScreen(
                    haySesion = sesion != null,
                    onCliente = { navController.navigate(Ruta.Cliente.ruta) },
                    onColaborador = {
                        navController.navigate(if (sesion != null) Ruta.Probadores.ruta else Ruta.Login.ruta)
                    }
                )
            }
            composable(Ruta.Cliente.ruta) {
                ClienteScreen(seccionViewModel, onVolver = { navController.navigateUp() })
            }
            composable(Ruta.Login.ruta) {
                LoginScreen(
                    onVolver = { navController.navigateUp() },
                    onIngreso = {
                        navController.navigate(Ruta.Probadores.ruta) {
                            popUpTo(Ruta.Inicio.ruta)
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ------------------------- PANEL (barra inferior) ------------------------
            composable(Ruta.Probadores.ruta) {
                ProbadoresScreen(navController, seccionViewModel)
            }
            composable(Ruta.Fila.ruta) {
                FilaScreen(navController, seccionViewModel)
            }
            composable(Ruta.Resumen.ruta) {
                ResumenScreen(navController, seccionViewModel)
            }
            composable(Ruta.Ajustes.ruta) {
                AjustesScreen(navController, seccionViewModel)
            }

            // -------------------------------- TURNOS --------------------------------
            composable(Ruta.NuevoTurno.ruta) {
                NuevoTurnoScreen(
                    seccionViewModel = seccionViewModel,
                    onVolver = { navController.navigateUp() },
                    onRegistrado = { codigo, minutos ->
                        navController.navigate(Ruta.TurnoConfirmado.crear(codigo, minutos)) {
                            // "Nuevo turno" sale de la pila: Volver desde la confirmación lleva a la fila.
                            popUpTo(Ruta.NuevoTurno.ruta) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route = Ruta.TurnoConfirmado.ruta,
                arguments = listOf(
                    navArgument("codigo") { type = NavType.StringType },
                    navArgument("minutos") { type = NavType.IntType }
                )
            ) { entrada ->
                TurnoConfirmadoScreen(
                    codigo = entrada.arguments?.getString("codigo") ?: "",
                    minutos = entrada.arguments?.getInt("minutos")?.takeIf { it >= 0 },
                    onVolverFila = { navController.navigateUp() },
                    onOtroTurno = {
                        navController.navigate(Ruta.NuevoTurno.ruta) {
                            popUpTo(Ruta.TurnoConfirmado.ruta) { inclusive = true }
                        }
                    }
                )
            }

            // ------------------------------- JEFATURA -------------------------------
            composable(
                route = Ruta.FueraServicio.ruta,
                arguments = listOf(navArgument("codigo") { type = NavType.StringType })
            ) { entrada ->
                FueraServicioScreen(
                    codigo = entrada.arguments?.getString("codigo") ?: "",
                    onVolver = { navController.navigateUp() },
                    onGuardado = {
                        seccionViewModel.mostrarMensaje("Probador marcado fuera de servicio")
                        navController.navigateUp()
                    }
                )
            }
            composable(Ruta.Colaboradores.ruta) {
                ColaboradoresScreen(navController, seccionViewModel)
            }
            composable(
                route = Ruta.ColaboradorForm.ruta,
                arguments = listOf(navArgument("rut") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { entrada ->
                ColaboradorFormScreen(
                    rut = entrada.arguments?.getString("rut"),
                    onVolver = { navController.navigateUp() },
                    onGuardado = {
                        seccionViewModel.mostrarMensaje("Colaborador guardado")
                        navController.navigateUp()
                    }
                )
            }
            composable(Ruta.Parametros.ruta) {
                ParametrosScreen(
                    onVolver = { navController.navigateUp() },
                    onGuardado = {
                        seccionViewModel.mostrarMensaje("Parámetros actualizados")
                        navController.navigateUp()
                    }
                )
            }
        }

        // Avisos de éxito / error de cualquier pantalla, sobre la barra inferior.
        MensajeFlotante(
            mensaje = mensaje,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp)
        )
    }
}
