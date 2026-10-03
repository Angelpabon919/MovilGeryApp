package com.example.molvigeryapp.ui.cuidador.agenda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.molvigeryapp.data.repository.PacienteRepository

class AgendaViewModelFactory(
    private val repository: PacienteRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(AgendaViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return AgendaViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "ViewModel desconocido: ${modelClass.name}"
        )
    }
}