package com.example.molvigeryapp.ui.cuidador.agenda

import android.util.Log
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


    fun cargarAgenda(idUsuario: Int) {

        viewModelScope.launch {

            _cargando.value = true
            _error.value = null

            try {

                // --------------------------------------------------
                // 1. VALIDAR USUARIO
                // --------------------------------------------------

                if (idUsuario <= 0) {

                    Log.e(
                        "AGENDA_ERROR",
                        "No se encontró un ID_USUARIO válido."
                    )

                    _pacientesConCitas.value = emptyList()
                    _citas.value = emptyList()

                    _error.value =
                        "No se pudo identificar al cuidador."

                    return@launch
                }


                Log.d(
                    "AGENDA_DEBUG",
                    "======================================"
                )

                Log.d(
                    "AGENDA_DEBUG",
                    "ID DEL CUIDADOR ACTUAL: $idUsuario"
                )

                Log.d(
                    "AGENDA_DEBUG",
                    "======================================"
                )


                // --------------------------------------------------
                // 2. OBTENER TODAS LAS ASIGNACIONES
                // --------------------------------------------------

                val asignaciones =
                    repository.obtenerAsignacionesPacienteCuidador()

                Log.d(
                    "AGENDA_DEBUG",
                    "TOTAL DE ASIGNACIONES RECIBIDAS: ${asignaciones.size}"
                )


                // --------------------------------------------------
                // 3. MOSTRAR TODAS LAS ASIGNACIONES
                // --------------------------------------------------

                asignaciones.forEach { asignacion ->

                    Log.d(
                        "AGENDA_DEBUG",
                        """
                        ASIGNACION ->
                        idAsignacion=${asignacion.idAsignacion}
                        idUsuario=${asignacion.idUsuario}
                        idPaciente=${asignacion.idPaciente}
                        estado=${asignacion.estado}
                        fechaInicio=${asignacion.fechaInicio}
                        fechaFin=${asignacion.fechaFin}
                        """.trimIndent()
                    )
                }


                // --------------------------------------------------
                // 4. FILTRAR ASIGNACIONES DEL CUIDADOR ACTUAL
                //    Y QUE ESTEN ACTIVAS
                // --------------------------------------------------

                val asignacionesDelCuidador =
                    asignaciones.filter { asignacion ->

                        asignacion.idUsuario == idUsuario &&
                                asignacion.estado.equals(
                                    "Activo",
                                    ignoreCase = true
                                )
                    }


                Log.d(
                    "AGENDA_DEBUG",
                    "======================================"
                )

                Log.d(
                    "AGENDA_DEBUG",
                    "ASIGNACIONES ACTIVAS DEL CUIDADOR: " +
                            asignacionesDelCuidador.size
                )


                asignacionesDelCuidador.forEach { asignacion ->

                    Log.d(
                        "AGENDA_DEBUG",
                        "ACTIVA -> " +
                                "idAsignacion=${asignacion.idAsignacion}, " +
                                "idPaciente=${asignacion.idPaciente}, " +
                                "estado=${asignacion.estado}"
                    )
                }


                // --------------------------------------------------
                // 5. OBTENER IDS DE PACIENTES ASIGNADOS
                // --------------------------------------------------

                val idsPacientesSeleccionados =
                    asignacionesDelCuidador
                        .map { it.idPaciente }
                        .toSet()


                Log.d(
                    "AGENDA_DEBUG",
                    "======================================"
                )

                Log.d(
                    "AGENDA_DEBUG",
                    "IDS DE PACIENTES SELECCIONADOS: " +
                            idsPacientesSeleccionados
                )

                Log.d(
                    "AGENDA_DEBUG",
                    "CANTIDAD DE PACIENTES SELECCIONADOS: " +
                            idsPacientesSeleccionados.size
                )


                // --------------------------------------------------
                // 6. OBTENER PACIENTES
                // --------------------------------------------------

                val pacientes =
                    repository.obtenerPacientes()


                Log.d(
                    "AGENDA_DEBUG",
                    "TOTAL DE PACIENTES RECIBIDOS: " +
                            pacientes.size
                )


                // --------------------------------------------------
                // 7. OBTENER CITAS
                // --------------------------------------------------

                val todasLasCitas =
                    repository.obtenerCitas()


                Log.d(
                    "AGENDA_DEBUG",
                    "TOTAL DE CITAS RECIBIDAS: " +
                            todasLasCitas.size
                )


                // --------------------------------------------------
                // 8. MOSTRAR TODAS LAS CITAS
                // --------------------------------------------------

                todasLasCitas.forEach { cita ->

                    Log.d(
                        "AGENDA_DEBUG",
                        """
                        CITA ->
                        id=${cita.idCita}
                        paciente=${cita.idPaciente}
                        fecha=${cita.fecha}
                        hora=${cita.hora}
                        estado=${cita.estado}
                        """.trimIndent()
                    )
                }


                // --------------------------------------------------
                // 9. FILTRAR CITAS DE LOS PACIENTES SELECCIONADOS
                // --------------------------------------------------

                val citasDelCuidador =
                    todasLasCitas.filter { cita ->

                        cita.idPaciente != null &&
                                cita.idPaciente in
                                idsPacientesSeleccionados
                    }


                Log.d(
                    "AGENDA_DEBUG",
                    "======================================"
                )

                Log.d(
                    "AGENDA_DEBUG",
                    "CITAS DE PACIENTES SELECCIONADOS: " +
                            citasDelCuidador.size
                )


                // --------------------------------------------------
                // 10. GUARDAR CITAS
                // --------------------------------------------------

                _citas.value =
                    citasDelCuidador


                // --------------------------------------------------
                // 11. OBTENER PACIENTES QUE TIENEN CITAS
                // --------------------------------------------------

                val idsPacientesConCitas =
                    citasDelCuidador
                        .mapNotNull { it.idPaciente }
                        .toSet()


                Log.d(
                    "AGENDA_DEBUG",
                    "IDS DE PACIENTES CON CITAS: " +
                            idsPacientesConCitas
                )


                // --------------------------------------------------
                // 12. FILTRAR PACIENTES PARA MOSTRAR EN AGENDA
                // --------------------------------------------------

                val pacientesFiltrados =
                    pacientes.filter { paciente ->

                        paciente.idPaciente != null &&

                                paciente.idPaciente in
                                idsPacientesSeleccionados &&

                                paciente.idPaciente in
                                idsPacientesConCitas
                    }


                // --------------------------------------------------
                // 13. MOSTRAR RESULTADO FINAL
                // --------------------------------------------------

                Log.d(
                    "AGENDA_DEBUG",
                    "======================================"
                )

                Log.d(
                    "AGENDA_DEBUG",
                    "PACIENTES MOSTRADOS EN AGENDA: " +
                            pacientesFiltrados.size
                )


                pacientesFiltrados.forEach { paciente ->

                    Log.d(
                        "AGENDA_DEBUG",
                        "PACIENTE MOSTRADO -> " +
                                "idPaciente=${paciente.idPaciente}"
                    )
                }


                Log.d(
                    "AGENDA_DEBUG",
                    "======================================"
                )


                // --------------------------------------------------
                // 14. ACTUALIZAR LIVE DATA
                // --------------------------------------------------

                _pacientesConCitas.value =
                    pacientesFiltrados

            } catch (e: Exception) {

                Log.e(
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


    // ------------------------------------------------------
    // OBTENER CITAS DE UN PACIENTE
    // ------------------------------------------------------

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
                    it.fecha

                }.thenBy {
                    it.hora
                }
            )
    }
}