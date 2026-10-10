
package com.example.molvigeryapp.ui.encargado.citas

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import java.text.SimpleDateFormat
import java.util.Locale

class CitasEncargadoFragment : Fragment() {

    private var _binding: FragmentCitasEncargadoBinding? = null
    private val binding get() = requireNotNull(_binding)

    private lateinit var pacienteAdapter: PacienteCitaAdapter
    private lateinit var citaAdapter: CitaAdapter

    private var listaPacientes: List<Paciente> = emptyList()
    private var listaCitas: List<Cita> = emptyList()

    private var cargandoDatos = false

    companion object {
        private const val INTERVALO_ACTUALIZACION = 2_000L
        private const val TAG = "CITAS_ENCARGADO"
    }

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

        WindowInsetsEncargado.aplicar(
            root = binding.root,
            contenido = binding.scrollContenidoCitas,
            menuInferior = binding.bottomNavigationCitas
        )

        configurarPacientes()
        configurarCitas()
        configurarBuscador()
        configurarPestanas()
        configurarNotificaciones()
        configurarNavegacion()

        mostrarPacientes()
        cargarDatos()
        iniciarActualizacionAutomatica()

        ContadorNotificaciones.iniciar(
            fragment = this,
            badge = binding.txtNotificacionesCitas
        )
    }

    // =========================================================
    // PACIENTES
    // =========================================================

    private fun configurarPacientes() {
        pacienteAdapter = PacienteCitaAdapter(
            emptyList(),
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

    private fun actualizarListaPacientes() {
        if (_binding == null) return

        val textoBusqueda = binding.edtBuscarPaciente.text
            .toString()
            .trim()
            .lowercase(Locale.getDefault())

        val pacientesFiltrados = if (textoBusqueda.isBlank()) {
            listaPacientes
        } else {
            listaPacientes.filter { paciente ->
                "${paciente.nombre} ${paciente.apellido}"
                    .trim()
                    .lowercase(Locale.getDefault())
                    .contains(textoBusqueda)
            }
        }

        pacienteAdapter.actualizarLista(pacientesFiltrados)
    }

    private fun abrirPaciente(paciente: Paciente) {
        if (!isAdded) return

        val idPaciente = paciente.idPaciente
        if (idPaciente == null || idPaciente <= 0) {
            Toast.makeText(
                requireContext(),
                "El paciente no tiene un identificador válido.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val datos = Bundle().apply {
            putInt("idPaciente", idPaciente)
            putString("nombrePaciente", paciente.nombre)
            putString("apellidoPaciente", paciente.apellido)
            putInt("habitacionPaciente", paciente.habitacion ?: -1)
            putInt("camaPaciente", paciente.cama ?: -1)
        }

        val fragment = NuevaCitaEncargadoFragment().apply {
            arguments = datos
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // =========================================================
    // CITAS
    // =========================================================

    private fun configurarCitas() {
        citaAdapter = CitaAdapter(emptyList()) { cita ->
            abrirDetalleCita(cita)
        }

        binding.recyclerCitasEncargado.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = citaAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
        }
    }

    private fun ordenarCitasPorFechaRegistro(
        citas: List<Cita>
    ): List<Cita> {

        fun convertirFechaRegistro(fecha: String?): Long? {
            if (fecha.isNullOrBlank()) return null

            val formatos = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )

            for (patron in formatos) {
                try {
                    val formato = SimpleDateFormat(
                        patron,
                        Locale.getDefault()
                    ).apply {
                        isLenient = false
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }

                    val fechaParseada = formato.parse(fecha)
                    if (fechaParseada != null) {
                        return fechaParseada.time
                    }
                } catch (_: Exception) {
                    // Probar el siguiente formato.
                }
            }

            return null
        }

        return citas.withIndex()
            .sortedWith(
                compareByDescending<IndexedValue<Cita>> {
                    convertirFechaRegistro(it.value.fechaRegistro)
                        ?: Long.MIN_VALUE
                }.thenBy { it.index }
            )
            .map { it.value }
    }

    private fun cargarDatos(mostrarError: Boolean = true) {
        if (_binding == null || cargandoDatos) return

        cargandoDatos = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val pacientes = RetrofitClient.apiService.getPacientes()

                if (!isActive || _binding == null) return@launch

                listaPacientes = pacientes
                actualizarListaPacientes()

                val citas = CitasRepository.obtenerCitasDesdeApi()

                if (!isActive || _binding == null) return@launch

                listaCitas = ordenarCitasPorFechaRegistro(citas)
                actualizarListaCitas()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Error actualizando pacientes y citas", e)

                if (mostrarError && _binding != null) {
                    context?.let { contexto ->
                        Toast.makeText(
                            contexto,
                            "No se pudieron actualizar las citas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } finally {
                cargandoDatos = false
            }
        }
    }

    private fun actualizarListaCitas() {
        if (_binding == null) return

        actualizarListaCitasFiltradas()
    }

    //permite buscar entre las citas

    private fun actualizarListaCitasFiltradas() {
        if (_binding == null) return

        val textoBusqueda = binding.edtBuscarPaciente.text
            .toString()
            .trim()
            .lowercase(Locale.getDefault())

        val citasFiltradas = if (textoBusqueda.isBlank()) {
            listaCitas
        } else {
            listaCitas.filter { cita ->
                listOfNotNull(
                    cita.nombrePaciente,
                    cita.tipoCita,
                    cita.especialidad,
                    cita.motivo
                ).any { dato ->
                    dato.lowercase(Locale.getDefault())
                        .contains(textoBusqueda)
                }
            }
        }

        citaAdapter.actualizarLista(citasFiltradas)

        binding.txtSinCitas.visibility =
            if (citasFiltradas.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun actualizarEstadoListaCitas() {
        if (_binding == null) return

        binding.txtSinCitas.visibility =
            if (listaCitas.isEmpty()) View.VISIBLE else View.GONE
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

    private fun mostrarPacientes() {
        if (_binding == null) return

        binding.contenedorPacientesCitas.visibility = View.VISIBLE
        binding.contenedorCitasProgramadas.visibility = View.GONE

        actualizarPestanaPacientes()
    }

    private fun mostrarCitas() {
        if (_binding == null) return

        binding.contenedorPacientesCitas.visibility = View.GONE
        binding.contenedorCitasProgramadas.visibility = View.VISIBLE

        actualizarListaCitasFiltradas()
        actualizarPestanaCitas()
    }

    private fun actualizarPestanaPacientes() {
        if (_binding == null) return

        binding.tabPacientesCitas.setBackgroundResource(
            R.drawable.bg_tab_seleccionada
        )
        binding.tabCitasCitas.background = null

        binding.iconTabPacientesCitas.setColorFilter(Color.WHITE)
        binding.textTabPacientesCitas.setTextColor(Color.WHITE)
        binding.textTabPacientesCitas.setTypeface(null, Typeface.BOLD)

        val colorInactivo = Color.rgb(152, 162, 179)

        binding.iconTabCitasCitas.setColorFilter(colorInactivo)
        binding.textTabCitasCitas.setTextColor(colorInactivo)
        binding.textTabCitasCitas.setTypeface(null, Typeface.NORMAL)
    }

    private fun actualizarPestanaCitas() {
        if (_binding == null) return

        binding.tabCitasCitas.setBackgroundResource(
            R.drawable.bg_tab_seleccionada
        )
        binding.tabPacientesCitas.background = null

        binding.iconTabCitasCitas.setColorFilter(Color.WHITE)
        binding.textTabCitasCitas.setTextColor(Color.WHITE)
        binding.textTabCitasCitas.setTypeface(null, Typeface.BOLD)

        val colorInactivo = Color.rgb(152, 162, 179)

        binding.iconTabPacientesCitas.setColorFilter(colorInactivo)
        binding.textTabPacientesCitas.setTextColor(colorInactivo)
        binding.textTabPacientesCitas.setTypeface(null, Typeface.NORMAL)
    }

    // =========================================================
    // BUSCADOR
    // =========================================================

    private fun configurarBuscador() {
        binding.edtBuscarPaciente.addTextChangedListener {
            actualizarListaPacientes()
            actualizarListaCitasFiltradas()
        }
    }

    // =========================================================
    // DETALLE DE CITA
    // =========================================================


    private fun abrirDetalleCita(cita: Cita) {
        if (!isAdded) return

        // Usar el identificador que Gson recibe desde "id_cita".
        val identificador = cita.id
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        if (identificador == null) {
            Log.e(
                TAG,
                "La cita seleccionada no tiene un ID válido. " +
                        "id=${cita.id}, idCita=${cita.idCita}, cita=$cita"
            )

            Toast.makeText(
                requireContext(),
                "No se encontró el identificador de esta cita.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val datos = Bundle().apply {
            putString("idCita", identificador)
            putInt("idPaciente", cita.idPaciente ?: -1)
            putString("nombrePaciente", cita.nombrePaciente)
            putString("fechaCita", cita.fecha)
            putString("horaCita", cita.hora)
        }

        val fragment = DetalleCitaEncargadoFragment().apply {
            arguments = datos
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }


    // =========================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =========================================================

    private fun iniciarActualizacionAutomatica() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(INTERVALO_ACTUALIZACION)

                    if (!isActive) break

                    cargarDatos(mostrarError = false)
                }
            }
        }
    }

    // =========================================================
    // NOTIFICACIONES
    // =========================================================

    private fun configurarNotificaciones() {
        binding.btnNotificacionesCitas.setOnClickListener {
            if (!isAdded) return@setOnClickListener

            parentFragmentManager.beginTransaction()
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
            iconInicio = binding.iconInicioCitas,
            textInicio = binding.textInicioCitas,

            navAsignarTurno = binding.navAsignarTurnoCitas,
            iconAsignarTurno = binding.iconAsignarTurnoCitas,
            textAsignarTurno = binding.textAsignarTurnoCitas,

            navCitas = binding.navCitasCitas,
            iconCitas = binding.iconCitasCitas,
            textCitas = binding.textCitasCitas,

            navPerfil = binding.navPerfilCitas,
            iconPerfil = binding.iconPerfilCitas,
            textPerfil = binding.textPerfilCitas,

            pantallaActual = NavegacionEncargado.Pantalla.CITAS,

            onInicio = {
                abrirSeccion(HomeEncargadoFragment())
            },

            onAsignarTurno = {
                abrirSeccion(AsignarTurnoEncargadoFragment())
            },

            onCitas = {
                // Ya estamos en Citas.
            },

            onPerfil = {
                abrirSeccion(PerfilEncargadoFragment())
            }
        )
    }

    private fun abrirSeccion(fragment: Fragment) {
        if (!isAdded) return

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // =========================================================
    // ACTUALIZAR AL VOLVER
    // =========================================================

    override fun onResume() {
        super.onResume()

        if (_binding != null) {
            cargarDatos(mostrarError = false)
        }
    }

    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
