package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient

object FcmTokenRepository {

    suspend fun registrarToken(
        idUsuario: Int,
        token: String
    ): Boolean {

        if (idUsuario <= 0 || token.isBlank()) {
            return false
        }

        return try {

            val datos = mapOf(
                "id_usuario" to idUsuario,
                "token" to token
            )

            val respuesta = RetrofitClient.apiService.registrarTokenFCM(datos)

            respuesta.isSuccessful

        } catch (e: Exception) {
            false
        }
    }
}