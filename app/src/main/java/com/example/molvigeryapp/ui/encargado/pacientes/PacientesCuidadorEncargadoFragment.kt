package com.example.molvigeryapp.ui.encargado.pacientes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacientesCuidadorEncargadoBinding
import kotlinx.coroutines.launch

class PacientesCuidadorEncargadoFragment : Fragment() {

    private var _binding: FragmentPacientesCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    private val repository = PacienteRepository()

    private lateinit var adapter: PacientesAsignadosAdapter

    // ID del cuidador que seleccionó el Encargado
    private var idUsuarioCuidador: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recibimos el ID del cuidador desde
        // DetalleCuidadorEncargadoFragment
        idUsuarioCuidador =
            arguments?.getInt("id_usuario", 0) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentPacientesCuidadorEncargadoBinding.inflate(
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

        configurarInformacionCuidador()
        configurarRecyclerView()
        configurarBotonVolver()

        cargarPacientesAsignados()
    }

    /**
     * Muestra el nombre y cargo del cuidador
     * que estamos consultando.
     */
    private fun configurarInformacionCuidador() {

        val nombre =
            arguments?.getString("nombre") ?: ""

        val cargo =
            arguments?.getString("cargo") ?: ""

        binding.txtNombreCuidadorPacientes.text = nombre
        binding.txtCargoCuidadorPacientes.text = cargo
    }

    /**
     * Configura el RecyclerView.
     */
    private fun configurarRecyclerView() {

        adapter = PacientesAsignadosAdapter()

        binding.recyclerPacientesCuidador.apply {

            layoutManager =
                LinearLayoutManager(requireContext())

            adapter =
                this@PacientesCuidadorEncargadoFragment.adapter
        }
    }

    /**
     * Regresa a la pantalla anterior.
     */
    private fun configurarBotonVolver() {

        binding.btnVolverPacientes.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Obtiene los pacientes asignados al cuidador.
     */
    private fun cargarPacientesAsignados() {

        // Si no recibimos el ID del cuidador,
        // no podemos consultar sus asignaciones.
        if (idUsuarioCuidador == 0) {

            mostrarSinPacientes()

            return
        }

        lifecycleScope.launch {

            try {

                // =====================================================
                // 1. OBTENER TODAS LAS ASIGNACIONES
                // =====================================================

                val asignaciones =
                    repository.obtenerAsignacionesPacienteCuidador()


                // =====================================================
                // 2. FILTRAR LAS ASIGNACIONES DEL CUIDADOR
                // =====================================================

                val asignacionesCuidador =
                    asignaciones.filter { asignacion ->

                        asignacion.idUsuario == idUsuarioCuidador &&
                                asignacion.estado.equals(
                                    "Activo",
                                    ignoreCase = true
                                )
                    }


                // =====================================================
                // 3. OBTENER LOS ID DE LOS PACIENTES
                // =====================================================

                val idsPacientes =
                    asignacionesCuidador
                        .map { asignacion ->
                            asignacion.idPaciente
                        }
                        .distinct()


                // Si el cuidador no tiene pacientes asignados
                if (idsPacientes.isEmpty()) {

                    mostrarSinPacientes()

                    return@launch
                }


                // =====================================================
                // 4. OBTENER TODOS LOS PACIENTES
                // =====================================================

                val todosLosPacientes =
                    repository.obtenerPacientes()


                // =====================================================
                // 5. FILTRAR LOS PACIENTES ASIGNADOS
                // =====================================================

                val pacientesAsignados =
                    todosLosPacientes.filter { paciente ->

                        paciente.idPaciente in idsPacientes
                    }


                // =====================================================
                // 6. MOSTRAR LOS PACIENTES
                // =====================================================

                mostrarPacientes(pacientesAsignados)

            } catch (e: Exception) {

                Log.e(
                    "PACIENTES_ENCARGADO",
                    "Error al cargar pacientes asignados",
                    e
                )

                mostrarSinPacientes()

                Toast.makeText(
                    requireContext(),
                    "Error al cargar los pacientes asignados",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Muestra la lista de pacientes asignados.
     */
    private fun mostrarPacientes(
        pacientes: List<Paciente>
    ) {

        if (pacientes.isEmpty()) {

            mostrarSinPacientes()

            return
        }

        // Enviar pacientes al adapter
        adapter.actualizarLista(pacientes)

        // Actualizar contador
        binding.txtCantidadPacientesAsignados.text =
            pacientes.size.toString()

        // Mostrar RecyclerView
        binding.recyclerPacientesCuidador.visibility =
            View.VISIBLE

        // Ocultar mensaje vacío
        binding.txtSinPacientesCuidador.visibility =
            View.GONE
    }

    /**
     * Muestra el mensaje cuando el cuidador
     * no tiene pacientes asignados.
     */
    private fun mostrarSinPacientes() {

        // Limpiar adapter
        adapter.actualizarLista(emptyList())

        // Reiniciar contador
        binding.txtCantidadPacientesAsignados.text = "0"

        // Ocultar lista
        binding.recyclerPacientesCuidador.visibility =
            View.GONE

        // Mostrar mensaje
        binding.txtSinPacientesCuidador.visibility =
            View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}