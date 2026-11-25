package cl.duoc.rinconsur.probadores.data.externa

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// API externa: Open-Meteo (https://open-meteo.com), gratuita y sin clave.
// Es independiente de los microservicios propios y de Room: solo se muestra en pantalla.
interface ClimaApi {
    // GET https://api.open-meteo.com/v1/forecast?latitude=-33.36&longitude=-70.73
    //     &current=temperature_2m,apparent_temperature,weather_code&timezone=America/Santiago
    @GET("v1/forecast")
    suspend fun actual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") campos: String = "temperature_2m,apparent_temperature,weather_code",
        @Query("timezone") zona: String = "America/Santiago"
    ): PronosticoDto
}

data class PronosticoDto(@SerializedName("current") val actual: ClimaActualDto?)

data class ClimaActualDto(
    @SerializedName("time") val hora: String?,
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("apparent_temperature") val sensacion: Double?,
    @SerializedName("weather_code") val codigo: Int?
)
