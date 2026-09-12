package com.example.molvigeryapp.ui.cuidador.pacientes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.launch

class PacienteViewModel(private val repository: PacienteRepository) : ViewModel() {
    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    // Guardará la lista original completa que vino de la API
    private var listaPacientesCompleta: List<Paciente> = emptyList()

    private val _pacienteSeleccionado = MutableLiveData<Paciente?>()
    val pacienteSeleccionado: LiveData<Paciente?> get() = _pacienteSeleccionado

    private val _aplicacionesMedicamentos = MutableLiveData<List<AplicacionMedicamento>>()
    val aplicacionesMedicamentos: LiveData<List<AplicacionMedicamento>> get() = _aplicacionesMedicamentos

    private val _recomendaciones = MutableLiveData<List<Recomendacion>>()
    val recomendaciones: LiveData<List<Recomendacion>> get() = _recomendaciones

    fun cargarPacientes() {
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
            _pacientes.value = listaPacientesCompleta
        }
    }

    // Filtra la lista del LiveData para mostrar ÚNICAMENTE los seleccionados para el día
    fun confirmarSeleccionDelDia() {
        val seleccionados = listaPacientesCompleta.filter { it.isSelected }
        _pacientes.value = seleccionados
    }

    // Restaura la vista con la lista completa si necesitas volver a elegir
    fun restaurarListaCompleta() {
        _pacientes.value = listaPacientesCompleta
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

    fun cargarAplicacionesMedicamentos(idPaciente: Int) {
        viewModelScope.launch {
            val lista = repository.getAplicacionesporPaciente(idPaciente)
            _aplicacionesMedicamentos.value = lista ?: emptyList()
        }
    }

    fun cargarRecomendaciones(idPaciente: Int) {
        viewModelScope.launch {
            val lista = repository.getRecomendaciones(idPaciente)
            _recomendaciones.value = lista ?: emptyList()
        }
    }
}