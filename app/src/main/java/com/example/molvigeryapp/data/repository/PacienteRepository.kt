package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.TipoInsumo
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

    suspend fun guardarAsignacion(asignacion: AsignacionPacienteCuidador): Boolean {
        return try {
            val respuesta = api.guardarAsignacion(asignacion)
            respuesta.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e("API_ERROR", "Error en la petición POST", e)
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

    suspend fun getElementosPorPaciente(idPaciente: Int): List<ElementoPaciente>? =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getElementosPorPaciente(idPaciente)
                if (res.isSuccessful) {
                    val listaCompleta = res.body() ?: emptyList()
                    // Filtro para mostrar solo los datos del paciente actual
                    listaCompleta.filter { it.idPaciente == idPaciente }
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Error al obtener elementos", e)
                emptyList()
            }
        }

    suspend fun guardarElementoPaciente(elemento: ElementoPaciente): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val res = api.guardarElementoPaciente(elemento)
                if (res.isSuccessful) {
                    true
                } else {
                    android.util.Log.e(
                        "API_ERROR",
                        "Error al guardar elemento ${res.code()}: ${res.errorBody()?.string()}"
                    )
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Excepción al guardar elemento", e)
                false
            }
        }

    suspend fun getMedicamentos(): List<Medicamento>? =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getMedicamentos()
                if (res.isSuccessful) res.body() else null
            } catch (e: Exception) {
                null
            }
        }

    suspend fun getTiposInsumos(): List<TipoInsumo>? =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getTiposInsumos()
                if (res.isSuccessful) {
                    res.body()
                } else {
                    android.util.Log.e("INSUMOS_DEBUG", "Error Tipos HTTP ${res.code()}: ${res.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("INSUMOS_DEBUG", "Excepción al cargar tipos de insumo", e)
                null
            }
        }

    suspend fun getInsumosPorTipo(idTipo: Int): List<Insumo>? =
        withContext(Dispatchers.IO) {
            try {
                val res = api.getInsumosPorTipo(idTipo)
                if (res.isSuccessful) {
                    res.body()
                } else {
                    android.util.Log.e("INSUMOS_DEBUG", "Error Insumos HTTP ${res.code()}: ${res.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("INSUMOS_DEBUG", "Excepción al cargar insumos por tipo", e)
                null
            }
        }
}