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
                val resultado = repository.obtenerPacientes()
                _pacientes.value = resultado
            } catch (e: Exception) {
                _pacientes.value = emptyList()
            }
        }
    }
}