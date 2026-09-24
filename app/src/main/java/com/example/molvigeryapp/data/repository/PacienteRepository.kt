package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AplicacionResponse
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.TipoInsumo
import com.example.molvigeryapp.data.model.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class PacienteRepository {

    private val api = RetrofitClient.apiService

    suspend fun obtenerPacientes(): List<Paciente> =
        withContext(Dispatchers.IO) {
            api.getPacientes()
        }

    suspend fun obtenerPacientePorId(id: Int): Paciente =
        withContext(Dispatchers.IO) {
            api.getPacienteById(id)
        }

    suspend fun getRecomendaciones(
        idPaciente: Int
    ): List<Recomendacion>? =
        withContext(Dispatchers.IO) {
            try {
                api.getRecomendacionesPorPaciente(idPaciente)
            } catch (e: Exception) {
                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener recomendaciones",
                    e
                )
                null
            }
        }

    suspend fun guardarRecomendacion(
        recomendacion: Recomendacion
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                api.guardarRecomendacion(recomendacion)
                true
            } catch (e: Exception) {
                android.util.Log.e(
                    "API_ERROR",
                    "Error al guardar recomendación",
                    e
                )
                false
            }
        }

    suspend fun guardarCuidadoEnfermeria(
        cuidado: CuidadoEnfermeria
    ): Boolean =
        withContext(Dispatchers.IO) {
            try {
                api.guardarCuidadoEnfermeria(cuidado)
                true
            } catch (e: Exception) {
                android.util.Log.e(
                    "API_ERROR",
                    "Error al guardar cuidado de enfermería",
                    e
                )
                false
            }
        }

    suspend fun getCuidadosPorPaciente(
        idPaciente: Int
    ): List<CuidadoEnfermeria>? =
        withContext(Dispatchers.IO) {
            try {
                api.getCuidadosPorPaciente(idPaciente)
            } catch (e: Exception) {
                android.util.Log.e(
                    "API_ERROR",
                    "Error al obtener cuidados de enfermería",
                    e
                )
                null
            }
        }

    suspend fun guardarAsignacion(
        asignacion: AsignacionPacienteCuidador
    ): Boolean {
        return try {
            val respuesta = api.guardarAsignacion(asignacion)
            respuesta.isSuccessful
        } catch (e: Exception) {
            android.util.Log.e(
                "API_ERROR",
                "Error en la petición POST de asignación",
                e
            )
            false
        }
    }

    suspend fun obtenerAsignacionesPacienteCuidador():
            List<AsignacionPacienteCuidador> {

        return try {
            val respuesta =
                api.getAsignacionesPacienteCuidador()

            if (respuesta.isSuccessful) {
                respuesta.body() ?: emptyList()
            } else {
                android.util.Log.e(
                    "API_ERROR",
                    "Error HTTP ${respuesta.code()} al obtener asignaciones"
                )
                emptyList()
            }

        } catch (e: Exception) {
            android.util.Log.e(
                "API_ERROR",
                "Error al obtener asignaciones de pacientes",
                e
            )
            emptyList()
        }
    }
    suspend fun getElementosPorPaciente(
        idPaciente: Int
    ): List<ElementoPaciente>? =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.getElementosPorPaciente(idPaciente)
                if (respuesta.isSuccessful) {
                    val listaCompleta = respuesta.body() ?: emptyList()
                    listaCompleta.filter {
                        it.idPaciente == idPaciente
                    }
                } else {
                    android.util.Log.e("API_ERROR", "Error HTTP ${respuesta.code()} al obtener elementos")
                    emptyList() // <--- OJO AQUÍ
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Error al obtener elementos", e)
                emptyList() // <--- OJO AQUÍ
            }
        }

    suspend fun guardarElementoPaciente(
        elemento: ElementoPaciente
    ): Boolean =
        withContext(Dispatchers.IO) {

            try {
                val respuesta =
                    api.guardarElementoPaciente(elemento)

                if (respuesta.isSuccessful) {
                    true
                } else {
                    android.util.Log.e(
                        "API_ERROR",
                        "Error al guardar elemento ${respuesta.code()}: ${respuesta.errorBody()?.string()}"
                    )
                    false
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Excepción al guardar elemento",
                    e
                )

                false
            }
        }

    suspend fun getMedicamentos(): List<Medicamento>? =
        withContext(Dispatchers.IO) {

            try {
                val respuesta = api.getMedicamentos()

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    null
                }

            } catch (e: Exception) {
                null
            }
        }

    suspend fun getTiposInsumos(): List<TipoInsumo>? =
        withContext(Dispatchers.IO) {

            try {
                val respuesta = api.getTiposInsumos()

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    android.util.Log.e(
                        "INSUMOS_DEBUG",
                        "Error Tipos HTTP ${respuesta.code()}: ${respuesta.errorBody()?.string()}"
                    )
                    null
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "INSUMOS_DEBUG",
                    "Excepción al cargar tipos de insumo",
                    e
                )

                null
            }
        }

    suspend fun getInsumosPorTipo(
        idTipo: Int
    ): List<Insumo>? =
        withContext(Dispatchers.IO) {

            try {
                val respuesta =
                    api.getInsumosPorTipo(idTipo)

                if (respuesta.isSuccessful) {
                    respuesta.body()
                } else {
                    android.util.Log.e(
                        "INSUMOS_DEBUG",
                        "Error Insumos HTTP ${respuesta.code()}: ${respuesta.errorBody()?.string()}"
                    )
                    null
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "INSUMOS_DEBUG",
                    "Excepción al cargar insumos por tipo",
                    e
                )

                null
            }
        }

    suspend fun obtenerUsuarioPorId(
        idUsuario: Int
    ): Usuario? =
        withContext(Dispatchers.IO) {

            try {
                android.util.Log.d(
                    "PERFIL_ENCARGADO",
                    "Consultando GET /usuarios/$idUsuario/"
                )

                val usuario =
                    api.getUsuarioById(idUsuario)

                android.util.Log.d(
                    "PERFIL_ENCARGADO",
                    "Usuario recibido correctamente"
                )

                android.util.Log.d(
                    "PERFIL_ENCARGADO",
                    "ID: ${usuario.idUsuario}"
                )

                android.util.Log.d(
                    "PERFIL_ENCARGADO",
                    "Nombres: ${usuario.nombres}"
                )

                android.util.Log.d(
                    "PERFIL_ENCARGADO",
                    "Apellidos: ${usuario.apellidos}"
                )

                android.util.Log.d(
                    "PERFIL_ENCARGADO",
                    "Correo: ${usuario.correo}"
                )

                android.util.Log.d(
                    "PERFIL_ENCARGADO",
                    "Teléfono: ${usuario.telefono}"
                )

                usuario

            } catch (e: HttpException) {

                android.util.Log.e(
                    "PERFIL_ENCARGADO",
                    "ERROR HTTP ${e.code()} al consultar /usuarios/$idUsuario/",
                    e
                )

                null

            } catch (e: Exception) {

                android.util.Log.e(
                    "PERFIL_ENCARGADO",
                    "ERROR DE CONEXIÓN al consultar /usuarios/$idUsuario/: ${e.message}",
                    e
                )

                null
            }
        }

    suspend fun cambiarContrasena(
        idUsuario: Int,
        contrasenaActual: String,
        nuevaContrasena: String
    ): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {

            try {

                val datos = mapOf(
                    "id_usuario" to idUsuario,
                    "contrasena_actual" to contrasenaActual,
                    "nueva_contrasena" to nuevaContrasena
                )

                val respuesta =
                    api.cambiarContrasena(datos)

                if (respuesta.isSuccessful) {

                    val mensaje =
                        respuesta.body()
                            ?.get("mensaje")
                            ?: "Contraseña actualizada correctamente."

                    Pair(true, mensaje)

                } else {

                    android.util.Log.e(
                        "API_ERROR",
                        "Error HTTP ${respuesta.code()} al cambiar contraseña"
                    )

                    Pair(
                        false,
                        "No se pudo actualizar la contraseña."
                    )
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "API_ERROR",
                    "Error al cambiar contraseña",
                    e
                )

                Pair(
                    false,
                    "Error de conexión con el servidor."
                )
            }
        }
    suspend fun registrarAplicacionMedicamento(
        request: AplicacionRequest
    ): Result<AplicacionResponse> =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api.registrarAplicacion(request)
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    Result.success(respuesta.body()!!)
                } else {
                    val errorMsg = respuesta.errorBody()?.string() ?: "Error al registrar aplicación"
                    android.util.Log.e("API_ERROR", "Error HTTP ${respuesta.code()}: $errorMsg")
                    Result.failure(Exception(errorMsg))
                }
            } catch (e: Exception) {
                android.util.Log.e("API_ERROR", "Excepción al registrar aplicación", e)
                Result.failure(e)
            }
        }
} // <--- Esta es la última llave del PacienteRepository

