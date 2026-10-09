package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.GrupoMedicacion
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Inventario
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

    private val _formulacionesMedicamentos = MutableLiveData<List<FormulacionMedicamento>>()
    val formulacionesMedicamentos: LiveData<List<FormulacionMedicamento>> get() = _formulacionesMedicamentos

    val medicamentosManana = MutableLiveData<List<FormulacionMedicamento>>()
    val medicamentosTarde = MutableLiveData<List<FormulacionMedicamento>>()
    val medicamentosNoche = MutableLiveData<List<FormulacionMedicamento>>()

    fun obtenerMedicamentosPaciente(idPaciente: Int): List<FormulacionMedicamento> {
        val lista = formulacionesMedicamentos.value ?: emptyList()
        return lista.filter { it.idPaciente == idPaciente }
    }

    private val _gruposMedicacion = MutableLiveData<List<GrupoMedicacion>>()
    val gruposMedicacion: LiveData<List<GrupoMedicacion>> get() = _gruposMedicacion

    private val _elementos = MutableLiveData<List<ElementoPaciente>>(emptyList())
    val elementos: LiveData<List<ElementoPaciente>> get() = _elementos
    val elementosPaciente: LiveData<List<ElementoPaciente>> get() = _elementos

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

    private val _registroAplicacionState = MutableLiveData<Result<String>>()
    val registroAplicacionState: LiveData<Result<String>> get() = _registroAplicacionState

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
                Log.e("API_ERROR_REAL", "Error al cargar", e)
                _pacientes.value = emptyList()
                _pacientesSeleccionadosHome.value = emptyList()
            }
        }
    }

    fun confirmarSeleccionDelDia(idUsuarioLogueado: Int) {
        tienePacientesSeleccionados = true
        val seleccionados = listaPacientesCompleta.filter { it.isSelected }
        _pacientesSeleccionadosHome.value = seleccionados

        val fechaActual = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

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
                        Log.e("API_ERROR", "Error al guardar asignacion", e)
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
                Log.e("API_ERROR_REAL", "Error al obtener paciente $id", e)
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

    fun cargarElementosPaciente(idPaciente: Int) {
        viewModelScope.launch {
            val lista = repository.getElementosPorPaciente(idPaciente)
            _elementos.value = lista
        }
    }

    fun cargarElementosDelPaciente(idPaciente: Int) {
        cargarElementosPaciente(idPaciente)
    }

    fun guardarElementoPaciente(elemento: ElementoPaciente) {
        viewModelScope.launch {
            val exito = repository.guardarElementoPaciente(elemento)
            if (exito) {
                kotlinx.coroutines.delay(300)
                elemento.idPaciente?.let { cargarElementosPaciente(it) }
            } else {
                Log.e("PACIENTE_VM", "Error al guardar el elemento en la API")
            }
        }
    }

    fun registrarAplicacionMedicamento(
        idTratamientoMedicamento: Int,
        idUsuario: Int,
        dosis: String,
        via: String,
        observacion: String,
        idElementoPaciente: Int,
        cantidadActual: Int,
        idEncargado: Int = 1,
        nombreMedicamento: String = "Medicamento",
        nombrePaciente: String = "Paciente",
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val cantidadADescontar = dosis.toIntOrNull() ?: 1
                val listaInventario = repository.obtenerInventario()

                val inventarioEncontrado = listaInventario.find { inv ->
                    inv.idElemento == idElementoPaciente || inv.idMedicamentos == idTratamientoMedicamento
                }

                val idInventarioValido = inventarioEncontrado?.idInventario ?: 1

                val fechaActual = java.text.SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())

                val idPaciente = _pacienteSeleccionado.value?.idPaciente ?: 1

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
                    val stockRestante = if (cantidadActual >= cantidadADescontar) cantidadActual - cantidadADescontar else 0
                    try {
                        repository.actualizarStockInventario(idInventarioValido, stockRestante)
                    } catch (e: Exception) {
                        Log.e("INVENTARIO_UPDATE", "Error al actualizar stock local ", e)
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
                            val respNotif = repository.crearNotificacion(notificacionReq)
                            respNotif.getOrNull()?.let { notifCreada ->
                                val destinatarioReq = NotificacionDestinatarioRequest(
                                    id_notificacion = notifCreada.id_notificacion,
                                    id_usuario = idEncargado,
                                    leido = false
                                )
                                repository.asociarNotificacionDestinatario(destinatarioReq)
                            }
                        } catch (e: Exception) {
                            Log.e("NOTIF_ERROR", "Error al registrar notificación", e)
                        }
                    }

                    _registroAplicacionState.value = Result.success("Aplicación registrada con éxito")
                    cargarElementosPaciente(idPaciente)
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

    fun cargarFormulacionesMedicamentos() {
        viewModelScope.launch {
            try {
                val listaMedicamentos = repository.getFormulacionesMedicamentos() ?: emptyList()
                val grupos = _gruposMedicacion.value ?: emptyList()
                _formulacionesMedicamentos.value = listaMedicamentos
                separarMedicamentosPorHorario(listaMedicamentos, grupos)
            } catch (e: Exception) {
                Log.e("FORMULACION_ERROR", "Error cargando formulaciones", e)
                _formulacionesMedicamentos.value = emptyList()
                separarMedicamentosPorHorario(emptyList(), emptyList())
            }
        }
    }

    fun cargarGruposMedicacion() {
        viewModelScope.launch {
            try {
                val lista = repository.getGrupoMedicacion()
                _gruposMedicacion.value = lista ?: emptyList()
            } catch (e: Exception) {
                Log.e("GRUPO_MEDICACION_ERROR", "Error cargando grupos de medicación", e)
                _gruposMedicacion.value = emptyList()
            }
        }
    }

    fun separarMedicamentosPorHorario(
        lista: List<FormulacionMedicamento>,
        grupos: List<GrupoMedicacion> = emptyList()
    ) {
        val horaActual = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        val medicamentosConGrupo = lista.filter { medicamento ->
            grupos.any { it.idGrupo == medicamento.idGrupo }
        }

        val manana = medicamentosConGrupo.filter {
            val hora = it.horaAdministrada?.substringBefore(":")?.toIntOrNull() ?: 0
            hora in 6..11
        }

        val tarde = medicamentosConGrupo.filter {
            val hora = it.horaAdministrada?.substringBefore(":")?.toIntOrNull() ?: 0
            hora in 12..17
        }

        val noche = medicamentosConGrupo.filter {
            val hora = it.horaAdministrada?.substringBefore(":")?.toIntOrNull() ?: 0
            hora >= 18 || hora < 6
        }

        medicamentosManana.value = manana
        medicamentosTarde.value = tarde
        medicamentosNoche.value = noche

        Log.d(
            "MEDICAMENTOS_HOME",
            "HORA CELULAR: $horaActual | MAÑANA: $manana | TARDE: $tarde | NOCHE: $noche"
        )
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
