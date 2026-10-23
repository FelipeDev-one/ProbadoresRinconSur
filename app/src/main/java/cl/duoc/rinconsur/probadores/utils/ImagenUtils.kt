package cl.duoc.rinconsur.probadores.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

// =====================================================================
//  ImagenUtils.kt — dónde se guardan las fotos de evidencia (HU-08)
//  ---------------------------------------------------------------------
//  Cámara : la app de cámara escribe la foto en un archivo NUESTRO.
//           Para eso le pasamos un Uri "content://" creado por el FileProvider
//           (declarado en AndroidManifest.xml y res/xml/file_paths.xml).
//  Galería: el Uri elegido pertenece a OTRA app y puede dejar de ser válido,
//           así que copiamos la imagen a nuestra carpeta.
//
//  En ambos casos queda un archivo en filesDir/evidencias, y su ruta se guarda
//  en Probador.fotoEvidencia.
// =====================================================================

object ImagenUtils {
    private const val CARPETA = "evidencias"

    private fun carpeta(context: Context): File =
        File(context.filesDir, CARPETA).apply { mkdirs() }

    // Crea un archivo vacío para la cámara y devuelve el archivo y su Uri compartible.
    fun crearArchivoFoto(context: Context): Pair<File, Uri> {
        val archivo = File(carpeta(context), "foto_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
        return archivo to uri
    }

    // Copia la imagen elegida en la galería a nuestra carpeta. null si no se pudo leer.
    fun copiarDesdeGaleria(context: Context, origen: Uri): File? = try {
        val destino = File(carpeta(context), "galeria_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(origen)?.use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        }
        if (destino.exists() && destino.length() > 0) destino
        else {
            destino.delete()
            null
        }
    } catch (e: Exception) {
        null
    }

    // Borra una foto que ya no se usará (reemplazada, quitada o sin guardar).
    fun borrar(ruta: String?) {
        if (ruta != null) File(ruta).delete()
    }
}
