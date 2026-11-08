package cl.duoc.rinconsur.probadores.model

// Pasos del ingreso: RUT para todos; la jefatura sigue a contraseña (o la crea / recupera).
enum class PasoLogin { RUT, CONTRASENA, CREAR_CONTRASENA, RECUPERAR, CODIGO_NUEVO }

data class LoginUiState(
    val paso: PasoLogin = PasoLogin.RUT,
    val rut: String = "",
    val nombre: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val codigo: String = "",
    val codigoGenerado: String? = null,
    val cargando: Boolean = false,
    val errores: LoginErrores = LoginErrores()
) {
    private val datosNombre: Colaborador
        get() = Colaborador(rut, nombre, Perfil.JEFATURA, TurnoLaboral.MANANA)
    val primerNombre: String get() = datosNombre.primerNombre
    val iniciales: String get() = datosNombre.iniciales
}

data class LoginErrores(
    val rut: String? = null,
    val contrasena: String? = null,
    val confirmacion: String? = null,
    val codigo: String? = null
)

enum class MotivoPreferente(val etiqueta: String) {
    MOVILIDAD("Movilidad reducida"),
    EMBARAZO("Embarazada"),
    ADULTO_MAYOR("Adulto mayor"),
    COCHE("Coche de bebé")
}

data class TurnoUiState(
    val prendas: String = "1",
    val preferente: Boolean = false,
    val motivoPreferente: MotivoPreferente? = null,
    val guardando: Boolean = false,
    val errores: TurnoErrores = TurnoErrores()
)

data class TurnoErrores(
    val prendas: String? = null,
    val motivoPreferente: String? = null,
    val general: String? = null
)

data class ColaboradorUiState(
    val esNuevo: Boolean = true,
    val rut: String = "",
    val nombre: String = "",
    val perfil: Perfil? = null,
    val turno: TurnoLaboral? = null,
    val activo: Boolean = true,
    val errores: ColaboradorErrores = ColaboradorErrores()
)

data class ColaboradorErrores(
    val rut: String? = null,
    val nombre: String? = null,
    val perfil: String? = null,
    val turno: String? = null,
    val general: String? = null
)

data class FueraServicioUiState(
    val codigo: String = "",
    val motivo: String = "",
    val fotoRuta: String? = null,
    val guardando: Boolean = false,
    val errores: FueraServicioErrores = FueraServicioErrores()
)

data class FueraServicioErrores(
    val motivo: String? = null,
    val foto: String? = null,
    val general: String? = null
)

data class UsoSinFilaErrores(
    val prendas: String? = null,
    val atraso: String? = null
) {
    val hayErrores: Boolean get() = prendas != null || atraso != null
}

data class ParametrosUiState(
    val maxPrendas: String = "",
    val tiempoReferenciaUso: String = "",
    val umbralProlongado: String = "",
    val umbralAyuda: String = "",
    val minutosPresentarse: String = "",
    val ticketPromedio: String = "",
    val errores: ParametrosErrores = ParametrosErrores()
)

data class ParametrosErrores(
    val maxPrendas: String? = null,
    val tiempoReferenciaUso: String? = null,
    val umbralProlongado: String? = null,
    val umbralAyuda: String? = null,
    val minutosPresentarse: String? = null,
    val ticketPromedio: String? = null,
    val general: String? = null
)
