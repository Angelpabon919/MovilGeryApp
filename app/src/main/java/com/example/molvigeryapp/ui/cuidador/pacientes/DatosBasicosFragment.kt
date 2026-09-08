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

    // Compartimos el ViewModel con la Activity o Fragment contenedor
    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDatosBasicosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Obtención correcta de argumentos (buscando en 'arguments' directo o en 'parentFragment')
        val pacienteArgs = (arguments?.getSerializable("paciente_data")
            ?: parentFragment?.arguments?.getSerializable("paciente_data")) as? Paciente

        // Si llegaron argumentos, los asignamos al ViewModel
        pacienteArgs?.let {
            viewModel.seleccionarPaciente(it)
        }

        // 2. Observamos el LiveData del ViewModel para llenar los EditText de forma reactiva
        viewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { p ->
            p?.let {
                binding.etTipoDocumento.setText(it.tipo_documento ?: "")
                binding.etNumeroDocumento.setText(it.numero_documento ?: "")
                binding.etEps.setText(it.eps ?: "")
                binding.etSede.setText(it.sede ?: "")
                binding.etHabitacion.setText(it.habitacion?.toString() ?: "")
                binding.etCama.setText(it.cama?.toString() ?: "")
                binding.etGrupoSanguineo.setText("${it.grupo_sanguineo ?: ""}${it.rh ?: ""}")
            }
        }

        // 3. Evento Guardar Cambios
        binding.btnGuardarDatos.setOnClickListener {
            val pacienteActual = viewModel.pacienteSeleccionado.value
            if (pacienteActual != null) {
                // Aquí extraes los textos modificados por el usuario
                val pacienteModificado = pacienteActual.copy(
                    tipo_documento = binding.etTipoDocumento.text.toString(),
                    numero_documento = binding.etNumeroDocumento.text.toString(),
                    eps = binding.etEps.text.toString(),
                    sede = binding.etSede.text.toString(),
                    habitacion = binding.etHabitacion.text.toString().toIntOrNull(),
                    cama = binding.etCama.text.toString().toIntOrNull()
                )

                Toast.makeText(requireContext(), "Guardando cambios de ${pacienteModificado.nombre ?: "paciente"}...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No se encontró el paciente para actualizar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}