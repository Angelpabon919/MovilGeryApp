package com.example.molvigeryapp.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.CambiarContrasenaRequest
import com.example.molvigeryapp.data.model.RespuestaMensaje
import com.example.molvigeryapp.data.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class UsuarioRepository {

    private val api = RetrofitClient.apiService

    // =========================================================
    // OBTENER USUARIO POR ID
    // =========================================================

    suspend fun obtenerUsuarioPorId(
        idUsuario: Int
    ): Usuario? = withContext(Dispatchers.IO) {
        try {
            Log.d(
                "USUARIO_REPOSITORY",
                "Consultando GET /usuarios/$idUsuario/"
            )

            val usuario = api.getUsuarioById(idUsuario)

            Log.d(
                "USUARIO_REPOSITORY",
                "Usuario obtenido correctamente"
            )

            usuario
        } catch (e: Exception) {
            Log.e(
                "USUARIO_REPOSITORY",
                "Error al obtener usuario $idUsuario",
                e
            )
            null
        }
    }

    // =========================================================
    // ACTUALIZAR DATOS PERSONALES
    // =========================================================

    suspend fun actualizarUsuario(
        idUsuario: Int,
        nombres: String,
        apellidos: String,
        tipoDocumento: String,
        numeroDocumento: String,
        correo: String,
        telefono: String
    ): Usuario? = withContext(Dispatchers.IO) {
        try {
            val datos = mapOf(
                "nombres" to nombres,
                "apellidos" to apellidos,
                "tipo_documento" to tipoDocumento,
                "numero_documento" to numeroDocumento,
                "correo" to correo,
                "telefono" to telefono
            )

            Log.d(
                "USUARIO_REPOSITORY",
                "PATCH /usuarios/$idUsuario/"
            )

            val respuesta = api.actualizarUsuario(idUsuario, datos)

            if (respuesta.isSuccessful) {
                Log.d(
                    "USUARIO_REPOSITORY",
                    "Usuario actualizado correctamente"
                )
                respuesta.body()
            } else {
                Log.e(
                    "USUARIO_REPOSITORY",
                    "Error HTTP ${respuesta.code()}: " +
                            "${respuesta.errorBody()?.string()}"
                )
                null
            }
        } catch (e: Exception) {
            Log.e(
                "USUARIO_REPOSITORY",
                "Error al actualizar usuario",
                e
            )
            null
        }
    }

    // =========================================================
    // SUBIR O CAMBIAR FOTO DE PERFIL
    // =========================================================

    suspend fun actualizarFotoUsuario(
        context: Context,
        idUsuario: Int,
        uriFoto: Uri
    ): Usuario? = withContext(Dispatchers.IO) {
        var archivoTemporal: File? = null

        try {
            val archivo = File.createTempFile(
                "foto_perfil_",
                ".jpg",
                context.cacheDir
            )
            archivoTemporal = archivo

            val inputStream = context.contentResolver
                .openInputStream(uriFoto)
                ?: throw IllegalStateException(
                    "No se pudo abrir la imagen seleccionada."
                )

            inputStream.use { entrada ->
                FileOutputStream(archivo).use { salida ->
                    entrada.copyTo(salida)
                }
            }

            val requestBody = archivo.asRequestBody(
                "image/*".toMediaTypeOrNull()
            )

            val parteFoto = MultipartBody.Part.createFormData(
                "foto",
                archivo.name,
                requestBody
            )

            Log.d(
                "USUARIO_REPOSITORY",
                "Subiendo foto de perfil del usuario $idUsuario"
            )

            val respuesta = api.actualizarFotoUsuario(
                idUsuario,
                parteFoto
            )

            if (respuesta.isSuccessful) {
                Log.d(
                    "USUARIO_REPOSITORY",
                    "Foto actualizada correctamente"
                )
                respuesta.body()
            } else {
                Log.e(
                    "USUARIO_REPOSITORY",
                    "Error al subir foto. HTTP ${respuesta.code()}: " +
                            "${respuesta.errorBody()?.string()}"
                )
                null
            }
        } catch (e: Exception) {
            Log.e(
                "USUARIO_REPOSITORY",
                "Error al subir la foto de perfil",
                e
            )
            null
        } finally {
            archivoTemporal?.delete()
        }
    }

    // =========================================================
    // ELIMINAR FOTO DE PERFIL
    // =========================================================

    suspend fun eliminarFotoUsuario(
        idUsuario: Int
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(
                "USUARIO_REPOSITORY",
                "Eliminando foto del usuario $idUsuario"
            )

            val respuesta = api.eliminarFotoUsuario(idUsuario)

            if (respuesta.isSuccessful) {
                Log.d(
                    "USUARIO_REPOSITORY",
                    "Foto eliminada correctamente"
                )
                true
            } else {
                Log.e(
                    "USUARIO_REPOSITORY",
                    "Error HTTP ${respuesta.code()}: " +
                            "${respuesta.errorBody()?.string()}"
                )
                false
            }
        } catch (e: Exception) {
            Log.e(
                "USUARIO_REPOSITORY",
                "Error al eliminar foto de perfil",
                e
            )
            false
        }
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    suspend fun cambiarContrasena(
        datos: CambiarContrasenaRequest
    ): Response<RespuestaMensaje> =
        withContext(Dispatchers.IO) {
            api.cambiarContrasena(datos)
        }
}
