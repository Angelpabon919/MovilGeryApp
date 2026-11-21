package com.example.molvigeryapp.ui.cuidador.llegada

import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.RegistrarLlegadaResponse
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.utils.AsignacionVigenciaHelper
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class LlegadaTurnoHelperTest {

    private val turnoDiurno = Turno(
        id_turno = 1,
        hora_inicio = "07:00:00",
        hora_fin = "19:00:00",
        estado = true,
        nombre = "Diurno"
    )

    private val turnoNocturno = Turno(
        id_turno = 2,
        hora_inicio = "19:00:00",
        hora_fin = "07:00:00",
        estado = true,
        nombre = "Nocturno"
    )

    private fun fechaCol(iso: String) =
        requireNotNull(AsignacionVigenciaHelper.parsearFechaIso(iso))

    @Test
    fun seleccionaTurnoDiurnoEnJornadaActual_yRespetaUsuarioYEstado() {
        val otroUsuario = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 1,
            id_usuario = 99,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "Asignado"
        )
        val finalizado = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 2,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "Finalizado"
        )
        val valido = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 3,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "Asignado"
        )

        val resultado = LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
            idUsuario = 7,
            asignaciones = listOf(otroUsuario, finalizado, valido),
            turnos = listOf(turnoDiurno),
            ahora = fechaCol("2026-11-23T07:15:00-05:00")
        )

        assertNotNull(resultado)
        assertEquals(3, resultado!!.asignacion.id_asignacion_turno_usuario)
    }

    @Test
    fun seleccionaTurnoEnCurso_paraReingresoIdempotente() {
        val enCurso = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 15,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "En curso",
            fecha_hora_llegada = "2026-11-23T12:05:00Z"
        )

        val resultado = LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
            idUsuario = 7,
            asignaciones = listOf(enCurso),
            turnos = listOf(turnoDiurno),
            ahora = fechaCol("2026-11-23T10:30:00-05:00")
        )

        assertNotNull(resultado)
        assertEquals(15, resultado!!.asignacion.id_asignacion_turno_usuario)
    }

    @Test
    fun seleccionaTurnoNocturnoIniciadoAyerEnMadrugada() {
        val nocturnoAyer = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 20,
            id_usuario = 7,
            id_turno = 2,
            fecha = "2026-11-23",
            estado = "En curso",
            id_grupo_asignacion = "GRP-NOC"
        )
        val nocturnoHoy = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 21,
            id_usuario = 7,
            id_turno = 2,
            fecha = "2026-11-24",
            estado = "Asignado",
            id_grupo_asignacion = "GRP-NOC"
        )

        val resultado = LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
            idUsuario = 7,
            asignaciones = listOf(nocturnoAyer, nocturnoHoy),
            turnos = listOf(turnoNocturno),
            ahora = fechaCol("2026-11-24T02:00:00-05:00")
        )

        assertNotNull(resultado)
        assertEquals(20, resultado!!.asignacion.id_asignacion_turno_usuario)
    }

    @Test
    fun bloqueMultiDiaConDia1Finalizado_noSeleccionaDia2LaNocheAnterior() {
        val dia1Cerrado = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 30,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "Finalizado",
            id_grupo_asignacion = "GRP-1"
        )
        val dia2Pendiente = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 31,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-24",
            estado = "Asignado",
            id_grupo_asignacion = "GRP-1"
        )

        // Noche del día 1 (20:00): el día 1 cerró y el día 2 inicia en 11 horas -> no hay turno activo para llegada.
        val nocheDia1 = LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
            idUsuario = 7,
            asignaciones = listOf(dia1Cerrado, dia2Pendiente),
            turnos = listOf(turnoDiurno),
            ahora = fechaCol("2026-11-23T20:00:00-05:00")
        )
        assertNull(nocheDia1)

        // Día 2 a las 06:30 (30 min antes): llegada anticipada sí selecciona día 2.
        val llegadaAnticipadaDia2 = LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
            idUsuario = 7,
            asignaciones = listOf(dia1Cerrado, dia2Pendiente),
            turnos = listOf(turnoDiurno),
            ahora = fechaCol("2026-11-24T06:30:00-05:00")
        )
        assertNotNull(llegadaAnticipadaDia2)
        assertEquals(31, llegadaAnticipadaDia2!!.asignacion.id_asignacion_turno_usuario)
    }

    @Test
    fun sinAsignacionValidaODiaYaVencido_devuelveNull() {
        val turnoVencidoHoy = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 40,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "Asignado"
        )

        val resultado = LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
            idUsuario = 7,
            asignaciones = listOf(turnoVencidoHoy),
            turnos = listOf(turnoDiurno),
            ahora = fechaCol("2026-11-23T20:30:00-05:00")
        )

        assertNull(resultado)
    }

    @Test
    fun interpretaRespuestasHttp_200Nueva_200YaRegistrada_403_409_500() {
        val asignacionResp = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 50,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "En curso",
            fecha_hora_llegada = "2026-11-23T12:00:00Z"
        )

        val okNuevo = LlegadaTurnoHelper.interpretarRespuestaRegistroLlegada(
            Response.success(
                RegistrarLlegadaResponse(
                    mensaje = "Llegada registrada correctamente.",
                    asignacion = asignacionResp
                )
            )
        )
        assertTrue(okNuevo is LlegadaTurnoHelper.ResultadoRegistroLlegada.Exito)
        val exitoNuevo = okNuevo as LlegadaTurnoHelper.ResultadoRegistroLlegada.Exito
        assertFalse(exitoNuevo.yaEstabaRegistrada)
        assertEquals("En curso", exitoNuevo.asignacion?.estado)
        assertEquals("2026-11-23T12:00:00Z", exitoNuevo.asignacion?.fecha_hora_llegada)

        val okYaRegistrada = LlegadaTurnoHelper.interpretarRespuestaRegistroLlegada(
            Response.success(
                RegistrarLlegadaResponse(
                    mensaje = "La llegada ya estaba registrada.",
                    asignacion = asignacionResp
                )
            )
        )
        assertTrue(okYaRegistrada is LlegadaTurnoHelper.ResultadoRegistroLlegada.Exito)
        assertTrue((okYaRegistrada as LlegadaTurnoHelper.ResultadoRegistroLlegada.Exito).yaEstabaRegistrada)

        val jsonMediaType = "application/json".toMediaType()

        val resp403 = LlegadaTurnoHelper.interpretarRespuestaRegistroLlegada(
            Response.error(
                403,
                """{"error":"No tienes autorización para registrar la llegada de este turno."}"""
                    .toResponseBody(jsonMediaType)
            )
        )
        assertTrue(resp403 is LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorPermisos)
        assertEquals(
            "No tienes autorización para registrar la llegada de este turno.",
            (resp403 as LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorPermisos).mensaje
        )

        val resp409 = LlegadaTurnoHelper.interpretarRespuestaRegistroLlegada(
            Response.error(
                409,
                """{"error":"Este turno ya fue finalizado."}"""
                    .toResponseBody(jsonMediaType)
            )
        )
        assertTrue(resp409 is LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorTurnoFinalizado)
        assertEquals(
            "Este turno ya fue finalizado.",
            (resp409 as LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorTurnoFinalizado).mensaje
        )

        val resp500 = LlegadaTurnoHelper.interpretarRespuestaRegistroLlegada(
            Response.error(
                500,
                "Internal Server Error".toResponseBody("text/plain".toMediaType())
            )
        )
        assertTrue(resp500 is LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorServidor)
        assertEquals(500, (resp500 as LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorServidor).codigo)
    }

    @Test
    fun deserializaRegistrarLlegadaResponseConGson() {
        val json = """
            {
              "mensaje": "Llegada registrada correctamente.",
              "asignacion": {
                "id_asignacion_turno_usuario": 88,
                "id_usuario": 7,
                "id_turno": 1,
                "fecha": "2026-11-23",
                "estado": "En curso",
                "id_grupo_asignacion": "GRP-88",
                "fecha_hora_llegada": "2026-11-23T12:01:22.123456Z"
              }
            }
        """.trimIndent()

        val resp = Gson().fromJson(json, RegistrarLlegadaResponse::class.java)
        assertEquals("Llegada registrada correctamente.", resp.mensaje)
        assertEquals(88, resp.asignacion?.id_asignacion_turno_usuario)
        assertEquals("En curso", resp.asignacion?.estado)
        assertEquals("2026-11-23T12:01:22.123456Z", resp.asignacion?.fecha_hora_llegada)
    }
}
