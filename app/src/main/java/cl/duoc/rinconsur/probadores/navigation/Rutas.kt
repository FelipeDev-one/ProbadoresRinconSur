package cl.duoc.rinconsur.probadores.navigation

import android.net.Uri

// =====================================================================
//  Rutas.kt — las "direcciones" de cada pantalla
//  ---------------------------------------------------------------------
//  "ruta" es el patrón que registra el NavHost (puede llevar {argumentos}).
//  crear(...) arma la ruta real con los valores, para navController.navigate().
//
//  requiereSesion: solo colaboradores que ingresaron con su RUT (RF-10).
//  soloJefatura:   acciones que la RN-11 reserva a la jefatura.
// =====================================================================

sealed class Ruta(
    val ruta: String,
    val requiereSesion: Boolean = true,
    val soloJefatura: Boolean = false
) {
    // Sin sesión: el cliente no se identifica (Aclaración nº 6)
    data object Inicio : Ruta("inicio", requiereSesion = false)
    data object Cliente : Ruta("cliente", requiereSesion = false)
    data object Login : Ruta("login", requiereSesion = false)

    // Panel de colaboradores (barra inferior)
    data object Probadores : Ruta("probadores")
    data object Fila : Ruta("fila")
    data object Resumen : Ruta("resumen", soloJefatura = true)
    data object Ajustes : Ruta("ajustes")

    // Turnos (HU-02)
    data object NuevoTurno : Ruta("nuevo_turno")

    data object TurnoConfirmado : Ruta("turno_confirmado/{codigo}/{minutos}") {
        // Sin estimado se envía -1 (los argumentos Int de navegación no aceptan null).
        fun crear(codigo: String, minutos: Int?) = "turno_confirmado/${Uri.encode(codigo)}/${minutos ?: -1}"
    }

    // Jefatura
    data object FueraServicio : Ruta("fuera_servicio/{codigo}", soloJefatura = true) {
        fun crear(codigo: String) = "fuera_servicio/${Uri.encode(codigo)}"
    }

    data object Colaboradores : Ruta("colaboradores", soloJefatura = true)

    data object ColaboradorForm : Ruta("colaborador_form?rut={rut}", soloJefatura = true) {
        fun crear(rut: String? = null) =
            if (rut == null) "colaborador_form" else "colaborador_form?rut=${Uri.encode(rut)}"
    }

    data object Parametros : Ruta("parametros", soloJefatura = true)

    companion object {
        val todas: List<Ruta> by lazy {
            listOf(
                Inicio, Cliente, Login, Probadores, Fila, Resumen, Ajustes, NuevoTurno,
                TurnoConfirmado, FueraServicio, Colaboradores, ColaboradorForm, Parametros
            )
        }

        // El NavController informa el PATRÓN de la pantalla actual (ej. "fuera_servicio/{codigo}").
        fun desdePatron(patron: String?): Ruta? = todas.find { it.ruta == patron }
    }
}
