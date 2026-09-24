package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.ApiService
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AplicacionResponse
import retrofit2.Response

class MedicamentosRepository(private val apiService: ApiService) {

    suspend fun registrarAplicacion(request: AplicacionRequest): Result<AplicacionResponse> {
        return try {
            val response = apiService.registrarAplicacion(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error al registrar"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}