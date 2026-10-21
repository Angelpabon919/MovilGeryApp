package com.example.molvigeryapp.data.repository

import android.util.Log
import com.example.molvigeryapp.data.api.RetrofitClient

object FcmTokenRepository {

    private const val TAG = "FCM_TOKEN"

    suspend fun registrarToken(
        idUsuario: Int,
        token: String
    ): Boolean {

        if (idUsuario <= 0) {
            Log.e(TAG, "Error: el ID del usuario no es válido.")
            return false
        }

        if (token.isBlank()) {
            Log.e(TAG, "Error: el token de Firebase está vacío.")
            return false
        }

        return try {
            val datos = mapOf(
                "id_usuario" to idUsuario,
                "token" to token
            )

            Log.d(
                TAG,
                "Enviando token FCM. Usuario=$idUsuario, token=${token.take(10)}..."
            )

            val respuesta =
                RetrofitClient.apiService.registrarTokenFCM(datos)

            if (respuesta.isSuccessful) {
                Log.i(
                    TAG,
                    "Token registrado correctamente. HTTP ${respuesta.code()}. Usuario=$idUsuario"
                )
                true
            } else {
                val error = respuesta.errorBody()?.string()

                Log.e(
                    TAG,
                    "Falló el registro. HTTP ${respuesta.code()}. Detalle: $error"
                )
                false
            }

        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error de conexión al registrar el token FCM.",
                e
            )
            false
        }
    }

    suspend fun configurarPreferenciaPush(
        idUsuario: Int,
        activadas: Boolean
    ): Boolean {
        if (idUsuario <= 0) return false

        return try {
            val datos = mapOf(
                "id_usuario" to idUsuario,
                "activadas" to activadas
            )

            val respuesta = RetrofitClient.apiService
                .configurarPreferenciaPush(datos)

            respuesta.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

}