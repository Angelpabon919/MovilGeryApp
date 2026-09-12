package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.SignosVitales

class EventoAdversoRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun crearBitacora(
        bitacora: Bitacora
    ): Bitacora {
        return apiService.crearBitacora(bitacora)
    }

    suspend fun crearEventoAdverso(
        evento: EventoAdverso
    ): EventoAdverso {
        return apiService.crearEventoAdverso(evento)
    }

    suspend fun crearSignosVitales(
        signos: SignosVitales
    ): SignosVitales {
        return apiService.crearSignosVitales(signos)
    }
}