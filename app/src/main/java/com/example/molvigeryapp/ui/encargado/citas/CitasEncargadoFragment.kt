package com.example.molvigeryapp.ui.encargado.citas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.CitasRepository
import com.example.molvigeryapp.databinding.FragmentCitasEncargadoBinding
import com.example.molvigeryapp.ui.encargado.bitacora.BitacoraEncargadoFragment
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment
import kotlinx.coroutines.launch

class CitasEncargadoFragment : Fragment() {

    private var _binding: FragmentCitasEncargadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var pacienteAdapter: PacienteCitaAdapter
    private lateinit var citaAdapter: CitaAdapter

    private var listaPacientes = listOf<Paciente>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCitasEncargadoBinding.inflate(
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

        configurarPacientes()
        configurarCitas()
        configurarBuscador()
        configurarPestanas()
        configurarNotificaciones()
        configurarNavegacion()

        cargarPacientes()
        mostrarPacientes()
    }

    // =========================================================
    // PACIENTES
    // =========================================================

    private fun configurarPacientes() {

        pacienteAdapter = PacienteCitaAdapter(
            listaPacientes,
            onPacienteClick = { paciente ->
                abrirPaciente(paciente)
            }
        )

        binding.recyclerPacientesCita.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pacienteAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun cargarPacientes() {

        lifecycleScope.launch {

            try {

                val pacientes = RetrofitClient.api.getPacientes()

                listaPacientes = pacientes

                pacienteAdapter.actualizarLista(pacientes)

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "No se pudieron cargar los pacientes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun mostrarPacientes() {

        binding.contenedorPacientesCitas.visibility = View.VISIBLE
        binding.contenedorCitasProgramadas.visibility = View.GONE

        actualizarPestanaPacientes()
    }


    // =========================================================
    // ABRIR PACIENTE
    // =========================================================

    private fun abrirPaciente(paciente: Paciente) {

        val datos = Bundle()

        datos.putInt(
            "idPaciente",
            paciente.idPaciente ?: -1
        )

        datos.putString(
            "nombrePaciente",
            paciente.nombre
        )

        datos.putString(
            "apellidoPaciente",
            paciente.apellido
        )

        datos.putInt(
            "habitacionPaciente",
            paciente.habitacion ?: -1
        )

        datos.putInt(
            "camaPaciente",
            paciente.cama ?: -1
        )

        val fragment = NuevaCitaEncargadoFragment()

        fragment.arguments = datos

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
    // CITAS
    // =========================================================

    private fun configurarCitas() {

        citaAdapter = CitaAdapter(
            CitasRepository.obtenerCitas()
        ) { cita ->

            abrirDetalleCita(cita)
        }

        binding.recyclerCitasEncargado.apply {

            layoutManager = LinearLayoutManager(requireContext())

            adapter = citaAdapter

            setHasFixedSize(false)

            isNestedScrollingEnabled = false
        }
    }

    private fun mostrarCitas() {

        binding.contenedorPacientesCitas.visibility = View.GONE

        binding.contenedorCitasProgramadas.visibility = View.VISIBLE

        actualizarListaCitas()

        actualizarPestanaCitas()
    }

    private fun actualizarListaCitas() {

        val citas = CitasRepository.obtenerCitas()

        citaAdapter.actualizarLista(citas)

        if (citas.isEmpty()) {

            binding.txtSinCitas.visibility = View.VISIBLE

        } else {

            binding.txtSinCitas.visibility = View.GONE
        }
    }


    // =========================================================
    // PESTAÑAS
    // =========================================================

    private fun configurarPestanas() {

        binding.tabPacientesCitas.setOnClickListener {

            mostrarPacientes()
        }

        binding.tabCitasCitas.setOnClickListener {

            mostrarCitas()
        }
    }


    // =========================================================
    // ESTADO VISUAL - PACIENTES
    // =========================================================

    private fun actualizarPestanaPacientes() {

        binding.tabPacientesCitas.setBackgroundResource(
            R.drawable.bg_tab_seleccionada
        )

        binding.tabCitasCitas.background = null

        binding.iconTabPacientesCitas.setColorFilter(
            android.graphics.Color.WHITE
        )

        binding.textTabPacientesCitas.setTextColor(
            android.graphics.Color.WHITE
        )

        binding.textTabPacientesCitas.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        binding.iconTabCitasCitas.setColorFilter(
            android.graphics.Color.rgb(
                152,
                162,
                179
            )
        )

        binding.textTabCitasCitas.setTextColor(
            android.graphics.Color.rgb(
                152,
                162,
                179
            )
        )

        binding.textTabCitasCitas.setTypeface(
            null,
            android.graphics.Typeface.NORMAL
        )
    }


    // =========================================================
    // ESTADO VISUAL - CITAS
    // =========================================================

    private fun actualizarPestanaCitas() {

        binding.tabCitasCitas.setBackgroundResource(
            R.drawable.bg_tab_seleccionada
        )

        binding.tabPacientesCitas.background = null

        binding.iconTabCitasCitas.setColorFilter(
            android.graphics.Color.WHITE
        )

        binding.textTabCitasCitas.setTextColor(
            android.graphics.Color.WHITE
        )

        binding.textTabCitasCitas.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        binding.iconTabPacientesCitas.setColorFilter(
            android.graphics.Color.rgb(
                152,
                162,
                179
            )
        )

        binding.textTabPacientesCitas.setTextColor(
            android.graphics.Color.rgb(
                152,
                162,
                179
            )
        )

        binding.textTabPacientesCitas.setTypeface(
            null,
            android.graphics.Typeface.NORMAL
        )
    }


    // =========================================================
    // BUSCADOR
    // =========================================================

    private fun configurarBuscador() {

        binding.edtBuscarPaciente.addTextChangedListener {

            val texto = it
                ?.toString()
                ?.trim()
                ?.lowercase()
                ?: ""

            val filtrados = listaPacientes.filter { paciente ->

                val nombreCompleto =
                    "${paciente.nombre} ${paciente.apellido}"
                        .lowercase()

                nombreCompleto.contains(texto)
            }

            pacienteAdapter.actualizarLista(filtrados)
        }
    }


    // =========================================================
    // DETALLE DE CITA
    // =========================================================

    private fun abrirDetalleCita(cita: Cita) {

        Toast.makeText(
            requireContext(),
            "Cita de ${cita.nombrePaciente}",
            Toast.LENGTH_SHORT
        ).show()

        // Aquí posteriormente abriremos:
        //
        // DetalleCitaEncargadoFragment
        //
        // para editar o eliminar la cita.
    }


    // =========================================================
    // NOTIFICACIONES
    // =========================================================

    private fun configurarNotificaciones() {

        binding.btnNotificacionesCitas.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    NotificacionesEncargadoFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }


    // =========================================================
    // MENÚ INFERIOR
    // =========================================================

    private fun configurarNavegacion() {

        NavegacionEncargado.configurar(
            navInicio = binding.navInicioCitas,
            navCitas = binding.navCitasCitas,
            navBitacora = binding.navBitacoraCitas,
            navPerfil = binding.navPerfilCitas,

            iconInicio = binding.iconInicioCitas,
            iconCitas = binding.iconCitasCitas,
            iconBitacora = binding.iconBitacoraCitas,
            iconPerfil = binding.iconPerfilCitas,

            textInicio = binding.textInicioCitas,
            textCitas = binding.textCitasCitas,
            textBitacora = binding.textBitacoraCitas,
            textPerfil = binding.textPerfilCitas,

            pantallaActual = NavegacionEncargado.Pantalla.CITAS,


            onInicio = {

                abrirSeccion(
                    HomeEncargadoFragment()
                )
            },

            onCitas = {
                // Ya estamos en Citas
            },

            onBitacora = {

                abrirSeccion(
                    BitacoraEncargadoFragment()
                )
            },

            onPerfil = {

                abrirSeccion(
                    PerfilEncargadoFragment()
                )
            }
        )
    }


    // =========================================================
    // CAMBIO DE SECCIÓN
    // =========================================================

    private fun abrirSeccion(fragment: Fragment) {

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
    // ACTUALIZAR AL VOLVER
    // =========================================================

    override fun onResume() {

        super.onResume()

        if (_binding != null) {

            actualizarListaCitas()
        }
    }


    // =========================================================
    // VIEW BINDING
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}