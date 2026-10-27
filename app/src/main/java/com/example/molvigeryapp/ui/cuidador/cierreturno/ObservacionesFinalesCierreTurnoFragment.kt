package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.FragmentObservacionesFinalesCierreTurnoBinding

class ObservacionesFinalesCierreTurnoFragment : Fragment(
    R.layout.fragment_observaciones_finales_cierre_turno
) {

    private var _binding: FragmentObservacionesFinalesCierreTurnoBinding? = null
    private val binding get() = _binding!!

    private var idAsignacionTurno: Int = -1

    companion object {
        private const val ARG_ID_ASIGNACION = "id_asignacion_turno_usuario"

        const val RESULT_KEY = "resultado_observaciones_cierre"
        const val KEY_ID_ASIGNACION = "id_asignacion"
        const val KEY_OBSERVACIONES = "observaciones"

        fun newInstance(idAsignacion: Int) =
            ObservacionesFinalesCierreTurnoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_ASIGNACION, idAsignacion)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding =
            FragmentObservacionesFinalesCierreTurnoBinding.bind(view)

        idAsignacionTurno =
            arguments?.getInt(ARG_ID_ASIGNACION, -1) ?: -1

        // Volver a la pantalla anterior.
        binding.btnVolverObservacionesFinal.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Cancelar no envía ni finaliza el turno.
        binding.btnCancelarObservacionesFinales.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Validar confirmaciones y solicitar el cierre.
        binding.btnTerminarTurnoFinal.setOnClickListener {
            confirmarFinalizacion()
        }
    }

    private fun confirmarFinalizacion() {
        if (idAsignacionTurno <= 0) {
            Toast.makeText(
                requireContext(),
                "No se encontró la asignación del turno.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!binding.cbRevisoPacientes.isChecked) {
            Toast.makeText(
                requireContext(),
                "Confirma que revisaste a los pacientes de tu turno.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!binding.cbConfirmarFinalizacion.isChecked) {
            Toast.makeText(
                requireContext(),
                "Confirma que deseas finalizar el turno.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val observaciones = binding.etObservacionesFinales.text
            ?.toString()
            ?.trim()
            .orEmpty()

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar finalización")
            .setMessage(
                "¿Estás seguro de que deseas terminar este turno?"
            )
            .setNegativeButton("Seguir revisando", null)
            .setPositiveButton("Terminar turno") { _, _ ->
                devolverResultado(observaciones)
            }
            .show()
    }

    private fun devolverResultado(observaciones: String) {
        parentFragmentManager.setFragmentResult(
            RESULT_KEY,
            Bundle().apply {
                putInt(KEY_ID_ASIGNACION, idAsignacionTurno)
                putString(KEY_OBSERVACIONES, observaciones)
            }
        )

        // Regresa al fragmento que gestiona la petición al backend.
        parentFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}