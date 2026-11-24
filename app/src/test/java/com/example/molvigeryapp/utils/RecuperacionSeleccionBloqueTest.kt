package com.example.molvigeryapp.utils

import com.example.molvigeryapp.data.api.ApiService
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.lang.reflect.Proxy
import java.util.Date

/**
 * Pruebas compatibles con HEAD para la corrección aislada de recuperación de pacientes
 * seleccionados mediante [AsignacionVigenciaHelper.perteneceABloqueVigente] y
 * [RecuperacionAsignacionHelper].
 */
class RecuperacionSeleccionBloqueTest {

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

    private fun fechaCol(iso: String): Date =
        requireNotNull(AsignacionVigenciaHelper.parsearFechaIso(iso))

    private fun crearPaciente(id: Int, nombre: String): Paciente =
        Paciente(
            idPaciente = id,
            nombre = nombre,
            apellido = "Prueba",
            eps = "EPS Test",
            habitacion = 100 + id,
            estado = true,
            isSelected = false
        )

    private class FakeBackendRecuperacion(
        val pacientesCatalogo: List<Paciente>,
        val desfaseLecturaHoras: Int = 5
    ) {
        val asignaciones = mutableListOf<AsignacionPacienteCuidador>()
        var usuarioAutenticadoActual: Int = 7
        var postsEjecutados: Int = 0
        private var siguienteId = 100

        private fun desplazarIso(iso: String?, horas: Int): String? {
            if (iso == null || horas == 0) return iso
            val fecha = AsignacionVigenciaHelper.parsearFechaIso(iso) ?: return iso
            return AsignacionVigenciaHelper.formatearIsoColombia(
                Date(fecha.time + horas * 60L * 60L * 1000L)
            )
        }

        private fun proyectarParaGet(asignacion: AsignacionPacienteCuidador): AsignacionPacienteCuidador {
            if (desfaseLecturaHoras == 0) return asignacion
            return asignacion.copy(
                fechaInicio = desplazarIso(asignacion.fechaInicio, desfaseLecturaHoras)
                    ?: asignacion.fechaInicio,
                fechaFin = desplazarIso(asignacion.fechaFin, desfaseLecturaHoras)
            )
        }

        private fun idsOcupadosPorOtros(idUsuario: Int, ahora: Date): Set<Int> =
            asignaciones
                .filter { asig ->
                    asig.idUsuario != idUsuario &&
                        AsignacionVigenciaHelper.estaVigente(asig, ahora)
                }
                .map { it.idPaciente }
                .toSet()

        private fun hayConflictoConOtro(
            idPaciente: Int,
            idUsuario: Int,
            fechaInicioIso: String,
            fechaFinIso: String
        ): Boolean {
            val inicioNuevo = AsignacionVigenciaHelper.parsearFechaIso(fechaInicioIso) ?: return false
            val finNuevo = AsignacionVigenciaHelper.parsearFechaIso(fechaFinIso) ?: return false
            return asignaciones.any { existente ->
                existente.idPaciente == idPaciente &&
                    existente.idUsuario != idUsuario &&
                    AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(existente.estado) &&
                    AsignacionVigenciaHelper.perteneceATurno(existente, inicioNuevo, finNuevo)
            }
        }

        fun crearApiService(ahoraProvider: () -> Date): ApiService {
            return Proxy.newProxyInstance(
                ApiService::class.java.classLoader,
                arrayOf(ApiService::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "getPacientesDisponiblesSeleccion" -> {
                        val ocupadosOtros = idsOcupadosPorOtros(
                            usuarioAutenticadoActual,
                            ahoraProvider()
                        )
                        pacientesCatalogo
                            .filterNot { it.idPaciente in ocupadosOtros }
                            .map { it.copy(isSelected = false) }
                    }

                    "getAsignacionesPacienteCuidador" -> {
                        Response.success(asignaciones.map(::proyectarParaGet))
                    }

                    "guardarAsignacion" -> {
                        val nueva = args[0] as AsignacionPacienteCuidador
                        if (
                            AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(nueva.estado) &&
                            hayConflictoConOtro(
                                idPaciente = nueva.idPaciente,
                                idUsuario = nueva.idUsuario,
                                fechaInicioIso = nueva.fechaInicio,
                                fechaFinIso = nueva.fechaFin.orEmpty()
                            )
                        ) {
                            val cuerpo =
                                """{"error":"Este paciente ya fue seleccionado por otro cuidador y no se encuentra disponible para este turno."}"""
                                    .toResponseBody("application/json".toMediaType())
                            Response.error<AsignacionPacienteCuidador>(409, cuerpo)
                        } else {
                            val creada = nueva.copy(idAsignacion = siguienteId++)
                            postsEjecutados++
                            asignaciones.add(creada)
                            Response.success(201, proyectarParaGet(creada))
                        }
                    }

                    else -> throw UnsupportedOperationException("Método no simulado: ${method.name}")
                }
            } as ApiService
        }
    }

    @Test
    fun flujoPrincipal_seleccionar3_guardar_salir_volverAEntrar_recupera3_yMantieneExclusividadEntreCuidadores() =
        runBlocking {
            val cuidador1 = 7
            val cuidador2 = 8
            var ahora = fechaCol("2026-11-23T09:00:00-05:00")

            val asignacionesTurno = listOf(
                AsignacionTurnoUsuario(10, cuidador1, 1, "2026-11-23", "En curso", "GRP-C1"),
                AsignacionTurnoUsuario(11, cuidador2, 1, "2026-11-23", "En curso", "GRP-C2")
            )
            val turnos = listOf(turnoDiurno)

            val backend = FakeBackendRecuperacion(
                pacientesCatalogo = listOf(
                    crearPaciente(1, "Ana"),
                    crearPaciente(2, "Luis"),
                    crearPaciente(3, "Marta"),
                    crearPaciente(4, "Pedro")
                ),
                desfaseLecturaHoras = 5
            )
            val repo = PacienteRepository(backend.crearApiService { ahora })

            // 1. Cuidador 1 carga disponibles y selecciona 3 pacientes (1, 2, 3).
            backend.usuarioAutenticadoActual = cuidador1
            val disponiblesIniciales = repo.obtenerPacientesDisponiblesParaSeleccion()
            assertEquals(setOf(1, 2, 3, 4), disponiblesIniciales.mapNotNull { it.idPaciente }.toSet())

            val bloqueCuidador1 = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                    idUsuario = cuidador1,
                    asignacionesTurno = asignacionesTurno,
                    turnosCatalogo = turnos,
                    ahora = ahora
                )
            )

            // 2. Guardar correctamente los 3 pacientes seleccionados.
            val resultadoGuardado = repo.sincronizarSeleccionDelCuidadorDetallado(
                idUsuario = cuidador1,
                idsSeleccionados = setOf(1, 2, 3),
                fechaInicioIso = bloqueCuidador1.fechaInicioIso,
                fechaFinIso = bloqueCuidador1.fechaFinIso,
                ahora = ahora,
                idGrupoAsignacion = bloqueCuidador1.asignacionTurno.id_grupo_asignacion
            )
            assertTrue(resultadoGuardado.exitoso)
            assertEquals(setOf(1, 2, 3), resultadoGuardado.idsConfirmados)
            assertEquals(3, backend.postsEjecutados)

            // 3 y 4. Salir de la pantalla y volver a entrar (sin estado en memoria, consultando GET +5h).
            ahora = fechaCol("2026-11-23T09:30:00-05:00")
            val bloqueReingreso = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                    idUsuario = cuidador1,
                    asignacionesTurno = asignacionesTurno,
                    turnosCatalogo = turnos,
                    ahora = ahora
                )
            )
            val asignacionesDesdeBackend = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()

            // 5. Recuperar exactamente los 3 pacientes seleccionados (flujo exacto de PacienteViewModel en HEAD).
            val idsRecuperadosCuidador1 = asignacionesDesdeBackend
                .filter { asignacion ->
                    asignacion.idUsuario == cuidador1 &&
                        AsignacionVigenciaHelper.perteneceABloqueVigente(
                            asignacion = asignacion,
                            bloque = bloqueReingreso,
                            ahora = ahora
                        )
                }
                .map { it.idPaciente }
                .toSet()

            assertEquals(setOf(1, 2, 3), idsRecuperadosCuidador1)

            val listaVisualReingreso = repo.obtenerPacientesDisponiblesParaSeleccion().onEach { paciente ->
                paciente.isSelected = paciente.idPaciente in idsRecuperadosCuidador1
            }
            assertEquals(
                setOf(1, 2, 3),
                listaVisualReingreso.filter { it.isSelected }.mapNotNull { it.idPaciente }.toSet()
            )
            assertFalse(listaVisualReingreso.first { it.idPaciente == 4 }.isSelected)
            // No se crearon nuevos POST para recuperar la selección.
            assertEquals(3, backend.postsEjecutados)

            // 6. Otro cuidador no los ve disponibles, no los recupera ni puede seleccionarlos.
            backend.usuarioAutenticadoActual = cuidador2
            val disponiblesCuidador2 = repo.obtenerPacientesDisponiblesParaSeleccion()
            assertEquals(setOf(4), disponiblesCuidador2.mapNotNull { it.idPaciente }.toSet())

            val bloqueCuidador2 = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                    idUsuario = cuidador2,
                    asignacionesTurno = asignacionesTurno,
                    turnosCatalogo = turnos,
                    ahora = ahora
                )
            )
            val idsRecuperadosCuidador2 = asignacionesDesdeBackend
                .filter { asignacion ->
                    asignacion.idUsuario == cuidador2 &&
                        AsignacionVigenciaHelper.perteneceABloqueVigente(
                            asignacion = asignacion,
                            bloque = bloqueCuidador2,
                            ahora = ahora
                        )
                }
                .map { it.idPaciente }
                .toSet()
            assertTrue(idsRecuperadosCuidador2.isEmpty())

            val intentoConflictoCuidador2 = repo.sincronizarSeleccionDelCuidadorDetallado(
                idUsuario = cuidador2,
                idsSeleccionados = setOf(1, 2, 3, 4),
                fechaInicioIso = bloqueCuidador2.fechaInicioIso,
                fechaFinIso = bloqueCuidador2.fechaFinIso,
                ahora = ahora,
                idGrupoAsignacion = bloqueCuidador2.asignacionTurno.id_grupo_asignacion
            )
            assertFalse(intentoConflictoCuidador2.exitoso)
            assertEquals(setOf(1, 2, 3), intentoConflictoCuidador2.idsEnConflicto)
            assertEquals(setOf(4), intentoConflictoCuidador2.idsConfirmados)
        }

    @Test
    fun compensacionCincoHoras_turnoDiurnoNocturnoYMultiDia_respetaVigenciaReal() {
        val idUsuario = 7

        // Turno diurno (07:00..19:00 COL -> GET +5h: 12:00..00:00+1d)
        val bloqueDiurno = requireNotNull(
            AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                idUsuario = idUsuario,
                asignacionesTurno = listOf(
                    AsignacionTurnoUsuario(301, idUsuario, 1, "2026-11-23", "En curso", "GRP-DIURNO")
                ),
                turnosCatalogo = listOf(turnoDiurno),
                ahora = fechaCol("2026-11-23T09:00:00-05:00")
            )
        )
        val diurnaConDesfase5h = AsignacionPacienteCuidador(
            idAsignacion = 402,
            idUsuario = idUsuario,
            idPaciente = 2,
            fechaInicio = "2026-11-23T12:00:00-05:00",
            fechaFin = "2026-11-24T00:00:00-05:00",
            estado = "Activo",
            idGrupoAsignacion = "GRP-DIURNO"
        )
        assertTrue(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                diurnaConDesfase5h,
                bloqueDiurno,
                fechaCol("2026-11-23T09:00:00-05:00")
            )
        )
        assertTrue(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                diurnaConDesfase5h,
                bloqueDiurno,
                fechaCol("2026-11-23T15:00:00-05:00")
            )
        )
        // Tras las 19:00 reales ya venció aunque el GET crudo diga 00:00+1d.
        assertFalse(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                diurnaConDesfase5h,
                bloqueDiurno,
                fechaCol("2026-11-23T19:05:00-05:00")
            )
        )
        // Antes del inicio real (06:55) aún no está vigente.
        assertFalse(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                diurnaConDesfase5h,
                bloqueDiurno,
                fechaCol("2026-11-23T06:55:00-05:00")
            )
        )

        // Turno nocturno con cruce de medianoche (19:00..07:00+1d -> GET +5h: 00:00+1d..12:00+1d)
        val bloqueNocturno = requireNotNull(
            AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                idUsuario = idUsuario,
                asignacionesTurno = listOf(
                    AsignacionTurnoUsuario(302, idUsuario, 2, "2026-11-23", "En curso", "GRP-NOCTURNO")
                ),
                turnosCatalogo = listOf(turnoNocturno),
                ahora = fechaCol("2026-11-23T21:00:00-05:00")
            )
        )
        val nocturnaConDesfase5h = AsignacionPacienteCuidador(
            idAsignacion = 403,
            idUsuario = idUsuario,
            idPaciente = 3,
            fechaInicio = "2026-11-24T00:00:00-05:00",
            fechaFin = "2026-11-24T12:00:00-05:00",
            estado = "Activo",
            idGrupoAsignacion = "GRP-NOCTURNO"
        )
        assertTrue(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                nocturnaConDesfase5h,
                bloqueNocturno,
                fechaCol("2026-11-23T21:00:00-05:00")
            )
        )
        assertTrue(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                nocturnaConDesfase5h,
                bloqueNocturno,
                fechaCol("2026-11-24T02:00:00-05:00")
            )
        )
        assertFalse(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                nocturnaConDesfase5h,
                bloqueNocturno,
                fechaCol("2026-11-24T07:05:00-05:00")
            )
        )

        // Bloque multi-día nocturno (20..29 nov -> fin real 30 nov 07:00, GET +5h: 30 nov 12:00)
        val diasNocturno = (20..29).mapIndexed { idx, dia ->
            AsignacionTurnoUsuario(
                id_asignacion_turno_usuario = 310 + idx,
                id_usuario = 5,
                id_turno = 2,
                fecha = "2026-11-$dia",
                estado = "Asignado",
                id_grupo_asignacion = "487cc568-45b5-42e1-a58c-b8d6b47b4318"
            )
        }
        val bloqueMultiDia = requireNotNull(
            AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                idUsuario = 5,
                asignacionesTurno = diasNocturno,
                turnosCatalogo = listOf(turnoNocturno),
                ahora = fechaCol("2026-11-20T21:30:00-05:00")
            )
        )
        val asignacionHistoricaSinGrupo = AsignacionPacienteCuidador(
            idAsignacion = 131,
            idUsuario = 5,
            idPaciente = 12,
            fechaInicio = "2026-11-21T00:00:00-05:00",
            fechaFin = "2026-11-30T12:00:00-05:00",
            estado = "Activo",
            idGrupoAsignacion = null
        )
        assertTrue(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                asignacionHistoricaSinGrupo,
                bloqueMultiDia,
                fechaCol("2026-11-20T21:30:00-05:00")
            )
        )
        assertTrue(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                asignacionHistoricaSinGrupo,
                bloqueMultiDia,
                fechaCol("2026-11-23T16:00:00-05:00")
            )
        )
    }

    @Test
    fun sinDobleConversionConUtcZNiColombiaNormal_yRechazaDesfaseNoExactoYGrupoDistinto() {
        val idUsuario = 7
        val ahora = fechaCol("2026-11-23T14:00:00-05:00")
        val bloqueActual = requireNotNull(
            AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                idUsuario = idUsuario,
                asignacionesTurno = listOf(
                    AsignacionTurnoUsuario(320, idUsuario, 1, "2026-11-23", "En curso", "GRP-ACTUAL")
                ),
                turnosCatalogo = listOf(turnoDiurno),
                ahora = ahora
            )
        )

        // UTC 'Z' equivalente a 07:00..19:00 COL: pasa directo, sin doble conversión.
        val asignacionUtcZ = AsignacionPacienteCuidador(
            idAsignacion = 500,
            idUsuario = idUsuario,
            idPaciente = 1,
            fechaInicio = "2026-11-23T12:00:00Z",
            fechaFin = "2026-11-24T00:00:00Z",
            estado = "Activo",
            idGrupoAsignacion = "GRP-ACTUAL"
        )
        assertTrue(AsignacionVigenciaHelper.perteneceABloqueVigente(asignacionUtcZ, bloqueActual, ahora))
        assertFalse(
            AsignacionVigenciaHelper.perteneceABloqueVigente(
                asignacionUtcZ,
                bloqueActual,
                fechaCol("2026-11-23T19:05:00-05:00")
            )
        )

        // Colombia normal -05:00: pasa directo, sin doble conversión.
        val asignacionCol = asignacionUtcZ.copy(
            idAsignacion = 501,
            fechaInicio = "2026-11-23T07:00:00-05:00",
            fechaFin = "2026-11-23T19:00:00-05:00"
        )
        assertTrue(AsignacionVigenciaHelper.perteneceABloqueVigente(asignacionCol, bloqueActual, ahora))

        // Desfase +5h no muta cadenas originales.
        val asignacionDesfase5h = asignacionUtcZ.copy(
            idAsignacion = 502,
            fechaInicio = "2026-11-23T12:00:00-05:00",
            fechaFin = "2026-11-24T00:00:00-05:00"
        )
        assertTrue(AsignacionVigenciaHelper.perteneceABloqueVigente(asignacionDesfase5h, bloqueActual, ahora))
        assertEquals("2026-11-23T12:00:00-05:00", asignacionDesfase5h.fechaInicio)
        assertEquals("2026-11-24T00:00:00-05:00", asignacionDesfase5h.fechaFin)

        // Desfase no exacto (+3h y +6h) -> rechazado.
        val desfase3h = asignacionDesfase5h.copy(
            idAsignacion = 503,
            fechaInicio = "2026-11-23T10:00:00-05:00",
            fechaFin = "2026-11-23T22:00:00-05:00"
        )
        val desfase6h = asignacionDesfase5h.copy(
            idAsignacion = 504,
            fechaInicio = "2026-11-23T13:00:00-05:00",
            fechaFin = "2026-11-24T01:00:00-05:00"
        )
        assertFalse(AsignacionVigenciaHelper.perteneceABloqueVigente(desfase3h, bloqueActual, ahora))
        assertFalse(AsignacionVigenciaHelper.perteneceABloqueVigente(desfase6h, bloqueActual, ahora))

        // Otro bloque (+5h de bloque que termina otro día) -> rechazado.
        val otroBloque = asignacionDesfase5h.copy(
            idAsignacion = 505,
            fechaInicio = "2026-11-23T12:00:00-05:00",
            fechaFin = "2026-11-25T00:00:00-05:00",
            idGrupoAsignacion = null
        )
        assertFalse(AsignacionVigenciaHelper.perteneceABloqueVigente(otroBloque, bloqueActual, ahora))

        // Grupo diferente (tanto directo como con +5h) -> rechazado.
        val otroGrupoDirecto = asignacionCol.copy(idAsignacion = 506, idGrupoAsignacion = "GRP-OTRO")
        val otroGrupo5h = asignacionDesfase5h.copy(idAsignacion = 507, idGrupoAsignacion = "GRP-OTRO")
        assertFalse(AsignacionVigenciaHelper.perteneceABloqueVigente(otroGrupoDirecto, bloqueActual, ahora))
        assertFalse(AsignacionVigenciaHelper.perteneceABloqueVigente(otroGrupo5h, bloqueActual, ahora))

        // Estado inactivo -> rechazado.
        val inactiva5h = asignacionDesfase5h.copy(idAsignacion = 508, estado = "Inactivo")
        assertFalse(AsignacionVigenciaHelper.perteneceABloqueVigente(inactiva5h, bloqueActual, ahora))
    }
}
