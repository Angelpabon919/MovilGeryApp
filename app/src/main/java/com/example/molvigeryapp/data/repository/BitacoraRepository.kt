package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Actividad
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.SignosVitales
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BitacoraRepository {

    private val api = RetrofitClient.apiService

    // =========================================================
    // BITÁCORA
    // =========================================================

    suspend fun obtenerBitacoras(): List<Bitacora> =
        withContext(Dispatchers.IO) {
            api.getBitacoras()
        }

    suspend fun crearBitacora(bitacora: Bitacora): Bitacora =
        withContext(Dispatchers.IO) {
            api.crearBitacora(bitacora)
        }


    // =========================================================
    // ACTIVIDADES
    // =========================================================

    suspend fun obtenerActividades(): List<Actividad> =
        withContext(Dispatchers.IO) {
            api.getActividades()
        }


    // =========================================================
    // SIGNOS VITALES
    // =========================================================

    suspend fun crearSignosVitales(
        signosVitales: SignosVitales
    ): SignosVitales =
        withContext(Dispatchers.IO) {
            api.crearSignosVitales(signosVitales)
        }


    // =========================================================
    // EVENTOS ADVERSOS
    // =========================================================

    suspend fun crearEventoAdverso(
        eventoAdverso: EventoAdverso
    ): EventoAdverso =
        withContext(Dispatchers.IO) {
            api.crearEventoAdverso(eventoAdverso)
        }
}