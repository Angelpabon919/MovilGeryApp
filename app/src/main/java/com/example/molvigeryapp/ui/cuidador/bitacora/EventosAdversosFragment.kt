package com.example.molvigeryapp.ui.cuidador.bitacora

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.SignosVitales
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentEventosAdversosBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventosAdversosFragment : Fragment() {

    private var _binding: FragmentEventosAdversosBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var idPaciente: Int? = null

    /*
     * ESTE ID QUEDA PREPARADO PARA RECIBIRLO
     * CUANDO TU COMPAÑERA CREE LA BITÁCORA.
     */
    private var idBitacora: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEventosAdversosBinding.inflate(
            inflater,
            container,
            false
        )

        /*
         * Si en algún momento el fragment recibe
         * el ID de la bitácora por arguments,
         * lo tomamos automáticamente.
         */
        idBitacora = arguments?.getInt("ID_BITACORA")

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        observarPaciente()
        configurarBotones()
    }

    // =========================================================
    // PACIENTE SELECCIONADO
    // =========================================================

    private fun observarPaciente() {

        pacienteViewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            paciente?.let {

                idPaciente = it.idPaciente

                android.util.Log.d(
                    "EVENTO_ADVERSO",
                    "Paciente seleccionado: ${it.idPaciente}"
                )
            }
        }
    }

    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        binding.btnGuardar.setOnClickListener {
            guardarEvento()
        }

        binding.btnCancelar.setOnClickListener {
            limpiarFormulario()
        }
    }

    // =========================================================
    // FECHA Y HORA
    // =========================================================

    private fun obtenerFechaHoraActual(): String {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        return formato.format(Date())
    }

    // =========================================================
    // GUARDAR EVENTO
    // =========================================================

    private fun guardarEvento() {

        /*
         * Primero verificamos la bitácora.
         *
         * Por ahora tu compañera todavía no la crea,
         * así que no podremos enviar el evento hasta
         * tener este ID.
         */
        if (idBitacora == null) {

            Toast.makeText(
                requireContext(),
                "Aún no se ha creado la bitácora",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val pacienteId = idPaciente

        if (pacienteId == null) {

            Toast.makeText(
                requireContext(),
                "No se encontró el paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // =====================================================
        // CAMPOS DEL EVENTO
        // =====================================================

        val descripcion =
            binding.etDescripcion.text.toString().trim()

        val acciones =
            binding.etAccionesRealizadas.text.toString().trim()

        val estado =
            binding.etEstado.text.toString().trim()

        // =====================================================
        // VALIDACIONES
        // =====================================================

        if (descripcion.isEmpty()) {

            binding.etDescripcion.error =
                "Ingrese una descripción"

            return
        }

        if (acciones.isEmpty()) {

            binding.etAccionesRealizadas.error =
                "Ingrese las acciones realizadas"

            return
        }

        if (estado.isEmpty()) {

            binding.etEstado.error =
                "Ingrese el estado del evento"

            return
        }

        // =====================================================
        // TIPO DE EMERGENCIA
        // =====================================================

        val idTipoEmergencia = when {

            binding.rbLeve.isChecked -> 1

            binding.rbModerado.isChecked -> 2

            binding.rbGrave.isChecked -> 3

            else -> {

                Toast.makeText(
                    requireContext(),
                    "Seleccione una evaluación",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }
        }

        // =====================================================
        // SIGNOS VITALES
        // =====================================================

        val temperatura =
            binding.etTemperatura.text.toString().trim()

        val frecuenciaCardiaca =
            binding.etFrecuencia.text.toString().trim()

        val presionSistolica =
            binding.etPresionSistolica.text.toString().trim()

        val presionDiastolica =
            binding.etPresionDiastolica.text.toString().trim()

        val frecuenciaRespiratoria =
            binding.etFrecuenciaRespiratoria.text.toString().trim()

        val saturacion =
            binding.etSaturacion.text.toString().trim()

        val peso =
            binding.etPeso.text.toString().trim()

        val observaciones =
            binding.etObservaciones.text.toString().trim()

        // =====================================================
        // FECHA
        // =====================================================

        val fechaHora = obtenerFechaHoraActual()

        // =====================================================
        // ENVIAR AL API
        // =====================================================

        lifecycleScope.launch {

            try {

                val evento = EventoAdverso(
                    idBitacora = idBitacora,
                    idEvento = null,
                    idTipoEmergencia = idTipoEmergencia,
                    fechaHora = fechaHora,
                    descripcion = descripcion,
                    accionesRealizadas = acciones,
                    estado = estado
                )
                val respuestaEvento =
                    RetrofitClient.apiService.crearEventoAdverso(
                        evento
                    )
                android.util.Log.d(
                    "EVENTO_ADVERSO",
                    "Evento creado: ${respuestaEvento.idEventoAdverso}"
                )

                val signos = SignosVitales(
                    idBitacora = idBitacora,
                    temperatura = temperatura,
                    presionSistolica = presionSistolica,
                    presionDiastolica = presionDiastolica,
                    frecuenciaCardiaca = frecuenciaCardiaca,
                    frecuenciaRespiratoria = frecuenciaRespiratoria,
                    saturacionOxigeno = saturacion,
                    peso = peso,
                    fechaHora = fechaHora,
                    observaciones = observaciones
                )
                RetrofitClient.apiService.crearSignosVitales(
                    signos
                )

                Toast.makeText(
                    requireContext(),
                    "Evento adverso guardado correctamente",
                    Toast.LENGTH_LONG
                ).show()

                limpiarFormulario()
            } catch (e: Exception) {
                android.util.Log.e(
                    "EVENTO_ADVERSO",
                    "Error al guardar evento",
                    e
                )
                Toast.makeText(
                    requireContext(),
                    "Error al guardar el evento",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun limpiarFormulario() {
        binding.etDescripcion.text.clear()
        binding.etTemperatura.text.clear()
        binding.etFrecuencia.text.clear()
        binding.etPresionSistolica.text.clear()
        binding.etPresionDiastolica.text.clear()
        binding.etFrecuenciaRespiratoria.text.clear()
        binding.etSaturacion.text.clear()
        binding.etPeso.text.clear()
        binding.etObservaciones.text.clear()
        binding.etAccionesRealizadas.text.clear()
        binding.etEstado.text.clear()
        binding.rgEvaluacion.clearCheck()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}