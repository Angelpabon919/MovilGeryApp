package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.CrearSeguimientoMedicamentoRequest
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.GrupoMedicacion
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.NotificacionDestinatarioRequest
import com.example.molvigeryapp.data.model.NotificacionRequest
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.TipoInsumo
import com.example.molvigeryapp.data.model.Inventario
import com.example.molvigeryapp.data.model.MotivoNoAdministracionRequest
import com.example.molvigeryapp.data.model.SeguimientoAdministracionMedicamento
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.data.repository.TurnoRepository
import com.example.molvigeryapp.utils.AsignacionVigenciaHelper
import kotlinx.coroutines.launch
import java.util.Date

class PacienteViewModel(
    private val repository: PacienteRepository,
    private val turnoRepository: TurnoRepository = TurnoRepository()
) : ViewModel() {

    // =========================================================
    // PACIENTES
    // =========================================================

    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    private val _pacientesDisponiblesSeleccion =
        MutableLiveData<List<Paciente>>()
    val pacientesDisponiblesSeleccion: LiveData<List<Paciente>>
        get() = _pacientesDisponiblesSeleccion

    private val _pacientesSeleccionadosHome =
        MutableLiveData<List<Paciente>>()

    val pacientesSeleccionadosHome: LiveData<List<Paciente>>
        get() = _pacientesSeleccionadosHome

    private var listaPacientesCompleta: List<Paciente> = emptyList()
    private var listaPacientesSeleccion: List<Paciente> = emptyList()
    private var idsSeleccionadosVigentes: Set<Int> = emptySet()
    private var solicitudCatalogoGeneralSeq: Long = 0L
    private var solicitudSeleccionSeq: Long = 0L

    private val _pacienteSeleccionado = MutableLiveData<Paciente?>()
    val pacienteSeleccionado: LiveData<Paciente?>
        get() = _pacienteSeleccionado

    var tienePacientesSeleccionados: Boolean = false
        private set

    // null = todavía no hay resultado
    // true = guardado correcto
    // false = error al guardar
    private val _seleccionGuardada = MutableLiveData<Boolean?>()
    val seleccionGuardada: LiveData<Boolean?> get() = _seleccionGuardada

    private val _mensajeErrorSeleccion = MutableLiveData<String?>()
    val mensajeErrorSeleccion: LiveData<String?> get() = _mensajeErrorSeleccion

    private var guardandoSeleccion = false

    // =========================================================
    // SELECCIÓN DE PACIENTES
    // =========================================================

    private fun claveSeleccion(idUsuario: Int) =
        "seleccion_pacientes_usuario_$idUsuario"

    private fun claveIdsSeleccionados(idUsuario: Int) =
        "pacientes_seleccionados_ids_usuario_$idUsuario"

    private fun aplicarEstadoSeleccionALista(
        lista: List<Paciente>,
        idsSeleccionados: Set<Int>
    ) {
        lista.forEach { paciente ->
            paciente.isSelected = paciente.idPaciente in idsSeleccionados
        }
    }

    private fun resolverPacientesSeleccionadosHome(): List<Paciente> {
        if (!tienePacientesSeleccionados || idsSeleccionadosVigentes.isEmpty()) {
            return emptyList()
        }

        val seleccionadosPorId = LinkedHashMap<Int, Paciente>()
        listaPacientesCompleta.forEach { paciente ->
            val id = paciente.idPaciente
            if (id != null && id in idsSeleccionadosVigentes) {
                seleccionadosPorId[id] = paciente
            }
        }
        listaPacientesSeleccion.forEach { paciente ->
            val id = paciente.idPaciente
            if (id != null && id in idsSeleccionadosVigentes && id !in seleccionadosPorId) {
                seleccionadosPorId[id] = paciente
            }
        }
        return seleccionadosPorId.values.toList()
    }

    /**
     * Reinicia el estado local al finalizar el turno.
     * No modifica las asignaciones del backend.
     */
    fun reiniciarSeleccionDespuesDelCierre(
        idUsuario: Int,
        preferencias: SharedPreferences
    ) {
        if (idUsuario <= 0) {
            Log.w("SELECCION_PACIENTES", "ID de usuario inválido")
            return
        }

        solicitudSeleccionSeq++
        idsSeleccionadosVigentes = emptySet()

        listaPacientesCompleta.forEach { paciente ->
            paciente.isSelected = false
        }
        listaPacientesSeleccion.forEach { paciente ->
            paciente.isSelected = false
        }

        tienePacientesSeleccionados = false
        _pacientes.value = listaPacientesCompleta.toList()
        _pacientesDisponiblesSeleccion.value = listaPacientesSeleccion.toList()
        _pacientesSeleccionadosHome.value = emptyList()
        _seleccionGuardada.value = null

        preferencias.edit()
            .putBoolean(claveSeleccion(idUsuario), false)
            .remove(claveIdsSeleccionados(idUsuario))
            .apply()

        Log.d(
            "SELECCION_PACIENTES",
            "Selección reiniciada para usuario $idUsuario"
        )
    }

    /**
     * Resuelve los IDs de pacientes seleccionados que siguen vigentes
     * específicamente dentro del bloque de turno activo del cuidador.
     * Si el bloque ya finalizó y no existe un bloque activo, devuelve vacío.
     */
    private suspend fun obtenerIdsVigentesBloqueActual(
        idUsuario: Int,
        ahora: Date = Date()
    ): Set<Int> {
        val rangoBloque = AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
            idUsuario = idUsuario,
            turnoRepository = turnoRepository,
            ahora = ahora,
            forceRefresh = true
        )

        if (rangoBloque == null) {
            Log.d(
                "SELECCION_PACIENTES",
                "Usuario=$idUsuario sin bloque de turno vigente; no se restauran pacientes seleccionados."
            )
            return emptySet()
        }

        val asignaciones = repository
            .obtenerAsignacionesPacienteCuidadorResult()
            .getOrThrow()

        val idsVigentes = asignaciones
            .filter { asignacion ->
                asignacion.idUsuario == idUsuario &&
                    AsignacionVigenciaHelper.perteneceABloqueVigente(
                        asignacion = asignacion,
                        bloque = rangoBloque,
                        ahora = ahora
                    )
            }
            .map { it.idPaciente }
            .toSet()

        Log.d(
            "SELECCION_PACIENTES",
            "Usuario=$idUsuario | grupo=${rangoBloque.asignacionTurno.id_grupo_asignacion.orEmpty()} | " +
                "bloque=[${rangoBloque.fechaInicioIso} .. ${rangoBloque.fechaFinIso}] | " +
                "idsVigentes=$idsVigentes"
        )

        return idsVigentes
    }

    fun cargarPacientes() {
        val solicitudId = ++solicitudCatalogoGeneralSeq
        viewModelScope.launch {
            try {
                val catalogoGeneral = if (listaPacientesCompleta.isEmpty()) {
                    repository.obtenerPacientes()
                } else {
                    listaPacientesCompleta
                }

                if (solicitudId != solicitudCatalogoGeneralSeq) {
                    return@launch
                }

                aplicarEstadoSeleccionALista(
                    catalogoGeneral,
                    idsSeleccionadosVigentes
                )
                listaPacientesCompleta = catalogoGeneral

                _pacientes.value = listaPacientesCompleta.toList()

                if (tienePacientesSeleccionados) {
                    _pacientesSeleccionadosHome.value =
                        resolverPacientesSeleccionadosHome()
                }
            } catch (e: Exception) {
                if (solicitudId != solicitudCatalogoGeneralSeq) {
                    return@launch
                }

                Log.e(
                    "API_ERROR_REAL",
                    "Error al cargar pacientes",
                    e
                )
                _pacientes.value = listaPacientesCompleta.toList()
            }
        }
    }

    /**
     * Carga los pacientes y recupera las asignaciones vigentes del cuidador
     * correspondientes únicamente al bloque de turno activo.
     */
    fun cargarPacientes(idUsuarioLogueado: Int) {
        val solicitudCatalogoId = ++solicitudCatalogoGeneralSeq
        val solicitudSeleccionId = if (idUsuarioLogueado > 0) {
            ++solicitudSeleccionSeq
        } else {
            null
        }

        viewModelScope.launch {
            try {
                val pacientesActuales = if (listaPacientesCompleta.isEmpty()) {
                    repository.obtenerPacientes()
                } else {
                    listaPacientesCompleta
                }

                if (solicitudCatalogoId == solicitudCatalogoGeneralSeq) {
                    aplicarEstadoSeleccionALista(
                        pacientesActuales,
                        idsSeleccionadosVigentes
                    )
                    listaPacientesCompleta = pacientesActuales
                }

                if (idUsuarioLogueado <= 0) {
                    Log.e(
                        "ASIGNACION_DEBUG",
                        "ID de usuario inválido: $idUsuarioLogueado"
                    )
                    if (solicitudCatalogoId == solicitudCatalogoGeneralSeq) {
                        _pacientes.value = listaPacientesCompleta.toList()
                    }
                    return@launch
                }

                val idsSeleccionados =
                    obtenerIdsVigentesBloqueActual(idUsuarioLogueado)

                if (solicitudSeleccionId == solicitudSeleccionSeq) {
                    idsSeleccionadosVigentes = idsSeleccionados
                    tienePacientesSeleccionados =
                        idsSeleccionados.isNotEmpty()

                    if (listaPacientesCompleta.isNotEmpty()) {
                        aplicarEstadoSeleccionALista(
                            listaPacientesCompleta,
                            idsSeleccionados
                        )
                        _pacientes.value = listaPacientesCompleta.toList()
                    }

                    if (listaPacientesSeleccion.isNotEmpty()) {
                        aplicarEstadoSeleccionALista(
                            listaPacientesSeleccion,
                            idsSeleccionados
                        )
                        _pacientesDisponiblesSeleccion.value =
                            listaPacientesSeleccion.toList()
                    }

                    _pacientesSeleccionadosHome.value =
                        resolverPacientesSeleccionadosHome()

                    Log.d(
                        "ASIGNACION_DEBUG",
                        "Usuario: $idUsuarioLogueado | IDs seleccionados del bloque vigente: $idsSeleccionados"
                    )
                } else if (solicitudCatalogoId == solicitudCatalogoGeneralSeq) {
                    aplicarEstadoSeleccionALista(
                        listaPacientesCompleta,
                        idsSeleccionadosVigentes
                    )
                    _pacientes.value = listaPacientesCompleta.toList()
                }
            } catch (e: Exception) {
                Log.e(
                    "ASIGNACION_DEBUG",
                    "Error al recuperar la selección",
                    e
                )
                if (solicitudCatalogoId == solicitudCatalogoGeneralSeq) {
                    _pacientes.value =
                        listaPacientesCompleta.toList()
                }
            }
        }
    }

    fun reiniciarResultadoSeleccion() {
        _seleccionGuardada.value = null
        _mensajeErrorSeleccion.value = null
    }

    private suspend fun sincronizarEstadoRealDesdeBackend(
        idUsuario: Int,
        preferencias: SharedPreferences,
        solicitudId: Long = ++solicitudSeleccionSeq
    ): Set<Int> {
        val pacientesDisponibles =
            repository.obtenerPacientesDisponiblesParaSeleccion()

        val claveEstado = claveSeleccion(idUsuario)
        val claveIds = claveIdsSeleccionados(idUsuario)

        // Consultar las asignaciones vigentes correspondientes al bloque activo.
        val idsBackend = obtenerIdsVigentesBloqueActual(idUsuario)

        if (solicitudId != solicitudSeleccionSeq) {
            Log.d(
                "SELECCION_PACIENTES",
                "Respuesta anterior descartada en sincronizarEstadoRealDesdeBackend " +
                    "(solicitud=$solicitudId, actual=$solicitudSeleccionSeq)"
            )
            return idsSeleccionadosVigentes
        }

        // Cuando el backend responde sin error, usar únicamente el estado real
        // del backend sin restaurar caché de SharedPreferences.
        aplicarEstadoSeleccionALista(pacientesDisponibles, idsBackend)

        listaPacientesSeleccion = pacientesDisponibles
        idsSeleccionadosVigentes = idsBackend
        tienePacientesSeleccionados = idsBackend.isNotEmpty()

        if (listaPacientesCompleta.isNotEmpty()) {
            aplicarEstadoSeleccionALista(listaPacientesCompleta, idsBackend)
            _pacientes.value = listaPacientesCompleta.toList()
        }

        _pacientesDisponiblesSeleccion.value = listaPacientesSeleccion.toList()
        _pacientesSeleccionadosHome.value = resolverPacientesSeleccionadosHome()

        if (idsBackend.isNotEmpty()) {
            preferencias.edit()
                .putBoolean(claveEstado, true)
                .putStringSet(
                    claveIds,
                    idsBackend.map { it.toString() }.toSet()
                )
                .apply()
        } else {
            preferencias.edit()
                .putBoolean(claveEstado, false)
                .remove(claveIds)
                .apply()
        }

        return idsBackend
    }

    /**
     * Guarda la selección y sincroniza las asignaciones.
     */
    fun confirmarSeleccionDelDia(
        idUsuarioLogueado: Int,
        preferences: SharedPreferences
    ) {
        if (guardandoSeleccion) return

        _seleccionGuardada.value = null
        _mensajeErrorSeleccion.value = null

        if (idUsuarioLogueado <= 0) {
            Log.e(
                "ASIGNACION_DEBUG",
                "ID de usuario inválido"
            )
            _mensajeErrorSeleccion.value =
                "No se pudo identificar al cuidador. Inicia sesión nuevamente."
            _seleccionGuardada.value = false
            return
        }

        val fuenteSeleccion = if (listaPacientesSeleccion.isNotEmpty()) {
            listaPacientesSeleccion
        } else {
            listaPacientesCompleta
        }

        val seleccionados =
            fuenteSeleccion.filter { it.isSelected }

        val idsSeleccionados = seleccionados
            .mapNotNull { it.idPaciente }
            .toSet()

        if (idsSeleccionados.isEmpty()) {
            Log.e(
                "ASIGNACION_DEBUG",
                "No hay pacientes seleccionados"
            )
            _mensajeErrorSeleccion.value =
                "Por favor, selecciona al menos un paciente."
            _seleccionGuardada.value = false
            return
        }

        guardandoSeleccion = true
        val solicitudGuardadoId = ++solicitudSeleccionSeq
        viewModelScope.launch {
            try {
                val rangoTurno =
                    AsignacionVigenciaHelper.resolverRangoBloqueTurnoActual(
                        idUsuario = idUsuarioLogueado,
                        turnoRepository = turnoRepository
                    )

                if (rangoTurno == null) {
                    Log.w(
                        "ASIGNACION_DEBUG",
                        "Cuidador $idUsuarioLogueado sin turno activo asignado"
                    )
                    _mensajeErrorSeleccion.value =
                        "No tienes un turno asignado activo para guardar la selección de pacientes."
                    _seleccionGuardada.value = false
                    return@launch
                }

                val resultado =
                    repository.sincronizarSeleccionDelCuidadorDetallado(
                        idUsuario = idUsuarioLogueado,
                        idsSeleccionados = idsSeleccionados,
                        fechaInicioIso = rangoTurno.fechaInicioIso,
                        fechaFinIso = rangoTurno.fechaFinIso,
                        idGrupoAsignacion = rangoTurno.asignacionTurno.id_grupo_asignacion
                            ?: ""
                    )

                if (resultado.exitoso) {
                    try {
                        sincronizarEstadoRealDesdeBackend(
                            idUsuario = idUsuarioLogueado,
                            preferencias = preferences,
                            solicitudId = solicitudGuardadoId
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "ASIGNACION_DEBUG",
                            "Guardado confirmado; no se pudo recargar lista inmediatamente",
                            e
                        )
                        if (solicitudGuardadoId == solicitudSeleccionSeq) {
                            val confirmados = resultado.idsConfirmados
                            idsSeleccionadosVigentes = confirmados
                            tienePacientesSeleccionados = confirmados.isNotEmpty()

                            if (listaPacientesSeleccion.isNotEmpty()) {
                                aplicarEstadoSeleccionALista(
                                    listaPacientesSeleccion,
                                    confirmados
                                )
                                _pacientesDisponiblesSeleccion.value =
                                    listaPacientesSeleccion.toList()
                            }
                            if (listaPacientesCompleta.isNotEmpty()) {
                                aplicarEstadoSeleccionALista(
                                    listaPacientesCompleta,
                                    confirmados
                                )
                                _pacientes.value = listaPacientesCompleta.toList()
                            }

                            preferences.edit()
                                .putBoolean(
                                    claveSeleccion(idUsuarioLogueado),
                                    confirmados.isNotEmpty()
                                )
                                .putStringSet(
                                    claveIdsSeleccionados(idUsuarioLogueado),
                                    confirmados.map { it.toString() }.toSet()
                                )
                                .apply()
                            _pacientesSeleccionadosHome.value =
                                resolverPacientesSeleccionadosHome()
                        }
                    }

                    Log.d(
                        "ASIGNACION_DEBUG",
                        "Selección sincronizada: $idsSeleccionados | " +
                            "grupo=${rangoTurno.asignacionTurno.id_grupo_asignacion.orEmpty()} | " +
                            "rango=(${rangoTurno.fechaInicioIso} - ${rangoTurno.fechaFinIso})"
                    )

                    _mensajeErrorSeleccion.value = null
                    _seleccionGuardada.value = true
                } else {
                    // Reflejar siempre el estado real del backend (incluyendo
                    // resultados parciales y eliminando selecciones no confirmadas).
                    try {
                        sincronizarEstadoRealDesdeBackend(
                            idUsuario = idUsuarioLogueado,
                            preferencias = preferences,
                            solicitudId = solicitudGuardadoId
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "ASIGNACION_DEBUG",
                            "No se pudo recargar la lista tras el fallo; aplicando estado confirmado local",
                            e
                        )
                        if (solicitudGuardadoId == solicitudSeleccionSeq) {
                            val confirmados = resultado.idsConfirmados
                            idsSeleccionadosVigentes = confirmados
                            tienePacientesSeleccionados =
                                confirmados.isNotEmpty()

                            if (listaPacientesSeleccion.isNotEmpty()) {
                                val listaActualizada = listaPacientesSeleccion
                                    .filterNot { it.idPaciente in resultado.idsEnConflicto }
                                aplicarEstadoSeleccionALista(
                                    listaActualizada,
                                    confirmados
                                )
                                listaPacientesSeleccion = listaActualizada
                                _pacientesDisponiblesSeleccion.value =
                                    listaPacientesSeleccion.toList()
                            }
                            if (listaPacientesCompleta.isNotEmpty()) {
                                aplicarEstadoSeleccionALista(
                                    listaPacientesCompleta,
                                    confirmados
                                )
                                _pacientes.value = listaPacientesCompleta.toList()
                            }
                            _pacientesSeleccionadosHome.value =
                                resolverPacientesSeleccionadosHome()
                        }
                    }

                    val mensajeFinal = if (resultado.huboConflictoExclusividad) {
                        val base = resultado.mensajeError
                            ?.takeIf { it.isNotBlank() }
                            ?: "Este paciente ya fue seleccionado por otro cuidador y no se encuentra disponible para este turno."
                        if (resultado.idsConfirmados.isNotEmpty()) {
                            "$base Los demás pacientes confirmados se conservaron y la lista fue actualizada."
                        } else {
                            base
                        }
                    } else {
                        resultado.mensajeError
                            ?.takeIf { it.isNotBlank() }
                            ?: "No se pudo guardar la selección. Inténtalo de nuevo."
                    }

                    Log.e(
                        "ASIGNACION_DEBUG",
                        "No se confirmó el guardado completo | conflicto=${resultado.huboConflictoExclusividad} | " +
                            "idsEnConflicto=${resultado.idsEnConflicto} | confirmados=${resultado.idsConfirmados}"
                    )
                    _mensajeErrorSeleccion.value = mensajeFinal
                    _seleccionGuardada.value = false
                }
            } catch (e: Exception) {
                Log.e(
                    "ASIGNACION_DEBUG",
                    "Error al sincronizar selección del cuidador",
                    e
                )
                try {
                    sincronizarEstadoRealDesdeBackend(
                        idUsuario = idUsuarioLogueado,
                        preferencias = preferences,
                        solicitudId = solicitudGuardadoId
                    )
                } catch (_: Exception) {
                    // Si tampoco hay conexión, no se marca como guardada.
                }
                _mensajeErrorSeleccion.value =
                    "No se pudo guardar la selección. Inténtalo de nuevo."
                _seleccionGuardada.value = false
            } finally {
                guardandoSeleccion = false
            }
        }
    }

    /**
     * Limpia la selección local; no elimina asignaciones
     * del servidor.
     */
    fun restaurarListaCompleta() {
        solicitudSeleccionSeq++
        idsSeleccionadosVigentes = emptySet()
        tienePacientesSeleccionados = false

        listaPacientesCompleta.forEach {
            it.isSelected = false
        }
        listaPacientesSeleccion.forEach {
            it.isSelected = false
        }

        _pacientes.value = listaPacientesCompleta.toList()
        _pacientesDisponiblesSeleccion.value = listaPacientesSeleccion.toList()
        _pacientesSeleccionadosHome.value = emptyList()
        _seleccionGuardada.value = null
    }

    /**
     * Recupera siempre los pacientes disponibles para selección y las
     * asignaciones activas del backend.
     *
     * No depende de la preferencia local para decidir si
     * consulta las asignaciones.
     */
    fun cargarPacientesParaSeleccion(
        idUsuario: Int,
        preferencias: SharedPreferences
    ) {
        if (idUsuario <= 0) {
            Log.e(
                "SELECCION_PACIENTES",
                "ID de usuario inválido: $idUsuario"
            )
            return
        }

        val solicitudId = ++solicitudSeleccionSeq
        viewModelScope.launch {
            try {
                val idsBackend = sincronizarEstadoRealDesdeBackend(
                    idUsuario = idUsuario,
                    preferencias = preferencias,
                    solicitudId = solicitudId
                )

                if (solicitudId == solicitudSeleccionSeq) {
                    Log.d(
                        "SELECCION_PACIENTES",
                        "Usuario=$idUsuario | IDs backend vigentes en bloque=$idsBackend"
                    )
                }

            } catch (e: Exception) {
                if (solicitudId != solicitudSeleccionSeq) {
                    return@launch
                }

                Log.e(
                    "SELECCION_PACIENTES",
                    "Error al recuperar la selección",
                    e
                )

                /*
                 * Si falla la consulta, intentar restaurar desde
                 * el respaldo local, siempre que no se haya
                 * reiniciado explícitamente la selección.
                 */
                val claveEstado = claveSeleccion(idUsuario)
                val claveIds = claveIdsSeleccionados(idUsuario)

                val seleccionActiva =
                    preferencias.getBoolean(claveEstado, false)

                if (seleccionActiva) {
                    val idsLocales = preferencias
                        .getStringSet(claveIds, emptySet())
                        .orEmpty()
                        .mapNotNull { it.toIntOrNull() }
                        .toSet()

                    idsSeleccionadosVigentes = idsLocales
                    tienePacientesSeleccionados = idsLocales.isNotEmpty()

                    if (listaPacientesSeleccion.isNotEmpty()) {
                        aplicarEstadoSeleccionALista(
                            listaPacientesSeleccion,
                            idsLocales
                        )
                        _pacientesDisponiblesSeleccion.value =
                            listaPacientesSeleccion.toList()
                    }

                    if (listaPacientesCompleta.isNotEmpty()) {
                        aplicarEstadoSeleccionALista(
                            listaPacientesCompleta,
                            idsLocales
                        )
                        _pacientes.value = listaPacientesCompleta.toList()
                    }

                    if (
                        listaPacientesSeleccion.isNotEmpty() ||
                        listaPacientesCompleta.isNotEmpty()
                    ) {
                        _pacientesSeleccionadosHome.value =
                            resolverPacientesSeleccionadosHome()
                    }
                }
            }
        }
    }

    /**
     * Fuerza una nueva consulta de pacientes y asignaciones.
     * No elimina asignaciones ni modifica el estado del turno.
     */
    fun actualizarSeleccionDespuesDelCierre(
        idUsuario: Int,
        preferencias: SharedPreferences
    ) {
        solicitudCatalogoGeneralSeq++
        solicitudSeleccionSeq++
        listaPacientesCompleta = emptyList()
        listaPacientesSeleccion = emptyList()
        idsSeleccionadosVigentes = emptySet()

        cargarPacientesParaSeleccion(
            idUsuario,
            preferencias
        )
    }

    // =========================================================
    // DETALLE DEL PACIENTE
    // =========================================================

    fun cargarPacientePorId(id: Int) {
        viewModelScope.launch {
            try {
                _pacienteSeleccionado.value =
                    repository.obtenerPacientePorId(id)
            } catch (e: Exception) {
                Log.e(
                    "API_ERROR_REAL",
                    "Error al obtener paciente $id",
                    e
                )
                _pacienteSeleccionado.value = null
            }
        }
    }

    fun seleccionarPaciente(paciente: Paciente) {
        _pacienteSeleccionado.value = paciente
    }

    // =========================================================
    // BITÁCORA Y CUIDADOS
    // =========================================================

    private val _idBitacora = MutableLiveData<Int?>()
    val idBitacora: LiveData<Int?> get() = _idBitacora

    fun guardarIdBitacora(id: Int) {
        _idBitacora.value = id
    }

    private val _cuidados =
        MutableLiveData<List<CuidadoEnfermeria>?>()

    val cuidados: LiveData<List<CuidadoEnfermeria>?> get() = _cuidados

    fun cargarCuidados(idPaciente: Int) {
        viewModelScope.launch {
            try {
                _cuidados.value =
                    repository.getCuidadosPorPaciente(idPaciente)
            } catch (e: Exception) {
                Log.e(
                    "CUIDADOS_ERROR",
                    "Error al cargar cuidados",
                    e
                )
                _cuidados.value = emptyList()
            }
        }
    }

    fun guardarCuidado(cuidado: CuidadoEnfermeria) {
        viewModelScope.launch {
            try {
                val exito =
                    repository.guardarCuidadoEnfermeria(cuidado)

                if (exito && cuidado.idPaciente != null) {
                    cargarCuidados(cuidado.idPaciente)
                }
            } catch (e: Exception) {
                Log.e(
                    "CUIDADOS_ERROR",
                    "Error al guardar cuidado",
                    e
                )
            }
        }
    }

    // =========================================================
    // RECOMENDACIONES
    // =========================================================

    private val _recomendaciones =
        MutableLiveData<List<Recomendacion>>()

    val recomendaciones: LiveData<List<Recomendacion>>
        get() = _recomendaciones

    fun cargarRecomendaciones(idPaciente: Int) {
        viewModelScope.launch {
            try {
                _recomendaciones.value = emptyList()

                val lista =
                    repository.getRecomendaciones(idPaciente)

                _recomendaciones.value = lista ?: emptyList()
            } catch (e: Exception) {
                Log.e(
                    "RECOMENDACIONES_ERROR",
                    "Error al cargar recomendaciones",
                    e
                )
                _recomendaciones.value = emptyList()
            }
        }
    }

    fun guardarRecomendacion(recomendacion: Recomendacion) {
        viewModelScope.launch {
            try {
                repository.guardarRecomendacion(recomendacion)
            } catch (e: Exception) {
                Log.e(
                    "RECOMENDACIONES_ERROR",
                    "Error al guardar recomendación",
                    e
                )
            }
        }
    }

    // =========================================================
    // ELEMENTOS DEL PACIENTE
    // =========================================================

    private val _elementos =
        MutableLiveData<List<ElementoPaciente>>(emptyList())

    val elementos: LiveData<List<ElementoPaciente>> get() = _elementos

    val elementosPaciente: LiveData<List<ElementoPaciente>>
        get() = _elementos
    // =========================================================
// INVENTARIO DEL PACIENTE
// =========================================================

    private val _inventariosPaciente =
        MutableLiveData<List<Inventario>>(emptyList())

    val inventariosPaciente: LiveData<List<Inventario>>
        get() = _inventariosPaciente

    fun cargarElementosPaciente(idPaciente: Int) {
        viewModelScope.launch {
            try {
                _elementos.value =
                    repository.getElementosPorPaciente(idPaciente)
            } catch (e: Exception) {
                Log.e(
                    "PACIENTE_VM",
                    "Error al cargar elementos",
                    e
                )
                _elementos.value = emptyList()
            }
        }
    }

    fun cargarElementosDelPaciente(idPaciente: Int) {
        cargarElementosPaciente(idPaciente)
    }

    fun cargarInventariosPaciente(idPaciente: Int) {
        viewModelScope.launch {
            try {

                val inventarios =
                    repository.obtenerInventario()

                val inventariosFiltrados =
                    inventarios.filter {
                        it.idPaciente == idPaciente
                    }

                _inventariosPaciente.value =
                    inventariosFiltrados

                Log.d(
                    "INVENTARIO_PACIENTE",
                    "Paciente=$idPaciente | " +
                            "Inventarios=${inventariosFiltrados.size}"
                )

                inventariosFiltrados.forEach {
                    Log.d(
                        "INVENTARIO_PACIENTE",
                        "ID=${it.idInventario} | " +
                                "MEDICAMENTO=${it.idMedicamentos} | " +
                                "ELEMENTO=${it.idElemento} | " +
                                "STOCK=${it.cantidadActual} | " +
                                "VENCIMIENTO=${it.fechaVencimiento}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "INVENTARIO_PACIENTE",
                    "Error al cargar inventarios",
                    e
                )

                _inventariosPaciente.value =
                    emptyList()
            }
        }
    }

    fun guardarElementoPaciente(elemento: ElementoPaciente) {
        viewModelScope.launch {
            try {
                val exito =
                    repository.guardarElementoPaciente(elemento)

                if (exito) {
                    kotlinx.coroutines.delay(300)
                    elemento.idPaciente?.let {
                        cargarElementosPaciente(it)
                    }
                } else {
                    Log.e(
                        "PACIENTE_VM",
                        "Error al guardar elemento"
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "PACIENTE_VM",
                    "Error al guardar elemento",
                    e
                )
            }
        }
    }

    // =========================================================
    // CATÁLOGO DE INSUMOS
    // =========================================================

    private val _tiposInsumosCatalogo =
        MutableLiveData<List<TipoInsumo>?>()

    val tiposInsumosCatalogo: LiveData<List<TipoInsumo>?>
        get() = _tiposInsumosCatalogo

    val tiposInsumos: LiveData<List<TipoInsumo>?>
        get() = _tiposInsumosCatalogo

    private val _insumosCatalogo =
        MutableLiveData<List<Insumo>?>()

    val insumosCatalogo: LiveData<List<Insumo>?>
        get() = _insumosCatalogo

    val insumosPorTipo: LiveData<List<Insumo>?>
        get() = _insumosCatalogo

    fun cargarTiposInsumos() {
        viewModelScope.launch {
            try {
                _tiposInsumosCatalogo.value =
                    repository.getTiposInsumos()
            } catch (e: Exception) {
                Log.e(
                    "INSUMOS_ERROR",
                    "Error al cargar tipos de insumos",
                    e
                )
                _tiposInsumosCatalogo.value = emptyList()
            }
        }
    }

    fun cargarInsumosPorTipo(idTipoInsumo: Int) {
        viewModelScope.launch {
            try {
                _insumosCatalogo.value =
                    repository.getInsumosPorTipo(idTipoInsumo)
            } catch (e: Exception) {
                Log.e(
                    "INSUMOS_ERROR",
                    "Error al cargar insumos",
                    e
                )
                _insumosCatalogo.value = emptyList()
            }
        }
    }

    // =========================================================
    // MEDICAMENTOS
    // =========================================================

    private val _formulacionesMedicamentos =
        MutableLiveData<List<FormulacionMedicamento>>()

    // =========================================================
// SEGUIMIENTOS DE ADMINISTRACIÓN DE MEDICAMENTOS
// =========================================================

    private val _seguimientosAdministracionMedicamentos =
        MutableLiveData<List<SeguimientoAdministracionMedicamento>>(
            emptyList()
        )

    val seguimientosAdministracionMedicamentos:
            LiveData<List<SeguimientoAdministracionMedicamento>>
        get() = _seguimientosAdministracionMedicamentos

    val formulacionesMedicamentos: LiveData<List<FormulacionMedicamento>>
        get() = _formulacionesMedicamentos

    val medicamentosManana =
        MutableLiveData<List<FormulacionMedicamento>>()

    val medicamentosTarde =
        MutableLiveData<List<FormulacionMedicamento>>()

    val medicamentosNoche =
        MutableLiveData<List<FormulacionMedicamento>>()


    // =========================================================
    // APLICACIONES DE MEDICAMENTOS REGISTRADAS
    // =========================================================

    private val _aplicacionesMedicamentos =
        MutableLiveData<List<AplicacionMedicamento>>(emptyList())

    val aplicacionesMedicamentos: LiveData<List<AplicacionMedicamento>>
        get() = _aplicacionesMedicamentos

    fun cargarAplicacionesMedicamento() {
        viewModelScope.launch {
            try {
                val resultado =
                    repository.obtenerAplicacionesMedicamento()

                resultado.onSuccess { aplicaciones ->

                    _aplicacionesMedicamentos.value = aplicaciones

                    Log.d(
                        "APLICACIONES_MEDICAMENTO",
                        "Total de aplicaciones recibidas: ${aplicaciones.size}"
                    )

                    aplicaciones.forEach { aplicacion ->
                        Log.d(
                            "APLICACIONES_MEDICAMENTO",
                            "ID=${aplicacion.idAplicacion} | " +
                                    "MEDICAMENTO=${aplicacion.idMedicamentoMedicamento} | " +
                                    "INVENTARIO=${aplicacion.idInventario} | " +
                                    "FECHA=${aplicacion.fechaHora} | " +
                                    "ESTADO=${aplicacion.estado}"
                        )
                    }
                }

                resultado.onFailure { error ->
                    Log.e(
                        "APLICACIONES_MEDICAMENTO",
                        "No se pudieron cargar las aplicaciones",
                        error
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "APLICACIONES_MEDICAMENTO",
                    "Error inesperado al consultar aplicaciones",
                    e
                )
            }
        }
    }

    fun cargarSeguimientosAdministracionMedicamentos(
        idPaciente: Int
    ) {
        if (idPaciente <= 0) return

        viewModelScope.launch {
            try {
                val resultado =
                    repository.obtenerSeguimientosMedicamento(idPaciente)

                resultado.onSuccess { seguimientos ->

                    _seguimientosAdministracionMedicamentos.value =
                        seguimientos

                    Log.d(
                        "SEGUIMIENTO_MEDICAMENTO",
                        "Paciente=$idPaciente | " +
                                "Total=${seguimientos.size}"
                    )

                    seguimientos.forEach { seguimiento ->
                        Log.d(
                            "SEGUIMIENTO_MEDICAMENTO",
                            "ID=${seguimiento.idSeguimiento} | " +
                                    "FORMULACION=${seguimiento.idFormulacion} | " +
                                    "FECHA=${seguimiento.fechaHoraProgramada} | " +
                                    "ESTADO=${seguimiento.estado}"
                        )
                    }
                }

                resultado.onFailure { error ->
                    Log.e(
                        "SEGUIMIENTO_MEDICAMENTO",
                        "Error al cargar seguimientos del paciente",
                        error
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "SEGUIMIENTO_MEDICAMENTO",
                    "Error inesperado al consultar seguimientos",
                    e
                )
            }
        }
    }


    fun obtenerMedicamentosPaciente(
        idPaciente: Int
    ): List<FormulacionMedicamento> {
        return (formulacionesMedicamentos.value ?: emptyList())
            .filter { it.idPaciente == idPaciente }
    }

    private val _gruposMedicacion =
        MutableLiveData<List<GrupoMedicacion>>()

    val gruposMedicacion: LiveData<List<GrupoMedicacion>>
        get() = _gruposMedicacion

    private val _medicamentosCatalogo =
        MutableLiveData<List<Medicamento>?>()

    val medicamentosCatalogo: LiveData<List<Medicamento>?>
        get() = _medicamentosCatalogo

    fun cargarCatalogoMedicamentos() {
        viewModelScope.launch {
            try {
                _medicamentosCatalogo.value =
                    repository.getMedicamentos()
            } catch (e: Exception) {
                Log.e(
                    "MEDICAMENTOS_ERROR",
                    "Error al cargar catálogo",
                    e
                )
                _medicamentosCatalogo.value = emptyList()
            }
        }
    }

    fun cargarFormulacionesMedicamentos() {
        viewModelScope.launch {
            try {
                val lista =
                    repository.getFormulacionesMedicamentos()
                        ?: emptyList()
                Log.d(
                    "FORMULACIONES_MEDICAMENTOS",
                    lista.joinToString(" | ") {
                        "ID=${it.idFormulacion}, " +
                                "GRUPO=${it.idGrupo}, " +
                                "HORA=${it.horaAdministrada}, " +
                                "FECHA=${it.fecha}, " +
                                "ADMINISTRADO=${it.actualAdministrado}"
                    }
                )


                val grupos =
                    _gruposMedicacion.value ?: emptyList()

                _formulacionesMedicamentos.value = lista
                separarMedicamentosPorHorario(lista, grupos)
            } catch (e: Exception) {
                Log.e(
                    "FORMULACION_ERROR",
                    "Error al cargar formulaciones",
                    e
                )
                _formulacionesMedicamentos.value = emptyList()
                separarMedicamentosPorHorario(
                    emptyList(),
                    emptyList()
                )
            }
        }
    }

    fun cargarGruposMedicacion() {
        viewModelScope.launch {
            try {
                val grupos = repository.getGrupoMedicacion() ?: emptyList()

                _gruposMedicacion.value = grupos

                val formulaciones = _formulacionesMedicamentos.value
                if (!formulaciones.isNullOrEmpty()) {
                    separarMedicamentosPorHorario(formulaciones, grupos)
                }

                Log.d(
                    "GRUPOS_MEDICACION",
                    grupos.joinToString(" | ") {
                        "ID=${it.idGrupo}, " +
                                "NOMBRE=${it.nombre}, " +
                                "HORA=${it.horaAdministracion}, " +
                                "ESTADO=${it.estado}"
                    }
                )
            } catch (e: Exception) {
                Log.e(
                    "GRUPO_MEDICACION_ERROR",
                    "Error al cargar grupos",
                    e
                )
                _gruposMedicacion.value = emptyList()
            }
        }
    }

    fun separarMedicamentosPorHorario(
        lista: List<FormulacionMedicamento>,
        grupos: List<GrupoMedicacion> = emptyList()
    ) {
        val horaActual = java.util.Calendar.getInstance()
            .get(java.util.Calendar.HOUR_OF_DAY)

        val medicamentosConGrupo = lista.filter { medicamento ->
            medicamento.actualAdministrado == true &&
                    medicamento.suspendidoFecha.isNullOrBlank() &&
                    grupos.any {
                        it.idGrupo == medicamento.idGrupo
                    }
        }

        medicamentosManana.value =
            medicamentosConGrupo.filter {
                val hora = it.horaAdministrada
                    ?.substringBefore(":")
                    ?.toIntOrNull() ?: 0

                hora in 6..11
            }

        medicamentosTarde.value =
            medicamentosConGrupo.filter {
                val hora = it.horaAdministrada
                    ?.substringBefore(":")
                    ?.toIntOrNull() ?: 0

                hora in 12..17
            }

        medicamentosNoche.value =
            medicamentosConGrupo.filter {
                val hora = it.horaAdministrada
                    ?.substringBefore(":")
                    ?.toIntOrNull() ?: 0

                hora >= 18 || hora < 6
            }

        Log.d(
            "MEDICAMENTOS_HOME",
            "HORA CELULAR: $horaActual | " +
                    "MAÑANA: ${medicamentosManana.value} | " +
                    "TARDE: ${medicamentosTarde.value} | " +
                    "NOCHE: ${medicamentosNoche.value}"
        )
    }
    // =========================================================
// REGISTRO DE MEDICAMENTO NO ADMINISTRADO
// =========================================================

    private val _noAdministradoState =
        MutableLiveData<Result<String>>()

    val noAdministradoState: LiveData<Result<String>>
        get() = _noAdministradoState

    private suspend fun obtenerOCrearSeguimientoPendiente(
        idSeguimiento: Int?,
        idFormulacion: Int?,
        horaAdministrada: String?
    ): Int {
        idSeguimiento?.takeIf { it > 0 }?.let { return it }

        val formulacionValida = idFormulacion
            ?.takeIf { it > 0 }
            ?: throw Exception(
                "No se identificó la formulación del medicamento"
            )

        val horaTexto = horaAdministrada
            ?.trim()
            ?.substringAfterLast("T")
            ?.substringBefore("Z")
            ?.substringBefore("+")
            ?.substringBefore("-")
            ?: throw Exception(
                "No se encontró la hora programada"
            )

        val partesHora = horaTexto.split(":")
        val horaNum = partesHora.getOrNull(0)?.toIntOrNull() ?: 0
        val minutoNum = partesHora.getOrNull(1)?.take(2)?.toIntOrNull() ?: 0

        val calProg = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, horaNum)
            set(java.util.Calendar.MINUTE, minutoNum)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val sdfProg = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.US
        ).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        val fechaHoraProgramada = sdfProg.format(calProg.time)

        val resultadoSeguimiento =
            repository.crearSeguimientoMedicamento(
                CrearSeguimientoMedicamentoRequest(
                    idFormulacion = formulacionValida,
                    fechaHoraProgramada = fechaHoraProgramada
                )
            )

        val seguimiento = resultadoSeguimiento.getOrElse {
            throw it
        }

        val estadoSeguimiento = seguimiento.estado?.trim().orEmpty()
        if (
            estadoSeguimiento.isNotEmpty() &&
            !estadoSeguimiento.equals("PENDIENTE", ignoreCase = true)
        ) {
            val mensaje = when {
                estadoSeguimiento.equals("ADMINISTRADO", ignoreCase = true) ->
                    "Esta dosis ya fue administrada."
                estadoSeguimiento.equals("NO_ADMINISTRADO", ignoreCase = true) ->
                    "Esta dosis ya fue registrada como no administrada."
                else ->
                    "Esta dosis ya tiene un seguimiento cerrado."
            }
            throw Exception(mensaje)
        }

        val idRecuperado = seguimiento.idSeguimiento
            ?.takeIf { it > 0 }
            ?: throw Exception(
                "No se recibió el ID del seguimiento"
            )

        Log.d(
            "SEGUIMIENTO_MEDICAMENTO",
            "Seguimiento asociado: $idRecuperado | " +
                    "Formulación: $formulacionValida | " +
                    "Fecha: $fechaHoraProgramada"
        )

        return idRecuperado
    }

    fun registrarMedicamentoNoAdministrado(
        idSeguimiento: Int? = null,
        motivo: String,
        idFormulacion: Int? = null,
        horaAdministrada: String? = null
    ) {
        if (motivo.isBlank()) {
            _noAdministradoState.value =
                Result.failure(
                    Exception("Debes indicar el motivo")
                )
            return
        }

        viewModelScope.launch {
            try {
                val idSeguimientoValido =
                    obtenerOCrearSeguimientoPendiente(
                        idSeguimiento = idSeguimiento,
                        idFormulacion = idFormulacion,
                        horaAdministrada = horaAdministrada
                    )

                val resultado =
                    repository.registrarNoAdministrado(
                        idSeguimientoValido,
                        MotivoNoAdministracionRequest(
                            motivoNoAdministracion = motivo.trim()
                        )
                    )

                resultado.onSuccess {
                    _noAdministradoState.value =
                        Result.success(
                            "Medicamento registrado como no administrado"
                        )

                    _pacienteSeleccionado.value?.idPaciente?.let { id ->
                        cargarSeguimientosAdministracionMedicamentos(id)
                    }
                }.onFailure { error ->
                    _noAdministradoState.value =
                        Result.failure(error)
                }

            } catch (e: Exception) {
                _noAdministradoState.value =
                    Result.failure(e)
            }
        }
    }


    // =========================================================
    // REGISTRO DE APLICACIÓN DE MEDICAMENTO
    // =========================================================

    private val _registroAplicacionState =
        MutableLiveData<Result<String>>()

    val registroAplicacionState: LiveData<Result<String>>
        get() = _registroAplicacionState

    fun registrarAplicacionMedicamento(
        idTratamientoMedicamento: Int,
        idUsuario: Int,
        dosis: String,
        via: String,
        observacion: String,
        idInventario: Int? = null,
        idElementoPaciente: Int,
        cantidadActual: Int,
        cantidadAplicada: Int = 1,
        idEncargado: Int = 1,
        nombreMedicamento: String = "Medicamento",
        nombrePaciente: String = "Paciente",
        context: Context,
        idSeguimiento: Int? = null,
        idFormulacion: Int? = null,
        horaAdministrada: String? = null
    ) {
        viewModelScope.launch {
            try {
                val cantidadADescontar =
                    if (cantidadAplicada > 0) {
                        cantidadAplicada
                    } else {
                        dosis.toIntOrNull() ?: 1
                    }

                val idPaciente =
                    _pacienteSeleccionado.value?.idPaciente
                        ?: return@launch

// ================================================
// VALIDAR EL INVENTARIO SELECCIONADO EN EL FRAGMENT
// ================================================

                val idInventarioSeleccionado = idInventario

                if (idInventarioSeleccionado == null) {
                    _registroAplicacionState.value =
                        Result.failure(
                            Exception(
                                "No se recibió el inventario seleccionado"
                            )
                        )
                    return@launch
                }

// Consultar los inventarios disponibles.
                val listaInventario =
                    repository.obtenerInventario()

// buscar por el ID seleccionado.
                val inventarioEncontrado =
                    listaInventario.firstOrNull {
                        it.idInventario == idInventarioSeleccionado
                    }

                if (inventarioEncontrado == null) {
                    _registroAplicacionState.value =
                        Result.failure(
                            Exception(
                                "El inventario seleccionado ya no existe"
                            )
                        )
                    return@launch
                }

                // Comprobar que corresponda al paciente,
                // medicamento y elemento seleccionados.
                val inventarioValido =
                    inventarioEncontrado.idPaciente == idPaciente &&
                            inventarioEncontrado.idMedicamentos == idTratamientoMedicamento &&
                            inventarioEncontrado.idElemento == idElementoPaciente &&
                            inventarioEncontrado.estado == true

                if (!inventarioValido) {
                    _registroAplicacionState.value =
                        Result.failure(
                            Exception(
                                "El inventario seleccionado no corresponde al medicamento del paciente"
                            )
                        )
                    return@launch
                }

// Verificar las existencias actuales.
                val stockDisponible =
                    inventarioEncontrado.cantidadActual ?: 0

                if (stockDisponible < cantidadADescontar) {
                    _registroAplicacionState.value =
                        Result.failure(
                            Exception(
                                "Stock insuficiente. Disponible: $stockDisponible"
                            )
                        )
                    return@launch
                }

                // Conservar el ID exacto seleccionado.
                val idInventarioValido =
                    inventarioEncontrado.idInventario

                val fechaActual = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    java.util.Locale.US
                ).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())

                Log.d(
                    "APLICACION_DEBUG",
                    "idPaciente=$idPaciente | " +
                            "idMedicamento=$idTratamientoMedicamento | " +
                            "idElementoPaciente=$idElementoPaciente | " +
                            "idInventarioRecibido=$idInventario | " +
                            "idInventarioValido=$idInventarioValido | " +
                            "cantidadActual=$cantidadActual | " +
                            "cantidadAplicada=$cantidadADescontar"
                )
                // ================================================
                // OBTENER O CREAR SEGUIMIENTO PENDIENTE
                // ================================================

                val idSeguimientoParaAplicacion =
                    obtenerOCrearSeguimientoPendiente(
                        idSeguimiento = idSeguimiento,
                        idFormulacion = idFormulacion,
                        horaAdministrada = horaAdministrada
                    )

                val request = AplicacionRequest(
                    idPaciente = idPaciente,
                    idMedicamento = idTratamientoMedicamento,
                    idInventario = idInventarioValido,
                    idUsuario = idUsuario,
                    fechaHora = fechaActual,
                    dosisAdministrada = dosis,
                    viaAdministracion = via,
                    estado = true,
                    observacion = observacion,
                    cantidadAplicada = cantidadADescontar,
                    idSeguimiento = idSeguimientoParaAplicacion
                )

                val result =
                    repository.registrarAplicacionMedicamento(request)

                if (result.isSuccess) {
                    _registroAplicacionState.value =
                        Result.success(
                            "Aplicación registrada con éxito"
                        )

                    cargarElementosPaciente(idPaciente)
                    cargarInventariosPaciente(idPaciente)
                } else {
                    _registroAplicacionState.value =
                        Result.failure(
                            result.exceptionOrNull()
                                ?: Exception(
                                    "Error al registrar aplicación"
                                )
                        )
                }
            } catch (e: Exception) {
                _registroAplicacionState.value =
                    Result.failure(e)
            }
        }
    }
}