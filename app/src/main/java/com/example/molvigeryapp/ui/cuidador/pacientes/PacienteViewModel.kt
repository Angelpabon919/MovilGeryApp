package com.example.molvigeryapp.ui.cuidador.pacientes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.launch

class PacienteViewModel(private val repository: PacienteRepository) : ViewModel() {
    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    // Lista original completa de respaldo
    private var listaPacientesCompleta: List<Paciente> = emptyList()

    private val _pacienteSeleccionado = MutableLiveData<Paciente?>()
    val pacienteSeleccionado: LiveData<Paciente?> get() = _pacienteSeleccionado

    private val _idBitacora = MutableLiveData<Int?>()
    val idBitacora: LiveData<Int?> get() = _idBitacora

    fun guardarIdBitacora(id: Int){
        _idBitacora.value = id
    }

    private val _recomendaciones = MutableLiveData<List<Recomendacion>>()
    val recomendaciones: LiveData<List<Recomendacion>> get() = _recomendaciones

    private val _cuidados = MutableLiveData<List<CuidadoEnfermeria>?>()
    val cuidados: LiveData<List<CuidadoEnfermeria>?> = _cuidados

    private val _elementos = MutableLiveData<List<ElementoPaciente>>()
    // Asegúrate de que inicie con una lista vacía y no con datos de prueba
    val elementos = MutableLiveData<List<ElementoPaciente>>(emptyList())
    var tienePacientesSeleccionados: Boolean = false
        private set

    fun cargarPacientes() {
        // SI YA TENEMOS PACIENTES SELECCIONADOS, NO REEMPLAZAMOS LA LISTA CON LA COMPLETA
        if (tienePacientesSeleccionados) {
            _pacientes.value = listaPacientesCompleta.filter { it.isSelected }
            return
        }

        viewModelScope.launch {
            try {
                val resultado = repository.obtenerPacientes()
                listaPacientesCompleta = resultado
                _pacientes.value = resultado
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR_REAL", "Error al cargar", e)
                _pacientes.value = emptyList()
            }
        }
    }

    // Alterna el estado isSelected de un paciente específico
    fun toggleSeleccionPaciente(idPaciente: Int) {
        listaPacientesCompleta.find { it.idPaciente == idPaciente }?.let { paciente ->
            paciente.isSelected = !paciente.isSelected
            _pacientes.value = ArrayList(listaPacientesCompleta)
        }
    }

    // Filtra la lista para mostrar ÚNICAMENTE los seleccionados para el día
    fun confirmarSeleccionDelDia(idUsuarioLogueado: Int) {
        _pacientes.value = listaPacientesCompleta.filter { it.isSelected }
        tienePacientesSeleccionados = true

        // Enviamos a la API cada paciente seleccionado para persistirlo en la base de datos
        val fechaActual = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date())

        listaPacientesCompleta.filter { it.isSelected }.forEach { paciente ->
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
    // Restaura la vista con la lista completa si necesitas volver a elegir
    fun restaurarListaCompleta() {
        tienePacientesSeleccionados = false
        // Desmarcamos las selecciones
        listaPacientesCompleta.forEach { it.isSelected = false }

        if (listaPacientesCompleta.isNotEmpty()) {
            _pacientes.value = listaPacientesCompleta
        } else {
            cargarPacientes()
        }
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
            _elementos.value = lista ?: emptyList()
        }
    }

    fun guardarElementoPaciente(elemento: ElementoPaciente) {
        viewModelScope.launch {
            val exito = repository.guardarElementoPaciente(elemento)
            if (exito) {
                cargarElementosPaciente(elemento.idPaciente)
            }
        }
    }
}