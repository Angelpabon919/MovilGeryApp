package com.example.molvigeryapp.utils

import android.content.SharedPreferences
import com.example.molvigeryapp.data.api.ApiService
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.RegistrarLlegadaResponse
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.data.repository.TurnoRepository
import com.example.molvigeryapp.ui.cuidador.llegada.LlegadaTurnoHelper
import com.example.molvigeryapp.ui.cuidador.pacientes.DatosAlarmaMedicamento
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.Date

/**
 * Suite de regresión del ciclo completo de selección, sesión y automatizaciones del cuidador:
 * - A: Seleccionar 3, guardar y recuperar los 3 (con desfase +5h e id_grupo_asignacion = null en BD).
 * - B/C/G/H: Deseleccionar 1 (B), guardar, recuperar {A, C} sin duplicados ni pérdida,
 *   liberar B en backend (PATCH) y confirmar disponibilidad exclusiva para otro cuidador.
 * - D: Cierre de sesión limpia únicamente datos locales del teléfono SIN enviar PATCH Inactivo al
 *   servidor; al volver a iniciar sesión recupera las asignaciones activas del turno vigente.
 * - E: Cierre de turno multidía conserva días intermedios, respeta caché/inicioBase de TurnoRepository
 *   y libera únicamente al confirmar el último día del bloque.
 * - F: Reinstalación / nuevo dispositivo limpia residuos locales sin cancelar alarmas en login ni
 *   desactivar asignaciones vigentes en el servidor.
 * - Regresión de alarmas de medicamentos, llegada idempotente y sesión FCM/CargaAccesoActivity.
 */
class CicloSeleccionPacientesTest {

    private val turnoDiurno = Turno(
        id_turno = 1,
        hora_inicio = "07:00:00",
        hora_fin = "19:00:00",
        estado = true,
        nombre = "Diurno"
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

    private class InMemorySharedPreferences : SharedPreferences {
        private val data = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = LinkedHashMap(data)
        override fun getString(key: String?, defValue: String?): String? =
            (data[key] as? String) ?: defValue

        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (data[key] as? Set<String>)?.toMutableSet() ?: defValues

        override fun getInt(key: String?, defValue: Int): Int =
            (data[key] as? Int) ?: defValue

        override fun getLong(key: String?, defValue: Long): Long =
            (data[key] as? Long) ?: defValue

        override fun getFloat(key: String?, defValue: Float): Float =
            (data[key] as? Float) ?: defValue

        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (data[key] as? Boolean) ?: defValue

        override fun contains(key: String?): Boolean = data.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = linkedMapOf<String, Any?>()
            private val removals = linkedSetOf<String>()
            private var clearAll = false

            override fun putString(key: String?, value: String?) = apply {
                if (key != null) pending[key] = value
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {
                if (key != null) pending[key] = values?.toSet()
            }

            override fun putInt(key: String?, value: Int) = apply {
                if (key != null) pending[key] = value
            }

            override fun putLong(key: String?, value: Long) = apply {
                if (key != null) pending[key] = value
            }

            override fun putFloat(key: String?, value: Float) = apply {
                if (key != null) pending[key] = value
            }

            override fun putBoolean(key: String?, value: Boolean) = apply {
                if (key != null) pending[key] = value
            }

            override fun remove(key: String?) = apply {
                if (key != null) removals.add(key)
            }

            override fun clear() = apply { clearAll = true }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) data.clear()
                removals.forEach { data.remove(it) }
                pending.forEach { (k, v) ->
                    if (v == null) data.remove(k) else data[k] = v
                }
            }
        }

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
    }

    /**
     * Simula el backend real de PostgreSQL + DRF:
     * - `asignacion_paciente_cuidador` NO persiste `id_grupo_asignacion` (siempre devuelve null).
     * - GET proyecta `fecha_inicio` y `fecha_fin` con +5h (desfase real observado en PG + DRF).
     * - PATCH valida `fecha_fin >= fecha_inicio` sobre la fecha leída por DRF (+5h).
     * - Exige TOKEN presente al recibir PATCH si se configura `verificarTokenEnPatch`.
     * - Mantiene exclusividad atómica entre cuidadores (409 Conflict y exclusión en disponibles).
     */
    private class FakeBackendRealPG(
        val pacientesCatalogo: List<Paciente>,
        val turnos: MutableList<Turno>,
        val asignacionesTurno: MutableList<AsignacionTurnoUsuario>,
        val asignacionesPacientes: MutableList<AsignacionPacienteCuidador> = mutableListOf(),
        val desfaseLecturaHoras: Int = 5,
        val eliminarGrupoAlGuardarComoEnPgReal: Boolean = true,
        val tokenProvider: (() -> String?)? = null
    ) {
        var usuarioAutenticadoActual: Int = 7
        var postsEjecutados: Int = 0
        var patchesEjecutados: Int = 0
        val historialPatches = mutableListOf<Pair<Int, Map<String, String>>>()
        var fallosTransitoriosPatchRestantes: Int = 0
        var fallarSiemprePatch: Boolean = false
        var tokenPresenteEnUltimoPatch: Boolean? = null
        private var siguienteId = 200

        private fun desplazarIso(iso: String?, horas: Int): String? {
            if (iso == null || horas == 0) return iso
            val fecha = AsignacionVigenciaHelper.parsearFechaIso(iso) ?: return iso
            return AsignacionVigenciaHelper.formatearIsoColombia(
                Date(fecha.time + horas * 60L * 60L * 1000L)
            )
        }

        private fun proyectarParaGet(asig: AsignacionPacienteCuidador): AsignacionPacienteCuidador {
            if (desfaseLecturaHoras == 0) return asig
            return asig.copy(
                fechaInicio = desplazarIso(asig.fechaInicio, desfaseLecturaHoras) ?: asig.fechaInicio,
                fechaFin = desplazarIso(asig.fechaFin, desfaseLecturaHoras)
            )
        }

        private fun idsOcupadosPorOtros(idUsuario: Int, ahora: Date): Set<Int> =
            asignacionesPacientes
                .filter { asig ->
                    asig.idUsuario != idUsuario &&
                        AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(asig.estado) &&
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
            val iniNuevo = AsignacionVigenciaHelper.parsearFechaIso(fechaInicioIso) ?: return false
            val finNuevo = AsignacionVigenciaHelper.parsearFechaIso(fechaFinIso) ?: return false
            return asignacionesPacientes.any { existente ->
                existente.idPaciente == idPaciente &&
                    existente.idUsuario != idUsuario &&
                    AsignacionVigenciaHelper.esEstadoAsignacionPacienteActivo(existente.estado) &&
                    AsignacionVigenciaHelper.perteneceATurno(existente, iniNuevo, finNuevo)
            }
        }

        fun crearApiService(ahoraProvider: () -> Date): ApiService {
            return Proxy.newProxyInstance(
                ApiService::class.java.classLoader,
                arrayOf(ApiService::class.java)
            ) { _, method, args ->
                when (method.name) {
                    "getTurnos" -> turnos.toList()
                    "getAsignacionesTurno" -> asignacionesTurno.toList()
                    "getPacientesDisponiblesSeleccion" -> {
                        val ocupados = idsOcupadosPorOtros(usuarioAutenticadoActual, ahoraProvider())
                        pacientesCatalogo
                            .filterNot { it.idPaciente in ocupados }
                            .map { it.copy(isSelected = false) }
                    }
                    "getAsignacionesPacienteCuidador" -> {
                        Response.success(asignacionesPacientes.map(::proyectarParaGet))
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
                            val body = """{"error":"Este paciente ya fue seleccionado por otro cuidador y no se encuentra disponible para este turno."}"""
                                .toResponseBody("application/json".toMediaType())
                            Response.error<AsignacionPacienteCuidador>(409, body)
                        } else {
                            val creada = nueva.copy(
                                idAsignacion = siguienteId++,
                                idGrupoAsignacion = if (eliminarGrupoAlGuardarComoEnPgReal) null else nueva.idGrupoAsignacion
                            )
                            postsEjecutados++
                            asignacionesPacientes.add(creada)
                            Response.success(201, proyectarParaGet(creada))
                        }
                    }
                    "actualizarEstadoAsignacion" -> {
                        if (tokenProvider != null) {
                            val tok = tokenProvider.invoke()
                            tokenPresenteEnUltimoPatch = !tok.isNullOrBlank()
                            if (tok.isNullOrBlank()) {
                                val body = """{"detail":"No autenticado"}"""
                                    .toResponseBody("application/json".toMediaType())
                                return@newProxyInstance Response.error<AsignacionPacienteCuidador>(401, body)
                            }
                        }
                        if (fallarSiemprePatch) {
                            throw IOException("Sin conexión simulada")
                        }
                        if (fallosTransitoriosPatchRestantes > 0) {
                            fallosTransitoriosPatchRestantes--
                            throw IOException("Fallo transitorio simulado")
                        }

                        val id = args[0] as Int
                        @Suppress("UNCHECKED_CAST")
                        val datos = args[1] as Map<String, String>
                        val idx = asignacionesPacientes.indexOfFirst { it.idAsignacion == id }
                        if (idx < 0) {
                            val body = """{"detail":"Not found."}"""
                                .toResponseBody("application/json".toMediaType())
                            Response.error<AsignacionPacienteCuidador>(404, body)
                        } else {
                            val actual = asignacionesPacientes[idx]
                            val proyectada = proyectarParaGet(actual)
                            val iniVal = AsignacionVigenciaHelper.parsearFechaIso(
                                datos["fecha_inicio"] ?: proyectada.fechaInicio
                            )
                            val finVal = AsignacionVigenciaHelper.parsearFechaIso(
                                datos["fecha_fin"] ?: proyectada.fechaFin
                            )
                            if (iniVal != null && finVal != null && iniVal.after(finVal)) {
                                val body = """{"fecha_fin":["La fecha de fin debe ser mayor o igual a la fecha de inicio."]}"""
                                    .toResponseBody("application/json".toMediaType())
                                return@newProxyInstance Response.error<AsignacionPacienteCuidador>(400, body)
                            }
                            patchesEjecutados++
                            historialPatches.add(id to datos)
                            val actualizada = actual.copy(
                                estado = datos["estado"] ?: actual.estado,
                                fechaInicio = datos["fecha_inicio"] ?: actual.fechaInicio,
                                fechaFin = datos["fecha_fin"] ?: actual.fechaFin
                            )
                            asignacionesPacientes[idx] = actualizada
                            Response.success(proyectarParaGet(actualizada))
                        }
                    }
                    else -> throw UnsupportedOperationException("Método no simulado: ${method.name}")
                }
            } as ApiService
        }
    }

    @Test
    fun escenariosABCGH_seleccionar3_deseleccionarB_recuperarAC_yLiberarBParaOtroCuidador() =
        runBlocking {
            val cuidador1 = 7
            val cuidador2 = 8
            var ahora = fechaCol("2026-11-23T09:00:00-05:00") // Dentro de las primeras 5h del turno diurno

            val backend = FakeBackendRealPG(
                pacientesCatalogo = listOf(
                    crearPaciente(1, "A - Ana"),
                    crearPaciente(2, "B - Luis"),
                    crearPaciente(3, "C - Marta"),
                    crearPaciente(4, "D - Pedro")
                ),
                turnos = mutableListOf(turnoDiurno),
                asignacionesTurno = mutableListOf(
                    AsignacionTurnoUsuario(10, cuidador1, 1, "2026-11-23", "En curso", "GRP-C1"),
                    AsignacionTurnoUsuario(11, cuidador2, 1, "2026-11-23", "En curso", "GRP-C2")
                ),
                desfaseLecturaHoras = 5,
                eliminarGrupoAlGuardarComoEnPgReal = true
            )
            val api = backend.crearApiService { ahora }
            val repo = PacienteRepository(api)
            val turnoRepo = TurnoRepository(api)

            val bloqueC1 = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                    idUsuario = cuidador1,
                    turnoRepository = turnoRepo,
                    ahora = ahora
                )
            )

            // A. Seleccionar 3 pacientes (A=1, B=2, C=3) y guardar.
            val resGuardar3 = repo.sincronizarSeleccionDelCuidadorDetallado(
                idUsuario = cuidador1,
                idsSeleccionados = setOf(1, 2, 3),
                fechaInicioIso = bloqueC1.fechaInicioIso,
                fechaFinIso = bloqueC1.fechaFinIso,
                ahora = ahora,
                idGrupoAsignacion = bloqueC1.asignacionTurno.id_grupo_asignacion
            )
            assertTrue(resGuardar3.exitoso)
            assertEquals(setOf(1, 2, 3), resGuardar3.idsConfirmados)
            assertEquals(3, backend.postsEjecutados)

            // Salir y volver a entrar -> recupera exactamente {1, 2, 3} (PR #89 intacto).
            ahora = fechaCol("2026-11-23T09:15:00-05:00")
            val recuperadosTrasGuardar3 = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter {
                    it.idUsuario == cuidador1 &&
                        AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloqueC1, ahora)
                }
                .map { it.idPaciente }
                .toSet()
            assertEquals(setOf(1, 2, 3), recuperadosTrasGuardar3)

            // B y C. Deseleccionar paciente B (2) y guardar nuevamente {1, 3}.
            ahora = fechaCol("2026-11-23T09:20:00-05:00")
            val resGuardar2 = repo.sincronizarSeleccionDelCuidadorDetallado(
                idUsuario = cuidador1,
                idsSeleccionados = setOf(1, 3),
                fechaInicioIso = bloqueC1.fechaInicioIso,
                fechaFinIso = bloqueC1.fechaFinIso,
                ahora = ahora,
                idGrupoAsignacion = bloqueC1.asignacionTurno.id_grupo_asignacion
            )
            assertTrue(resGuardar2.exitoso)
            assertEquals(setOf(1, 3), resGuardar2.idsConfirmados)
            // H. A (1) y C (3) NO se pierden ni generan nuevos POST duplicados.
            assertEquals("No deben crearse POST duplicados para A y C", 3, backend.postsEjecutados)
            assertEquals("Debe enviarse exactamente 1 PATCH de desactivación para B", 1, backend.patchesEjecutados)
            assertEquals(
                "Inactivo",
                backend.asignacionesPacientes.first { it.idPaciente == 2 }.estado
            )
            assertEquals("Activo", backend.asignacionesPacientes.first { it.idPaciente == 1 }.estado)
            assertEquals("Activo", backend.asignacionesPacientes.first { it.idPaciente == 3 }.estado)

            // Salir y volver a entrar -> recupera únicamente {1, 3}; B (2) aparece deseleccionado.
            ahora = fechaCol("2026-11-23T09:40:00-05:00")
            val recuperadosTrasDesmarcar = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter {
                    it.idUsuario == cuidador1 &&
                        AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloqueC1, ahora)
                }
                .map { it.idPaciente }
                .toSet()
            assertEquals(setOf(1, 3), recuperadosTrasDesmarcar)

            // G. Cuidador 2 ahora SÍ ve disponible a B (2) y D (4), y NO ve a A (1) ni C (3).
            backend.usuarioAutenticadoActual = cuidador2
            val disponiblesC2 = repo.obtenerPacientesDisponiblesParaSeleccion()
                .mapNotNull { it.idPaciente }
                .toSet()
            assertEquals(setOf(2, 4), disponiblesC2)

            // Cuidador 2 puede seleccionar B (2), pero si intenta seleccionar A (1) recibe 409.
            val bloqueC2 = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                    idUsuario = cuidador2,
                    turnoRepository = turnoRepo,
                    ahora = ahora
                )
            )
            val resC2 = repo.sincronizarSeleccionDelCuidadorDetallado(
                idUsuario = cuidador2,
                idsSeleccionados = setOf(1, 2),
                fechaInicioIso = bloqueC2.fechaInicioIso,
                fechaFinIso = bloqueC2.fechaFinIso,
                ahora = ahora,
                idGrupoAsignacion = bloqueC2.asignacionTurno.id_grupo_asignacion
            )
            assertFalse(resC2.exitoso)
            assertEquals(setOf(1), resC2.idsEnConflicto)
            assertEquals(setOf(2), resC2.idsConfirmados)
        }

    @Test
    fun escenarioD_cerrarSesionYVolverAEntrar_limpiaSoloTelefono_conservaServidorYRecuperaActivas() =
        runBlocking {
            val cuidador1 = 7
            val cuidador2 = 8
            val ahora = fechaCol("2026-11-23T10:00:00-05:00")

            val prefsSesion = InMemorySharedPreferences()
            val prefsInstalacion = InMemorySharedPreferences()
            val prefsLegacy = InMemorySharedPreferences()
            CicloSeleccionCuidadorHelper.registrarInstalacionSiFalta(prefsInstalacion)

            prefsSesion.edit()
                .putBoolean("SESION_INICIADA", true)
                .putInt("ID_USUARIO", cuidador1)
                .putInt("ID_ROL", 3)
                .putString("TOKEN", "tok-valido-123")
                .putBoolean("FLUJO_CUIDADOR_COMPLETADO", true)
                .putBoolean("seleccion_pacientes_usuario_$cuidador1", true)
                .putStringSet("pacientes_seleccionados_ids_usuario_$cuidador1", mutableSetOf("1", "2"))
                .commit()
            prefsLegacy.edit().putBoolean("SESION_INICIADA", true).commit()

            val backend = FakeBackendRealPG(
                pacientesCatalogo = listOf(
                    crearPaciente(1, "Ana"),
                    crearPaciente(2, "Luis"),
                    crearPaciente(3, "Marta")
                ),
                turnos = mutableListOf(turnoDiurno),
                asignacionesTurno = mutableListOf(
                    AsignacionTurnoUsuario(10, cuidador1, 1, "2026-11-23", "En curso", "GRP-HOY")
                ),
                asignacionesPacientes = mutableListOf(
                    AsignacionPacienteCuidador(301, cuidador1, 1, "2026-11-23T07:00:00-05:00", "2026-11-23T19:00:00-05:00", "Activo", idGrupoAsignacion = "GRP-HOY"),
                    AsignacionPacienteCuidador(302, cuidador1, 2, "2026-11-23T07:00:00-05:00", "2026-11-23T19:00:00-05:00", "Activo", idGrupoAsignacion = null),
                    AsignacionPacienteCuidador(303, cuidador1, 1, "2026-11-28T07:00:00-05:00", "2026-11-28T19:00:00-05:00", "Activo", idGrupoAsignacion = "GRP-FUT"),
                    AsignacionPacienteCuidador(304, cuidador2, 3, "2026-11-23T07:00:00-05:00", "2026-11-23T19:00:00-05:00", "Activo", idGrupoAsignacion = "GRP-C2")
                ),
                desfaseLecturaHoras = 5,
                tokenProvider = { prefsSesion.getString("TOKEN", null) }
            )
            // Incluso sin conexión, cerrar sesión local no debe fallar ni invocar al servidor.
            backend.fallarSiemprePatch = true

            val api = backend.crearApiService { ahora }
            val repo = PacienteRepository(api)
            val turnoRepo = TurnoRepository(api)

            var alarmasCanceladasEnLogout = false
            var viewModelLimpiadoEnLogout = false

            CicloSeleccionCuidadorHelper.cerrarSesionCuidador(
                idUsuario = cuidador1,
                preferenciasSesion = prefsSesion,
                preferenciasInstalacion = prefsInstalacion,
                preferenciasLegacy = prefsLegacy,
                onCancelarAlarmas = { alarmasCanceladasEnLogout = true },
                onLimpiarViewModel = { viewModelLimpiadoEnLogout = true }
            )

            assertTrue(alarmasCanceladasEnLogout)
            assertTrue(viewModelLimpiadoEnLogout)
            assertEquals("Cerrar sesión NO debe enviar PATCH Inactivo al servidor", 0, backend.patchesEjecutados)

            // Preferencias locales del teléfono limpias.
            assertNull(prefsSesion.getString("TOKEN", null))
            assertFalse(prefsSesion.getBoolean("SESION_INICIADA", false))
            assertFalse(prefsSesion.getBoolean("FLUJO_CUIDADOR_COMPLETADO", false))
            assertFalse(prefsSesion.getBoolean("seleccion_pacientes_usuario_$cuidador1", false))
            assertNull(prefsSesion.getStringSet("pacientes_seleccionados_ids_usuario_$cuidador1", null))
            assertFalse(prefsLegacy.getBoolean("SESION_INICIADA", false))

            // Todas las asignaciones continúan Activas en el servidor (protege otro teléfono del cuidador).
            assertEquals("Activo", backend.asignacionesPacientes.first { it.idAsignacion == 301 }.estado)
            assertEquals("Activo", backend.asignacionesPacientes.first { it.idAsignacion == 302 }.estado)
            assertEquals("Activo", backend.asignacionesPacientes.first { it.idAsignacion == 303 }.estado)
            assertEquals("Activo", backend.asignacionesPacientes.first { it.idAsignacion == 304 }.estado)

            // Volver a iniciar sesión en el turno vigente: LoginActivity guarda SESION y prepara local.
            prefsSesion.edit()
                .putBoolean("SESION_INICIADA", true)
                .putInt("ID_ROL", 3)
                .putInt("ID_USUARIO", cuidador1)
                .putString("TOKEN", "tok-reingreso-456")
                .putBoolean("PUSH_ACTIVADAS", false)
                .commit()

            val prepLogin = CicloSeleccionCuidadorHelper.prepararSesionCuidadorTrasLogin(
                idUsuario = cuidador1,
                idRol = 3,
                preferenciasSesion = prefsSesion,
                preferenciasInstalacion = prefsInstalacion,
                preferenciasLegacy = prefsLegacy
            )
            assertFalse(prepLogin.fueInstalacionNueva)
            assertTrue(prepLogin.limpiezaLocalEjecutada)
            assertFalse(prepLogin.liberacionRemotaEjecutada)
            assertEquals("Iniciar sesión NO debe enviar PATCH Inactivo al servidor", 0, backend.patchesEjecutados)

            // Claves requeridas por CargaAccesoActivity, SplashActivity y FirebaseMessagingService intactas.
            assertTrue(prefsSesion.getBoolean("SESION_INICIADA", false))
            assertEquals(3, prefsSesion.getInt("ID_ROL", -1))
            assertEquals(cuidador1, prefsSesion.getInt("ID_USUARIO", -1))
            assertEquals("tok-reingreso-456", prefsSesion.getString("TOKEN", null))
            assertFalse(prefsSesion.getBoolean("PUSH_ACTIVADAS", true))

            // Al abrir la selección/Home en el turno vigente, recupera del servidor {1, 2} activos.
            val bloque = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(cuidador1, turnoRepo, ahora)
            )
            val recuperados = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter {
                    it.idUsuario == cuidador1 &&
                        AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloque, ahora)
                }
                .map { it.idPaciente }
                .toSet()
            assertEquals(setOf(1, 2), recuperados)
        }

    @Test
    fun escenarioE_cierreDeTurnoMultidia_conservaDiaIntermedio_invalidaCacheYLiberaEnUltimoDia() =
        runBlocking {
            val cuidador1 = 7
            val cuidador2 = 8
            val dia1EnCurso = AsignacionTurnoUsuario(10, cuidador1, 1, "2026-11-23", "En curso", "GRP-B1")
            val dia2Asignado = AsignacionTurnoUsuario(11, cuidador1, 1, "2026-11-24", "Asignado", "GRP-B1")
            val nuevoTurno = AsignacionTurnoUsuario(12, cuidador1, 1, "2026-11-25", "Asignado", "GRP-B2")

            var ahora = fechaCol("2026-11-23T18:55:00-05:00")
            val backend = FakeBackendRealPG(
                pacientesCatalogo = listOf(crearPaciente(1, "Ana"), crearPaciente(2, "Luis")),
                turnos = mutableListOf(turnoDiurno),
                asignacionesTurno = mutableListOf(dia1EnCurso, dia2Asignado, nuevoTurno),
                asignacionesPacientes = mutableListOf(
                    AsignacionPacienteCuidador(501, cuidador1, 1, "2026-11-23T07:00:00-05:00", "2026-11-24T19:00:00-05:00", "Activo", idGrupoAsignacion = "GRP-B1"),
                    AsignacionPacienteCuidador(502, cuidador1, 2, "2026-11-23T07:00:00-05:00", "2026-11-24T19:00:00-05:00", "Activo", idGrupoAsignacion = null),
                    AsignacionPacienteCuidador(503, cuidador1, 3, "2026-11-30T07:00:00-05:00", "2026-11-30T19:00:00-05:00", "Activo", idGrupoAsignacion = "GRP-FUT")
                ),
                desfaseLecturaHoras = 5
            )
            val api = backend.crearApiService { ahora }
            val repo = PacienteRepository(api)
            val turnoRepo = TurnoRepository(api)

            // Verificar que inicioBase programado del turno (fe69e21) se mantiene intacto en el bloque.
            val bloqueDia1 = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(cuidador1, turnoRepo, ahora)
            )
            assertEquals("2026-11-23T07:00:00-05:00", bloqueDia1.fechaInicioIso)
            assertEquals("2026-11-24T19:00:00-05:00", bloqueDia1.fechaFinIso)

            // Cierre del día 1 (intermedio): actualiza en backend, invalida caché de TurnoRepository y NO libera.
            backend.asignacionesTurno[0] = dia1EnCurso.copy(estado = "Finalizado")
            turnoRepo.invalidarCacheAsignaciones()
            assertFalse(
                AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                    asignacionCerrada = backend.asignacionesTurno[0],
                    turnoRepository = turnoRepo,
                    forceRefresh = true
                )
            )
            assertEquals(0, backend.patchesEjecutados)

            // En el día 2 del mismo bloque GRP-B1, los pacientes {1, 2} siguen activos y vigentes.
            ahora = fechaCol("2026-11-24T08:00:00-05:00")
            backend.asignacionesTurno[1] = dia2Asignado.copy(estado = "En curso")
            turnoRepo.invalidarCacheAsignaciones()
            val bloqueDia2 = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(cuidador1, turnoRepo, ahora)
            )
            val vigentesDia2 = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter { it.idUsuario == cuidador1 && AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloqueDia2, ahora) }
                .map { it.idPaciente }
                .toSet()
            assertEquals(setOf(1, 2), vigentesDia2)

            // Cierre del día 2 (último día del bloque GRP-B1): SÍ libera asignaciones de GRP-B1.
            ahora = fechaCol("2026-11-24T18:55:00-05:00")
            val dia2Cerrado = dia2Asignado.copy(estado = "Finalizado")
            backend.asignacionesTurno[1] = dia2Cerrado
            turnoRepo.invalidarCacheAsignaciones()
            assertTrue(
                AsignacionVigenciaHelper.esUltimoDiaBloqueTurno(
                    asignacionCerrada = dia2Cerrado,
                    turnoRepository = turnoRepo,
                    forceRefresh = true
                )
            )

            val resCierre = CicloSeleccionCuidadorHelper.liberarBloqueAlCerrarUltimoDiaTurno(
                asignacionCerrada = dia2Cerrado,
                turnoCerrado = turnoDiurno,
                todasLasAsignacionesTurno = turnoRepo.obtenerAsignaciones(forceRefresh = false),
                pacienteRepository = repo,
                maxReintentos = 3,
                esperaEntreReintentosMs = 0L
            )
            assertTrue(resCierre.exitoso)
            assertEquals(2, resCierre.totalDesactivadas)
            assertEquals("Inactivo", backend.asignacionesPacientes.first { it.idAsignacion == 501 }.estado)
            assertEquals("Inactivo", backend.asignacionesPacientes.first { it.idAsignacion == 502 }.estado)
            assertEquals("Activo", backend.asignacionesPacientes.first { it.idAsignacion == 503 }.estado)

            // Al día siguiente (nuevo bloque GRP-B2), entra con selección completamente vacía.
            ahora = fechaCol("2026-11-25T07:10:00-05:00")
            val bloqueNuevo = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(cuidador1, turnoRepo, ahora)
            )
            assertEquals("GRP-B2", bloqueNuevo.asignacionTurno.id_grupo_asignacion)
            val recuperadosNuevoTurno = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter { it.idUsuario == cuidador1 && AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloqueNuevo, ahora) }
            assertTrue(recuperadosNuevoTurno.isEmpty())

            // Otro cuidador dispone ahora de los pacientes 1 y 2 liberados.
            backend.usuarioAutenticadoActual = cuidador2
            assertEquals(
                setOf(1, 2),
                repo.obtenerPacientesDisponiblesParaSeleccion().mapNotNull { it.idPaciente }.toSet()
            )
        }

    @Test
    fun escenarioF_reinstalacionONuevoDispositivo_limpiaResiduosLocales_sinDesactivarServidorYRecuperaSeleccionVigente() =
        runBlocking {
            val cuidador1 = 7
            val cuidador2 = 8
            val ahora = fechaCol("2026-11-23T10:00:00-05:00")

            val prefsSesionNueva = InMemorySharedPreferences()
            val prefsInstalacionNueva = InMemorySharedPreferences()
            val prefsLegacyResidual = InMemorySharedPreferences()
            prefsLegacyResidual.edit().putBoolean("SESION_INICIADA", true).commit()
            assertTrue(CicloSeleccionCuidadorHelper.esInstalacionNueva(prefsInstalacionNueva))

            // LoginActivity guarda TOKEN y sesión antes de preparar la sesión del cuidador.
            prefsSesionNueva.edit()
                .putBoolean("SESION_INICIADA", true)
                .putInt("ID_ROL", 3)
                .putInt("ID_USUARIO", cuidador1)
                .putString("TOKEN", "tok-reinstalacion")
                .commit()

            val backend = FakeBackendRealPG(
                pacientesCatalogo = listOf(
                    crearPaciente(1, "Ana"),
                    crearPaciente(2, "Luis"),
                    crearPaciente(3, "Marta")
                ),
                turnos = mutableListOf(turnoDiurno),
                asignacionesTurno = mutableListOf(
                    AsignacionTurnoUsuario(10, cuidador1, 1, "2026-11-23", "En curso", "GRP-C1")
                ),
                asignacionesPacientes = mutableListOf(
                    AsignacionPacienteCuidador(601, cuidador1, 1, "2026-11-23T07:00:00-05:00", "2026-11-23T19:00:00-05:00", "Activo", idGrupoAsignacion = "GRP-C1"),
                    AsignacionPacienteCuidador(602, cuidador1, 2, "2026-11-23T07:00:00-05:00", "2026-11-23T19:00:00-05:00", "Activo", idGrupoAsignacion = null)
                ),
                desfaseLecturaHoras = 5,
                tokenProvider = { prefsSesionNueva.getString("TOKEN", null) }
            )
            val api = backend.crearApiService { ahora }
            val repo = PacienteRepository(api)
            val turnoRepo = TurnoRepository(api)

            val resultado = CicloSeleccionCuidadorHelper.prepararSesionCuidadorTrasLogin(
                idUsuario = cuidador1,
                idRol = 3,
                preferenciasSesion = prefsSesionNueva,
                preferenciasInstalacion = prefsInstalacionNueva,
                preferenciasLegacy = prefsLegacyResidual
            )

            assertTrue(resultado.fueInstalacionNueva)
            assertTrue(resultado.limpiezaLocalEjecutada)
            assertFalse(resultado.liberacionRemotaEjecutada)
            assertEquals("Reinstalar e iniciar sesión NO debe enviar PATCH Inactivo", 0, backend.patchesEjecutados)
            assertNotNull(prefsInstalacionNueva.getString(CicloSeleccionCuidadorHelper.KEY_ID_INSTALACION, null))
            assertFalse(CicloSeleccionCuidadorHelper.esInstalacionNueva(prefsInstalacionNueva))
            assertFalse(prefsLegacyResidual.getBoolean("SESION_INICIADA", false))

            // En el turno vigente, el cuidador recupera sus pacientes activos {1, 2} desde el servidor.
            val bloque = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(cuidador1, turnoRepo, ahora)
            )
            val recuperados = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter { it.idUsuario == cuidador1 && AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloque, ahora) }
                .map { it.idPaciente }
                .toSet()
            assertEquals(setOf(1, 2), recuperados)

            // La exclusividad frente a otro cuidador se mantiene intacta para {1, 2}.
            backend.usuarioAutenticadoActual = cuidador2
            assertEquals(
                setOf(3),
                repo.obtenerPacientesDisponiblesParaSeleccion().mapNotNull { it.idPaciente }.toSet()
            )
        }

    @Test
    fun regresionAlarmasMedicamentos_noCancelaEnLogin_cancelaLocalEnLogout_yReconstruyeAlReingresar() =
        runBlocking {
            val cuidador1 = 7
            val ahora = fechaCol("2026-11-23T10:30:00-05:00")
            val prefsSesion = InMemorySharedPreferences()
            val prefsInstalacion = InMemorySharedPreferences()

            val backend = FakeBackendRealPG(
                pacientesCatalogo = listOf(
                    crearPaciente(1, "Ana"),
                    crearPaciente(2, "Luis"),
                    crearPaciente(3, "Marta")
                ),
                turnos = mutableListOf(turnoDiurno),
                asignacionesTurno = mutableListOf(
                    AsignacionTurnoUsuario(10, cuidador1, 1, "2026-11-23", "En curso", "GRP-ALM")
                ),
                asignacionesPacientes = mutableListOf(
                    AsignacionPacienteCuidador(701, cuidador1, 1, "2026-11-23T07:00:00-05:00", "2026-11-23T19:00:00-05:00", "Activo", idGrupoAsignacion = "GRP-ALM"),
                    AsignacionPacienteCuidador(702, cuidador1, 2, "2026-11-23T07:00:00-05:00", "2026-11-23T19:00:00-05:00", "Activo", idGrupoAsignacion = null)
                ),
                desfaseLecturaHoras = 5
            )
            val api = backend.crearApiService { ahora }
            val repo = PacienteRepository(api)
            val turnoRepo = TurnoRepository(api)

            val formulacionesServidor = listOf(
                FormulacionMedicamento(idFormulacion = 101, idPaciente = 1, idMedicamentos = 10, dosis = "500 mg", via = "Oral", horaAdministrada = "08:00:00", actualAdministrado = true, suspendidoFecha = null),
                FormulacionMedicamento(idFormulacion = 102, idPaciente = 2, idMedicamentos = 11, dosis = "10 ml", via = "Oral", horaAdministrada = "2026-11-23T14:30:00Z", actualAdministrado = true, suspendidoFecha = ""),
                // Suspendida -> no debe programarse.
                FormulacionMedicamento(idFormulacion = 103, idPaciente = 1, idMedicamentos = 12, dosis = "1 tab", via = "Oral", horaAdministrada = "12:00:00", actualAdministrado = true, suspendidoFecha = "2026-11-20"),
                // Paciente 3 no asignado al cuidador -> no debe programarse.
                FormulacionMedicamento(idFormulacion = 104, idPaciente = 3, idMedicamentos = 10, dosis = "500 mg", via = "Oral", horaAdministrada = "09:00:00", actualAdministrado = true, suspendidoFecha = null)
            )

            // Criterio idéntico a HomeFragment.observarYProgramarAlarmas + ProgramadorAlarmasMedicamentos.
            fun construirAlarmasParaSeleccion(idsAsignados: Set<Int>): List<DatosAlarmaMedicamento> {
                if (idsAsignados.isEmpty()) return emptyList()
                return formulacionesServidor
                    .filter { f ->
                        f.actualAdministrado == true &&
                            f.suspendidoFecha.isNullOrBlank() &&
                            f.idPaciente != null &&
                            f.idPaciente in idsAsignados
                    }
                    .mapNotNull { f ->
                        val partes = f.horaAdministrada?.trim()?.substringAfterLast("T")?.substringBefore("Z")?.split(":")
                            ?: return@mapNotNull null
                        val h = partes.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                        val m = partes.getOrNull(1)?.take(2)?.toIntOrNull() ?: 0
                        DatosAlarmaMedicamento(
                            id = f.idFormulacion ?: 0,
                            hora = h,
                            minuto = m,
                            paciente = "Paciente ${f.idPaciente}",
                            medicamento = "Med ${f.idMedicamentos}",
                            dosis = f.dosis.orEmpty(),
                            via = f.via.orEmpty(),
                            idFormulacion = f.idFormulacion,
                            idPaciente = f.idPaciente
                        )
                    }
            }

            val alarmasDispositivo = mutableListOf<DatosAlarmaMedicamento>()
            val bloque = requireNotNull(
                AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(cuidador1, turnoRepo, ahora)
            )

            // 1. Selección inicial activa: se programan las 2 alarmas de los pacientes {1, 2}.
            val idsIniciales = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter { it.idUsuario == cuidador1 && AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloque, ahora) }
                .map { it.idPaciente }
                .toSet()
            alarmasDispositivo.addAll(construirAlarmasParaSeleccion(idsIniciales))
            assertEquals(listOf(101, 102), alarmasDispositivo.map { it.id })

            // 2. Cerrar sesión: cancela las alarmas locales de este teléfono sin tocar el servidor.
            CicloSeleccionCuidadorHelper.cerrarSesionCuidador(
                idUsuario = cuidador1,
                preferenciasSesion = prefsSesion,
                preferenciasInstalacion = prefsInstalacion,
                onCancelarAlarmas = { alarmasDispositivo.clear() }
            )
            assertTrue(alarmasDispositivo.isEmpty())
            assertEquals(0, backend.patchesEjecutados)

            // 3. Nuevo inicio de sesión: prepararSesionCuidadorTrasLogin NO cancela alarmas existentes
            //    y al entrar a HomeFragment reconstruye exactamente [101, 102] desde el servidor.
            val alarmaPreexistente = DatosAlarmaMedicamento(999, 18, 0, "Test", "Med", "1", "Oral", 999, 1)
            alarmasDispositivo.add(alarmaPreexistente)

            prefsSesion.edit()
                .putBoolean("SESION_INICIADA", true)
                .putInt("ID_ROL", 3)
                .putInt("ID_USUARIO", cuidador1)
                .putString("TOKEN", "tok-alarmas")
                .commit()
            CicloSeleccionCuidadorHelper.prepararSesionCuidadorTrasLogin(
                idUsuario = cuidador1,
                idRol = 3,
                preferenciasSesion = prefsSesion,
                preferenciasInstalacion = prefsInstalacion
            )
            assertEquals("El login NO debe borrar alarmas", listOf(999), alarmasDispositivo.map { it.id })

            // Al cargar HomeFragment con los pacientes vigentes recuperados, se reconstruyen [101, 102].
            val idsRecuperados = repo.obtenerAsignacionesPacienteCuidadorResult().getOrThrow()
                .filter { it.idUsuario == cuidador1 && AsignacionVigenciaHelper.perteneceABloqueVigente(it, bloque, ahora) }
                .map { it.idPaciente }
                .toSet()
            alarmasDispositivo.clear()
            alarmasDispositivo.addAll(construirAlarmasParaSeleccion(idsRecuperados))
            assertEquals(listOf(101, 102), alarmasDispositivo.map { it.id })
            assertEquals(8 to 0, alarmasDispositivo[0].hora to alarmasDispositivo[0].minuto)
            assertEquals(14 to 30, alarmasDispositivo[1].hora to alarmasDispositivo[1].minuto)
        }

    @Test
    fun regresionLlegadaYAcceso_reingresoEnTurnoEnCursoEsIdempotente200_yConservaContratoSesionFcm() {
        val cuidador1 = 7
        val ahora = fechaCol("2026-11-23T11:00:00-05:00")
        val turnoEnCurso = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 44,
            id_usuario = cuidador1,
            id_turno = 1,
            fecha = "2026-11-23",
            estado = "En curso",
            id_grupo_asignacion = "GRP-HOY"
        )
        val turnoManana = AsignacionTurnoUsuario(
            id_asignacion_turno_usuario = 45,
            id_usuario = cuidador1,
            id_turno = 1,
            fecha = "2026-11-24",
            estado = "Asignado",
            id_grupo_asignacion = "GRP-HOY"
        )

        val prefsSesion = InMemorySharedPreferences()
        val prefsInstalacion = InMemorySharedPreferences()
        prefsSesion.edit()
            .putBoolean("SESION_INICIADA", true)
            .putInt("ID_ROL", 3)
            .putInt("ID_USUARIO", cuidador1)
            .putString("TOKEN", "tok-fcm-acceso")
            .putBoolean("PUSH_ACTIVADAS", true)
            .putBoolean("FLUJO_CUIDADOR_COMPLETADO", true)
            .commit()

        // Tras re-login, FLUJO_CUIDADOR_COMPLETADO se reinicia localmente, pero SESION_INICIADA,
        // ID_ROL, ID_USUARIO, TOKEN y PUSH_ACTIVADAS siguen válidos para CargaAccesoActivity y FCM.
        CicloSeleccionCuidadorHelper.prepararSesionCuidadorTrasLogin(
            idUsuario = cuidador1,
            idRol = 3,
            preferenciasSesion = prefsSesion,
            preferenciasInstalacion = prefsInstalacion
        )
        assertTrue(prefsSesion.getBoolean("SESION_INICIADA", false))
        assertEquals(3, prefsSesion.getInt("ID_ROL", -1))
        assertEquals(cuidador1, prefsSesion.getInt("ID_USUARIO", -1))
        assertEquals("tok-fcm-acceso", prefsSesion.getString("TOKEN", null))
        assertTrue(prefsSesion.getBoolean("PUSH_ACTIVADAS", false))
        assertFalse(prefsSesion.getBoolean("FLUJO_CUIDADOR_COMPLETADO", false))

        // Al pasar por VerificacionLlegadaFragment en el mismo turno "En curso", LlegadaTurnoHelper
        // resuelve la jornada actual e interpreta 200 OK idempotente sin bloquear al cuidador.
        val activa = LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
            idUsuario = cuidador1,
            asignaciones = listOf(turnoEnCurso, turnoManana),
            turnos = listOf(turnoDiurno),
            ahora = ahora
        )
        assertNotNull(activa)
        assertEquals(44, activa?.asignacion?.id_asignacion_turno_usuario)

        val respuestaReingreso = Response.success(
            200,
            RegistrarLlegadaResponse(
                mensaje = "La llegada a este turno ya estaba registrada.",
                asignacion = turnoEnCurso
            )
        )
        val interpretacion = LlegadaTurnoHelper.interpretarRespuestaRegistroLlegada(respuestaReingreso)
        assertTrue(interpretacion is LlegadaTurnoHelper.ResultadoRegistroLlegada.Exito)
        val exito = interpretacion as LlegadaTurnoHelper.ResultadoRegistroLlegada.Exito
        assertTrue(exito.yaEstabaRegistrada)
        assertEquals(44, exito.asignacion?.id_asignacion_turno_usuario)
    }
}
