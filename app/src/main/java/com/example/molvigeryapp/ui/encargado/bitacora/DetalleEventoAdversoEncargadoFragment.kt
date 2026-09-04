package com.example.molvigeryapp.ui.encargado.bitacora

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentDetalleEventoAdversoEncargadoBinding

class DetalleEventoAdversoEncargadoFragment : Fragment() {

    private var _binding: FragmentDetalleEventoAdversoEncargadoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDetalleEventoAdversoEncargadoBinding.inflate(
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

        cargarDatos()
        configurarBotonVolver()
    }

    private fun cargarDatos() {

        val paciente =
            arguments?.getString("paciente") ?: "Paciente"

        val tipo =
            arguments?.getString("tipo") ?: "Evento adverso"

        val fecha =
            arguments?.getString("fecha") ?: ""

        val estado =
            arguments?.getString("estado") ?: "Pendiente"

        val cuidador =
            arguments?.getString("cuidador") ?: ""

        val descripcion =
            arguments?.getString("descripcion") ?: ""

        val observaciones =
            arguments?.getString("observaciones") ?: ""

        binding.txtPacienteEvento.text = paciente
        binding.txtTipoEvento.text = tipo
        binding.txtFechaEvento.text = fecha
        binding.txtEstadoEvento.text = estado
        binding.txtCuidadorEvento.text = cuidador
        binding.txtDescripcionEvento.text = descripcion
        binding.txtObservacionesEvento.text = observaciones
    }

    private fun configurarBotonVolver() {

        binding.btnVolverEvento.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}