package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.EventoIa
import com.example.molvigeryapp.data.model.EvidenciaIa
import com.example.molvigeryapp.data.model.TipoEventoIa
import retrofit2.Response

class EventoIaRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun obtenerEventos(): Response<List<EventoIa>> {
        return apiService.getEventosIa()
    }
        suspend fun obtenerTiposEventos(): Response<List<TipoEventoIa>> {
            return apiService.getTiposEventoIa()
        }
          suspend fun obtenerEvidencias(): Response<List<EvidenciaIa>> {
                return apiService.getEvidenciasIa()



        }
}