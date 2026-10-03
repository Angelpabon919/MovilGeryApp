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

        // Obtener citas del backend
        val respuestas =
            RetrofitClient.apiService.getCitas()

        // Obtener pacientes del backend
        val pacientes =
            RetrofitClient.apiService.getPacientes()

        // Convertir las respuestas de la API
        // al modelo que utiliza la aplicación.
        return respuestas.mapIndexed { index, citaApi ->

            val paciente =
                pacientes.firstOrNull { paciente ->
                    paciente.idPaciente == citaApi.idPaciente
                }

            convertirCita(
                citaApi = citaApi,
                paciente = paciente,
                indice = index
            )
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
    // CONVERTIR CITA DE API A MODELO DE LA APP
    // =========================================================

    private fun convertirCita(
        citaApi: CitaApiResponse,
        paciente: Paciente?,
        indice: Int
    ): Cita {

        val partesMotivo =
            separarMotivo(
                citaApi.motivo
            )

        return Cita(

            // ID
            id = citaApi.idCita,

            idCita = citaApi.idCita,

            // Paciente relacionado
            idPaciente = citaApi.idPaciente,

            // Nombre del paciente
            nombrePaciente =
                if (paciente != null) {
                    "${paciente.nombre} ${paciente.apellido}".trim()
                } else {
                    "Paciente no encontrado"
                },

            // Habitación
            habitacion = paciente?.habitacion,

            // Cama
            cama = paciente?.cama,

            // Tipo de cita
            tipoCita = partesMotivo.first,

            // Especialidad
            especialidad = partesMotivo.second,

            // Fecha
            fecha = citaApi.fecha,

            // Hora
            hora = citaApi.hora,

            // Observaciones
            observaciones = citaApi.observaciones,

            // Estado
            estado = citaApi.estado,

            // Lugar
            lugar = citaApi.lugar,

            // Motivo
            motivo = citaApi.motivo,

            // Fecha Registro
            fechaRegistro = citaApi.fechaRegistro,

            // ID Usuario
            idUsuario = citaApi.idUsuario
        )
    }


    // =========================================================
    // SEPARAR MOTIVO
    // =========================================================

    private fun separarMotivo(
        motivo: String
    ): Pair<String, String> {

        if (motivo.isBlank()) {
            return Pair(
                "Sin tipo",
                "Sin especialidad"
            )
        }

        return try {

            val partesGuion =
                motivo.split(
                    " - ",
                    limit = 2
                )

            if (partesGuion.size < 2) {
                return Pair(
                    motivo.trim(),
                    "Sin especialidad"
                )
            }

            val tipoCita = partesGuion[0].trim()
            val resto = partesGuion[1].trim()

            val partesDosPuntos =
                resto.split(
                    ":",
                    limit = 2
                )

            val especialidad = partesDosPuntos[0].trim()

            Pair(
                tipoCita,
                especialidad
            )

        } catch (e: Exception) {

            Pair(
                motivo.trim(),
                "Sin especialidad"
            )
        }
    }


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
