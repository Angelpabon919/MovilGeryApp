package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.FormulacionMedicamento

object FormulacionRepository {
    suspend fun obtenerFormulaciones():
            List<FormulacionMedicamento> {


        return try {

            val respuesta =
                RetrofitClient.apiService
                    .getFormulacionesMedicamentos()


            if (respuesta.isSuccessful) {

                respuesta.body() ?: emptyList()

            } else {

                emptyList()

            }


        } catch (e: Exception) {

            emptyList()

        }
    }
}