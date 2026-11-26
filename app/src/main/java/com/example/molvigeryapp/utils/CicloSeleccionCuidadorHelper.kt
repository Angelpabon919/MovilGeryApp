package com.example.molvigeryapp.utils

import android.content.SharedPreferences
import android.util.Log
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * Coordina el ciclo de vida de la selección de pacientes del cuidador desde Android:
 * - Cierre de sesión exclusivamente local en el teléfono (sin enviar PATCH Inactivo al servidor).
 * - Preparación local de sesión al autenticar o reinstalar (sin cancelar alarmas ni desactivar
 *   asignaciones vigentes en el servidor, permitiendo recuperarlas en el turno activo).
 * - Liberación en servidor de las asignaciones del bloque únicamente cuando se confirma el cierre
 *   del último día del bloque de turno.
 */
object CicloSeleccionCuidadorHelper {

    private const val TAG = "CICLO_SELECCION"

    const val PREF_SESION = "SESION"
    const val PREF_INSTALACION = "INSTALACION_APP"
    const val PREF_LEGACY_SESSION = "GerIAppPrefs"

    const val KEY_ID_INSTALACION = "ID_INSTALACION"
    const val KEY_FLUJO_CUIDADOR_COMPLETADO = "FLUJO_CUIDADOR_COMPLETADO"

    private const val ROL_CUIDADOR = 3
    private const val ROL_CUIDADOR_LEGACY = 5

    data class ResultadoLiberacionBloque(
        val exitoso: Boolean,
        val totalEncontradas: Int = 0,
        val totalDesactivadas: Int = 0,
        val intentosRealizados: Int = 0,
        val errorMensaje: String? = null
    )

    data class ResultadoPreparacionLogin(
        val fueInstalacionNueva: Boolean,
        val limpiezaLocalEjecutada: Boolean,
        val liberacionRemotaEjecutada: Boolean = false
    )

    fun claveSeleccionUsuario(idUsuario: Int): String =
        "seleccion_pacientes_usuario_$idUsuario"

    fun claveIdsSeleccionadosUsuario(idUsuario: Int): String =
        "pacientes_seleccionados_ids_usuario_$idUsuario"

    fun claveLiberacionPendienteUsuario(idUsuario: Int): String =
        "pendiente_liberacion_bloque_usuario_$idUsuario"

    fun esRolCuidador(idRol: Int): Boolean =
        idRol == ROL_CUIDADOR || idRol == ROL_CUIDADOR_LEGACY

    fun esInstalacionNueva(preferenciasInstalacion: SharedPreferences): Boolean =
        preferenciasInstalacion.getString(KEY_ID_INSTALACION, null).isNullOrBlank()

    fun registrarInstalacionSiFalta(preferenciasInstalacion: SharedPreferences): String {
        val existente = preferenciasInstalacion.getString(KEY_ID_INSTALACION, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (existente != null) return existente

        val nuevoId = UUID.randomUUID().toString()
        preferenciasInstalacion.edit()
            .putString(KEY_ID_INSTALACION, nuevoId)
            .commit()
        return nuevoId
    }

    private fun logDebug(mensaje: String) {
        try {
            Log.d(TAG, mensaje)
        } catch (_: Throwable) {
            // Entorno JVM de pruebas unitarias sin android.util.Log
        }
    }

    private fun logWarn(mensaje: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.w(TAG, mensaje, throwable)
            } else {
                Log.w(TAG, mensaje)
            }
        } catch (_: Throwable) {
            // Entorno JVM de pruebas unitarias sin android.util.Log
        }
    }

    /**
     * Desactiva únicamente las asignaciones activas del cuidador pertenecientes al rango/grupo
     * indicado, con reintentos controlados para tolerar errores transitorios de conexión.
     */
    suspend fun desactivarAsignacionesActivasEnRango(
        idUsuario: Int,
        fechaInicioIso: String,
        fechaFinIso: String,
        pacienteRepository: PacienteRepository,
        idGrupoAsignacion: String? = null,
        fechaInicioBaseIso: String? = null,
        maxReintentos: Int = 3,
        esperaEntreReintentosMs: Long = 400L
    ): ResultadoLiberacionBloque {
        if (idUsuario <= 0) {
            return ResultadoLiberacionBloque(
                exitoso = true,
                intentosRealizados = 0
            )
        }

        val intentosMax = maxReintentos.coerceAtLeast(1)
        var ultimoResultado = ResultadoLiberacionBloque(
            exitoso = false,
            errorMensaje = "No se pudo liberar la selección de pacientes."
        )

        for (intento in 1..intentosMax) {
            try {
                val res = pacienteRepository.desactivarAsignacionesActivasDeBloque(
                    idUsuario = idUsuario,
                    fechaInicioIso = fechaInicioIso,
                    fechaFinIso = fechaFinIso,
                    idGrupoAsignacion = idGrupoAsignacion,
                    fechaInicioBaseIso = fechaInicioBaseIso
                )

                ultimoResultado = ResultadoLiberacionBloque(
                    exitoso = res.exitoso,
                    totalEncontradas = res.totalEncontradas,
                    totalDesactivadas = res.totalDesactivadas,
                    intentosRealizados = intento,
                    errorMensaje = res.errorMensaje
                )

                if (res.exitoso) {
                    logDebug(
                        "Liberación de bloque exitosa | usuario=$idUsuario | " +
                            "encontradas=${res.totalEncontradas} | desactivadas=${res.totalDesactivadas} | " +
                            "intento=$intento/$intentosMax"
                    )
                    return ultimoResultado
                }

                logWarn(
                    "Liberación incompleta en intento $intento/$intentosMax | usuario=$idUsuario | " +
                        "error=${res.errorMensaje}"
                )
            } catch (e: Exception) {
                logWarn(
                    "Error de conexión liberando bloque en intento $intento/$intentosMax | usuario=$idUsuario",
                    e
                )
                ultimoResultado = ResultadoLiberacionBloque(
                    exitoso = false,
                    intentosRealizados = intento,
                    errorMensaje = e.message ?: "Error de conexión al liberar asignaciones."
                )
            }

            if (intento < intentosMax && esperaEntreReintentosMs > 0L) {
                delay(esperaEntreReintentosMs * intento)
            }
        }

        return ultimoResultado
    }

    /**
     * Flujo de cierre de sesión del cuidador exclusivamente local en este teléfono:
     * - Cancela las alarmas locales de medicamentos del dispositivo (se reconstruirán al reingresar).
     * - Limpia el estado en memoria del ViewModel.
     * - Limpia `SESION`, `GerIAppPrefs`, `FLUJO_CUIDADOR_COMPLETADO` y las claves locales de selección.
     * - NO envía `PATCH estado="Inactivo"` al servidor para conservar las asignaciones activas del
     *   turno vigente y no afectar otros dispositivos del mismo cuidador.
     */
    fun cerrarSesionCuidador(
        idUsuario: Int,
        preferenciasSesion: SharedPreferences,
        preferenciasInstalacion: SharedPreferences? = null,
        preferenciasLegacy: SharedPreferences? = null,
        onCancelarAlarmas: () -> Unit = {},
        onLimpiarViewModel: () -> Unit = {}
    ) {
        onCancelarAlarmas()
        onLimpiarViewModel()

        preferenciasLegacy?.edit()?.clear()?.commit()

        val editorSesion = preferenciasSesion.edit().clear()
        if (idUsuario > 0) {
            editorSesion
                .putBoolean(claveSeleccionUsuario(idUsuario), false)
                .remove(claveIdsSeleccionadosUsuario(idUsuario))
        }
        editorSesion.remove(KEY_FLUJO_CUIDADOR_COMPLETADO)
        editorSesion.commit()

        if (idUsuario > 0 && preferenciasInstalacion != null) {
            preferenciasInstalacion.edit()
                .remove(claveLiberacionPendienteUsuario(idUsuario))
                .commit()
        }

        logDebug(
            "Cierre de sesión local completado en el dispositivo | usuario=$idUsuario | sin PATCH al servidor"
        )
    }

    /**
     * Se ejecuta inmediatamente después de guardar el TOKEN de un nuevo login:
     * - Registra el identificador local de instalación si es instalación nueva / reinstalación.
     * - Si el usuario es cuidador, limpia únicamente residuos locales de flujo y caché de selección
     *   (`FLUJO_CUIDADOR_COMPLETADO`, `GerIAppPrefs` y claves locales de selección), conservando
     *   intactos `SESION_INICIADA`, `ID_ROL`, `ID_USUARIO`, `TOKEN` y `PUSH_ACTIVADAS`.
     * - NO cancela alarmas de medicamentos durante el login ni envía `PATCH Inactivo` al servidor,
     *   de modo que al reingresar en un turno vigente se recuperen desde el backend los pacientes activos.
     */
    fun prepararSesionCuidadorTrasLogin(
        idUsuario: Int,
        idRol: Int,
        preferenciasSesion: SharedPreferences,
        preferenciasInstalacion: SharedPreferences,
        preferenciasLegacy: SharedPreferences? = null
    ): ResultadoPreparacionLogin {
        val instalacionNueva = esInstalacionNueva(preferenciasInstalacion)
        registrarInstalacionSiFalta(preferenciasInstalacion)

        if (idUsuario > 0) {
            preferenciasInstalacion.edit()
                .remove(claveLiberacionPendienteUsuario(idUsuario))
                .commit()
        }

        if (!esRolCuidador(idRol) || idUsuario <= 0) {
            return ResultadoPreparacionLogin(
                fueInstalacionNueva = instalacionNueva,
                limpiezaLocalEjecutada = false,
                liberacionRemotaEjecutada = false
            )
        }

        preferenciasLegacy?.edit()?.clear()?.commit()
        preferenciasSesion.edit()
            .remove(KEY_FLUJO_CUIDADOR_COMPLETADO)
            .putBoolean(claveSeleccionUsuario(idUsuario), false)
            .remove(claveIdsSeleccionadosUsuario(idUsuario))
            .commit()

        logDebug(
            "Sesión de cuidador preparada localmente tras login | usuario=$idUsuario | " +
                "instalacionNueva=$instalacionNueva | sin PATCH al servidor ni cancelación de alarmas"
        )

        return ResultadoPreparacionLogin(
            fueInstalacionNueva = instalacionNueva,
            limpiezaLocalEjecutada = true,
            liberacionRemotaEjecutada = false
        )
    }

    /**
     * Respalda desde Android la desactivación en backend de las asignaciones activas del bloque
     * que acaba de finalizar cuando se confirmó el último día del bloque.
     * No altera el comportamiento de días intermedios (solo debe invocarse si esUltimoDia == true).
     */
    suspend fun liberarBloqueAlCerrarUltimoDiaTurno(
        asignacionCerrada: AsignacionTurnoUsuario,
        turnoCerrado: Turno?,
        todasLasAsignacionesTurno: List<AsignacionTurnoUsuario>,
        pacienteRepository: PacienteRepository,
        maxReintentos: Int = 3,
        esperaEntreReintentosMs: Long = 400L
    ): ResultadoLiberacionBloque {
        val idUsuario = asignacionCerrada.id_usuario
        if (idUsuario <= 0 || turnoCerrado == null) {
            return ResultadoLiberacionBloque(
                exitoso = false,
                errorMensaje = "Datos insuficientes del turno cerrado para calcular el bloque."
            )
        }

        val fechaCerrada = asignacionCerrada.fecha?.trim()?.take(10).orEmpty()
        if (fechaCerrada.isBlank()) {
            return ResultadoLiberacionBloque(
                exitoso = false,
                errorMensaje = "Fecha inválida en la asignación cerrada."
            )
        }

        val idGrupo = asignacionCerrada.id_grupo_asignacion?.trim()?.takeIf { it.isNotBlank() }
        val fechasBloque = if (idGrupo != null) {
            todasLasAsignacionesTurno
                .filter { otra ->
                    otra.id_usuario == idUsuario &&
                        otra.id_grupo_asignacion?.trim() == idGrupo &&
                        !AsignacionVigenciaHelper.esEstadoTurnoExcluidoDeBloque(otra.estado)
                }
                .mapNotNull { otra ->
                    otra.fecha?.trim()?.take(10)?.takeIf { it.length == 10 }
                }
        } else {
            listOf(fechaCerrada)
        }

        val fechaMin = fechasBloque.minOrNull() ?: fechaCerrada
        val fechaMax = fechasBloque.maxOrNull() ?: fechaCerrada

        val rangoInicioDia = AsignacionVigenciaHelper.calcularRangoTurnoDia(
            asignacionTurno = asignacionCerrada.copy(fecha = fechaMin),
            turno = turnoCerrado
        ) ?: return ResultadoLiberacionBloque(
            exitoso = false,
            errorMensaje = "No se pudo calcular el inicio del bloque cerrado."
        )

        val rangoFinDia = AsignacionVigenciaHelper.calcularRangoTurnoDia(
            asignacionTurno = asignacionCerrada.copy(fecha = fechaMax),
            turno = turnoCerrado
        ) ?: return ResultadoLiberacionBloque(
            exitoso = false,
            errorMensaje = "No se pudo calcular el fin del bloque cerrado."
        )

        val fechaInicioIso = AsignacionVigenciaHelper.formatearIsoColombia(rangoInicioDia.first)
        val fechaFinIso = AsignacionVigenciaHelper.formatearIsoColombia(rangoFinDia.second)

        return desactivarAsignacionesActivasEnRango(
            idUsuario = idUsuario,
            fechaInicioIso = fechaInicioIso,
            fechaFinIso = fechaFinIso,
            pacienteRepository = pacienteRepository,
            idGrupoAsignacion = idGrupo,
            fechaInicioBaseIso = fechaInicioIso,
            maxReintentos = maxReintentos,
            esperaEntreReintentosMs = esperaEntreReintentosMs
        )
    }
}
