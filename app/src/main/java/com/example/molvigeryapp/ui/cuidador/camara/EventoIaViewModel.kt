
package com.example.molvigeryapp.ui.cuidador.camara
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.Camara
import com.example.molvigeryapp.data.model.EventoIa
import com.example.molvigeryapp.data.model.Habitacion
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.TipoEventoIa
import com.example.molvigeryapp.data.repository.EventoIaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class EventoIaViewModel : ViewModel() {

    private val repository = EventoIaRepository()

    private val _eventos = MutableLiveData<List<EventoIa>>(emptyList())
    val eventos: LiveData<List<EventoIa>> = _eventos

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error


    private val _tiposEventos =
        MutableLiveData<List<TipoEventoIa>>(emptyList())

    val tiposEventos: LiveData<List<TipoEventoIa>> =
        _tiposEventos

    private val _pacientes = MutableLiveData<List<Paciente>>(emptyList())

    val pacientes: LiveData<List<Paciente>> = _pacientes


    private var pacientesCargados = false
    private var cargandoPacientes = false
    private var actualizacionJob: Job? = null

    // =====================================================
// CÁMARAS OBTENIDAS DESDE EL BACKEND
// =====================================================

    private val _camaras = MutableLiveData<List<Camara>>(emptyList())

    val camaras: LiveData<List<Camara>> = _camaras

    private var camarasCargadas = false
    private var cargandoCamaras = false

    // =====================================================
// HABITACIONES OBTENIDAS DESDE EL BACKEND
// =====================================================

    private val _habitaciones =
        MutableLiveData<List<Habitacion>>(emptyList())

    val habitaciones: LiveData<List<Habitacion>> = _habitaciones

    private var habitacionesCargadas = false
    private var cargandoHabitaciones = false

    fun obtenerEventos() {
        viewModelScope.launch {
            cargarEventos()
        }
    }

    private suspend fun cargarEventos() {
        _cargando.value = true

        try {
            val respuesta = repository.obtenerEventos()

            if (respuesta.isSuccessful) {
                val lista = respuesta.body().orEmpty()


                _eventos.value = lista.sortedByDescending {
                    it.fecha_hora
                }

                _error.value = null
            } else {
                _error.value =
                    "Error al obtener eventos: ${respuesta.code()}"
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _error.value =
                "Error de conexión: ${e.localizedMessage}"
        } finally {
            _cargando.value = false
        }
    }

    fun iniciarActualizacionAutomatica() {
        if (actualizacionJob?.isActive == true) return

        actualizacionJob = viewModelScope.launch {
            while (isActive) {
                cargarEventos()
                delay(5000L)
            }
        }
    }

    fun detenerActualizacionAutomatica() {
        actualizacionJob?.cancel()
        actualizacionJob = null
    }

    fun obtenerTiposEventos() {
        viewModelScope.launch {
            try {
                val respuesta = repository.obtenerTiposEventos()

                if (respuesta.isSuccessful) {
                    _tiposEventos.value = respuesta.body().orEmpty()
                } else {
                    _error.value =
                        "Error al obtener tipos: ${respuesta.code()}"
                }

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value =
                    "Error de conexión: ${e.localizedMessage}"
            }
        }
    }
    fun obtenerPacientes() {

        // No repetir la consulta si ya se realizó
        if (pacientesCargados || cargandoPacientes) return

        cargandoPacientes = true

        viewModelScope.launch {

            try {

                // Reutilizamos GET pacientes/
                val lista = repository.obtenerPacientes()

                _pacientes.value = lista

                pacientesCargados = true

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _error.value =
                    "Error al obtener pacientes: ${e.localizedMessage}"

            } finally {

                cargandoPacientes = false
            }
        }
    }

    // =====================================================
    // OBTENER CÁMARAS DESDE EL BACKEND
    // =====================================================

    fun obtenerCamaras() {

        // Evitar consultas repetidas
        if (camarasCargadas || cargandoCamaras) return

        cargandoCamaras = true

        viewModelScope.launch {

            try {

                val lista = repository.obtenerCamaras()

                _camaras.value = lista

                camarasCargadas = true

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _error.value =
                    "Error al obtener cámaras: ${e.localizedMessage}"

            } finally {

                cargandoCamaras = false
            }
        }
    }

    // =====================================================
    // OBTENER HABITACIONES DESDE EL BACKEND
    // =====================================================

    fun obtenerHabitaciones() {

        // Evitar consultas repetidas
        if (habitacionesCargadas || cargandoHabitaciones) return

        cargandoHabitaciones = true

        viewModelScope.launch {

            try {

                // Consultar GET habitaciones/
                val lista = repository.obtenerHabitaciones()

                _habitaciones.value = lista

                habitacionesCargadas = true

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                _error.value =
                    "Error al obtener habitaciones: ${e.localizedMessage}"

            } finally {

                cargandoHabitaciones = false
            }
        }
    }




    override fun onCleared() {
        detenerActualizacionAutomatica()
        super.onCleared()
    }
}
