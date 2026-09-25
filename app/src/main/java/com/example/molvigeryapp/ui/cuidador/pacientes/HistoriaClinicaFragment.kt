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
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentHistoriaClinicaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HistoriaClinicaFragment : Fragment() {

    private var _binding: FragmentHistoriaClinicaBinding? = null
    private val binding get() = _binding!!

    // Usamos el mismo ViewModel que guarda el paciente seleccionado
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

        configurarBotonVolver()
        observarPaciente()
    }

    private fun configurarBotonVolver() {

        binding.btnVolver.setOnClickListener {
            requireActivity()
                .onBackPressedDispatcher
                .onBackPressed()
        }
    }

    private fun observarPaciente() {

        pacienteViewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            paciente?.let {

                idPaciente = it.idPaciente

                val nombreCompleto =
                    "${it.nombre ?: ""} ${it.apellido ?: ""}".trim()

                binding.tvNombrePaciente.text =
                    if (nombreCompleto.isNotEmpty()) {
                        nombreCompleto
                    } else {
                        "Paciente sin nombre"
                    }

                cargarHistoriaClinica()
            }
        }
    }

    private fun cargarHistoriaClinica() {

        val pacienteId = idPaciente

        if (pacienteId == null) {
            Toast.makeText(
                requireContext(),
                "No se encontró el paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        lifecycleScope.launch {

            try {

                val historias =
                    RetrofitClient.apiService.getHistoriasClinicas()

                // Buscamos la historia que pertenece al paciente seleccionado
                val historia = historias.find {
                    it.idPaciente == pacienteId
                }

                if (historia != null) {

                    mostrarHistoriaClinica(historia)

                } else {

                    Toast.makeText(
                        requireContext(),
                        "Este paciente no tiene historia clínica registrada",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "HISTORIA_CLINICA",
                    "Error al cargar la historia clínica",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "Error al cargar la historia clínica",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun mostrarHistoriaClinica(
        historia: com.example.molvigeryapp.data.model.HistoriaClinica
    ) {

        // Número de historia clínica
        binding.tvNumeroHistoria.text =
            "Historia clínica #${historia.idHistoriaClinica ?: "-"}"

        // Fecha de apertura
        binding.tvFechaApertura.text =
            formatearFecha(historia.fechaApertura)

        // Antecedentes
        binding.tvAntecedentes.text =
            historia.antecedentes.ifBlank { "Ninguna" }

        // Alergias
        binding.tvAlergias.text =
            historia.alergias.ifBlank { "Ninguna" }

        // Observaciones
        binding.tvObservaciones.text =
            historia.observaciones.ifBlank { "Ninguna" }
    }

    private fun formatearFecha(
        fecha: String
    ): String {

        return try {

            val formatoEntrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.US
            )

            val formatoSalida = SimpleDateFormat(
                "d 'de' MMMM 'de' yyyy",
                Locale("es", "ES")
            )

            val fechaConvertida =
                formatoEntrada.parse(fecha)

            if (fechaConvertida != null) {
                formatoSalida.format(fechaConvertida)
            } else {
                fecha
            }

        } catch (e: Exception) {

            fecha
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}