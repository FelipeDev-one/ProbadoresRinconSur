package cl.duoc.rinconsur.probadores.navigation

import android.net.Uri
import cl.duoc.rinconsur.probadores.model.Perfil

// =====================================================================
//  Rutas.kt — las "direcciones" de cada pantalla
//  ---------------------------------------------------------------------
//  "ruta" es el patrón que registra el NavHost (puede llevar {argumentos}).
//  crear(...) arma la ruta real con los valores, para navController.navigate().
//
//  "acceso" dice quién puede entrar a cada pantalla. Nada se ve sin cuenta,
//  salvo el ingreso: cada tipo de cuenta tiene sus propias pantallas.
// =====================================================================

enum class Acceso {
    PUBLICA,           // sin sesión: solo el ingreso y el registro de cliente
    CLIENTE,           // cuenta de cliente
    COLABORADOR,       // asistente, vendedor(a) o jefatura (RF-10)
    JEFATURA,          // acciones que la RN-11 reserva a la jefatura
    CUALQUIER_SESION   // cliente o colaborador: perfil propio y cambio de clave
}

sealed class Ruta(val ruta: String, val acceso: Acceso = Acceso.COLABORADOR) {
    val requiereSesion: Boolean get() = acceso == Acceso.COLABORADOR || acceso == Acceso.JEFATURA
    val soloJefatura: Boolean get() = acceso == Acceso.JEFATURA

    // Primera pantalla de la app cuando no hay sesión abierta.
    data object Login : Ruta("login", Acceso.PUBLICA)

    // Cliente: su espera y su turno.
    data object ClienteInicio : Ruta("cliente_inicio", Acceso.CLIENTE)

    // Cliente o colaborador: datos propios.
    data object MiPerfil : Ruta("mi_perfil", Acceso.CUALQUIER_SESION)
    data object CambiarClave : Ruta("cambiar_clave", Acceso.CUALQUIER_SESION)

    // Vista del punto de espera: la abre un colaborador y salir pide su clave.
    data object Cliente : Ruta("punto_espera")

    // Panel de colaboradores (barra inferior)
    data object Probadores : Ruta("probadores")
    data object Fila : Ruta("fila")
    data object Resumen : Ruta("resumen", Acceso.JEFATURA)
    data object Ajustes : Ruta("ajustes")

    // Turnos (HU-02)
    data object NuevoTurno : Ruta("nuevo_turno")

    data object TurnoConfirmado : Ruta("turno_confirmado/{codigo}/{minutos}") {
        // Sin estimado se envía -1 (los argumentos Int de navegación no aceptan null).
        fun crear(codigo: String, minutos: Int?) = "turno_confirmado/${Uri.encode(codigo)}/${minutos ?: -1}"
    }

    // Jefatura
    data object FueraServicio : Ruta("fuera_servicio/{codigo}", Acceso.JEFATURA) {
        fun crear(codigo: String) = "fuera_servicio/${Uri.encode(codigo)}"
    }

    data object Colaboradores : Ruta("colaboradores", Acceso.JEFATURA)

    data object ColaboradorForm : Ruta("colaborador_form?rut={rut}", Acceso.JEFATURA) {
        fun crear(rut: String? = null) =
            if (rut == null) "colaborador_form" else "colaborador_form?rut=${Uri.encode(rut)}"
    }

    data object Parametros : Ruta("parametros", Acceso.JEFATURA)

    data object GestionProbadores : Ruta("gestion_probadores", Acceso.JEFATURA)

    companion object {
        val todas: List<Ruta> by lazy {
            listOf(
                Login, ClienteInicio, MiPerfil, CambiarClave, Cliente, Probadores, Fila, Resumen, Ajustes,
                NuevoTurno, TurnoConfirmado, FueraServicio, Colaboradores, ColaboradorForm, Parametros,
                GestionProbadores
            )
        }

        // El NavController informa el PATRÓN de la pantalla actual (ej. "fuera_servicio/{codigo}").
        fun desdePatron(patron: String?): Ruta? = todas.find { it.ruta == patron }

        // Cada tipo de cuenta abre en su propia vista. perfil == null es una cuenta de cliente.
        fun inicioPara(perfil: Perfil?): Ruta = when (perfil) {
            null -> ClienteInicio
            Perfil.VENDEDOR -> Fila
            Perfil.ASISTENTE -> Probadores
            Perfil.JEFATURA -> Resumen
        }
    }
}
