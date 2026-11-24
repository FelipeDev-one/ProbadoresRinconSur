package cl.duoc.rinconsur.probadores.data.remote

import com.google.gson.Gson
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Crea los clientes de Retrofit para una dirección del API Gateway.
// En el emulador el computador es 10.0.2.2; en un teléfono se usa la URL pública de VS Code.
object RetrofitClient {
    const val URL_EMULADOR = "http://10.0.2.2:8080/"

    val gson = Gson()

    private val http: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    fun crear(urlBase: String): ApiServidor {
        val retrofit = Retrofit.Builder()
            .baseUrl(normalizarUrl(urlBase))
            .client(http)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        return ApiServidor(
            usuarios = retrofit.create(UsuarioApi::class.java),
            probadores = retrofit.create(ProbadorApi::class.java),
            turnos = retrofit.create(TurnoApi::class.java)
        )
    }

    // Retrofit exige que la URL base termine en "/" y tenga http(s)://.
    fun normalizarUrl(url: String): String {
        var limpia = url.trim()
        if (limpia.isEmpty()) return ""
        if (!limpia.startsWith("http://") && !limpia.startsWith("https://")) limpia = "http://$limpia"
        return if (limpia.endsWith("/")) limpia else "$limpia/"
    }

    // null si es válida; si no, el error para mostrar bajo el campo.
    fun validarUrl(url: String): String? {
        if (url.isBlank()) return null
        val host = try {
            java.net.URI(normalizarUrl(url)).host
        } catch (e: java.net.URISyntaxException) {
            null
        }
        return if (host.isNullOrBlank() || url.trim().contains(' ')) {
            "Dirección no válida (ej.: https://abc123-8080.brs.devtunnels.ms/)"
        } else null
    }
}
