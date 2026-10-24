
package com.example.molvigeryapp.ui.cuidador.camara

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.Camara
import com.example.molvigeryapp.data.model.Habitacion
import com.example.molvigeryapp.data.repository.EventoIaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MonitoreoCamarasViewModel : ViewModel() {

    private val repository = EventoIaRepository()

    private val _camaras = MutableLiveData<List<Camara>>(emptyList())
    val camaras: LiveData<List<Camara>> = _camaras

    private val _habitaciones =
        MutableLiveData<List<Habitacion>>(emptyList())
    val habitaciones: LiveData<List<Habitacion>> = _habitaciones

    private val _cargando = MutableLiveData(false)
    val cargando: LiveData<Boolean> = _cargando

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun cargarDatos() {
        if (_cargando.value == true) return

        viewModelScope.launch {
            _cargando.value = true
            _error.value = null

            try {
                val listaHabitaciones = repository.obtenerHabitaciones()
                val listaCamaras = repository.obtenerCamaras()

                _habitaciones.value = listaHabitaciones
                _camaras.value = listaCamaras

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value =
                    "No se pudieron cargar las cámaras y habitaciones."
            } finally {
                _cargando.value = false
            }
        }
    }
}
