package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.MainActivity
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacientesListBinding
import com.example.molvigeryapp.ui.auth.LoginActivity

class PacientesListFragment : Fragment() {

    private var _binding: FragmentPacientesListBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PacienteAdapter

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPacientesListBinding.inflate(
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

        binding.btnMenuOpciones.setOnClickListener { vista ->

            val popupMenu = PopupMenu(
                requireContext(),
                vista
            )

            popupMenu.menuInflater.inflate(
                R.menu.menu_opciones,
                popupMenu.menu
            )

            popupMenu.setOnMenuItemClickListener { item ->

                when (item.itemId) {

                    R.id.menu_perfil -> {
                        true
                    }

                    R.id.menu_cerrar_sesion -> {
                        cerrarSesion()
                        true
                    }

                    else -> false
                }
            }

            popupMenu.show()
        }

        setupRecyclerView()
        observarDatos()
        configurarBotones()

        viewModel.cargarPacientes()
    }

    override fun onResume() {
        super.onResume()

        (requireActivity() as MainActivity)
            .mostrarBottomNavigation()
    }

    private fun setupRecyclerView() {

        adapter = PacienteAdapter(
            onSeleccionCambiada = { totalSeleccionados ->
                actualizarContador(totalSeleccionados)
            }
        )

        binding.rvPacientes.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientes.adapter = adapter
    }

    private fun observarDatos() {

        viewModel.pacientes.observe(viewLifecycleOwner) { lista ->

            adapter.actualizarLista(
                lista ?: emptyList()
            )

            actualizarContador(
                adapter.obtenerCantidadSeleccionados()
            )
        }
    }

    private fun actualizarContador(total: Int) {

        binding.tvContadorSeleccionados.text =
            "$total seleccionados"
    }

    private fun configurarBotones() {

        binding.btnBack.setOnClickListener {

            requireActivity()
                .onBackPressedDispatcher
                .onBackPressed()
        }

        binding.btnGuardarSeleccion.setOnClickListener {

            val totalSeleccionados =
                adapter.obtenerCantidadSeleccionados()

            if (totalSeleccionados == 0) {

                Toast.makeText(
                    requireContext(),
                    "Por favor seleccione al menos un paciente",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                val preferences =
                    requireContext().getSharedPreferences(
                        "SESION",
                        Context.MODE_PRIVATE
                    )

                val idUsuarioLogueado =
                    preferences.getInt(
                        "ID_USUARIO",
                        -1
                    )

                viewModel.confirmarSeleccionDelDia(
                    idUsuarioLogueado
                )

                Toast.makeText(
                    requireContext(),
                    "Selección guardada: $totalSeleccionados paciente(s)",
                    Toast.LENGTH_SHORT
                ).show()

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeFragment()
                    )
                    .commit()
            }
        }
    }

    private fun cerrarSesion() {

        val preferences = requireContext()
            .getSharedPreferences(
                "SESION",
                Context.MODE_PRIVATE
            )

        preferences.edit()
            .clear()
            .apply()

        val intent = Intent(
            requireContext(),
            LoginActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}