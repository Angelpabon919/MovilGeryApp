package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentDatosBasicosBinding

class DatosBasicosFragment : Fragment() {

    private var _binding: FragmentDatosBasicosBinding? = null
    private val binding get() = _binding!!

    // =========================================================
    // VIEWMODEL COMPARTIDO
    // =========================================================

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(
            PacienteRepository()
        )
    }

    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDatosBasicosBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    // =========================================================
    // CONFIGURAR VISTA
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        cargarPacienteDesdeArgumentos()

        observarPaciente()

        configurarGuardar()
    }

    // =========================================================
    // CARGAR PACIENTE
    // =========================================================

    private fun cargarPacienteDesdeArgumentos() {

        val pacienteArgs =
            arguments?.getSerializable(
                "paciente_data"
            ) as? Paciente

        pacienteArgs?.let { paciente ->

            viewModel.seleccionarPaciente(
                paciente
            )
        }
    }

    // =========================================================
    // OBSERVAR PACIENTE
    // =========================================================

    private fun observarPaciente() {

        viewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            paciente?.let {

                binding.etTipoDocumento.setText(
                    it.tipo_documento ?: ""
                )

                binding.etNumeroDocumento.setText(
                    it.numero_documento ?: ""
                )

                binding.etEps.setText(
                    it.eps ?: ""
                )

                binding.etSede.setText(
                    it.sede ?: ""
                )

                binding.etHabitacion.setText(
                    it.habitacion?.toString() ?: ""
                )

                binding.etCama.setText(
                    it.cama?.toString() ?: ""
                )

                binding.etGrupoSanguineo.setText(
                    "${it.grupo_sanguineo ?: ""}${it.rh ?: ""}"
                )
            }
        }
    }

    // =========================================================
    // GUARDAR CAMBIOS
    // =========================================================

    private fun configurarGuardar() {

        binding.btnGuardarDatos.setOnClickListener {

            val pacienteActual =
                viewModel.pacienteSeleccionado.value

            if (pacienteActual == null) {

                Toast.makeText(
                    requireContext(),
                    "No se encontró el paciente para actualizar",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val pacienteModificado =
                pacienteActual.copy(

                    tipo_documento =
                        binding.etTipoDocumento
                            .text
                            .toString()
                            .trim(),

                    numero_documento =
                        binding.etNumeroDocumento
                            .text
                            .toString()
                            .trim(),

                    eps =
                        binding.etEps
                            .text
                            .toString()
                            .trim(),

                    sede =
                        binding.etSede
                            .text
                            .toString()
                            .trim(),

                    habitacion =
                        binding.etHabitacion
                            .text
                            .toString()
                            .trim()
                            .toIntOrNull(),

                    cama =
                        binding.etCama
                            .text
                            .toString()
                            .trim()
                            .toIntOrNull()
                )

            // Actualizamos el paciente dentro del ViewModel
            viewModel.seleccionarPaciente(
                pacienteModificado
            )

            Toast.makeText(
                requireContext(),
                "Cambios guardados correctamente",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================================================
    // DESTRUIR BINDING
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}