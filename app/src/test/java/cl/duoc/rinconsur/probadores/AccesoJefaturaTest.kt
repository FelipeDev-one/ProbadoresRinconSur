package cl.duoc.rinconsur.probadores

import cl.duoc.rinconsur.probadores.model.ReglasAcceso
import cl.duoc.rinconsur.probadores.utils.Contrasenas
import cl.duoc.rinconsur.probadores.utils.RutVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccesoJefaturaTest {
    private val rut = "12984561-9"
    private val ahora = 1_000_000L

    @Test
    fun `la contrasena se guarda como hash y se verifica`() {
        val (c, _) = ReglasAcceso.crear(rut, "clave123")
        assertNotEquals("clave123", c.hash)
        assertTrue(ReglasAcceso.contrasenaCorrecta(c, "clave123"))
        assertFalse(ReglasAcceso.contrasenaCorrecta(c, "clave124"))
    }

    @Test
    fun `la misma contrasena genera hashes distintos por la sal`() {
        val (a, _) = ReglasAcceso.crear(rut, "clave123")
        val (b, _) = ReglasAcceso.crear(rut, "clave123")
        assertNotEquals(a.hash, b.hash)
    }

    @Test
    fun `el codigo de recuperacion sirve con o sin guion y en minusculas`() {
        val (c, codigo) = ReglasAcceso.crear(rut, "clave123")
        assertTrue(Regex("[A-Z2-9]{4}-[A-Z2-9]{4}").matches(codigo))
        assertTrue(ReglasAcceso.codigoCorrecto(c, codigo))
        assertTrue(ReglasAcceso.codigoCorrecto(c, codigo.replace("-", "").lowercase()))
        assertFalse(ReglasAcceso.codigoCorrecto(c, "AAAA-AAAA"))
    }

    @Test
    fun `cinco fallos bloquean un minuto y luego se libera`() {
        var c = ReglasAcceso.crear(rut, "clave123").first
        repeat(4) { c = ReglasAcceso.registrarFallo(c, ahora) }
        assertEquals(1, ReglasAcceso.intentosRestantes(c))
        assertEquals(0, ReglasAcceso.segundosBloqueo(c, ahora))

        c = ReglasAcceso.registrarFallo(c, ahora)
        assertEquals(60, ReglasAcceso.segundosBloqueo(c, ahora))
        assertEquals(0, ReglasAcceso.segundosBloqueo(c, ahora + ReglasAcceso.MILIS_BLOQUEO))
    }

    @Test
    fun `un ingreso correcto reinicia los intentos`() {
        var c = ReglasAcceso.crear(rut, "clave123").first
        repeat(3) { c = ReglasAcceso.registrarFallo(c, ahora) }
        c = ReglasAcceso.registrarExito(c)
        assertEquals(ReglasAcceso.MAX_INTENTOS, ReglasAcceso.intentosRestantes(c))
    }

    @Test
    fun `reglas para una contrasena nueva`() {
        assertEquals("Campo obligatorio", Contrasenas.validarNueva("", "").first)
        assertEquals("Debe tener al menos 6 caracteres", Contrasenas.validarNueva("ab1", "ab1").first)
        assertEquals("Debe tener al menos una letra y un número", Contrasenas.validarNueva("abcdefg", "abcdefg").first)
        assertEquals("Las contraseñas no coinciden", Contrasenas.validarNueva("clave123", "clave12").second)
        assertEquals(null to null, Contrasenas.validarNueva("clave123", "clave123"))
    }

    @Test
    fun `el RUT se dibuja con puntos y guion mientras se escribe`() {
        assertEquals("1", RutVisualTransformation.formatear("1").first)
        assertEquals("12-9", RutVisualTransformation.formatear("129").first)
        assertEquals("1.298-4", RutVisualTransformation.formatear("12984").first)
        assertEquals("12.984.561-9", RutVisualTransformation.formatear("129845619").first)
        assertEquals("16.842.311-K", RutVisualTransformation.formatear("16842311K").first)
        // El cursor al final del texto crudo queda al final del texto con formato.
        val (texto, mapa) = RutVisualTransformation.formatear("129845619")
        assertEquals(texto.length, mapa[9])
    }
}
