package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.ApiService
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Paciente

class PacienteRepository{
    private val api = RetrofitClient.api
    suspend fun obtenerPacientes(): List<Paciente> {
        return api.getPacientes()
    }
}