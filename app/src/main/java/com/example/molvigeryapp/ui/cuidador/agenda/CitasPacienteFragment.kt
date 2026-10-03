package com.example.molvigeryapp.ui.cuidador.agenda

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentCitasPacienteBinding

class CitasPacienteFragment : Fragment() {

    private var _binding: FragmentCitasPacienteBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CitasPacienteAdapter

    private val viewModel: AgendaViewModel by activityViewModels {
        AgendaViewModelFactory(PacienteRepository())
    }

    private var paciente: Paciente? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paciente =
            arguments?.getSerializable(ARG_PACIENTE) as? Paciente
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentCitasPacienteBinding.inflate(
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

        configurarPantalla()
        configurarRecyclerView()
        cargarCitas()
        configurarBotonVolver()
    }

    // =============================================
    // CONFIGURAR INFORMACIÓN DEL PACIENTE
    // =============================================

    private fun configurarPantalla() {

        val pacienteActual = paciente

        if (pacienteActual == null) {

            Toast.makeText(
                requireContext(),
                "No se pudo identificar al paciente.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        binding.tvNombrePaciente.text =
            "${pacienteActual.nombre} ${pacienteActual.apellido}"

        binding.tvHabitacion.text =
            if (pacienteActual.habitacion != null) {
                "Habitación ${pacienteActual.habitacion}"
            } else {
                "Habitación no registrada"
            }

        binding.tvCama.text =
            if (pacienteActual.cama != null) {
                "Cama ${pacienteActual.cama}"
            } else {
                "Cama no registrada"
            }
    }

    // =============================================
    // CONFIGURAR BOTÓN VOLVER
    // =============================================

    private fun configurarBotonVolver() {

        binding.tvVolverCitas.setOnClickListener {

            requireActivity()
                .onBackPressedDispatcher
                .onBackPressed()
        }
    }

    // =============================================
    // CONFIGURAR RECYCLERVIEW
    // =============================================

    private fun configurarRecyclerView() {

        adapter = CitasPacienteAdapter { cita ->

            val detalle =
                DetalleCitaFragment.newInstance(cita)

            parentFragmentManager
                .beginTransaction()
                .replace(
                    com.example.molvigeryapp.R.id.fragmentContainer,
                    detalle
                )
                .addToBackStack(null)
                .commit()
        }

        binding.rvCitasPaciente.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvCitasPaciente.adapter =
            adapter
    }

    // =============================================
    // CARGAR CITAS DEL PACIENTE
    // =============================================

    private fun cargarCitas() {

        val idPaciente =
            paciente?.idPaciente

        if (idPaciente == null) {

            mostrarSinCitas()

            return
        }

        val citas =
            viewModel.obtenerCitasDePaciente(
                idPaciente
            )

        adapter.submitList(citas)

        if (citas.isEmpty()) {

            mostrarSinCitas()

        } else {

            mostrarListaCitas()
        }
    }

    // =============================================
    // MOSTRAR MENSAJE SIN CITAS
    // =============================================

    private fun mostrarSinCitas() {

        binding.tvSinCitasPaciente.visibility =
            View.VISIBLE

        binding.rvCitasPaciente.visibility =
            View.GONE
    }

    // =============================================
    // MOSTRAR LISTA DE CITAS
    // =============================================

    private fun mostrarListaCitas() {

        binding.tvSinCitasPaciente.visibility =
            View.GONE

        binding.rvCitasPaciente.visibility =
            View.VISIBLE
    }

    // =============================================
    // LIMPIAR BINDING
    // =============================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    // =============================================
    // CREAR FRAGMENT CON PACIENTE
    // =============================================

    companion object {

        private const val ARG_PACIENTE =
            "arg_paciente"

        fun newInstance(
            paciente: Paciente
        ): CitasPacienteFragment {

            return CitasPacienteFragment().apply {

                arguments = Bundle().apply {

                    putSerializable(
                        ARG_PACIENTE,
                        paciente
                    )
                }
            }
        }
    }
}