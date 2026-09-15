package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
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

    suspend fun getAplicacionesporPaciente(idPaciente: Int): List<AplicacionMedicamento>? =
        withContext(Dispatchers.IO) {
            try {
                api.getAplicacionesPorPaciente(idPaciente)
            } catch (e: Exception) {
                null
            }
        }

    // Nuevo método para guardar o actualizar el medicamento sin modificar lo existente
    suspend fun guardarMedicamento(medicamento: AplicacionMedicamento): Boolean =
        withContext(Dispatchers.IO) {
            try {
                api.guardarAplicacionMedicamento(medicamento)
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getRecomendaciones(idPaciente: Int): List<Recomendacion>? =
        withContext(Dispatchers.IO) {
            try {
                api.getRecomendacionesPorPaciente(idPaciente)
            } catch (e: Exception) {
                null
            }
        }

    suspend fun guardarRecomendacion(recomendacion: Recomendacion): Boolean =
        withContext(Dispatchers.IO) {
            try {
                api.guardarRecomendacion(recomendacion)
                true
            } catch (e: Exception) {
                false
            }
        }
    suspend fun guardarCuidadoEnfermeria(cuidado: CuidadoEnfermeria): Boolean =
        withContext(Dispatchers.IO) {
            try {
                api.guardarCuidadoEnfermeria(cuidado)
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun getCuidadosPorPaciente(idPaciente: Int): List<CuidadoEnfermeria>? =
        withContext(Dispatchers.IO) {
            try {
                api.getCuidadosPorPaciente(idPaciente)
            } catch (e: Exception) {
                null
            }
        }
}