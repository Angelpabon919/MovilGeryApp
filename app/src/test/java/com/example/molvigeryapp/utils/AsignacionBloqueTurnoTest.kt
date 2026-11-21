package com.example.molvigeryapp.utils

import com.example.molvigeryapp.data.api.ApiService
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy

class AsignacionBloqueTurnoTest {

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

    private val turnoMadrugada = Turno(
        id_turno = 3,
        hora_inicio = "00:15:00",
        hora_fin = "08:15:00",
        estado = true,
        nombre = "Madrugada"
    )

    private fun fechaCol(iso: String) =
        requireNotNull(AsignacionVigenciaHelper.parsearFechaIso(iso))

    private class FakeAsignacionesBackend(
        iniciales: List<AsignacionPacienteCuidador> = emptyList()
    ) {
        val asignaciones = iniciales.toMutableList()
        val patchesEjecutados = mutableListOf<Pair<Int, Map<String, String>>>()
        val postsEjecutados = mutableListOf<AsignacionPacienteCuidador>()
        private var siguienteId = (iniciales.mapNotNull { it.idAsignacion }.maxOrNull() ?: 100) + 1

        fun crearApiService(): ApiService {
            return Proxy.newProxyInstance(
                ApiService::class.java.classLoader,
                arrayOf(ApiService::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "getAsignacionesPacienteCuidador" -> Response.success(asignaciones.toList())
                    "actualizarEstadoAsignacion" -> {
                        val id = args[0] as Int
                        @Suppress("UNCHECKED_CAST")
                        val datos = args[1] as Map<String, String>
                        patchesEjecutados.add(id to datos)

                        val idx = asignaciones.indexOfFirst { it.idAsignacion == id }
                        if (idx >= 0) {
                            val actual = asignaciones[idx]
                            val actualizada = actual.copy(
                                estado = datos["estado"] ?: actual.estado,
                                fechaInicio = datos["fecha_inicio"] ?: actual.fechaInicio,
                                fechaFin = datos["fecha_fin"] ?: actual.fechaFin
                            )
                            asignaciones[idx] = actualizada
                            Response.success(actualizada)
                        } else {
                            Response.success(null)
                        }
                    }

                    "guardarAsignacion" -> {
                        val nueva = args[0] as AsignacionPacienteCuidador
                        val creada = nueva.copy(idAsignacion = siguienteId++)
                        postsEjecutados.add(creada)
                        asignaciones.add(creada)
                        Response.success(creada)
                    }

                    else -> throw UnsupportedOperationException("Método no simulado: ${method.name}")
                }
            } as ApiService
        }
    }

    @Test
    fun bloqueMultiDia_conservaSeleccionEnDia1ReingresoYDiaIntermedio_yConservaAlarmas() = runBlocking {
        val idUsuario = 7
        val grupo = "GRP-BLOQUE-1"

        val dia1 = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 10,
            id_usuario = idUsuario,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "Asignado",
            id_grupo_asignacion = grupo
        )
        val dia2 = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 11,
            id_usuario = idUsuario,
            id_turno = 1,
            fecha = "2026-11-24",
            estado = "Asignado",
            id_grupo_asignacion = grupo
        )
        val dia3 = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 12,
            id_usuario = idUsuario,
            id_turno = 1,
            fecha = "2026-11-25",
            estado = "Asignado",
            id_grupo_asignacion = grupo
        )

        val turnos = listOf(turnoDiurno)

        // Día 1 (07:10): resolver bloque y sincronizar selección {1, 2}.
        val ahoraDia1 = fechaCol("2026-11-23T07:10:00-05:00")
        val bloqueDia1 = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = listOf(dia1, dia2, dia3),
            turnosCatalogo = turnos,
            ahora = ahoraDia1
        )
        assertNotNull(bloqueDia1)
        assertEquals("2026-11-23T07:00:00-05:00", bloqueDia1!!.fechaInicioIso)
        assertEquals("2026-11-25T19:00:00-05:00", bloqueDia1.fechaFinIso)

        val backend = FakeAsignacionesBackend()
        val repo = PacienteRepository(backend.crearApiService())
        val sincronizado = repo.sincronizarSeleccionDelCuidador(
            idUsuario = idUsuario,
            idsSeleccionados = setOf(1, 2),
            fechaInicioIso = bloqueDia1.fechaInicioIso,
            fechaFinIso = bloqueDia1.fechaFinIso,
            ahora = ahoraDia1
        )
        assertTrue(sincronizado)
        assertEquals(2, backend.asignaciones.size)

        // Cierre día 1 -> NO es último día: ni reinicia selección ni cancela alarmas.
        val asignacionesTrasDia1 = listOf(dia1.copy(estado = "Finalizado"), dia2, dia3)
        var alarmasCanceladas = false
        var seleccionReiniciada = false
        val esUltimoDia1 = AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
            asignacionCerrada = dia1,
            todasLasAsignacionesTurno = asignacionesTrasDia1
        )
        if (esUltimoDia1) {
            alarmasCanceladas = true
            seleccionReiniciada = true
        }
        assertFalse(esUltimoDia1)
        assertFalse(alarmasCanceladas)
        assertFalse(seleccionReiniciada)

        // Reingreso entre día 1 y día 2, llegada anticipada día 2 y durante día 2 -> siguen seleccionados.
        val momentosReingreso = listOf(
            fechaCol("2026-11-23T20:00:00-05:00"),
            fechaCol("2026-11-24T06:30:00-05:00"),
            fechaCol("2026-11-24T08:00:00-05:00")
        )
        for (momento in momentosReingreso) {
            val bloqueRecuperado = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                idUsuario = idUsuario,
                asignacionesTurno = asignacionesTrasDia1,
                turnosCatalogo = turnos,
                ahora = momento
            )
            assertNotNull("Debe existir bloque activo en $momento", bloqueRecuperado)
            val idsRecuperados = backend.asignaciones
                .filter {
                    it.idUsuario == idUsuario &&
                        AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloqueRecuperado!!, momento)
                }
                .map { it.idPaciente }
                .toSet()
            assertEquals(setOf(1, 2), idsRecuperados)
        }

        // Cierre día intermedio (día 2) -> NO es último día, conserva alarmas y pacientes el día 3.
        val asignacionesTrasDia2 = listOf(
            dia1.copy(estado = "Finalizado"),
            dia2.copy(estado = "Finalizado"),
            dia3
        )
        val esUltimoDia2 = AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
            asignacionCerrada = dia2,
            todasLasAsignacionesTurno = asignacionesTrasDia2
        )
        if (esUltimoDia2) {
            alarmasCanceladas = true
            seleccionReiniciada = true
        }
        assertFalse(esUltimoDia2)
        assertFalse(alarmasCanceladas)
        assertFalse(seleccionReiniciada)

        val ahoraDia3 = fechaCol("2026-11-25T07:30:00-05:00")
        val bloqueDia3 = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = asignacionesTrasDia2,
            turnosCatalogo = turnos,
            ahora = ahoraDia3
        )
        assertNotNull(bloqueDia3)
        val idsDia3 = backend.asignaciones
            .filter {
                it.idUsuario == idUsuario &&
                    AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloqueDia3!!, ahoraDia3)
            }
            .map { it.idPaciente }
            .toSet()
        assertEquals(setOf(1, 2), idsDia3)
    }

    @Test
    fun ultimoDiaReiniciaYCancelaAlarmas_noReSeleccionaTrasCierre_yNuevoBloquePermiteOtraSeleccion() = runBlocking {
        val idUsuario = 7
        val grupo1 = "GRP-BLOQUE-1"
        val grupo2 = "GRP-BLOQUE-2"

        val b1Dia1 = AsignacionTurnoUsuario(10, idUsuario, 1, "2026-11-23", "Finalizado", grupo1)
        val b1Dia2 = AsignacionTurnoUsuario(11, idUsuario, 1, "2026-11-24", "Finalizado", grupo1)
        val b1Dia3Cerrado = AsignacionTurnoUsuario(12, idUsuario, 1, "2026-11-25", "Finalizado", grupo1)

        // Cierre del último día -> devuelve true (cancela alarmas y reinicia selección).
        val esUltimoDia = AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
            asignacionCerrada = b1Dia3Cerrado.copy(estado = "Asignado"),
            todasLasAsignacionesTurno = listOf(b1Dia1, b1Dia2, b1Dia3Cerrado)
        )
        assertTrue(esUltimoDia)

        // Sin bloque vigente tras el último cierre no recupera asignaciones residuales.
        val cincoMinAntesFin = fechaCol("2026-11-25T18:55:00-05:00")
        val bloqueTrasUltimoCierre = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = listOf(b1Dia1, b1Dia2, b1Dia3Cerrado),
            turnosCatalogo = listOf(turnoDiurno),
            ahora = cincoMinAntesFin
        )
        assertNull(bloqueTrasUltimoCierre)

        // Bloque 2 nocturno inmediato (18:56 dentro del margen de 60 min): residuo del bloque 1 NO pertenece al bloque 2.
        val b2Dia1 = AsignacionTurnoUsuario(20, idUsuario, 2, "2026-11-25", "Asignado", grupo2)
        val b2Dia2 = AsignacionTurnoUsuario(21, idUsuario, 2, "2026-11-26", "Asignado", grupo2)
        val asignacionResidualB1 = AsignacionPacienteCuidador(
            idAsignacion = 50,
            idUsuario = idUsuario,
            idPaciente = 1,
            fechaInicio = "2026-11-23T07:00:00-05:00",
            fechaFin = "2026-11-25T19:00:00-05:00",
            estado = "Activo"
        )

        val ahoraInicioB2 = fechaCol("2026-11-25T18:56:00-05:00")
        val bloque2 = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = listOf(b1Dia1, b1Dia2, b1Dia3Cerrado, b2Dia1, b2Dia2),
            turnosCatalogo = listOf(turnoDiurno, turnoNocturno),
            ahora = ahoraInicioB2
        )
        assertNotNull(bloque2)
        assertFalse(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                asignacion = asignacionResidualB1,
                bloque = bloque2!!,
                ahora = ahoraInicioB2
            )
        )

        // Sincronizar nueva selección {3, 4} en bloque 2 sin alterar bloque 1 ni bloque futuro 3.
        val asignacionFuturoB3 = AsignacionPacienteCuidador(
            idAsignacion = 60,
            idUsuario = idUsuario,
            idPaciente = 9,
            fechaInicio = "2026-11-30T07:00:00-05:00",
            fechaFin = "2026-12-02T19:00:00-05:00",
            estado = "Activo"
        )
        val asignacionHistoricaInactiva = AsignacionPacienteCuidador(
            idAsignacion = 40,
            idUsuario = idUsuario,
            idPaciente = 3,
            fechaInicio = "2026-11-10T07:00:00-05:00",
            fechaFin = "2026-11-12T19:00:00-05:00",
            estado = "Inactivo"
        )

        val backend = FakeAsignacionesBackend(
            listOf(asignacionHistoricaInactiva, asignacionResidualB1, asignacionFuturoB3)
        )
        val repo = PacienteRepository(backend.crearApiService())
        val ok = repo.sincronizarSeleccionDelCuidador(
            idUsuario = idUsuario,
            idsSeleccionados = setOf(3, 4),
            fechaInicioIso = bloque2.fechaInicioIso,
            fechaFinIso = bloque2.fechaFinIso,
            ahora = ahoraInicioB2
        )
        assertTrue(ok)
        assertTrue(backend.patchesEjecutados.isEmpty())
        assertEquals(2, backend.postsEjecutados.size)
        assertEquals(setOf(3, 4), backend.postsEjecutados.map { it.idPaciente }.toSet())
        assertEquals("Activo", backend.asignaciones.first { it.idAsignacion == 60 }.estado)
        assertEquals("Inactivo", backend.asignaciones.first { it.idAsignacion == 40 }.estado)
        assertEquals("2026-11-12T19:00:00-05:00", backend.asignaciones.first { it.idAsignacion == 40 }.fechaFin)
    }

    @Test
    fun estadosUnificadosConBackend_normalizacionYDistincionExcluidoVsNoPendiente() {
        // Normalización de espacios y mayúsculas/minúsculas.
        assertTrue(AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque("  INACTIVO "))
        assertTrue(AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque("Inactiva"))
        assertTrue(AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque("  CANCELADO  "))
        assertTrue(AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque("cancelada"))
        assertTrue(AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque(" Anulado "))
        assertTrue(AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque("ANULADA"))

        // Finalizado/Completado NO excluyen del rango del bloque, pero SÍ marcan el día como no pendiente.
        for (estadoFinal in listOf(" Finalizado ", "FINALIZADA", "Completado", " completada ")) {
            assertFalse(
                "Estado '$estadoFinal' no debe excluirse del rango del bloque",
                AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque(estadoFinal)
            )
            assertTrue(
                "Estado '$estadoFinal' debe considerarse finalizado",
                AsignacionVigenciaHelper.esEstadoTurnoFinalizado(estadoFinal)
            )
            assertTrue(
                "Estado '$estadoFinal' debe considerarse no pendiente",
                AsignacionVigenciaHelper.esEstadoTurnoNoPendiente(estadoFinal)
            )
        }

        assertFalse(AsignacionVigenciaHelper.esEstadoTurnoNoPendiente(" Asignado "))
        assertTrue(AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo("  ACTIVO "))
        assertTrue(AsignacionVigenciaHelper.esEstadoAsignacionPacienteInactivo(" inactivo  "))

        val idUsuario = 7
        val turnos = listOf(turnoDiurno)

        // Caso A: Día 1 Finalizada, Día 2 Asignado, Día 3 Cancelado ->
        // el rango del bloque conserva inicio en Día 1 y recorta fin a Día 2 (excluye Día 3 cancelado).
        val d1Finalizada = AsignacionTurnoUsuario(1, idUsuario, 1, "2026-11-23", "  Finalizada ", "GRP-MIX")
        val d2Asignado = AsignacionTurnoUsuario(2, idUsuario, 1, "2026-11-24", "Asignado", "GRP-MIX")
        val d3Cancelado = AsignacionTurnoUsuario(3, idUsuario, 1, "2026-11-25", "  CANCELADO ", "GRP-MIX")

        val bloqueDia2 = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = listOf(d1Finalizada, d2Asignado, d3Cancelado),
            turnosCatalogo = turnos,
            ahora = fechaCol("2026-11-24T10:00:00-05:00")
        )
        assertNotNull(bloqueDia2)
        assertEquals("2026-11-23T07:00:00-05:00", bloqueDia2!!.fechaInicioIso)
        assertEquals("2026-11-24T19:00:00-05:00", bloqueDia2.fechaFinIso)

        // Al cerrar Día 2, Día 3 está Cancelado -> Día 2 es realmente el último día pendiente.
        assertTrue(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = d2Asignado,
                todasLasAsignacionesTurno = listOf(d1Finalizada, d2Asignado.copy(estado = "Finalizado"), d3Cancelado)
            )
        )

        // Caso B: Día 2 Anulada, Día 3 Completada -> Día 1 es el último día pendiente.
        val bD1 = AsignacionTurnoUsuario(10, idUsuario, 1, "2026-11-23", "Finalizado", "GRP-COMP")
        val bD2Anulada = AsignacionTurnoUsuario(11, idUsuario, 1, "2026-11-24", " anulada ", "GRP-COMP")
        val bD3Completada = AsignacionTurnoUsuario(12, idUsuario, 1, "2026-11-25", " Completada ", "GRP-COMP")
        assertTrue(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = bD1.copy(estado = "Asignado"),
                todasLasAsignacionesTurno = listOf(bD1, bD2Anulada, bD3Completada)
            )
        )

        // Caso C: Día 2 Cancelada pero Día 3 sigue Asignado -> Día 1 NO es último día.
        val cD3Pendiente = AsignacionTurnoUsuario(13, idUsuario, 1, "2026-11-25", "Asignado", "GRP-COMP")
        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = bD1.copy(estado = "Asignado"),
                todasLasAsignacionesTurno = listOf(bD1, bD2Anulada, cD3Pendiente)
            )
        )
    }

    @Test
    fun turnosNocturnosCruzanMedianoche_zonaColombia_yLlegadaAnticipadaMedianoche() {
        val idUsuario = 7
        val turnos = listOf(turnoNocturno, turnoMadrugada)

        val n1 = AsignacionTurnoUsuario(20, idUsuario, 2, "2026-11-23", "Asignado", "  GRP-NOCHE  ")
        val n2 = AsignacionTurnoUsuario(21, idUsuario, 2, "2026-11-24", "Asignado", "GRP-NOCHE")

        // 07:00 UTC del 24 nov = 02:00 hora Colombia del 24 nov (en medio de la noche 1).
        val enMadrugadaUtc = fechaCol("2026-11-24T07:00:00Z")
        val bloqueNoche1 = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = listOf(n1, n2),
            turnosCatalogo = turnos,
            ahora = enMadrugadaUtc
        )
        assertNotNull(bloqueNoche1)
        assertEquals("2026-11-23T19:00:00-05:00", bloqueNoche1!!.fechaInicioIso)
        assertEquals("2026-11-25T07:00:00-05:00", bloqueNoche1.fechaFinIso)

        // Cierre noche 1 -> NO es último día (noche 2 pendiente).
        val trasNoche1 = listOf(n1.copy(estado = "Finalizado"), n2)
        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = n1,
                todasLasAsignacionesTurno = trasNoche1
            )
        )

        // Entre noche 1 y noche 2 (24 nov 14:00 COL) -> sigue el mismo bloque y mantiene pacientes.
        val entreNoches = fechaCol("2026-11-24T14:00:00-05:00")
        val bloqueEntreNoches = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = trasNoche1,
            turnosCatalogo = turnos,
            ahora = entreNoches
        )
        assertNotNull(bloqueEntreNoches)
        assertEquals("2026-11-23T19:00:00-05:00", bloqueEntreNoches!!.fechaInicioIso)
        assertEquals("2026-11-25T07:00:00-05:00", bloqueEntreNoches.fechaFinIso)

        // Cierre noche 2 -> SÍ es último día.
        val trasNoche2 = listOf(n1.copy(estado = "Finalizado"), n2.copy(estado = "Finalizado"))
        assertTrue(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = n2,
                todasLasAsignacionesTurno = trasNoche2
            )
        )

        // Llegada anticipada cruzando medianoche: 23 nov 23:45 COL para turno del 24 nov 00:15..08:15.
        val mad = AsignacionTurnoUsuario(30, idUsuario, 3, "2026-11-24", "Asignado", "GRP-MAD")
        val antesMedianoche = fechaCol("2026-11-23T23:45:00-05:00")
        val bloqueMad = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = listOf(mad),
            turnosCatalogo = turnos,
            ahora = antesMedianoche
        )
        assertNotNull(bloqueMad)
        assertEquals("2026-11-23T23:45:00-05:00", bloqueMad!!.fechaInicioIso)
        assertEquals("2026-11-24T08:15:00-05:00", bloqueMad.fechaFinIso)
    }

    @Test
    fun bloqueAdyacentePreseleccionado_noSeContaminaNiSeDesactivaDuranteBloqueActual() = runBlocking {
        val idUsuario = 7
        val b1d1 = AsignacionTurnoUsuario(1, idUsuario, 1, "2026-11-23", "Finalizado", "GRP-B1")
        val b1d2 = AsignacionTurnoUsuario(2, idUsuario, 1, "2026-11-24", "Asignado", "GRP-B1")

        val ahora = fechaCol("2026-11-24T18:40:00-05:00")
        val bloque1 = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            asignacionesTurno = listOf(b1d1, b1d2),
            turnosCatalogo = listOf(turnoDiurno),
            ahora = ahora
        )
        assertNotNull(bloque1)

        val apBloque1 = AsignacionPacienteCuidador(
            idAsignacion = 10,
            idUsuario = idUsuario,
            idPaciente = 1,
            fechaInicio = "2026-11-23T07:00:00-05:00",
            fechaFin = "2026-11-24T19:00:00-05:00",
            estado = "Activo"
        )
        val apAdyacenteNocturno = AsignacionPacienteCuidador(
            idAsignacion = 11,
            idUsuario = idUsuario,
            idPaciente = 4,
            fechaInicio = "2026-11-24T18:30:00-05:00",
            fechaFin = "2026-11-26T07:00:00-05:00",
            estado = "Activo"
        )

        // Bloque 1 reconoce a apBloque1, pero NO al turno nocturno adyacente que termina días después.
        assertTrue(AsignacionVigenciaHelper.perteneceABloqueVigente(apBloque1, bloque1!!, ahora))
        assertFalse(AsignacionVigenciaHelper.perteneceABloqueVigente(apAdyacenteNocturno, bloque1, ahora))
        assertFalse(
            AsignacionVigenciaHelper.perteneceAlRangoDeSincronizacion(
                asignacion = apAdyacenteNocturno,
                fechaInicioIso = bloque1.fechaInicioIso,
                fechaFinIso = bloque1.fechaFinIso
            )
        )

        // Sincronizar cambios en bloque 1 (solo paciente 1) NO debe desactivar al paciente 4 del bloque adyacente.
        val backend = FakeAsignacionesBackend(listOf(apBloque1, apAdyacenteNocturno))
        val repo = PacienteRepository(backend.crearApiService())
        val ok = repo.sincronizarSeleccionDelCuidador(
            idUsuario = idUsuario,
            idsSeleccionados = setOf(1),
            fechaInicioIso = bloque1.fechaInicioIso,
            fechaFinIso = bloque1.fechaFinIso,
            ahora = ahora
        )
        assertTrue(ok)
        assertEquals("Activo", backend.asignaciones.first { it.idAsignacion == 11 }.estado)
        assertEquals("2026-11-26T07:00:00-05:00", backend.asignaciones.first { it.idAsignacion == 11 }.fechaFin)
    }

    @Test
    fun idGrupoVacioOFechaInvalida_noAsumeUltimoDiaYRecuperaGrupoDesdeBackendFresco() {
        val sinGrupoVacio = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 30,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "Asignado",
            id_grupo_asignacion = ""
        )
        val sinGrupoNull = sinGrupoVacio.copy(id_grupo_asignacion = null)
        val fechaInvalida = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 31,
            id_usuario = 7,
            id_turno = 1,
            fecha = "2026-02-30",
            estado = "Asignado",
            id_grupo_asignacion = "GRP-X"
        )
        val formatoInvalido = fechaInvalida.copy(fecha = "23/11/2026")

        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = sinGrupoVacio,
                todasLasAsignacionesTurno = listOf(sinGrupoVacio)
            )
        )
        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = sinGrupoNull,
                todasLasAsignacionesTurno = listOf(sinGrupoNull)
            )
        )
        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = fechaInvalida,
                todasLasAsignacionesTurno = listOf(fechaInvalida)
            )
        )
        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = formatoInvalido,
                todasLasAsignacionesTurno = listOf(formatoInvalido)
            )
        )
        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = AsignacionTurnoUsuario(32, 7, 1, "2026-11-23", "Asignado", "GRP-OK"),
                todasLasAsignacionesTurno = emptyList()
            )
        )

        // Si el objeto local no tenía id_grupo_asignacion pero el listado fresco sí, lo recupera correctamente.
        val frescaDia1 = sinGrupoNull.copy(id_grupo_asignacion = "GRP-REAL", estado = "Finalizado")
        val frescaDia2 = AsignacionTurnoUsuario(33, 7, 1, "2026-11-24", "Asignado", "GRP-REAL")
        assertFalse(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = sinGrupoNull,
                todasLasAsignacionesTurno = listOf(frescaDia1, frescaDia2)
            )
        )
        assertTrue(
            AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                asignacionCerrada = sinGrupoNull,
                todasLasAsignacionesTurno = listOf(frescaDia1)
            )
        )
    }

    @Test
    fun sincronizacion_desactivaSoloDesmarcadosDelBloqueActual_yReactivaSoloMismoBloque() = runBlocking {
        val idUsuario = 7
        val inicioBloque = "2026-11-23T07:00:00-05:00"
        val finBloque = "2026-11-25T19:00:00-05:00"
        val ahora = fechaCol("2026-11-24T10:00:00-05:00")

        val actualP1 = AsignacionPacienteCuidador(101, idUsuario, 1, inicioBloque, finBloque, " Activo ")
        val actualP2Desmarcado = AsignacionPacienteCuidador(102, idUsuario, 2, inicioBloque, finBloque, "Activo")
        val actualP3InactivaMismoBloque = AsignacionPacienteCuidador(
            103,
            idUsuario,
            3,
            inicioBloque,
            "2026-11-23T15:00:00-05:00",
            " Inactivo "
        )
        val pasadoActivo = AsignacionPacienteCuidador(
            104,
            idUsuario,
            8,
            "2026-11-10T07:00:00-05:00",
            "2026-11-12T19:00:00-05:00",
            "Activo"
        )
        val futuroActivo = AsignacionPacienteCuidador(
            105,
            idUsuario,
            9,
            "2026-11-28T07:00:00-05:00",
            "2026-11-30T19:00:00-05:00",
            "Activo"
        )

        val backend = FakeAsignacionesBackend(
            listOf(actualP1, actualP2Desmarcado, actualP3InactivaMismoBloque, pasadoActivo, futuroActivo)
        )
        val repo = PacienteRepository(backend.crearApiService())

        val ok = repo.sincronizarSeleccionDelCuidador(
            idUsuario = idUsuario,
            idsSeleccionados = setOf(1, 3),
            fechaInicioIso = inicioBloque,
            fechaFinIso = finBloque,
            ahora = ahora
        )
        assertTrue(ok)

        assertEquals("Inactivo", backend.asignaciones.first { it.idAsignacion == 102 }.estado)
        assertEquals("Activo", backend.asignaciones.first { it.idAsignacion == 104 }.estado)
        assertEquals("2026-11-12T19:00:00-05:00", backend.asignaciones.first { it.idAsignacion == 104 }.fechaFin)
        assertEquals("Activo", backend.asignaciones.first { it.idAsignacion == 105 }.estado)
        assertEquals("Activo", backend.asignaciones.first { it.idAsignacion == 103 }.estado)
        assertEquals(finBloque, backend.asignaciones.first { it.idAsignacion == 103 }.fechaFin)
        assertFalse(backend.patchesEjecutados.any { it.first == 101 })
        assertTrue(backend.postsEjecutados.isEmpty())
    }

    @Test
    fun modeloAsignacionTurnoUsuario_conservaIdGrupoAsignacionConGson() {
        val json = """
            {
              "id_asignacion_turno_usuario": 55,
              "id_usuario": 7,
              "id_turno": 1,
              "fecha": "2026-11-23",
              "estado": "Asignado",
              "id_grupo_asignacion": "grupo-abc-123"
            }
        """.trimIndent()

        val gson = Gson()
        val deserializada = gson.fromJson(json, AsignacionTurnoUsuario::class.java)
        assertEquals(55, deserializada.id_asignacion_turno_usuario)
        assertEquals(7, deserializada.id_usuario)
        assertEquals(1, deserializada.id_turno)
        assertEquals("2026-11-23", deserializada.fecha)
        assertEquals("Asignado", deserializada.estado)
        assertEquals("grupo-abc-123", deserializada.id_grupo_asignacion)

        val serializada = gson.toJson(deserializada)
        assertTrue(serializada.contains("\"id_grupo_asignacion\":\"grupo-abc-123\""))
    }

    @Test
    fun perteneceATurno_noMezclaTurnosConsecutivosEnElLimiteExacto() {
        val asignacionDiurna = AsignacionPacienteCuidador(
            idAsignacion = 200,
            idUsuario = 7,
            idPaciente = 1,
            fechaInicio = "2026-11-23T07:00:00-05:00",
            fechaFin = "2026-11-23T19:00:00-05:00",
            estado = "Inactivo"
        )

        val rangoNocturno = AsignacionVigenciaHelper.calcularRangoTurnoDia(
            asignacionTurno = AsignacionTurnoUsuario(201, 7, 2, "2026-11-23", "Asignado", "GRP-N"),
            turno = turnoNocturno
        )
        assertNotNull(rangoNocturno)
        assertFalse(
            AsignacionVigenciaHelper.perteneceATurno(
                asignacion = asignacionDiurna,
                inicio = rangoNocturno!!.first,
                fin = rangoNocturno.second
            )
        )
    }
}
