package com.example.molvigeryapp.ui.cuidador.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.databinding.FragmentDetalleCitaBinding

class DetalleCitaFragment : Fragment() {

    private var _binding: FragmentDetalleCitaBinding? = null
    private val binding get() = _binding!!

    private var cita: Cita? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cita =
            arguments?.getSerializable(ARG_CITA) as? Cita
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDetalleCitaBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        mostrarInformacion()

        binding.tvVolverDetalleCita.setOnClickListener {

            requireActivity()
                .onBackPressedDispatcher
                .onBackPressed()
        }
    }

    private fun mostrarInformacion() {

        val citaActual = cita

        if (citaActual == null) {

            Toast.makeText(
                requireContext(),
                "No se pudo cargar la información de la cita.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        binding.tvEstadoDetalle.text =
            if (citaActual.estado.isNotBlank()) {
                citaActual.estado
            } else {
                "Estado no registrado"
            }

        binding.tvFechaDetalle.text =
            if (citaActual.fecha.isNotBlank()) {
                citaActual.fecha
            } else {
                "Fecha no registrada"
            }

        binding.tvHoraDetalle.text =
            if (citaActual.hora.isNotBlank()) {
                citaActual.hora
            } else {
                "Hora no registrada"
            }

        binding.tvLugarDetalle.text =
            if (!citaActual.lugar.isNullOrBlank()) {
                citaActual.lugar
            } else {
                "Lugar no registrado"
            }

        binding.tvMotivoDetalle.text =
            if (!citaActual.motivo.isNullOrBlank()) {
                citaActual.motivo
            } else {
                "Sin motivo registrado"
            }

        binding.tvObservacionesDetalle.text =
            if (!citaActual.observaciones.isNullOrBlank()) {
                citaActual.observaciones
            } else {
                "Sin observaciones registradas"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {

        private const val ARG_CITA =
            "arg_cita"

        fun newInstance(
            cita: Cita
        ): DetalleCitaFragment {

            return DetalleCitaFragment().apply {

                arguments = Bundle().apply {

                    putSerializable(
                        ARG_CITA,
                        cita
                    )
                }
            }
        }
    }
}