package com.example.molvigeryapp.ui.cuidador.agenda

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import kotlinx.coroutines.launch

class AgendaViewModel(
    private val repository: PacienteRepository
) : ViewModel() {

    private val _pacientesConCitas =
        MutableLiveData<List<Paciente>>()

    val pacientesConCitas: LiveData<List<Paciente>>
        get() = _pacientesConCitas

    private val _citas =
        MutableLiveData<List<Cita>>()

    val citas: LiveData<List<Cita>>
        get() = _citas

    private val _cargando =
        MutableLiveData<Boolean>()

    val cargando: LiveData<Boolean>
        get() = _cargando

    private val _error =
        MutableLiveData<String?>()

    val error: LiveData<String?>
        get() = _error

    fun cargarAgenda() {

        viewModelScope.launch {

            _cargando.value = true
            _error.value = null

            try {

                // Obtener pacientes
                val pacientes =
                    repository.obtenerPacientes()

                // Obtener citas
                val citas =
                    repository.obtenerCitas()

                // Guardamos todas las citas
                _citas.value = citas

                // IDs de pacientes que tienen al menos una cita
                val idsPacientesConCitas =
                    citas
                        .mapNotNull { it.idPaciente }
                        .toSet()

                // Filtramos únicamente pacientes
                // que tienen citas
                val pacientesFiltrados =
                    pacientes.filter { paciente ->

                        paciente.idPaciente != null &&
                                paciente.idPaciente in idsPacientesConCitas

                    }

                _pacientesConCitas.value =
                    pacientesFiltrados

            } catch (e: Exception) {

                android.util.Log.e(
                    "AGENDA_ERROR",
                    "Error al cargar agenda",
                    e
                )

                _pacientesConCitas.value =
                    emptyList()

                _citas.value =
                    emptyList()

                _error.value =
                    "No se pudo cargar la agenda."

            } finally {

                _cargando.value = false
            }
        }
    }

    fun obtenerCitasDePaciente(
        idPaciente: Int
    ): List<Cita> {

        val lista =
            _citas.value ?: emptyList()

        return lista
            .filter {
                it.idPaciente == idPaciente
            }
            .sortedWith(
                compareBy<Cita> {

                    // Pendientes primero
                    if (
                        it.estado.equals(
                            "completada",
                            ignoreCase = true
                        )
                    ) {
                        1
                    } else {
                        0
                    }

                }.thenBy {

                    // Luego ordenamos por fecha
                    it.fecha

                }.thenBy {

                    // Y finalmente por hora
                    it.hora
                }
            )
    }
}