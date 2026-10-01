package com.example.molvigeryapp.ui.encargado.pacientes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPacientesCuidadorEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class PacientesCuidadorEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentPacientesCuidadorEncargadoBinding? =
        null

    private val binding
        get() = _binding!!


    // =========================================================
    // REPOSITORY
    // =========================================================

    private val repository =
        PacienteRepository()


    // =========================================================
    // ADAPTER
    // =========================================================

    private lateinit var adapter: PacientesAsignadosAdapter


    // =========================================================
    // DATOS DEL CUIDADOR
    // =========================================================

    private var idUsuarioCuidador: Int = 0


    // =========================================================
    // CONTROL DE CARGA
    // =========================================================

    private var cargandoPacientes = false

    private var primeraCarga = true


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {

        private const val TAG =
            "PACIENTES_ENCARGADO"

        private const val INTERVALO_ACTUALIZACION =
            2_000L
    }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        // ID del cuidador seleccionado
        // por el Encargado.

        idUsuarioCuidador =
            arguments?.getInt(
                "id_usuario",
                0
            ) ?: 0
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
            FragmentPacientesCuidadorEncargadoBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // =========================================================
    // VISTA CREADA
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // Reiniciar estado de carga cada vez
        // que se crea nuevamente la vista.

        primeraCarga = true
        cargandoPacientes = false


        configurarInformacionCuidador()

        configurarRecyclerView()

        configurarBotonVolver()

        iniciarActualizacionAutomatica()
    }


    // =========================================================
    // INFORMACIÓN DEL CUIDADOR
    // =========================================================

    private fun configurarInformacionCuidador() {

        val nombre =
            arguments?.getString(
                "nombre"
            ).orEmpty()


        val cargo =
            arguments?.getString(
                "cargo"
            ).orEmpty()


        binding
            .txtNombreCuidadorPacientes
            .text =
            nombre


        binding
            .txtCargoCuidadorPacientes
            .text =
            cargo
    }


    // =========================================================
    // RECYCLERVIEW
    // =========================================================

    private fun configurarRecyclerView() {

        adapter =
            PacientesAsignadosAdapter { paciente ->

                abrirPerfilPaciente(
                    paciente
                )
            }


        binding
            .recyclerPacientesCuidador
            .apply {

                layoutManager =
                    LinearLayoutManager(
                        requireContext()
                    )

                adapter =
                    this@PacientesCuidadorEncargadoFragment
                        .adapter
            }
    }


    // =========================================================
    // ABRIR PERFIL DEL PACIENTE
    // =========================================================

    private fun abrirPerfilPaciente(
        paciente: Paciente
    ) {

        val datos = Bundle().apply {
            putInt(
                "idPaciente",
                paciente.idPaciente ?: 0
            )
        }

        val fragment =
            PerfilPacienteEncargadoFragment()

        fragment.arguments =
            datos

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }


    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding
            .btnVolverPacientes
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }


    // =========================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =========================================================

    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                while (true) {

                    cargarPacientesAsignados(
                        mostrarCargaInicial =
                            primeraCarga
                    )

                    primeraCarga = false

                    delay(
                        INTERVALO_ACTUALIZACION
                    )
                }
            }
        }
    }


    // =========================================================
    // CARGAR PACIENTES
    // =========================================================

    private suspend fun cargarPacientesAsignados(
        mostrarCargaInicial: Boolean
    ) {

        // -----------------------------------------------------
        // EVITAR SOLICITUDES SIMULTÁNEAS
        // -----------------------------------------------------

        if (cargandoPacientes) {
            return
        }


        // -----------------------------------------------------
        // VALIDAR ID DEL CUIDADOR
        // -----------------------------------------------------

        if (idUsuarioCuidador == 0) {

            mostrarSinPacientes()

            return
        }


        cargandoPacientes = true


        if (mostrarCargaInicial) {

            mostrarCargando()
        }


        try {

            // =================================================
            // 1. OBTENER TODAS LAS ASIGNACIONES
            // =================================================

            val asignaciones =
                repository
                    .obtenerAsignacionesPacienteCuidador()


            // =================================================
            // 2. FILTRAR ASIGNACIONES DEL CUIDADOR
            // =================================================

            val asignacionesCuidador =
                asignaciones.filter { asignacion ->

                    asignacion.idUsuario ==
                            idUsuarioCuidador &&

                            asignacion.estado.equals(
                                "Activo",
                                ignoreCase = true
                            )
                }


            // =================================================
            // 3. OBTENER IDS DE PACIENTES
            // =================================================

            val idsPacientes =
                asignacionesCuidador
                    .map { asignacion ->

                        asignacion.idPaciente
                    }
                    .distinct()


            // =================================================
            // 4. SIN PACIENTES ASIGNADOS
            // =================================================

            if (idsPacientes.isEmpty()) {

                if (_binding != null) {

                    mostrarSinPacientes()
                }

                return
            }


            // =================================================
            // 5. OBTENER TODOS LOS PACIENTES
            // =================================================

            val todosLosPacientes =
                repository
                    .obtenerPacientes()


            // =================================================
            // 6. FILTRAR PACIENTES ASIGNADOS
            // =================================================

            val pacientesAsignados =
                todosLosPacientes.filter { paciente ->

                    paciente.idPaciente in idsPacientes
                }


            // =================================================
            // 7. ACTUALIZAR INTERFAZ
            // =================================================

            if (_binding != null) {

                mostrarPacientes(
                    pacientesAsignados
                )
            }


        } catch (
            e: CancellationException
        ) {

            // -------------------------------------------------
            // CANCELACIÓN NORMAL
            // -------------------------------------------------
            //
            // Puede ocurrir cuando el usuario abandona
            // rápidamente la pantalla.
            //
            // No debemos tratarla como error.

            throw e


        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error al cargar pacientes asignados",
                e
            )


            // -------------------------------------------------
            // MOSTRAR ERROR SOLO EN LA CARGA INICIAL
            // -------------------------------------------------
            //
            // Durante las actualizaciones cada 2 segundos
            // no mostramos Toast continuamente.

            if (
                mostrarCargaInicial &&
                _binding != null
            ) {

                mostrarSinPacientes()


                val contexto =
                    context


                if (contexto != null) {

                    Toast.makeText(
                        contexto,
                        "Error al cargar los pacientes asignados",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        } finally {

            cargandoPacientes = false


            if (
                mostrarCargaInicial &&
                _binding != null
            ) {

                ocultarCargando()
            }
        }
    }


    // =========================================================
    // MOSTRAR CARGANDO
    // =========================================================

    private fun mostrarCargando() {

        val bindingActual =
            _binding ?: return


        bindingActual
            .progressBarPacientes
            .visibility =
            View.VISIBLE


        bindingActual
            .recyclerPacientesCuidador
            .visibility =
            View.GONE


        bindingActual
            .txtSinPacientesCuidador
            .visibility =
            View.GONE
    }


    // =========================================================
    // OCULTAR CARGANDO
    // =========================================================

    private fun ocultarCargando() {

        val bindingActual =
            _binding ?: return


        bindingActual
            .progressBarPacientes
            .visibility =
            View.GONE
    }


    // =========================================================
    // MOSTRAR PACIENTES
    // =========================================================

    private fun mostrarPacientes(
        pacientes: List<Paciente>
    ) {

        val bindingActual =
            _binding ?: return


        if (pacientes.isEmpty()) {

            mostrarSinPacientes()

            return
        }


        // -----------------------------------------------------
        // ACTUALIZAR ADAPTER
        // -----------------------------------------------------

        adapter.actualizarLista(
            pacientes
        )


        // -----------------------------------------------------
        // ACTUALIZAR CONTADOR
        // -----------------------------------------------------

        bindingActual
            .txtCantidadPacientesAsignados
            .text =
            pacientes.size.toString()


        // -----------------------------------------------------
        // MOSTRAR LISTA
        // -----------------------------------------------------

        bindingActual
            .recyclerPacientesCuidador
            .visibility =
            View.VISIBLE


        // -----------------------------------------------------
        // OCULTAR MENSAJE VACÍO
        // -----------------------------------------------------

        bindingActual
            .txtSinPacientesCuidador
            .visibility =
            View.GONE
    }


    // =========================================================
    // SIN PACIENTES
    // =========================================================

    private fun mostrarSinPacientes() {

        val bindingActual =
            _binding ?: return


        // -----------------------------------------------------
        // LIMPIAR ADAPTER
        // -----------------------------------------------------

        adapter.actualizarLista(
            emptyList()
        )


        // -----------------------------------------------------
        // REINICIAR CONTADOR
        // -----------------------------------------------------

        bindingActual
            .txtCantidadPacientesAsignados
            .text =
            "0"


        // -----------------------------------------------------
        // OCULTAR LISTA
        // -----------------------------------------------------

        bindingActual
            .recyclerPacientesCuidador
            .visibility =
            View.GONE


        // -----------------------------------------------------
        // MOSTRAR MENSAJE
        // -----------------------------------------------------

        bindingActual
            .txtSinPacientesCuidador
            .visibility =
            View.VISIBLE
    }


    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        binding.recyclerPacientesCuidador.adapter =
            null

        _binding = null

        super.onDestroyView()
    }
}