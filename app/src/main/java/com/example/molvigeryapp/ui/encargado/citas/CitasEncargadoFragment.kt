package com.example.molvigeryapp.ui.encargado.citas

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.CitasRepository
import com.example.molvigeryapp.databinding.FragmentCitasEncargadoBinding
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.WindowInsetsEncargado
import com.example.molvigeryapp.ui.encargado.asignarturno.AsignarTurnoEncargadoFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.ContadorNotificaciones
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.widget.Toast

class CitasEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentCitasEncargadoBinding? = null

    private val binding
        get() = requireNotNull(_binding)


    // =========================================================
    // ADAPTERS
    // =========================================================

    private lateinit var pacienteAdapter: PacienteCitaAdapter

    private lateinit var citaAdapter: CitaAdapter


    // =========================================================
    // LISTAS
    // =========================================================

    private var listaPacientes: List<Paciente> = emptyList()

    private var listaCitas: List<Cita> = emptyList()


    // =========================================================
    // CONTROL DE CARGA
    // =========================================================

    /**
     * Evita que se ejecuten varias solicitudes al mismo tiempo.
     */
    private var cargandoDatos = false


    // =========================================================
    // INTERVALO DE ACTUALIZACIÓN
    // =========================================================

    companion object {

        /**
         * Actualización automática cada 2 segundos.
         */
        private const val INTERVALO_ACTUALIZACION = 2_000L

        private const val TAG = "CITAS_ENCARGADO"
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
            FragmentCitasEncargadoBinding.inflate(
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

        WindowInsetsEncargado.aplicar(
            root = binding.root,
            contenido = binding.scrollContenidoCitas,
            menuInferior = binding.bottomNavigationCitas
        )


        // =====================================================
        // CONFIGURACIONES
        // =====================================================

        configurarPacientes()

        configurarCitas()

        configurarBuscador()

        configurarPestanas()

        configurarNotificaciones()

        configurarNavegacion()


        // =====================================================
        // ESTADO INICIAL
        // =====================================================

        mostrarPacientes()


        // =====================================================
        // CARGA INICIAL
        // =====================================================

        cargarDatos()


        // =====================================================
        // ACTUALIZACIÓN AUTOMÁTICA
        // =====================================================

        iniciarActualizacionAutomatica()


        // =====================================================
        // CONTADOR DE NOTIFICACIONES
        // =====================================================

        ContadorNotificaciones.iniciar(
            fragment = this,
            badge = binding.txtNotificacionesCitas
        )
    }


    // =========================================================
    // CONFIGURAR PACIENTES
    // =========================================================

    private fun configurarPacientes() {

        pacienteAdapter =
            PacienteCitaAdapter(
                emptyList(),
                onPacienteClick = { paciente ->

                    abrirPaciente(
                        paciente
                    )
                }
            )

        binding.recyclerPacientesCita.apply {

            layoutManager =
                LinearLayoutManager(
                    requireContext()
                )

            adapter =
                pacienteAdapter

            setHasFixedSize(false)

            isNestedScrollingEnabled =
                false
        }
    }


    // =========================================================
    // CARGAR DATOS
    // =========================================================

    /**
     * Consulta pacientes y citas directamente desde la API.
     *
     * Se conserva la lógica original.
     */
    private fun cargarDatos(
        mostrarError: Boolean = true
    ) {

        if (_binding == null) {
            return
        }

        /**
         * Si ya existe una carga en curso, no iniciamos otra.
         */
        if (cargandoDatos) {
            return
        }

        cargandoDatos = true

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                // =================================================
                // PACIENTES
                // =================================================

                val pacientes =
                    RetrofitClient.apiService
                        .getPacientes()


                if (!isActive || _binding == null) {
                    return@launch
                }


                listaPacientes =
                    pacientes


                actualizarListaPacientes()


                // =================================================
                // CITAS
                // =================================================

                val citas =
                    CitasRepository
                        .obtenerCitasDesdeApi()


                if (!isActive || _binding == null) {
                    return@launch
                }


                listaCitas =
                    citas


                citaAdapter.actualizarLista(
                    citas
                )


                actualizarEstadoListaCitas()


            } catch (e: CancellationException) {

                /**
                 * La cancelación es normal cuando el Fragment
                 * deja de estar activo.
                 */
                throw e


            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Error actualizando datos",
                    e
                )


                if (
                    mostrarError &&
                    _binding != null
                ) {

                    val contexto =
                        context ?: return@launch


                    Toast.makeText(
                        contexto,
                        "No se pudieron actualizar las citas",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } finally {

                cargandoDatos = false
            }
        }
    }


    // =========================================================
    // ACTUALIZAR PACIENTES
    // =========================================================

    private fun actualizarListaPacientes() {

        if (_binding == null) {
            return
        }


        val textoBusqueda =
            binding.edtBuscarPaciente
                .text
                .toString()
                .trim()
                .lowercase()


        val pacientesFiltrados =

            if (textoBusqueda.isBlank()) {

                listaPacientes

            } else {

                listaPacientes.filter { paciente ->

                    val nombreCompleto =
                        "${paciente.nombre} ${paciente.apellido}"
                            .trim()
                            .lowercase()


                    nombreCompleto.contains(
                        textoBusqueda
                    )
                }
            }


        pacienteAdapter.actualizarLista(
            pacientesFiltrados
        )
    }


    // =========================================================
    // ABRIR PACIENTE
    // =========================================================

    private fun abrirPaciente(
        paciente: Paciente
    ) {

        if (!isAdded) {
            return
        }


        val datos =
            Bundle()


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


        val fragment =
            NuevaCitaEncargadoFragment()


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
    // CONFIGURAR CITAS
    // =========================================================

    private fun configurarCitas() {

        citaAdapter =
            CitaAdapter(
                emptyList()
            ) { cita ->

                abrirDetalleCita(
                    cita
                )
            }


        binding.recyclerCitasEncargado.apply {

            layoutManager =
                LinearLayoutManager(
                    requireContext()
                )

            adapter =
                citaAdapter

            setHasFixedSize(false)

            isNestedScrollingEnabled =
                false
        }
    }


    // =========================================================
    // MOSTRAR CITAS
    // =========================================================

    private fun mostrarCitas() {

        if (_binding == null) {
            return
        }


        binding.contenedorPacientesCitas.visibility =
            View.GONE


        binding.contenedorCitasProgramadas.visibility =
            View.VISIBLE


        actualizarListaCitas()

        actualizarPestanaCitas()
    }


    // =========================================================
    // ACTUALIZAR LISTA DE CITAS
    // =========================================================

    private fun actualizarListaCitas() {

        if (_binding == null) {
            return
        }


        citaAdapter.actualizarLista(
            listaCitas
        )


        actualizarEstadoListaCitas()
    }


    // =========================================================
    // ESTADO LISTA DE CITAS
    // =========================================================

    private fun actualizarEstadoListaCitas() {

        if (_binding == null) {
            return
        }


        if (listaCitas.isEmpty()) {

            binding.txtSinCitas.visibility =
                View.VISIBLE

        } else {

            binding.txtSinCitas.visibility =
                View.GONE
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
    // MOSTRAR PACIENTES
    // =========================================================

    private fun mostrarPacientes() {

        if (_binding == null) {
            return
        }


        binding.contenedorPacientesCitas.visibility =
            View.VISIBLE


        binding.contenedorCitasProgramadas.visibility =
            View.GONE


        actualizarPestanaPacientes()
    }


    // =========================================================
    // ESTADO VISUAL - PACIENTES
    // =========================================================

    private fun actualizarPestanaPacientes() {

        if (_binding == null) {
            return
        }


        binding.tabPacientesCitas.setBackgroundResource(
            R.drawable.bg_tab_seleccionada
        )


        binding.tabCitasCitas.background =
            null


        binding.iconTabPacientesCitas.setColorFilter(
            Color.WHITE
        )


        binding.textTabPacientesCitas.setTextColor(
            Color.WHITE
        )


        binding.textTabPacientesCitas.setTypeface(
            null,
            Typeface.BOLD
        )


        binding.iconTabCitasCitas.setColorFilter(
            Color.rgb(
                152,
                162,
                179
            )
        )


        binding.textTabCitasCitas.setTextColor(
            Color.rgb(
                152,
                162,
                179
            )
        )


        binding.textTabCitasCitas.setTypeface(
            null,
            Typeface.NORMAL
        )
    }


    // =========================================================
    // ESTADO VISUAL - CITAS
    // =========================================================

    private fun actualizarPestanaCitas() {

        if (_binding == null) {
            return
        }


        binding.tabCitasCitas.setBackgroundResource(
            R.drawable.bg_tab_seleccionada
        )


        binding.tabPacientesCitas.background =
            null


        binding.iconTabCitasCitas.setColorFilter(
            Color.WHITE
        )


        binding.textTabCitasCitas.setTextColor(
            Color.WHITE
        )


        binding.textTabCitasCitas.setTypeface(
            null,
            Typeface.BOLD
        )


        binding.iconTabPacientesCitas.setColorFilter(
            Color.rgb(
                152,
                162,
                179
            )
        )


        binding.textTabPacientesCitas.setTextColor(
            Color.rgb(
                152,
                162,
                179
            )
        )


        binding.textTabPacientesCitas.setTypeface(
            null,
            Typeface.NORMAL
        )
    }


    // =========================================================
    // BUSCADOR
    // =========================================================

    private fun configurarBuscador() {

        binding.edtBuscarPaciente
            .addTextChangedListener {

                actualizarListaPacientes()
            }
    }


    // =========================================================
    // DETALLE DE CITA
    // =========================================================

    private fun abrirDetalleCita(
        cita: Cita
    ) {

        if (!isAdded) {
            return
        }


        val datos =
            Bundle()


        // =====================================================
        // ID REAL DEL BACKEND
        // =====================================================

        datos.putString(
            "idCita",
            cita.idCita
        )


        // =====================================================
        // ID DEL PACIENTE
        // =====================================================

        datos.putInt(
            "idPaciente",
            cita.idPaciente ?: -1
        )


        // =====================================================
        // DATOS QUE YA TENEMOS
        // =====================================================

        datos.putString(
            "nombrePaciente",
            cita.nombrePaciente
        )


        datos.putString(
            "fechaCita",
            cita.fecha
        )


        datos.putString(
            "horaCita",
            cita.hora
        )


        val fragment =
            DetalleCitaEncargadoFragment()


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
    // ACTUALIZACIÓN AUTOMÁTICA
    // =========================================================

    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                while (isActive) {

                    delay(
                        INTERVALO_ACTUALIZACION
                    )


                    if (!isActive) {
                        break
                    }


                    cargarDatos(
                        mostrarError = false
                    )
                }
            }
        }
    }


    // =========================================================
    // NOTIFICACIONES
    // =========================================================

    private fun configurarNotificaciones() {

        binding.btnNotificacionesCitas
            .setOnClickListener {

                if (!isAdded) {
                    return@setOnClickListener
                }


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

            navInicio =
                binding.navInicioCitas,

            iconInicio =
                binding.iconInicioCitas,

            textInicio =
                binding.textInicioCitas,


            navAsignarTurno =
                binding.navAsignarTurnoCitas,

            iconAsignarTurno =
                binding.iconAsignarTurnoCitas,

            textAsignarTurno =
                binding.textAsignarTurnoCitas,


            navCitas =
                binding.navCitasCitas,

            iconCitas =
                binding.iconCitasCitas,

            textCitas =
                binding.textCitasCitas,


            navPerfil =
                binding.navPerfilCitas,

            iconPerfil =
                binding.iconPerfilCitas,

            textPerfil =
                binding.textPerfilCitas,


            pantallaActual =
                NavegacionEncargado.Pantalla.CITAS,


            onInicio = {

                abrirSeccion(
                    HomeEncargadoFragment()
                )
            },


            onAsignarTurno = {

                abrirSeccion(
                    AsignarTurnoEncargadoFragment()
                )
            },


            onCitas = {
                // Ya estamos en Citas.
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

    private fun abrirSeccion(
        fragment: Fragment
    ) {

        if (!isAdded) {
            return
        }


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
    // ACTUALIZAR AL VOLVER A LA PANTALLA
    // =========================================================

    override fun onResume() {

        super.onResume()


        if (_binding != null) {

            /**
             * Actualización inmediata al regresar desde:
             * NuevaCitaEncargadoFragment
             * DetalleCitaEncargadoFragment
             * u otra pantalla.
             */
            cargarDatos(
                mostrarError = false
            )
        }
    }


    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}