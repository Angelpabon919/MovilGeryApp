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

        _binding =
            FragmentDetalleEventoAdversoEncargadoBinding.inflate(
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

        cargarDatosEvento()

        configurarBotonVolver()
    }

    private fun cargarDatosEvento() {

        val tipoEvento =
            arguments?.getString("tipoEvento")
                ?: "EVENTO ADVERSO"

        val estado =
            arguments?.getString("estado")
                ?: "PENDIENTE"

        val paciente =
            arguments?.getString("paciente")
                ?: "Paciente no disponible"

        val fecha =
            arguments?.getString("fecha")
                ?: "Sin fecha"

        val hora =
            arguments?.getString("hora")
                ?: "Sin hora"

        val cuidador =
            arguments?.getString("cuidador")
                ?: "Cuidador no disponible"

        val descripcion =
            arguments?.getString("descripcion")
                ?: "Sin descripción"

        val observaciones =
            arguments?.getString("observaciones")
                ?: "Sin observaciones registradas"

        binding.txtTipoEvento.text = tipoEvento

        binding.txtEstadoEvento.text = estado

        binding.txtPacienteEvento.text = paciente

        binding.txtFechaEvento.text =
            "$fecha · $hora"

        binding.txtCuidadorEvento.text =
            cuidador

        binding.txtDescripcionEvento.text =
            descripcion

        binding.txtObservacionesEvento.text =
            observaciones

        binding.txtFechaFotografia.text =
            "Registrada el $fecha · $hora"
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