package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.CitaApiResponse
import com.example.molvigeryapp.data.model.CrearCitaRequest
import com.example.molvigeryapp.data.model.Paciente

object CitasRepository {

    // ALMACENAMIENTO TEMPORAL PARA LAS CITAS EN MEMORIA
    private val listaCitas = mutableListOf<Cita>()
    private var siguienteId = 1

    // =========================================================
    // OBTENER CITAS DESDE LA API
    // =========================================================

    suspend fun obtenerCitasDesdeApi(): List<Cita> {

        try {

            val respuesta =
                RetrofitClient.apiService.getCitas()

            if (respuesta.isSuccessful) {

                val citas = respuesta.body() ?: emptyList()

                val pacientes = try {
                    RetrofitClient.apiService.getPacientes()
                } catch (e: Exception) {
                    emptyList()
                }

                return citas.map { cita ->
                    if (cita.nombrePaciente.isNullOrBlank() && cita.idPaciente != null) {
                        val paciente = pacientes.firstOrNull { it.idPaciente == cita.idPaciente }
                        if (paciente != null) {
                            cita.copy(
                                nombrePaciente = "${paciente.nombre} ${paciente.apellido}".trim(),
                                habitacion = paciente.habitacion,
                                cama = paciente.cama
                            )
                        } else {
                            cita
                        }
                    } else {
                        cita
                    }
                }

            } else {

                android.util.Log.e(
                    "API_ERROR",
                    "Error HTTP ${respuesta.code()} al obtener citas"
                )

                return emptyList()
            }

        } catch (e: Exception) {

            android.util.Log.e(
                "API_ERROR",
                "Excepción al obtener citas: ${e.message}",
                e
            )

            return emptyList()
        }
    }

    // =========================================================
    // MÉTODOS EN MEMORIA LOCAL
    // =========================================================

    fun obtenerCitas(): List<Cita> {
        return listaCitas.toList()
    }

    fun agregarCita(cita: Cita) {
        val nuevaCita = cita.copy(
            id = siguienteId.toString()
        )
        listaCitas.add(nuevaCita)
        siguienteId++
    }

    fun obtenerCitaPorId(
        id: String?
    ): Cita? {
        return listaCitas.find {
            it.id == id
        }
    }

    fun obtenerCitaPorId(
        id: Int
    ): Cita? = obtenerCitaPorId(id.toString())

    fun actualizarCita(
        cita: Cita
    ) {
        val posicion =
            listaCitas.indexOfFirst {
                it.id == cita.id
            }

        if (posicion != -1) {
            listaCitas[posicion] = cita
        }
    }

    fun eliminarCita(
        id: String?
    ) {
        listaCitas.removeAll {
            it.id == id
        }
    }

    fun eliminarCita(
        id: Int
    ) = eliminarCita(id.toString())


    // =========================================================
    // CREAR CITA EN API
    // =========================================================

    suspend fun crearCita(
        cita: CrearCitaRequest
    ): Result<CitaApiResponse> {

        return try {

            val respuesta =
                RetrofitClient.apiService.crearCita(cita)

            if (respuesta.isSuccessful) {

                val cuerpo = respuesta.body()

                if (cuerpo != null) {
                    Result.success(cuerpo)
                } else {
                    Result.failure(
                        Exception("El servidor no devolvió la información de la cita")
                    )
                }

            } else {

                Result.failure(
                    Exception("Error HTTP ${respuesta.code()}")
                )
            }

        } catch (e: Exception) {

            Result.failure(e)
        }
    }
}
