package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.Paciente
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PacienteRepository {

    private val api = RetrofitClient.apiService

    suspend fun obtenerPacientes(): List<Paciente> =
        withContext(Dispatchers.IO) {
            api.getPacientes()
        }

    suspend fun obtenerPacientePorId(id: Int): Paciente =
        withContext(Dispatchers.IO) {
            api.getPacienteById(id)
        }
    suspend fun getAplicacionesMedicamentos(idPaciente: Int): List<AplicacionMedicamento>? =
        withContext(Dispatchers.IO) {
            try {
                api.getAplicacionesPorPaciente(idPaciente)
            } catch (e: Exception) {
                null
            }
        }

    }