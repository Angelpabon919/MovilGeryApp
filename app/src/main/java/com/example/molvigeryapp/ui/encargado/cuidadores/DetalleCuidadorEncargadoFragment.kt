package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.FragmentDetalleCuidadorEncBinding


class DetalleCuidadorEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentDetalleCuidadorEncBinding? = null

    private val binding
        get() = _binding!!


    // =====================================================
    // CREAR VISTA
    // =====================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDetalleCuidadorEncBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // =====================================================
    // VISTA CREADA
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        cargarDatosCuidador()

        configurarBotonVolver()

        configurarNavegacion()
    }


    // =====================================================
    // CARGAR DATOS DEL CUIDADOR
    // =====================================================

    private fun cargarDatosCuidador() {

        val nombre =
            arguments?.getString("nombre")
                ?: "Cuidador"


        val cargo =
            arguments?.getString("cargo")
                ?: "Cuidador"


        val estado =
            arguments?.getString("estado")
                ?: "Activo"


        val pacientes =
            arguments?.getInt("pacientes")
                ?: 0


        binding.txtNombreCuidador.text =
            nombre


        binding.txtCargoCuidador.text =
            cargo


        binding.txtEstadoCuidador.text =
            estado


        binding.txtCantidadPacientes.text =
            pacientes.toString()


        binding.txtEstadoResumen.text =
            estado
    }


    // =====================================================
    // BOTÓN VOLVER
    // =====================================================

    private fun configurarBotonVolver() {

        binding.btnVolver.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }


    // =====================================================
    // NAVEGACIÓN DE MÓDULOS
    // =====================================================

    private fun configurarNavegacion() {

        // =================================================
        // EVENTOS ADVERSOS
        // =================================================

        binding.moduloEventos.setOnClickListener {

            abrirPantalla(
                EventosCuidadorEncargadoFragment()
            )
        }


        // =================================================
        // ASIGNAR TURNO
        // =================================================

        binding.moduloTurnos.setOnClickListener {

            abrirPantalla(
                TurnosCuidadorEncargadoFragment()
            )
        }


        // =================================================
        // PACIENTES ASIGNADOS
        // =================================================

        binding.moduloPacientes.setOnClickListener {

            abrirPantalla(
                PacientesCuidadorEncargadoFragment()
            )
        }


        // =================================================
        // RESPONSABILIDADES
        // =================================================

        binding.moduloResponsabilidades.setOnClickListener {

            abrirPantalla(
                ResponsabilidadesCuidadorEncargadoFragment()
            )
        }
    }


    // =====================================================
    // ABRIR PANTALLA
    // =====================================================

    private fun abrirPantalla(
        fragment: Fragment
    ) {

        val datos =
            Bundle().apply {

                putString(
                    "nombre",
                    arguments?.getString("nombre")
                        ?: ""
                )

                putString(
                    "cargo",
                    arguments?.getString("cargo")
                        ?: ""
                )

                putString(
                    "estado",
                    arguments?.getString("estado")
                        ?: ""
                )

                putInt(
                    "pacientes",
                    arguments?.getInt("pacientes")
                        ?: 0
                )
            }


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


    // =====================================================
    // DESTRUIR VISTA
    // =====================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}