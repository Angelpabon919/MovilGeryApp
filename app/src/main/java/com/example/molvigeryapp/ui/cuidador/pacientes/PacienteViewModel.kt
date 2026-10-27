package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.AplicacionRequest
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
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.launch

class PacienteViewModel(
    private val repository: PacienteRepository
) : ViewModel() {

    // =========================================================
    // PACIENTES
    // =========================================================

    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    private val _pacientesSeleccionadosHome =
        MutableLiveData<List<Paciente>>()

    val pacientesSeleccionadosHome: LiveData<List<Paciente>>
        get() = _pacientesSeleccionadosHome

    private var listaPacientesCompleta: List<Paciente> = emptyList()

    private val _pacienteSeleccionado = MutableLiveData<Paciente?>()
    val pacienteSeleccionado: LiveData<Paciente?>
        get() = _pacienteSeleccionado

    var tienePacientesSeleccionados: Boolean = false
        private set

    // =========================================================
// REINICIAR SELECCIÓN AL FINALIZAR EL TURNO
// No modifica las asignaciones del backend.
// =========================================================
    fun reiniciarSeleccionDespuesDelCierre(
        idUsuario: Int,
        preferencias: SharedPreferences
    ) {
        if (idUsuario <= 0) {
            Log.w("SELECCION_PACIENTES", "ID de usuario inválido")
            return
        }

        // Limpiar la selección que conserva el ViewModel.
        listaPacientesCompleta.forEach { paciente ->
            paciente.isSelected = false
        }

        _pacientes.value = listaPacientesCompleta.toList()
        _pacientesSeleccionadosHome.value = emptyList()
        tienePacientesSeleccionados = false

        // Marcar la selección como reiniciada.
        preferencias.edit()
            .putBoolean(claveSeleccion(idUsuario), false)
            .apply()

        Log.d(
            "SELECCION_PACIENTES",
            "Selección reiniciada para el usuario $idUsuario"
        )
    }

    // null = todavía no hay resultado
    // true = guardado correcto
    // false = error al guardar
    private val _seleccionGuardada = MutableLiveData<Boolean?>()
    val seleccionGuardada: LiveData<Boolean?> get() = _seleccionGuardada

    fun cargarPacientes() {
        viewModelScope.launch {
            try {
                if (listaPacientesCompleta.isEmpty()) {
                    listaPacientesCompleta = repository.obtenerPacientes()
                }

                _pacientes.value = listaPacientesCompleta

                if (tienePacientesSeleccionados) {
                    _pacientesSeleccionadosHome.value =
                        listaPacientesCompleta.filter { it.isSelected }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR_REAL", "Error al cargar pacientes", e)
                _pacientes.value = emptyList()
            }
        }
    }

    /**
     * Carga los pacientes y recupera la selección activa del cuidador.
     */
    fun cargarPacientes(idUsuarioLogueado: Int) {
        viewModelScope.launch {
            try {
                if (listaPacientesCompleta.isEmpty()) {
                    listaPacientesCompleta = repository.obtenerPacientes()
                }

                if (idUsuarioLogueado <= 0) {
                    Log.e(
                        "ASIGNACION_DEBUG",
                        "ID de usuario inválido: $idUsuarioLogueado"
                    )
                    _pacientes.value = listaPacientesCompleta
                    return@launch
                }

                val asignaciones =
                    repository.obtenerAsignacionesPacienteCuidador()

                val idsSeleccionados = asignaciones
                    .filter { asignacion ->
                        asignacion.idUsuario == idUsuarioLogueado &&
                                asignacion.estado.equals("Activo", ignoreCase = true)
                    }
                    .mapNotNull { it.idPaciente }
                    .toSet()

                listaPacientesCompleta.forEach { paciente ->
                    paciente.isSelected =
                        paciente.idPaciente in idsSeleccionados
                }

                tienePacientesSeleccionados = idsSeleccionados.isNotEmpty()

                _pacientes.value = listaPacientesCompleta
                _pacientesSeleccionadosHome.value =
                    listaPacientesCompleta.filter { it.isSelected }

                Log.d("ASIGNACION_DEBUG", "Usuario: $idUsuarioLogueado")
                Log.d("ASIGNACION_DEBUG", "IDs seleccionados: $idsSeleccionados")

            } catch (e: Exception) {
                Log.e("ASIGNACION_DEBUG", "Error al recuperar la selección", e)
                _pacientes.value = listaPacientesCompleta
            }
        }
    }

    fun reiniciarResultadoSeleccion() {
        _seleccionGuardada.value = null
    }

    /**
     * Guarda la selección y sincroniza las asignaciones.
     */
    fun confirmarSeleccionDelDia(
        idUsuarioLogueado: Int,
        preferences: SharedPreferences
    ) {
        _seleccionGuardada.value = null

        if (idUsuarioLogueado <= 0) {
            Log.e("ASIGNACION_DEBUG", "ID de usuario inválido")
            _seleccionGuardada.value = false
            return
        }

        val seleccionados =
            listaPacientesCompleta.filter { it.isSelected }

        val idsSeleccionados = seleccionados
            .mapNotNull { it.idPaciente }
            .toSet()

        if (idsSeleccionados.isEmpty()) {
            Log.e("ASIGNACION_DEBUG", "No hay pacientes seleccionados")
            _seleccionGuardada.value = false
            return
        }

        val fechaActual = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        viewModelScope.launch {
            try {
                val guardadoCorrecto =
                    repository.sincronizarSeleccionDelCuidador(
                        idUsuario = idUsuarioLogueado,
                        idsSeleccionados = idsSeleccionados,
                        fechaActual = fechaActual
                    )

                if (guardadoCorrecto) {
                    tienePacientesSeleccionados = true

                    preferences.edit()
                        .putBoolean(claveSeleccion(idUsuarioLogueado), true)
                        .apply()

                    _pacientesSeleccionadosHome.value =
                        listaPacientesCompleta.filter { it.isSelected }

                    Log.d(
                        "ASIGNACION_DEBUG",
                        "Selección sincronizada: $idsSeleccionados"
                    )

                    _seleccionGuardada.value = true
                } else {
                    Log.e("ASIGNACION_DEBUG", "No se confirmó el guardado")
                    _seleccionGuardada.value = false
                }
            } catch (e: Exception) {
                Log.e("ASIGNACION_DEBUG", "Error al sincronizar la selección", e)
                _seleccionGuardada.value = false
            }
        }
    }

    /**
     * Limpia la selección local; no elimina asignaciones del servidor.
     */
    fun restaurarListaCompleta() {
        tienePacientesSeleccionados = false

        listaPacientesCompleta.forEach { it.isSelected = false }

        _pacientes.value = listaPacientesCompleta
        _pacientesSeleccionadosHome.value = emptyList()
        _seleccionGuardada.value = null
    }

    // =========================================================
    // DETALLE DEL PACIENTE
    // =========================================================

    fun cargarPacientePorId(id: Int) {
        viewModelScope.launch {
            try {
                _pacienteSeleccionado.value = repository.obtenerPacientePorId(id)
            } catch (e: Exception) {
                Log.e("API_ERROR_REAL", "Error al obtener paciente $id", e)
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

    private val _cuidados = MutableLiveData<List<CuidadoEnfermeria>?>()
    val cuidados: LiveData<List<CuidadoEnfermeria>?> get() = _cuidados

    fun cargarCuidados(idPaciente: Int) {
        viewModelScope.launch {
            try {
                _cuidados.value = repository.getCuidadosPorPaciente(idPaciente)
            } catch (e: Exception) {
                Log.e("CUIDADOS_ERROR", "Error al cargar cuidados", e)
                _cuidados.value = emptyList()
            }
        }
    }

    fun guardarCuidado(cuidado: CuidadoEnfermeria) {
        viewModelScope.launch {
            try {
                val exito = repository.guardarCuidadoEnfermeria(cuidado)

                if (exito && cuidado.idPaciente != null) {
                    cargarCuidados(cuidado.idPaciente)
                }
            } catch (e: Exception) {
                Log.e("CUIDADOS_ERROR", "Error al guardar cuidado", e)
            }
        }
    }

    // =========================================================
    // RECOMENDACIONES
    // =========================================================

    private val _recomendaciones = MutableLiveData<List<Recomendacion>>()
    val recomendaciones: LiveData<List<Recomendacion>> get() = _recomendaciones

    fun cargarRecomendaciones(idPaciente: Int) {
        viewModelScope.launch {
            try {
                _recomendaciones.value = emptyList()
                val lista = repository.getRecomendaciones(idPaciente)
                _recomendaciones.value = lista ?: emptyList()
            } catch (e: Exception) {
                Log.e("RECOMENDACIONES_ERROR", "Error al cargar recomendaciones", e)
                _recomendaciones.value = emptyList()
            }
        }
    }

    fun guardarRecomendacion(recomendacion: Recomendacion) {
        viewModelScope.launch {
            try {
                repository.guardarRecomendacion(recomendacion)
            } catch (e: Exception) {
                Log.e("RECOMENDACIONES_ERROR", "Error al guardar recomendación", e)
            }
        }
    }

    // =========================================================
    // ELEMENTOS DEL PACIENTE
    // =========================================================

    private val _elementos = MutableLiveData<List<ElementoPaciente>>(emptyList())
    val elementos: LiveData<List<ElementoPaciente>> get() = _elementos
    val elementosPaciente: LiveData<List<ElementoPaciente>> get() = _elementos

    fun cargarElementosPaciente(idPaciente: Int) {
        viewModelScope.launch {
            try {
                _elementos.value = repository.getElementosPorPaciente(idPaciente)
            } catch (e: Exception) {
                Log.e("PACIENTE_VM", "Error al cargar elementos", e)
                _elementos.value = emptyList()
            }
        }
    }

    fun cargarElementosDelPaciente(idPaciente: Int) {
        cargarElementosPaciente(idPaciente)
    }

    fun guardarElementoPaciente(elemento: ElementoPaciente) {
        viewModelScope.launch {
            try {
                val exito = repository.guardarElementoPaciente(elemento)

                if (exito) {
                    kotlinx.coroutines.delay(300)
                    elemento.idPaciente?.let { cargarElementosPaciente(it) }
                } else {
                    Log.e("PACIENTE_VM", "Error al guardar elemento")
                }
            } catch (e: Exception) {
                Log.e("PACIENTE_VM", "Error al guardar elemento", e)
            }
        }
    }

    // =========================================================
    // CATÁLOGO DE INSUMOS
    // =========================================================

    private val _tiposInsumosCatalogo = MutableLiveData<List<TipoInsumo>?>()
    val tiposInsumosCatalogo: LiveData<List<TipoInsumo>?> get() = _tiposInsumosCatalogo
    val tiposInsumos: LiveData<List<TipoInsumo>?> get() = _tiposInsumosCatalogo

    private val _insumosCatalogo = MutableLiveData<List<Insumo>?>()
    val insumosCatalogo: LiveData<List<Insumo>?> get() = _insumosCatalogo
    val insumosPorTipo: LiveData<List<Insumo>?> get() = _insumosCatalogo

    fun cargarTiposInsumos() {
        viewModelScope.launch {
            try {
                _tiposInsumosCatalogo.value = repository.getTiposInsumos()
            } catch (e: Exception) {
                Log.e("INSUMOS_ERROR", "Error al cargar tipos de insumos", e)
                _tiposInsumosCatalogo.value = emptyList()
            }
        }
    }

    fun cargarInsumosPorTipo(idTipoInsumo: Int) {
        viewModelScope.launch {
            try {
                _insumosCatalogo.value = repository.getInsumosPorTipo(idTipoInsumo)
            } catch (e: Exception) {
                Log.e("INSUMOS_ERROR", "Error al cargar insumos", e)
                _insumosCatalogo.value = emptyList()
            }
        }
    }

    // =========================================================
    // MEDICAMENTOS
    // =========================================================

    private val _formulacionesMedicamentos =
        MutableLiveData<List<FormulacionMedicamento>>()
    val formulacionesMedicamentos: LiveData<List<FormulacionMedicamento>>
        get() = _formulacionesMedicamentos

    val medicamentosManana = MutableLiveData<List<FormulacionMedicamento>>()
    val medicamentosTarde = MutableLiveData<List<FormulacionMedicamento>>()
    val medicamentosNoche = MutableLiveData<List<FormulacionMedicamento>>()

    fun obtenerMedicamentosPaciente(idPaciente: Int): List<FormulacionMedicamento> {
        return (formulacionesMedicamentos.value ?: emptyList())
            .filter { it.idPaciente == idPaciente }
    }

    private val _gruposMedicacion = MutableLiveData<List<GrupoMedicacion>>()
    val gruposMedicacion: LiveData<List<GrupoMedicacion>> get() = _gruposMedicacion

    private val _medicamentosCatalogo = MutableLiveData<List<Medicamento>?>()
    val medicamentosCatalogo: LiveData<List<Medicamento>?> get() = _medicamentosCatalogo

    fun cargarCatalogoMedicamentos() {
        viewModelScope.launch {
            try {
                _medicamentosCatalogo.value = repository.getMedicamentos()
            } catch (e: Exception) {
                Log.e("MEDICAMENTOS_ERROR", "Error al cargar catálogo", e)
                _medicamentosCatalogo.value = emptyList()
            }
        }
    }

    fun cargarFormulacionesMedicamentos() {
        viewModelScope.launch {
            try {
                val lista = repository.getFormulacionesMedicamentos() ?: emptyList()
                val grupos = _gruposMedicacion.value ?: emptyList()

                _formulacionesMedicamentos.value = lista
                separarMedicamentosPorHorario(lista, grupos)
            } catch (e: Exception) {
                Log.e("FORMULACION_ERROR", "Error al cargar formulaciones", e)
                _formulacionesMedicamentos.value = emptyList()
                separarMedicamentosPorHorario(emptyList(), emptyList())
            }
        }
    }

    fun cargarGruposMedicacion() {
        viewModelScope.launch {
            try {
                _gruposMedicacion.value = repository.getGrupoMedicacion() ?: emptyList()
            } catch (e: Exception) {
                Log.e("GRUPO_MEDICACION_ERROR", "Error al cargar grupos", e)
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
            grupos.any { it.idGrupo == medicamento.idGrupo }
        }

        medicamentosManana.value = medicamentosConGrupo.filter {
            val hora = it.horaAdministrada?.substringBefore(":")?.toIntOrNull() ?: 0
            hora in 6..11
        }

        medicamentosTarde.value = medicamentosConGrupo.filter {
            val hora = it.horaAdministrada?.substringBefore(":")?.toIntOrNull() ?: 0
            hora in 12..17
        }

        medicamentosNoche.value = medicamentosConGrupo.filter {
            val hora = it.horaAdministrada?.substringBefore(":")?.toIntOrNull() ?: 0
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
    // SELECCIONADOR DE PACIENTES
    // =========================================================

    private fun claveSeleccion(idUsuario: Int) =
        "seleccion_pacientes_usuario_$idUsuario"

    fun cargarPacientesParaSeleccion(
        idUsuario: Int,
        preferencias: SharedPreferences
    ) {
        viewModelScope.launch {
            try {
                if (idUsuario <= 0) {
                    Log.e("SELECCION_PACIENTES", "ID de usuario inválido: $idUsuario")
                    listaPacientesCompleta = repository.obtenerPacientes()
                    listaPacientesCompleta.forEach { it.isSelected = false }
                    _pacientes.value = listaPacientesCompleta
                    _pacientesSeleccionadosHome.value = emptyList()
                    tienePacientesSeleccionados = false
                    return@launch
                }

                // Consultar nuevamente el catálogo para evitar usar datos viejos.
                listaPacientesCompleta = repository.obtenerPacientes()

                val seleccionGuardada =
                    preferencias.getBoolean(claveSeleccion(idUsuario), false)

                if (seleccionGuardada) {
                    val asignaciones =
                        repository.obtenerAsignacionesPacienteCuidador()

                    val ids = asignaciones
                        .filter {
                            it.idUsuario == idUsuario &&
                                    it.estado.equals("Activo", ignoreCase = true)
                        }
                        .mapNotNull { it.idPaciente }
                        .toSet()

                    listaPacientesCompleta.forEach { paciente ->
                        paciente.isSelected = paciente.idPaciente in ids
                    }
                } else {
                    listaPacientesCompleta.forEach { it.isSelected = false }
                }

                tienePacientesSeleccionados =
                    listaPacientesCompleta.any { it.isSelected }

                _pacientes.value = listaPacientesCompleta
                _pacientesSeleccionadosHome.value =
                    listaPacientesCompleta.filter { it.isSelected }

                Log.d(
                    "SELECCION_PACIENTES",
                    "Usuario=$idUsuario, selección guardada=$seleccionGuardada, " +
                            "seleccionados=${listaPacientesCompleta.count { it.isSelected }}"
                )
            } catch (e: Exception) {
                Log.e(
                    "SELECCION_PACIENTES",
                    "Error al cargar la lista de selección",
                    e
                )
                _pacientes.value = listaPacientesCompleta
            }
        }
    }

    /**
     * Fuerza una nueva consulta de pacientes y asignaciones.
     * No elimina asignaciones ni modifica las preferencias de sesión.
     */
    fun actualizarSeleccionDespuesDelCierre(
        idUsuario: Int,
        preferencias: SharedPreferences
    ) {
        // Invalidar datos en memoria para evitar reutilizar una lista antigua.
        listaPacientesCompleta = emptyList()

        // La función de carga vuelve a consultar el servidor.
        cargarPacientesParaSeleccion(idUsuario, preferencias)
    }

    // =========================================================
    // REGISTRO DE APLICACIÓN DE MEDICAMENTO
    // =========================================================

    private val _registroAplicacionState = MutableLiveData<Result<String>>()
    val registroAplicacionState: LiveData<Result<String>> get() = _registroAplicacionState

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
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val cantidadADescontar =
                    if (cantidadAplicada > 0) cantidadAplicada
                    else (dosis.toIntOrNull() ?: 1)

                val idPaciente =
                    _pacienteSeleccionado.value?.idPaciente
                        ?: return@launch

                val listaInventario = repository.obtenerInventario()

                val inventarioEncontrado = listaInventario.find { inventario ->
                    inventario.idPaciente == idPaciente &&
                            (
                                    inventario.idMedicamentos == idTratamientoMedicamento ||
                                            inventario.idElemento == idElementoPaciente
                                    )
                }

                val idInventarioValido = inventarioEncontrado?.idInventario

                if (idInventarioValido == null) {
                    _registroAplicacionState.value = Result.failure(
                        Exception("No se encontró el inventario correcto para este medicamento")
                    )
                    return@launch
                }

                val fechaActual = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

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
                    cantidadAplicada = cantidadADescontar
                )

                val result = repository.registrarAplicacionMedicamento(request)

                if (result.isSuccess) {
                    val stockRestante =
                        if (cantidadActual >= cantidadADescontar) {
                            cantidadActual - cantidadADescontar
                        } else {
                            0
                        }

                    try {
                        repository.actualizarStockInventario(
                            idInventarioValido,
                            stockRestante
                        )

                        if (idElementoPaciente > 0) {
                            repository.actualizarCantidadElemento(
                                idElementoPaciente,
                                stockRestante
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("INVENTARIO_UPDATE", "Error al actualizar stock", e)
                    }

                    if (stockRestante <= 3) {
                        val notificationHelper = NotificationHelper(context)

                        notificationHelper.enviarNotificacionStockBajo(
                            nombreMedicamento = nombreMedicamento,
                            nombrePaciente = nombrePaciente,
                            cantidadRestante = stockRestante
                        )

                        try {
                            val notificacionReq = NotificacionRequest(
                                titulo = "⚠️ Stock Bajo: $nombreMedicamento",
                                mensaje = "El medicamento $nombreMedicamento para el paciente $nombrePaciente se está agotando. Quedan $stockRestante unidades.",
                                fecha_creacion = fechaActual,
                                enviar_correo = false
                            )

                            val respNotif =
                                repository.crearNotificacion(notificacionReq)

                            respNotif.getOrNull()?.let { notifCreada ->
                                val destinatarioReq =
                                    NotificacionDestinatarioRequest(
                                        id_notificacion = notifCreada.id_notificacion,
                                        id_usuario = idEncargado,
                                        leido = false
                                    )

                                repository.asociarNotificacionDestinatario(
                                    destinatarioReq
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("NOTIF_ERROR", "Error al registrar notificación", e)
                        }
                    }

                    _registroAplicacionState.value =
                        Result.success("Aplicación registrada con éxito")

                    cargarElementosPaciente(idPaciente)
                } else {
                    _registroAplicacionState.value = Result.failure(
                        result.exceptionOrNull()
                            ?: Exception("Error al registrar aplicación")
                    )
                }
            } catch (e: Exception) {
                _registroAplicacionState.value = Result.failure(e)
            }
        }
    }
}