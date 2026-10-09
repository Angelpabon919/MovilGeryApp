
package com.example.molvigeryapp.ui.cuidador.camara
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.EventoIa
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


    private var actualizacionJob: Job? = null

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


    override fun onCleared() {
        detenerActualizacionAutomatica()
        super.onCleared()
    }
}
