package com.example.molvigeryapp.ui.cuidador.agenda

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentAgendaBinding
import com.example.molvigeryapp.ui.cuidador.NavegacionCuidador
import com.example.molvigeryapp.ui.cuidador.camara.EventosCamaraFragment
import com.example.molvigeryapp.ui.cuidador.notificaciones.NotificacionesFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.HomeFragment
import com.example.molvigeryapp.ui.cuidador.perfil.PerfilCuidadorFragment
import kotlinx.coroutines.launch

class AgendaFragment : Fragment() {

    private var _binding: FragmentAgendaBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AgendaPacienteAdapter

    private val viewModel: AgendaViewModel by activityViewModels {
        AgendaViewModelFactory(PacienteRepository())
    }

    // Última revisión de novedades procesada.
    // Se conserva mientras exista esta instancia del Fragment.
    private var ultimaRevisionProcesada: Long? = null


    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAgendaBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        configurarBotonNotificaciones()
        configurarRecyclerView()
        observarDatos()
        configurarNavegacion()

        // Obtener el ID del cuidador que inició sesión.
        val preferences = requireActivity().getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )

        val idUsuario = preferences.getInt(
            "ID_USUARIO",
            -1
        )

        // Registrar la revisión actual antes de iniciar
        // la escucha para evitar una actualización duplicada.
        if (ultimaRevisionProcesada == null) {

            ultimaRevisionProcesada =
                AgendaNovedadManager.revisionNovedad.value
        }

        // Cargar la agenda del cuidador.
        viewModel.cargarAgenda(idUsuario)

        // Escuchar novedades mientras Agenda esté visible.
        observarNovedadesAgenda(idUsuario)
    }


    // =========================================================
    // ESCUCHAR NOVEDADES DE AGENDA
    // =========================================================

    private fun observarNovedadesAgenda(idUsuario: Int) {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                AgendaNovedadManager.revisionNovedad.collect { revision ->

                    val revisionAnterior =
                        ultimaRevisionProcesada

                    if (
                        revisionAnterior != null &&
                        revision != revisionAnterior
                    ) {

                        ultimaRevisionProcesada = revision

                        android.util.Log.d(
                            "AGENDA_ACTUALIZADOR",
                            "Nueva revisión detectada: $revision"
                        )

                        // Actualizar sin abandonar Agenda.
                        viewModel.actualizarAgenda(idUsuario)

                    } else if (revisionAnterior == null) {

                        ultimaRevisionProcesada = revision

                        Log.d(
                            "AGENDA_ACTUALIZADOR",
                            "FRAGMENT: Solicitando actualización de Agenda."
                        )

                        viewModel.actualizarAgenda(idUsuario)

                    } else if (revisionAnterior == null) {

                        ultimaRevisionProcesada = revision
                    }
                }
            }
        }
    }


    // =========================================================
    // NOTIFICACIONES
    // =========================================================

    private fun configurarBotonNotificaciones() {

        binding.btnNotificationAgenda.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    NotificacionesFragment()
                )
                .addToBackStack("notificaciones_cuidador")
                .commit()
        }
    }


    // =========================================================
    // NAVEGACIÓN INFERIOR
    // =========================================================

    private fun configurarNavegacion() {

        NavegacionCuidador.configurar(

            navInicio = binding.navInicio,
            iconInicio = binding.iconInicio,
            textInicio = binding.textInicio,

            navAgenda = binding.navAgenda,
            iconAgenda = binding.iconAgenda,
            textAgenda = binding.textAgenda,
            badgeAgenda = binding.badgeAgenda,

            navCamaras = binding.navCamaras,
            iconCamaras = binding.iconCamaras,
            textCamaras = binding.textCamaras,

            navPerfil = binding.navPerfil,
            iconPerfil = binding.iconPerfil,
            textPerfil = binding.textPerfil,

            pantallaActual = NavegacionCuidador.Pantalla.AGENDA,
            lifecycleOwner = viewLifecycleOwner,

            onInicio = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeFragment()
                    )
                    .commit()
            },

            onAgenda = {
                // Ya estamos en Agenda.
            },

            onCamaras = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        EventosCamaraFragment()
                    )
                    .commit()
            },

            onPerfil = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        PerfilCuidadorFragment()
                    )
                    .commit()
            }
        )
    }


    // =========================================================
    // CONFIGURAR RECYCLERVIEW
    // =========================================================

    private fun configurarRecyclerView() {

        adapter = AgendaPacienteAdapter { paciente ->

            val idPaciente = paciente.idPaciente

            if (idPaciente != null) {

                val fragment = CitasPacienteFragment.newInstance(
                    paciente
                )

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        fragment
                    )
                    .addToBackStack(null)
                    .commit()

            } else {

                Toast.makeText(
                    requireContext(),
                    "No se pudo identificar al paciente.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.rvPacientesAgenda.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientesAgenda.adapter = adapter
    }


    // =========================================================
    // OBSERVAR DATOS
    // =========================================================

    private fun observarDatos() {

        // Pacientes con citas.
        viewModel.pacientesConCitas.observe(
            viewLifecycleOwner
        ) { pacientes ->

            adapter.submitList(pacientes)

            if (pacientes.isEmpty()) {

                binding.tvSinCitas.visibility = View.VISIBLE
                binding.rvPacientesAgenda.visibility = View.GONE

            } else {

                binding.tvSinCitas.visibility = View.GONE
                binding.rvPacientesAgenda.visibility = View.VISIBLE
            }
        }


        // Citas actualizadas correctamente.
        viewModel.citas.observe(
            viewLifecycleOwner
        ) {

            // Solo se marca como vista cuando el ViewModel
            // ha recibido correctamente las citas.
            AgendaNovedadManager.marcarComoVista(
                requireContext()
            )

            binding.badgeAgenda.visibility = View.GONE
        }


        // Indicador de carga.
        viewModel.cargando.observe(
            viewLifecycleOwner
        ) { cargando ->

            binding.progressAgenda.visibility =
                if (cargando) View.VISIBLE else View.GONE
        }


        // Mensajes de error.
        viewModel.error.observe(
            viewLifecycleOwner
        ) { mensaje ->

            if (!mensaje.isNullOrEmpty()) {

                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // LIMPIAR BINDING
    // =========================================================

    override fun onDestroyView() {

        binding.rvPacientesAgenda.adapter = null

        _binding = null

        super.onDestroyView()
    }
}