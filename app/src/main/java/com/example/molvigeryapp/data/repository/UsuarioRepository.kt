package com.example.molvigeryapp.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.CambiarContrasenaRequest
import com.example.molvigeryapp.data.model.RespuestaMensaje
import com.example.molvigeryapp.data.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class UsuarioRepository {

    private val api = RetrofitClient.apiService

    companion object {
        private const val TAG = "USUARIO_REPOSITORY"

        private const val CACHE_USUARIO_MS = 30_000L
        private const val CACHE_LISTA_USUARIOS_MS = 30_000L

        // Caché compartida de usuarios individuales.
        private val cacheUsuarios = mutableMapOf<Int, Usuario>()
        private val tiemposCache = mutableMapOf<Int, Long>()
        private val mutexUsuarios = Mutex()

        // Caché compartida de la lista completa.
        @Volatile
        private var listaUsuariosCache: List<Usuario>? = null

        @Volatile
        private var tiempoListaUsuariosCache = 0L

        private val mutexListaUsuarios = Mutex()

        private fun invalidarUsuario(idUsuario: Int) {
            cacheUsuarios.remove(idUsuario)
            tiemposCache.remove(idUsuario)
        }

        /**
         * Limpia las cachés al cerrar sesión.
         */
        fun limpiarCache() {
            cacheUsuarios.clear()
            tiemposCache.clear()

            listaUsuariosCache = null
            tiempoListaUsuariosCache = 0L
        }
    }

    // =========================================================
    // OBTENER TODOS LOS USUARIOS CON CACHÉ COMPARTIDA
    // =========================================================

    suspend fun obtenerUsuarios(
        forceRefresh: Boolean = false
    ): List<Usuario> = withContext(Dispatchers.IO) {

        mutexListaUsuarios.withLock {

            val ahora = System.currentTimeMillis()
            val usuariosGuardados = listaUsuariosCache

            if (
                !forceRefresh &&
                usuariosGuardados != null &&
                ahora - tiempoListaUsuariosCache < CACHE_LISTA_USUARIOS_MS
            ) {
                return@withLock usuariosGuardados
            }

            try {
                Log.d(TAG, "Consultando lista de usuarios")

                val usuarios = api.getUsuarios()

                listaUsuariosCache = usuarios.toList()
                tiempoListaUsuariosCache = System.currentTimeMillis()

                // Mantener sincronizada la caché individual con la lista recibida.
                usuarios.forEach { usuario ->
                    val id = usuario.idUsuario

                    if (id != null) {
                        cacheUsuarios[id] = usuario
                        tiemposCache[id] = tiempoListaUsuariosCache
                    }
                }

                usuarios
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener la lista de usuarios", e)

                // Si la API falla, conservar los datos previos si existen.
                usuariosGuardados ?: throw e
            }
        }
    }

    // =========================================================
    // INVALIDAR CACHÉ DE LA LISTA
    // =========================================================

    fun invalidarCacheListaUsuarios() {
        listaUsuariosCache = null
        tiempoListaUsuariosCache = 0L
    }

    // =========================================================
    // OBTENER USUARIO POR ID
    // =========================================================

    suspend fun obtenerUsuarioPorId(
        idUsuario: Int,
        forceRefresh: Boolean = false
    ): Usuario? = withContext(Dispatchers.IO) {

        mutexUsuarios.withLock {

            val ahora = System.currentTimeMillis()
            val usuarioGuardado = cacheUsuarios[idUsuario]
            val tiempoGuardado = tiemposCache[idUsuario] ?: 0L

            if (
                !forceRefresh &&
                usuarioGuardado != null &&
                ahora - tiempoGuardado < CACHE_USUARIO_MS
            ) {
                return@withLock usuarioGuardado
            }

            try {
                Log.d(TAG, "Consultando usuario $idUsuario")

                val usuario = api.getUsuarioById(idUsuario)

                cacheUsuarios[idUsuario] = usuario
                tiemposCache[idUsuario] = System.currentTimeMillis()

                usuario
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener usuario $idUsuario", e)

                usuarioGuardado
            }
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

            val respuesta = api.actualizarUsuario(idUsuario, datos)

            if (respuesta.isSuccessful) {
                val usuarioActualizado = respuesta.body()

                if (usuarioActualizado != null) {
                    mutexUsuarios.withLock {
                        cacheUsuarios[idUsuario] = usuarioActualizado
                        tiemposCache[idUsuario] = System.currentTimeMillis()
                    }
                } else {
                    invalidarCacheDeFormaSegura(idUsuario)
                }

                invalidarCacheListaUsuarios()

                usuarioActualizado
            } else {
                Log.e(
                    TAG,
                    "Error HTTP ${respuesta.code()} al actualizar usuario"
                )
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar usuario", e)
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

            val respuesta = api.actualizarFotoUsuario(
                idUsuario,
                parteFoto
            )

            if (respuesta.isSuccessful) {
                val usuarioActualizado = respuesta.body()

                if (usuarioActualizado != null) {
                    mutexUsuarios.withLock {
                        cacheUsuarios[idUsuario] = usuarioActualizado
                        tiemposCache[idUsuario] = System.currentTimeMillis()
                    }
                } else {
                    invalidarCacheDeFormaSegura(idUsuario)
                }

                invalidarCacheListaUsuarios()

                usuarioActualizado
            } else {
                Log.e(
                    TAG,
                    "Error HTTP ${respuesta.code()} al subir foto"
                )
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al subir la foto de perfil", e)
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
            val respuesta = api.eliminarFotoUsuario(idUsuario)

            if (respuesta.isSuccessful) {
                invalidarCacheDeFormaSegura(idUsuario)
                invalidarCacheListaUsuarios()
                true
            } else {
                Log.e(
                    TAG,
                    "Error HTTP ${respuesta.code()} al eliminar foto"
                )
                false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error al eliminar foto de perfil", e)
            false
        }
    }

    // =========================================================
    // CAMBIAR CONTRASEÑA
    // =========================================================

    suspend fun cambiarContrasena(
        datos: CambiarContrasenaRequest
    ): Response<RespuestaMensaje> = withContext(Dispatchers.IO) {
        api.cambiarContrasena(datos)
    }

    // =========================================================
    // INVALIDAR CACHÉ INDIVIDUAL DE FORMA SEGURA
    // =========================================================

    private suspend fun invalidarCacheDeFormaSegura(
        idUsuario: Int
    ) {
        mutexUsuarios.withLock {
            invalidarUsuario(idUsuario)
        }
    }
}