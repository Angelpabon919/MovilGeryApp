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

    fun cargarPacientes() {
        viewModelScope.launch {
            try {
                // Intentar cargar desde backend
                val resultado = repository.obtenerPacientes()
                if (resultado.isNotEmpty()) {
                    _pacientes.value = resultado
                } else {
                    cargarDatosPrueba()
                }
            } catch (e: Exception) {
                // Si falla la red o el backend no responde, muestra datos de prueba
                cargarDatosPrueba()
            }
        }
    }

    private fun cargarDatosPrueba() {
        val listaPrueba = listOf(
            Paciente(id = 1, nombre = "Camilo", apellido = "Sánchez"),
            Paciente(id = 2, nombre = "María", apellido = "Rodríguez"),
            Paciente(id = 3, nombre = "Carlos", apellido = "López"),
            Paciente(id = 4, nombre = "Ana", apellido = "Gómez")
        )
        _pacientes.value = listaPrueba
    }
}