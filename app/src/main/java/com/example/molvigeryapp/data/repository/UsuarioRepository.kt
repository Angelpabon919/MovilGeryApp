package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.CambiarContrasenaRequest
import com.example.molvigeryapp.data.model.RespuestaMensaje
import com.example.molvigeryapp.data.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class UsuarioRepository {

    private val api = RetrofitClient.apiService

    // =========================================================
    // OBTENER USUARIO POR ID
    // =========================================================

    suspend fun obtenerUsuarioPorId(
        idUsuario: Int
    ): Usuario? =
        withContext(Dispatchers.IO) {

            try {

                android.util.Log.d(
                    "USUARIO_REPOSITORY",
                    "Consultando GET /usuarios/$idUsuario/"
                )

                val usuario =
                    api.getUsuarioById(idUsuario)

                android.util.Log.d(
                    "USUARIO_REPOSITORY",
                    "Usuario obtenido correctamente"
                )

                usuario

            } catch (e: Exception) {

                android.util.Log.e(
                    "USUARIO_REPOSITORY",
                    "Error al obtener usuario $idUsuario",
                    e
                )

                null
            }
        }


    // =========================================================
    // ACTUALIZAR USUARIO
    // =========================================================

    suspend fun actualizarUsuario(
        idUsuario: Int,
        nombres: String,
        apellidos: String,
        tipoDocumento: String,
        numeroDocumento: String,
        correo: String,
        telefono: String
    ): Usuario? =
        withContext(Dispatchers.IO) {

            try {

                val datos =
                    mapOf(
                        "nombres" to nombres,
                        "apellidos" to apellidos,
                        "tipo_documento" to tipoDocumento,
                        "numero_documento" to numeroDocumento,
                        "correo" to correo,
                        "telefono" to telefono
                    )

                android.util.Log.d(
                    "USUARIO_REPOSITORY",
                    "PATCH /usuarios/$idUsuario/"
                )

                android.util.Log.d(
                    "USUARIO_REPOSITORY",
                    "Datos enviados: $datos"
                )

                val respuesta =
                    api.actualizarUsuario(
                        idUsuario,
                        datos
                    )

                if (respuesta.isSuccessful) {

                    android.util.Log.d(
                        "USUARIO_REPOSITORY",
                        "Usuario actualizado correctamente"
                    )

                    respuesta.body()

                } else {

                    android.util.Log.e(
                        "USUARIO_REPOSITORY",
                        "Error HTTP ${respuesta.code()}"
                    )

                    android.util.Log.e(
                        "USUARIO_REPOSITORY",
                        "Respuesta: ${
                            respuesta.errorBody()?.string()
                        }"
                    )

                    null
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "USUARIO_REPOSITORY",
                    "Error al actualizar usuario",
                    e
                )

                null
            }
        }


    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    suspend fun cambiarContrasena(
        datos: CambiarContrasenaRequest
    ): Response<RespuestaMensaje> =
        withContext(Dispatchers.IO) {

            api.cambiarContrasena(
                datos
            )
        }
}