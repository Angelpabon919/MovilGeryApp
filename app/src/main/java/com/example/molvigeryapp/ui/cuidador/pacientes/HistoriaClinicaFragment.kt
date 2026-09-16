package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.HistoriaClinica
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentHistoriaClinicaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoriaClinicaFragment : Fragment() {

    private var _binding: FragmentHistoriaClinicaBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var idPaciente: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHistoriaClinicaBinding.inflate(
            inflater,
            container,
            false
        )

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

    private fun observarPaciente() {

        pacienteViewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            paciente?.let {

                idPaciente = it.idPaciente

                android.util.Log.d(
                    "HISTORIA_CLINICA",
                    "Paciente: ${it.nombre} ${it.apellido}"
                )

                android.util.Log.d(
                    "HISTORIA_CLINICA",
                    "ID Paciente: $idPaciente"
                )
            }
        }
    }

    private fun configurarBotones() {

        binding.btnGuardar.setOnClickListener {
            guardarHistoriaClinica()
        }

        binding.btnCancelar.setOnClickListener {
            limpiarFormulario()
        }
    }

    private fun obtenerFechaActual(): String {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        return formato.format(Date())
    }

    private fun guardarHistoriaClinica() {

        val pacienteId = idPaciente

        if (pacienteId == null) {

            Toast.makeText(
                requireContext(),
                "No se encontró el paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val antecedentes =
            binding.etAntecedentes.text.toString().trim()

        val alergias =
            binding.etAlergias.text.toString().trim()

        val observaciones =
            binding.etObservaciones.text.toString().trim()

        if (antecedentes.isEmpty()) {

            binding.etAntecedentes.error =
                "Ingrese los antecedentes"

            return
        }

        if (alergias.isEmpty()) {

            binding.etAlergias.error =
                "Ingrese las alergias"

            return
        }

        if (observaciones.isEmpty()) {

            binding.etObservaciones.error =
                "Ingrese las observaciones"

            return
        }

        val historia = HistoriaClinica(

            fechaApertura = obtenerFechaActual(),

            antecedentes = antecedentes,

            alergias = alergias,

            observaciones = observaciones,

            estado = true,

            idPaciente = pacienteId
        )

        lifecycleScope.launch {

            try {

                val respuesta =
                    RetrofitClient.apiService.crearHistoriaClinica(
                        historia
                    )

                android.util.Log.d(
                    "HISTORIA_CLINICA",
                    "Historia creada correctamente"
                )

                android.util.Log.d(
                    "HISTORIA_CLINICA",
                    "ID Historia: ${respuesta.idHistoriaClinica}"
                )

                android.util.Log.d(
                    "HISTORIA_CLINICA",
                    "ID Paciente: $pacienteId"
                )

                Toast.makeText(
                    requireContext(),
                    "Historia clínica guardada correctamente",
                    Toast.LENGTH_LONG
                ).show()

                limpiarFormulario()

            } catch (e: Exception) {

                android.util.Log.e(
                    "HISTORIA_CLINICA",
                    "Error al crear historia clínica",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "Error al guardar la historia clínica",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun limpiarFormulario() {

        binding.etAntecedentes.text.clear()
        binding.etAlergias.text.clear()
        binding.etObservaciones.text.clear()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}