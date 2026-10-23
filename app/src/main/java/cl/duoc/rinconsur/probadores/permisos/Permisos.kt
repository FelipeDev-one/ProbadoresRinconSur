package cl.duoc.rinconsur.probadores.permisos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat

// =====================================================================
//  Permisos.kt
//  ---------------------------------------------------------------------
//  Los 4 pasos para usar la cámara (permiso PELIGROSO):
//
//   1) DECLARAR   <uses-permission CAMERA> en AndroidManifest.xml
//   2) VERIFICAR  ¿ya está concedido?            -> tiene()
//   3) PEDIR      diálogo del sistema             -> FueraServicioScreen (RequestPermission)
//   4) REACCIONAR concedido -> abrir la cámara / rechazado -> avisar y ofrecer Ajustes
//
//  Si el usuario rechaza 2 veces, Android ya NO vuelve a mostrar el diálogo:
//  la única forma de concederlo es desde Ajustes -> abrirAjustes().
//
//  La galería NO necesita permiso: se usa el selector de fotos del sistema
//  (Photo Picker), que entrega solo la imagen que el usuario elige.
// =====================================================================

object Permisos {

    val CAMARA: String = Manifest.permission.CAMERA

    // PASO 2: devuelve true si el permiso ya fue concedido.
    fun tiene(context: Context, permiso: String): Boolean =
        ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED

    // Abre la pantalla de Ajustes de ESTA app para conceder el permiso a mano.
    fun abrirAjustes(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
