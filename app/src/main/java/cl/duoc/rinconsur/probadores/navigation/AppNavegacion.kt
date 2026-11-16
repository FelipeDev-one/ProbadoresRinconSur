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
import androidx.compose.runtime.remember
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
import cl.duoc.rinconsur.probadores.ui.screens.MiPerfilScreen
import cl.duoc.rinconsur.probadores.ui.screens.ClienteInicioScreen
import cl.duoc.rinconsur.probadores.ui.screens.CambiarClaveScreen
import cl.duoc.rinconsur.probadores.ui.screens.ClienteScreen
import cl.duoc.rinconsur.probadores.ui.screens.ColaboradorFormScreen
import cl.duoc.rinconsur.probadores.ui.screens.ColaboradoresScreen
import cl.duoc.rinconsur.probadores.ui.screens.FilaScreen
import cl.duoc.rinconsur.probadores.ui.screens.FueraServicioScreen
import cl.duoc.rinconsur.probadores.ui.screens.ConfiguracionInicialScreen
import cl.duoc.rinconsur.probadores.ui.screens.GestionProbadoresScreen
import cl.duoc.rinconsur.probadores.ui.screens.LoginScreen
import cl.duoc.rinconsur.probadores.ui.screens.NuevoTurnoScreen
import cl.duoc.rinconsur.probadores.ui.screens.ParametrosScreen
import cl.duoc.rinconsur.probadores.ui.screens.ProbadoresScreen
import cl.duoc.rinconsur.probadores.ui.screens.ResumenScreen
import cl.duoc.rinconsur.probadores.ui.screens.TurnoConfirmadoScreen
import cl.duoc.rinconsur.probadores.viewmodel.EstadoApp
import cl.duoc.rinconsur.probadores.viewmodel.SeccionViewModel
import kotlinx.coroutines.delay

// =====================================================================
//  AppNavegacion — mapa de pantallas de la app
//  ---------------------------------------------------------------------
//   App vacía ─▶ Configuración inicial (crea la jefatura y los probadores)
//
//   Login (RUT + contraseña, o "Crear cuenta de cliente")
//     ├─ Cliente ─────▶ ClienteInicio (su turno y su espera) ─▶ Mi perfil
//     ├─ Vendedor(a) ─▶ Fila ⇄ Probadores (solo consulta) ⇄ Ajustes
//     ├─ Asistente ───▶ Probadores ⇄ Fila ⇄ Ajustes
//     └─ Jefatura ────▶ Resumen ⇄ Probadores ⇄ Fila ⇄ Ajustes
//                          Ajustes ─▶ Mi perfil · Colaboradores · Probadores · Parámetros
//
//  Nada se ve sin cuenta. Con sesión abierta, la app salta el Login y abre la vista del perfil.
//  SeccionViewModel se crea UNA vez aquí y se comparte con todas las pantallas:
//  así todos ven la misma información en tiempo real (Aclaración nº 7).
// =====================================================================

@Composable
fun AppNavegacion() {
    val navController = rememberNavController()

    val seccionViewModel: SeccionViewModel = viewModel()
    val estadoApp by seccionViewModel.estadoApp.collectAsStateWithLifecycle()
    val sesion by seccionViewModel.sesion.collectAsStateWithLifecycle()
    val sesionCliente by seccionViewModel.sesionCliente.collectAsStateWithLifecycle()
    val mensaje by seccionViewModel.mensaje.collectAsStateWithLifecycle()

    when (estadoApp) {
        // Pantalla de carga mientras se lee la base de datos.
        EstadoApp.CARGANDO -> {
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
        // Primera vez: no hay jefatura ni probadores. Nada más se puede hacer hasta crearlos.
        EstadoApp.SIN_CONFIGURAR -> {
            ConfiguracionInicialScreen()
            return
        }
        EstadoApp.LISTA -> Unit
    }

    // Se decide una sola vez: con sesión abierta se entra directo al panel.
    val inicio = remember { (seccionViewModel.rutaInicialConSesion() ?: Ruta.Login).ruta }

    // ------------------------- GUARDIAS DE ACCESO -------------------------
    // RF-10 / RN-11: sin sesión no se entra al panel; sin perfil de jefatura
    // no se entra a las pantallas de jefatura (aunque alguien llegue por error).
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = Ruta.desdePatron(entradaActual?.destination?.route)
    LaunchedEffect(sesion, sesionCliente, rutaActual) {
        val ruta = rutaActual ?: return@LaunchedEffect
        val colaborador = sesion
        val cliente = sesionCliente
        val permitido = when (ruta.acceso) {
            Acceso.PUBLICA -> true
            Acceso.CLIENTE -> cliente != null
            Acceso.COLABORADOR -> colaborador != null
            Acceso.JEFATURA -> colaborador?.perfil?.esJefatura == true
            Acceso.CUALQUIER_SESION -> colaborador != null || cliente != null
        }
        if (permitido) return@LaunchedEffect
        if (ruta.soloJefatura && colaborador != null) {
            seccionViewModel.mostrarMensaje("Solo la jefatura puede entrar aquí (RN-11)", true)
            navController.popBackStack()
            return@LaunchedEffect
        }
        // Se espera un instante: justo después de ingresar, la sesión tarda unos
        // milisegundos en llegar desde DataStore. Si llega, este efecto se cancela solo.
        delay(400)
        // Cada cuenta vuelve a su propia vista; sin sesión, al ingreso. Se vacía la pila
        // para que "atrás" no regrese a una pantalla que no le corresponde.
        val destino = when {
            colaborador != null -> Ruta.inicioPara(colaborador.perfil)
            cliente != null -> Ruta.ClienteInicio
            else -> Ruta.Login
        }
        navController.navigate(destino.ruta) {
            popUpTo(navController.graph.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = inicio,
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
            composable(Ruta.Login.ruta) {
                LoginScreen(
                    onIngreso = { perfil ->
                        navController.navigate(Ruta.inicioPara(perfil).ruta) {
                            popUpTo(navController.graph.id) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // ------------------------------ CLIENTE ------------------------------
            composable(Ruta.ClienteInicio.ruta) {
                ClienteInicioScreen(seccionViewModel, onPerfil = { navController.navigate(Ruta.MiPerfil.ruta) })
            }

            // ------------------------- CUALQUIER CUENTA -------------------------
            composable(Ruta.MiPerfil.ruta) {
                MiPerfilScreen(
                    onVolver = { navController.navigateUp() },
                    onCambiarClave = { navController.navigate(Ruta.CambiarClave.ruta) },
                    onCerrarSesion = { seccionViewModel.cerrarSesion() }
                )
            }
            composable(Ruta.CambiarClave.ruta) {
                CambiarClaveScreen(onVolver = { navController.navigateUp() })
            }

            // Modo punto de espera (lo abre un colaborador desde Ajustes).
            composable(Ruta.Cliente.ruta) {
                ClienteScreen(seccionViewModel, onVolver = { navController.navigateUp() })
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
            composable(Ruta.GestionProbadores.ruta) {
                GestionProbadoresScreen(seccionViewModel, onVolver = { navController.navigateUp() })
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
