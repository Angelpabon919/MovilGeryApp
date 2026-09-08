package com.example.molvigeryapp.data.repository

import com.example.molvigeryapp.data.model.Cita

object CitasRepository {

    // ALMACENAMIENTO TEMPORAL PARA LAS CITAS

    private val listaCitas = mutableListOf<Cita>()

    private var siguienteId = 1



    // OBTENER CITAS

    fun obtenerCitas(): List<Cita> {
        return listaCitas.toList()
    }


    // AGREGAR CITA

    fun agregarCita(cita: Cita) {

        val nuevaCita = cita.copy(
            id = siguienteId
        )

        listaCitas.add(nuevaCita)

        siguienteId++
    }


    // BUSCAR CITA

    fun obtenerCitaPorId(
        id: Int
    ): Cita? {

        return listaCitas.find {
            it.id == id
        }
    }


    // ACTUALIZAR CITA

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


    // ELIMINAR CITA

    fun eliminarCita(
        id: Int
    ) {

        listaCitas.removeAll {
            it.id == id
        }
    }
}