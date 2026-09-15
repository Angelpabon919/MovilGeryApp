package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.databinding.FragmentCardexBinding

class CardexFragment : Fragment() {

    private var _binding: FragmentCardexBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels()
    private var cuidadoActual: CuidadoEnfermeria? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCardexBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Escuchar la selección del paciente por ID
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            val idPaciente = paciente?.idPaciente
            if (idPaciente != null) {
                pacienteViewModel.cargarCuidados(idPaciente)
            } else {
                limpiarCampos()
            }
        }

        // 2. Mapear la respuesta de la API a los campos
        pacienteViewModel.cuidados.observe(viewLifecycleOwner) { listaCuidados ->
            // Tomamos sólo un registro filtrado por id_paciente
            cuidadoActual = listaCuidados?.firstOrNull()
            cuidadoActual?.let { c ->
                binding.etBanoPaciente.setText(c.banoPaciente ?: "")
                binding.etPesoTalla.setText(c.pesoTalla ?: "")
                binding.etControlGlucemia.setText(c.controlGlucemia ?: "")
                binding.etCuraciones.setText(c.curaciones ?: "")
                binding.etLiquidos.setText(c.liquidosAdministradosEliminados ?: "")
                binding.etControlDeposicion.setText(c.controlDeposicion ?: "")
                binding.etAdminMedicamentos.setText(c.administracionMedicamentos ?: "")
            }
        }

        // 3. Botón de guardado
        binding.btnGuardarCardex.setOnClickListener {
            val idPaciente = pacienteViewModel.pacienteSeleccionado.value?.idPaciente

            val cuidadoAGuardar = CuidadoEnfermeria(
                idCuidado = cuidadoActual?.idCuidado,
                banoPaciente = binding.etBanoPaciente.text.toString().trim(),
                pesoTalla = binding.etPesoTalla.text.toString().trim(),
                controlGlucemia = binding.etControlGlucemia.text.toString().trim(),
                curaciones = binding.etCuraciones.text.toString().trim(),
                liquidosAdministradosEliminados = binding.etLiquidos.text.toString().trim(),
                controlDeposicion = binding.etControlDeposicion.text.toString().trim(),
                administracionMedicamentos = binding.etAdminMedicamentos.text.toString().trim(),
                idPaciente = idPaciente
            )

            pacienteViewModel.guardarCuidado(cuidadoAGuardar)
            Toast.makeText(requireContext(), "Guardando registro...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarCampos() {
        binding.etBanoPaciente.setText("")
        binding.etPesoTalla.setText("")
        binding.etControlGlucemia.setText("")
        binding.etCuraciones.setText("")
        binding.etLiquidos.setText("")
        binding.etControlDeposicion.setText("")
        binding.etAdminMedicamentos.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}