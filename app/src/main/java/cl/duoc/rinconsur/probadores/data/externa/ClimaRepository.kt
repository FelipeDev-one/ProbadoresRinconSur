package cl.duoc.rinconsur.probadores.data.externa

import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

// Clima actual de la tienda: el cliente ve qué ropa conviene probarse
// y la jefatura anticipa si vendrá más o menos público.
data class Clima(
    val temperatura: Int,
    val sensacion: Int,
    val descripcion: String,
    val sugerencia: String,
    val hora: String
)

class ClimaRepository(private val api: ClimaApi) {

    // Ubicación fija de la tienda (sucursal RS-014 Zona Norte, Santiago).
    suspend fun climaTienda(
        latitud: Double = LATITUD_TIENDA,
        longitud: Double = LONGITUD_TIENDA
    ): Result<Clima> = try {
        val actual = api.actual(latitud, longitud).actual
        if (actual?.temperatura == null || actual.codigo == null) {
            Result.failure(IllegalStateException("El servicio de clima no envió datos"))
        } else {
            Result.success(convertir(actual))
        }
    } catch (e: IOException) {
        Result.failure(IOException("Sin conexión con el servicio de clima", e))
    } catch (e: HttpException) {
        Result.failure(IllegalStateException("El servicio de clima respondió con error ${e.code()}"))
    }

    companion object {
        const val URL_BASE = "https://api.open-meteo.com/"
        const val LATITUD_TIENDA = -33.36
        const val LONGITUD_TIENDA = -70.73

        // Retrofit propio para la API externa: no comparte dirección con el API Gateway.
        fun crear(): ClimaRepository {
            val http = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val retrofit = Retrofit.Builder()
                .baseUrl(URL_BASE)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return ClimaRepository(retrofit.create(ClimaApi::class.java))
        }

        fun convertir(dto: ClimaActualDto): Clima {
            val temperatura = requireNotNull(dto.temperatura).roundToInt()
            return Clima(
                temperatura = temperatura,
                sensacion = (dto.sensacion ?: dto.temperatura).roundToInt(),
                descripcion = describir(requireNotNull(dto.codigo)),
                sugerencia = sugerir(temperatura),
                hora = dto.hora?.substringAfter('T') ?: ""
            )
        }

        // Códigos WMO que entrega Open-Meteo.
        fun describir(codigo: Int): String = when (codigo) {
            0 -> "Despejado"
            1, 2 -> "Parcialmente nublado"
            3 -> "Nublado"
            45, 48 -> "Niebla"
            in 51..57 -> "Llovizna"
            in 61..67, in 80..82 -> "Lluvia"
            in 71..77, 85, 86 -> "Nieve"
            in 95..99 -> "Tormenta"
            else -> "Sin información"
        }

        fun sugerir(temperatura: Int): String = when {
            temperatura < 10 -> "Día frío: buen momento para probarse abrigos y parkas"
            temperatura < 18 -> "Día templado: ideal para chaquetas livianas y polerones"
            temperatura < 26 -> "Día agradable: pruebe camisas y pantalones livianos"
            else -> "Día caluroso: busque poleras, shorts y vestidos"
        }
    }
}
