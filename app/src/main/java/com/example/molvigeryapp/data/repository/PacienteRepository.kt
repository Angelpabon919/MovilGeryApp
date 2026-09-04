package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.ApiService
import com.example.molvigeryapp.data.model.Paciente

class PacienteRepository(private val apiService: ApiService) {
    suspend fun obtenerPacientes(): List<Paciente> {
        return apiService.getPacientes()
    }
}