package cl.duoc.rinconsur.probadores.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.ui.theme.Dimens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// =====================================================================
//  FotoEvidencia — muestra la foto guardada en filesDir/evidencias
//  ---------------------------------------------------------------------
//  Una foto de cámara puede medir 4000 px y pesar varios MB. Para no
//  trabar la pantalla:
//   1) se lee en segundo plano (Dispatchers.IO) con produceState
//   2) se achica al leerla (inSampleSize) hasta ~1024 px
//   3) se gira según la orientación con que se tomó (EXIF)
// =====================================================================

@Composable
fun FotoEvidencia(
    ruta: String?,
    descripcion: String,
    modifier: Modifier = Modifier,
    tamano: Dp = Dimens.tamanoFoto
) {
    val forma = RoundedCornerShape(24.dp)
    // produceState: arranca con null y se actualiza cuando termina de cargar.
    // Si "ruta" cambia (foto nueva), vuelve a cargar.
    val imagen by produceState<ImageBitmap?>(initialValue = null, ruta) {
        value = if (ruta == null) null else withContext(Dispatchers.IO) { cargarImagen(ruta) }
    }

    Box(
        modifier = modifier
            .size(tamano)
            .clip(forma)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, forma),
        contentAlignment = Alignment.Center
    ) {
        val actual = imagen
        if (actual != null) {
            Image(
                bitmap = actual,
                contentDescription = descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(tamano)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    modifier = Modifier.size(tamano / 3),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(stringResource(R.string.sin_foto), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val LADO_MAXIMO = 1024

private fun cargarImagen(ruta: String): ImageBitmap? = try {
    // 1ª lectura: solo las medidas (no carga los pixeles)
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, limites)
    var escala = 1
    while (limites.outWidth / escala > LADO_MAXIMO || limites.outHeight / escala > LADO_MAXIMO) escala *= 2
    // 2ª lectura: la imagen ya achicada
    val bitmap = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = escala })
    bitmap?.let { girarSegunExif(it, ruta).asImageBitmap() }
} catch (e: Exception) {
    null
}

private fun girarSegunExif(bitmap: Bitmap, ruta: String): Bitmap {
    val grados = when (ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (grados == 0f) return bitmap
    val matriz = Matrix().apply { postRotate(grados) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
}
