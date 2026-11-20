package cl.duoc.rinconsur.probadores.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import cl.duoc.rinconsur.probadores.MainActivity
import cl.duoc.rinconsur.probadores.R
import cl.duoc.rinconsur.probadores.permisos.Permisos

// =====================================================================
//  Notificaciones — segundo recurso nativo (además de cámara y galería)
//  ---------------------------------------------------------------------
//  1) DECLARAR  <uses-permission POST_NOTIFICATIONS> en el AndroidManifest.
//  2) CANAL     Android 8+ agrupa las notificaciones en canales: se crean al abrir la app.
//  3) PEDIR     Desde Android 13 el permiso es PELIGROSO y se pide en tiempo de ejecución:
//               al personal, al entrar al panel; al cliente, al tomar su turno.
//  4) AVISAR    Solo si hay permiso (y el colaborador no las desactivó en Ajustes).
//
//  Avisos que se envían:
//   · Personal: cuando aparece una alerta urgente (uso prolongado con fila, probadores
//     libres sin asignar).
//   · Cliente:  cuando llaman su turno.
//  Funcionan mientras la app está abierta o en segundo plano (no si el sistema la cierra).
// =====================================================================

object Notificaciones {
    const val CANAL_ALERTAS = "alertas_seccion"
    const val CANAL_TURNO = "turno_cliente"

    private const val ID_ALERTA = 1001
    private const val ID_TURNO = 1002

    // Antes de Android 13 no hace falta pedirlo: se concede al instalar.
    val requierePermiso: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val PERMISO: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.POST_NOTIFICATIONS
    } else ""

    fun tienePermiso(context: Context): Boolean =
        !requierePermiso || Permisos.tiene(context, PERMISO)

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestor = context.getSystemService(NotificationManager::class.java)
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_ALERTAS, "Alertas de la sección", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Uso prolongado y probadores libres mientras hay personas esperando"
            }
        )
        gestor.createNotificationChannel(
            NotificationChannel(CANAL_TURNO, "Mi turno", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Aviso cuando llaman su turno al mesón"
            }
        )
    }

    fun alertaUrgente(context: Context, titulo: String, detalle: String) =
        enviar(context, CANAL_ALERTAS, ID_ALERTA, titulo, detalle)

    fun turnoLlamado(context: Context, codigo: String) =
        enviar(context, CANAL_TURNO, ID_TURNO, "¡Es su turno, $codigo!", "Acérquese al mesón de probadores.")

    private fun enviar(context: Context, canal: String, id: Int, titulo: String, texto: String) {
        if (!tienePermiso(context)) return
        // Al tocar la notificación se abre la app (la misma pantalla en que estaba).
        val abrir = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notificacion)
        } catch (_: SecurityException) {
            // El permiso se quitó justo ahora desde Ajustes del sistema: no se avisa.
        }
    }
}
