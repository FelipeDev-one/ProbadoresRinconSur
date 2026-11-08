package cl.duoc.rinconsur.probadores.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

// Dibuja "129845619" como "12.984.561-9" mientras se escribe, sin mover el cursor.
// El ViewModel guarda solo dígitos y K; los puntos y el guion son únicamente visuales.
object RutVisualTransformation : VisualTransformation {

    // Devuelve el texto con formato y, para cada posición original, su posición en el texto con formato.
    fun formatear(crudo: String): Pair<String, IntArray> {
        val n = crudo.length
        val mapa = IntArray(n + 1)
        if (n < 2) {
            for (i in 0..n) mapa[i] = i
            return crudo to mapa
        }
        val cuerpo = n - 1
        val salida = StringBuilder()
        for (i in 0 until cuerpo) {
            if (i > 0 && (cuerpo - i) % 3 == 0) salida.append('.')
            mapa[i] = salida.length
            salida.append(crudo[i])
        }
        salida.append('-')
        mapa[cuerpo] = salida.length
        salida.append(crudo[cuerpo])
        mapa[n] = salida.length
        return salida.toString() to mapa
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val (formateado, mapa) = formatear(text.text)
        val inverso = IntArray(formateado.length + 1) { t -> mapa.count { it < t }.coerceAtMost(text.length) }
        val mapeo = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = mapa[offset.coerceIn(0, text.length)]
            override fun transformedToOriginal(offset: Int) = inverso[offset.coerceIn(0, formateado.length)]
        }
        return TransformedText(AnnotatedString(formateado), mapeo)
    }
}
