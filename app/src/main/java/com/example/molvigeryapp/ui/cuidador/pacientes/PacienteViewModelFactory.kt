package com.example.molvigeryapp.ui.cuidador.pacientes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.molvigeryapp.data.repository.PacienteRepository

class PacienteViewModelFactory (
    private val repository: PacienteRepository
) : ViewModelProvider.Factory{

    override fun <T : ViewModel> create(modelClass: Class<T>):T {
        if (modelClass.isAssignableFrom(PacienteViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return PacienteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
