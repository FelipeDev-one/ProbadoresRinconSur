package cl.duoc.rinconsur.probadores.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// Interfaces de Retrofit: cada función es un endpoint del API Gateway.
// Son "suspend": se llaman desde corrutinas sin bloquear la pantalla.

interface UsuarioApi {
    @GET("api/usuarios")
    suspend fun listar(): List<UsuarioDto>

    @GET("api/usuarios/{rut}")
    suspend fun obtener(@Path("rut") rut: String): UsuarioDto

    @GET("api/usuarios/estado-inicial")
    suspend fun estadoInicial(): EstadoInicialDto

    @POST("api/usuarios/configuracion-inicial")
    suspend fun configuracionInicial(@Body datos: ConfiguracionInicialDto): CuentaDto

    @POST("api/usuarios/registro")
    suspend fun registrarCliente(@Body datos: RegistroDto): CuentaDto

    @POST("api/usuarios/colaboradores")
    suspend fun crearColaborador(@Body datos: ColaboradorNuevoDto): UsuarioDto

    @PUT("api/usuarios/{rut}")
    suspend fun actualizar(@Path("rut") rut: String, @Body datos: ActualizarUsuarioDto): UsuarioDto

    @POST("api/usuarios/login")
    suspend fun iniciarSesion(@Body datos: LoginDto): UsuarioDto

    @POST("api/usuarios/{rut}/contrasena")
    suspend fun crearContrasena(@Path("rut") rut: String, @Body datos: ContrasenaDto): CuentaDto

    @PUT("api/usuarios/{rut}/contrasena")
    suspend fun cambiarContrasena(@Path("rut") rut: String, @Body datos: CambiarContrasenaDto): CuentaDto

    @POST("api/usuarios/{rut}/recuperar")
    suspend fun recuperar(@Path("rut") rut: String, @Body datos: RecuperarDto): CuentaDto

    @DELETE("api/usuarios/{rut}/contrasena")
    suspend fun restablecerContrasena(@Path("rut") rut: String)
}

interface ProbadorApi {
    @GET("api/probadores")
    suspend fun listar(): List<ProbadorDto>

    @PUT("api/probadores/{codigo}")
    suspend fun guardar(@Path("codigo") codigo: String, @Body datos: ProbadorDto): ProbadorDto

    @DELETE("api/probadores/{codigo}")
    suspend fun eliminar(@Path("codigo") codigo: String)

    @GET("api/probadores/usos")
    suspend fun usos(): List<UsoDto>

    @PUT("api/probadores/usos/{id}")
    suspend fun guardarUso(@Path("id") id: String, @Body datos: UsoDto): UsoDto

    @GET("api/probadores/cambios")
    suspend fun cambios(): List<CambioEstadoDto>

    @POST("api/probadores/cambios")
    suspend fun registrarCambio(@Body datos: CambioEstadoDto): CambioEstadoDto
}

interface TurnoApi {
    @GET("api/turnos")
    suspend fun listar(): List<TurnoDto>

    @PUT("api/turnos/{id}")
    suspend fun guardar(@Path("id") id: String, @Body datos: TurnoDto): TurnoDto

    @GET("api/turnos/parametros")
    suspend fun parametros(): ParametrosDto

    @PUT("api/turnos/parametros")
    suspend fun guardarParametros(@Body datos: ParametrosDto): ParametrosDto
}

// Las tres APIs juntas: lo que necesita el repositorio para hablar con el servidor.
class ApiServidor(
    val usuarios: UsuarioApi,
    val probadores: ProbadorApi,
    val turnos: TurnoApi
)
