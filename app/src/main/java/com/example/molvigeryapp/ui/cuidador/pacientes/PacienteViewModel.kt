package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.TipoInsumo
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.launch

class PacienteViewModel(private val repository: PacienteRepository) : ViewModel() {

    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    private val _pacientesSeleccionadosHome = MutableLiveData<List<Paciente>>()
    val pacientesSeleccionadosHome: LiveData<List<Paciente>> get() = _pacientesSeleccionadosHome

    private var listaPacientesCompleta: List<Paciente> = emptyList()

    private val _pacienteSeleccionado = MutableLiveData<Paciente?>()
    val pacienteSeleccionado: LiveData<Paciente?> get() = _pacienteSeleccionado

    private val _idBitacora = MutableLiveData<Int?>()
    val idBitacora: LiveData<Int?> get() = _idBitacora

    fun guardarIdBitacora(id: Int) {
        _idBitacora.value = id
    }

    private val _recomendaciones = MutableLiveData<List<Recomendacion>>()
    val recomendaciones: LiveData<List<Recomendacion>> get() = _recomendaciones

    private val _cuidados = MutableLiveData<List<CuidadoEnfermeria>?>()
    val cuidados: LiveData<List<CuidadoEnfermeria>?> = _cuidados

    // --- ELEMENTOS DEL PACIENTE, MEDICAMENTOS E INSUMOS ---
    private val _elementos = MutableLiveData<List<ElementoPaciente>>(emptyList())
    val elementos: LiveData<List<ElementoPaciente>> get() = _elementos
    val elementosPaciente: LiveData<List<ElementoPaciente>> get() = _elementos

    fun cargarElementosDelPaciente(idPaciente: Int) {
        cargarElementosPaciente(idPaciente)
    }

    private val _medicamentosCatalogo = MutableLiveData<List<Medicamento>?>()
    val medicamentosCatalogo: LiveData<List<Medicamento>?> get() = _medicamentosCatalogo

    private val _tiposInsumosCatalogo = MutableLiveData<List<TipoInsumo>?>()
    val tiposInsumosCatalogo: LiveData<List<TipoInsumo>?> get() = _tiposInsumosCatalogo
    val tiposInsumos: LiveData<List<TipoInsumo>?> get() = _tiposInsumosCatalogo

    private val _insumosCatalogo = MutableLiveData<List<Insumo>?>()
    val insumosCatalogo: LiveData<List<Insumo>?> get() = _insumosCatalogo
    val insumosPorTipo: LiveData<List<Insumo>?> get() = _insumosCatalogo

    var tienePacientesSeleccionados: Boolean = false
        private set

    fun cargarPacientes() {
        viewModelScope.launch {
            try {
                if (listaPacientesCompleta.isEmpty()) {
                    listaPacientesCompleta = repository.obtenerPacientes()
                }

                _pacientes.value = listaPacientesCompleta

                if (tienePacientesSeleccionados) {
                    _pacientesSeleccionadosHome.value = listaPacientesCompleta.filter { it.isSelected }
                }

            } catch (e: Exception) {
                android.util.Log.e("API_ERROR_REAL", "Error al cargar", e)
                _pacientes.value = emptyList()
                _pacientesSeleccionadosHome.value = emptyList()
            }
        }
    }

    fun confirmarSeleccionDelDia(idUsuarioLogueado: Int) {
        tienePacientesSeleccionados = true
        val seleccionados = listaPacientesCompleta.filter { it.isSelected }
        _pacientesSeleccionadosHome.value = seleccionados

        val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date())

        seleccionados.forEach { paciente ->
            paciente.idPaciente?.let { idPaciente ->
                val asignacion = AsignacionPacienteCuidador(
                    idUsuario = idUsuarioLogueado,
                    idPaciente = idPaciente,
                    fechaInicio = fechaActual,
                    fechaFin = fechaActual,
                    estado = "Activo",
                    observaciones = "Asignación desde app Cuidador"
                )

                viewModelScope.launch {
                    try {
                        repository.guardarAsignacion(asignacion)
                    } catch (e: Exception) {
                        android.util.Log.e("API_ERROR", "Error al guardar asignacion", e)
                    }
                }
            }
        }
    }

    fun restaurarListaCompleta() {
        tienePacientesSeleccionados = false
        listaPacientesCompleta.forEach { it.isSelected = false }
        _pacientes.value = listaPacientesCompleta
        _pacientesSeleccionadosHome.value = emptyList()
    }

    fun cargarPacientePorId(id: Int) {
        viewModelScope.launch {
            try {
                val paciente = repository.obtenerPacientePorId(id)
                _pacienteSeleccionado.value = paciente
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR_REAL", "Error al obtener paciente $id", e)
                _pacienteSeleccionado.value = null
            }
        }
    }

    fun seleccionarPaciente(paciente: Paciente) {
        _pacienteSeleccionado.value = paciente
    }

    fun cargarRecomendaciones(idPaciente: Int) {
        viewModelScope.launch {
            _recomendaciones.value = emptyList()
            val lista = repository.getRecomendaciones(idPaciente)
            _recomendaciones.value = lista ?: emptyList()
        }
    }

    fun guardarRecomendacion(recomendacion: Recomendacion) {
        viewModelScope.launch {
            repository.guardarRecomendacion(recomendacion)
        }
    }

    fun cargarCuidados(idPaciente: Int) {
        viewModelScope.launch {
            val lista = repository.getCuidadosPorPaciente(idPaciente)
            _cuidados.value = lista
        }
    }

    fun guardarCuidado(cuidado: CuidadoEnfermeria) {
        viewModelScope.launch {
            val exito = repository.guardarCuidadoEnfermeria(cuidado)
            if (exito && cuidado.idPaciente != null) {
                cargarCuidados(cuidado.idPaciente)
            }
        }
    }

    // --- MÉTODOS DE ELEMENTOS PACIENTE (MEDICAMENTOS E INSUMOS) ---

    fun cargarElementosPaciente(idPaciente: Int) {
        viewModelScope.launch {
            val lista = repository.getElementosPorPaciente(idPaciente)
            _elementos.value = lista ?: emptyList()
        }
    }

    fun guardarElementoPaciente(elemento: ElementoPaciente) {
        viewModelScope.launch {
            val exito = repository.guardarElementoPaciente(elemento)
            if (exito) {
                kotlinx.coroutines.delay(300)
                elemento.idPaciente?.let { cargarElementosPaciente(it) }
            } else {
                android.util.Log.e("PACIENTE_VM", "Error al guardar el elemento en la API")
            }
        }
    }

    private val _registroAplicacionState = MutableLiveData<Result<String>>()
    val registroAplicacionState: LiveData<Result<String>> get() = _registroAplicacionState

    fun registrarAplicacionMedicamento(
        idTratamientoMedicamento: Int,
        idUsuario: Int,
        dosis: String,
        via: String,
        observacion: String,
        cantidadAplicadaInput: Int,
        idElementoPaciente: Int, // Este valor que entra lo usaremos como id_elemento o id_medicamento para buscar
        cantidadActual: Int,
        idEncargado: Int,
        nombreMedicamento: String,
        nombrePaciente: String,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val cantidadADescontar = if (cantidadAplicadaInput > 0) cantidadAplicadaInput else 1

                val idPaciente = _pacienteSeleccionado.value?.idPaciente ?: 1

                // 🔍 PASO SEGURO: Obtener el id_inventario real consultando la API de inventario
                val listaInventario = repository.obtenerInventario()

                // Buscamos el registro en inventario que coincida con el paciente y el medicamento/elemento
                val inventarioEncontrado = listaInventario?.find { inv ->
                    inv.idPaciente == idPaciente &&
                            (inv.idMedicamentos == idTratamientoMedicamento || inv.idElemento == idElementoPaciente)
                }

                val idInventarioValido = inventarioEncontrado?.idInventario ?: 0

                if (idInventarioValido <= 0){
                    _registroAplicacionState.value = Result.failure(
                        Exception("no se encontro un inventario para el medicamento")
                    )
                    return@launch
                }

                android.util.Log.d(
                    "INVENTARIO_DEBUG",
                    "Inventario real encontrado y validado: $idInventarioValido"
                )

                val fechaActual = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                val request = AplicacionRequest(
                    idPaciente = idPaciente,
                    idMedicamento = idTratamientoMedicamento,
                    idInventario = idInventarioValido, // <-- ¡Aquí va el ID real de inventario que Django exige!
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
                    val stockRestante = if (cantidadActual >= cantidadADescontar) cantidadActual - cantidadADescontar else 0
                     try {
                         repository.actualizarStockInventario(idInventarioValido, stockRestante)
                         if (idElementoPaciente > 0){
                             repository.actualizarCantidadElemento(idElementoPaciente, stockRestante)
                         }

                     } catch (e: Exception) {
                         android.util.Log.e("INVENTARIO_UPDATE", "Error al actualizar stock local ", e)
                     }

                    if (stockRestante <= 3) {
                        val notificationHelper = NotificationHelper(context)
                        notificationHelper.enviarNotificacionStockBajo(
                            nombreMedicamento = nombreMedicamento,
                            nombrePaciente = nombrePaciente,
                            cantidadRestante = stockRestante
                        )

                        try {
                            val notificacionReq = com.example.molvigeryapp.data.model.NotificacionRequest(
                                titulo = "⚠️ Stock Bajo: $nombreMedicamento",
                                mensaje = "El medicamento $nombreMedicamento para el paciente $nombrePaciente se está agotando. Quedan $stockRestante unidades.",
                                fecha_creacion = fechaActual,
                                enviar_correo = false
                            )
                            val respNotif = repository.crearNotificacion(notificacionReq)

                            respNotif.getOrNull()?.let { notifCreada ->
                                val destinatarioReq = com.example.molvigeryapp.data.model.NotificacionDestinatarioRequest(
                                    id_notificacion = notifCreada.id_notificacion,
                                    id_usuario = idEncargado,
                                    leido = false
                                )
                                repository.asociarNotificacionDestinatario(destinatarioReq)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NOTIF_ERROR", "Error al registrar notificación", e)
                        }
                    }

                    cargarElementosPaciente(idPaciente)
                    _registroAplicacionState.value = Result.success("Aplicación registrada con éxito")
                } else {
                    _registroAplicacionState.value = Result.failure(
                        result.exceptionOrNull() ?: Exception("Error al registrar aplicación")
                    )
                }
            } catch (e: Exception) {
                _registroAplicacionState.value = Result.failure(e)
            }
        }
    }

    fun cargarCatalogoMedicamentos() {
        viewModelScope.launch {
            val lista = repository.getMedicamentos()
            _medicamentosCatalogo.value = lista
        }
    }

    fun cargarTiposInsumos() {
        viewModelScope.launch {
            val lista = repository.getTiposInsumos()
            _tiposInsumosCatalogo.value = lista
        }
    }

    fun cargarInsumosPorTipo(idTipoInsumo: Int) {
        viewModelScope.launch {
            val lista = repository.getInsumosPorTipo(idTipoInsumo)
            _insumosCatalogo.value = lista
        }
    }
}