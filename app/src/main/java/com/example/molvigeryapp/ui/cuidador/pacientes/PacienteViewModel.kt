package com.example.molvigeryapp.ui.cuidador.pacientes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.launch

class PacienteViewModel(private val repository: PacienteRepository) : ViewModel() {
    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    private val _pacienteSeleccionado = MutableLiveData<Paciente?>()
    val pacienteSeleccionado: LiveData<Paciente?> get() = _pacienteSeleccionado

    fun cargarPacientes() {
        viewModelScope.launch {
            try {
                val resultado = repository.obtenerPacientes()
                _pacientes.value = resultado
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR_REAL", "Error al cargar", e)
                _pacientes.value = emptyList()
            }
        }
    }
    fun cargarPacientePorId(id: Int) {
        viewModelScope.launch {
            try {
                val paciente = repository.obtenerPacientePorId(id) // Asegúrate de tener este método en el repository
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
}