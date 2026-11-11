package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentHomeCuidadorBinding
import com.example.molvigeryapp.ui.cuidador.cierreturno.CierreTurnoFragment
import com.example.molvigeryapp.ui.cuidador.NavegacionCuidador
import com.example.molvigeryapp.ui.cuidador.agenda.AgendaFragment
import com.example.molvigeryapp.ui.cuidador.camara.EventosCamaraFragment
import com.example.molvigeryapp.ui.cuidador.notificaciones.NotificacionesFragment
import com.example.molvigeryapp.ui.cuidador.perfil.PerfilCuidadorFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.widget.TextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class HomeFragment : Fragment(R.layout.fragment_home_cuidador) {

    private var _binding: FragmentHomeCuidadorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }


    private var medicamentosExpandido = true


    private fun configurarExpansionMedicamentos() {

        // Sincronizar inicialmente el estado visual
        actualizarFlechaMedicamentos()

        binding.btnToggleMedicamentos.setOnClickListener {

            medicamentosExpandido = !medicamentosExpandido

            binding.rvMedicamentosHorario.visibility =
                if (medicamentosExpandido) View.VISIBLE
                else View.GONE

            actualizarFlechaMedicamentos()
        }
    }

    private fun actualizarFlechaMedicamentos() {

        binding.btnToggleMedicamentos.animate()
            .rotation(if (medicamentosExpandido) 180f else 0f)
            .setDuration(200)
            .start()

        binding.btnToggleMedicamentos.contentDescription =
            if (medicamentosExpandido)
                "Ocultar medicamentos"
            else
                "Mostrar medicamentos"
    }



    private var trabajoRevisionTurno: Job? = null

    // MODO DE PRUEBA: permite mostrar el botón sin esperar
    // los últimos 10 minutos del turno.
    // Cambiar a false cuando terminen las pruebas.
    private val MODO_PRUEBA_CIERRE = true

    @RequiresApi(Build.VERSION_CODES.O)
    private val formatoHoraTurno =
        DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT)

    private var isExpanded = false
    private lateinit var homeAdapter: HomeAdapter

    // =====================================================
// MEDICAMENTOS HOME
// =====================================================

    private lateinit var medicamentoAdapter: MedicamentoCuidadorAdapter

    private enum class HorarioMedicamento {
        MANANA,
        TARDE,
        NOCHE
    }

    private var horarioMedicamentoActual =
        obtenerHorarioActual()

    private var mapaMedicamentosHome: Map<Int, String> =
        emptyMap()

    private var mapaPacientesHome: Map<Int, String> =
        emptyMap()

    private var listaMedicamentosManana:
            List<FormulacionMedicamento> = emptyList()

    private var listaMedicamentosTarde:
            List<FormulacionMedicamento> = emptyList()

    private var listaMedicamentosNoche:
            List<FormulacionMedicamento> = emptyList()


    private var listaOriginalPacientes: List<Paciente> = emptyList()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeCuidadorBinding.bind(view)

        setupDespliegueMedicamentos()
        setupRecyclerViewPacientes()
        setupBuscador()
        setupBotonEditar()

        observarDatos()
        restaurarPacientesSeleccionadosHome()
        horarioSeleccionadoManualmente = false
        horarioMedicamentoActual = obtenerHorarioActual()

        observarMedicamentos()

        configurarNavegacion()
        configurarBotonNotificaciones()
        configurarExpansionMedicamentos()
        configurarBotonExitTurno()
        iniciarRevisionAutomaticaTurno()

    }

    override fun onPause() {
        super.onPause()

        handlerHorarioMedicamentos.removeCallbacks(
            actualizarHorarioMedicamentosRunnable
        )
    }

    override fun onResume() {
        super.onResume()

        if (view != null) {
            actualizarHorarioAutomatico()

            handlerHorarioMedicamentos.removeCallbacks(
                actualizarHorarioMedicamentosRunnable
            )

            handlerHorarioMedicamentos.post(
                actualizarHorarioMedicamentosRunnable
            )
        }
    }


    // =====================================================
    // NOTIFICACIONES
    // =====================================================

    private fun configurarBotonNotificaciones() {
        binding.btnNotification.setOnClickListener {
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

    // =====================================================
    // BOTÓN EXIT / CIERRE DE TURNO
    // =====================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private fun configurarBotonExitTurno() {

        binding.contenedorExitTurno.setOnClickListener {

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val preferencias = requireContext()
                        .getSharedPreferences("SESION", Context.MODE_PRIVATE)

                    val idUsuario = preferencias.getInt("ID_USUARIO", -1)

                    if (idUsuario <= 0) {
                        Toast.makeText(
                            requireContext(),
                            "No se encontró la sesión del cuidador",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    val api = RetrofitClient.apiService
                    val asignaciones = api.getAsignacionesTurno()
                    val turnos = api.getTurnos()

                    val ahora = LocalDateTime.now()
                    val hoy = ahora.toLocalDate()

                    Log.d(
                        "CIERRE_TURNO",
                        "Cuidador=$idUsuario, fecha=$hoy, " +
                                "modoPrueba=$MODO_PRUEBA_CIERRE"
                    )

                    val candidata = asignaciones.firstOrNull { asignacion ->

                        if (
                            asignacion.id_usuario != idUsuario ||
                            asignacion.estado.equals("Finalizado", true) ||
                            asignacion.estado.equals("Inactivo", true)
                        ) {
                            false
                        } else {

                            val fecha = asignacion.fecha
                                ?.take(10)
                                ?.let { texto ->
                                    runCatching {
                                        LocalDate.parse(texto)
                                    }.getOrNull()
                                }

                            val turno = turnos.firstOrNull {
                                it.id_turno == asignacion.id_turno
                            }

                            val horaInicio = turno?.hora_inicio
                                ?.let { parsearHora(it) }

                            val horaFin = turno?.hora_fin
                                ?.let { parsearHora(it) }

                            if (
                                fecha != hoy ||
                                turno == null ||
                                horaInicio == null ||
                                horaFin == null
                            ) {
                                Log.d(
                                    "CIERRE_TURNO",
                                    "Asignación descartada: " +
                                            "id=${asignacion.id_asignacion_turno_usuario}, " +
                                            "fecha=$fecha, hoy=$hoy, " +
                                            "inicio=${turno?.hora_inicio}, " +
                                            "fin=${turno?.hora_fin}, " +
                                            "estado=${asignacion.estado}"
                                )
                                false
                            } else {

                                val fin = fechaFinTurno(
                                    fecha,
                                    horaInicio,
                                    horaFin
                                )

                                val horarioPermitido =
                                    MODO_PRUEBA_CIERRE ||
                                            !ahora.isBefore(fin.minusMinutes(10))

                                Log.d(
                                    "CIERRE_TURNO",
                                    "Asignación=${asignacion.id_asignacion_turno_usuario}, " +
                                            "fin=$fin, permitido=$horarioPermitido"
                                )

                                horarioPermitido
                            }
                        }
                    }

                    val idAsignacion =
                        candidata?.id_asignacion_turno_usuario

                    if (idAsignacion == null) {

                        Toast.makeText(
                            requireContext(),
                            "No se encontró un turno disponible para cerrar",
                            Toast.LENGTH_SHORT
                        ).show()

                        revisarTurnoParaMostrarBoton()
                        return@launch
                    }

                    parentFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            CierreTurnoFragment.newInstance(idAsignacion)
                        )
                        .addToBackStack("cierre_turno")
                        .commit()

                } catch (e: Exception) {
                    Log.e(
                        "CIERRE_TURNO",
                        "Error abriendo cierre",
                        e
                    )

                    Toast.makeText(
                        requireContext(),
                        "No se pudo consultar el turno",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // =====================================================
    // NAVEGACIÓN CUIDADOR
    // =====================================================

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

            pantallaActual = NavegacionCuidador.Pantalla.INICIO,
            lifecycleOwner = viewLifecycleOwner,

            onInicio = {
                // Ya estamos en Inicio.
            },

            onAgenda = {
                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        AgendaFragment()
                    )
                    .commit()
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

    // =====================================================
    // EDITAR PACIENTES
    // =====================================================

    private fun setupBotonEditar() {
        binding.btnEditPacientes.setOnClickListener {
            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    PacientesListFragment()
                )
                .addToBackStack("editar_pacientes")
                .commit()
        }
    }

    // =====================================================
    // DESPLEGAR MEDICAMENTOS
    // =====================================================

    private fun setupDespliegueMedicamentos() {

        binding.btnToggleMedicamentos.setOnClickListener {

            isExpanded = !isExpanded

            binding.layoutListaMedicamentos.visibility =
                if (isExpanded) View.VISIBLE else View.GONE

            binding.btnToggleMedicamentos.animate()
                .rotation(if (isExpanded) 180f else 0f)
                .setDuration(200)
                .start()
        }
    }

    // =====================================================
    // LISTA DE PACIENTES
    // =====================================================

    private fun setupRecyclerViewPacientes() {

        homeAdapter = HomeAdapter { paciente ->
            viewModel.seleccionarPaciente(paciente)

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    DetallePacienteFragment()
                )
                .addToBackStack(null)
                .commit()
        }

        binding.rvPacientesAsignados.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientesAsignados.adapter = homeAdapter
    }

    // =====================================================
    // OBSERVAR MEDICAMENTOS
    // =====================================================


    private fun observarMedicamentos() {

        // Configurar RecyclerView
        configurarRecyclerMedicamentos()

        // Configurar selección de horarios
        configurarBotonesHorarioMedicamentos()

        // Cargar información desde la API
        viewModel.cargarPacientes()
        viewModel.cargarCatalogoMedicamentos()
        viewModel.cargarGruposMedicacion()

        // -------------------------------------------------
        // PACIENTES
        // -------------------------------------------------

        viewModel.pacientes.observe(viewLifecycleOwner) { pacientes ->

            mapaPacientesHome = pacientes.associateBy(
                { it.idPaciente ?: -1 },
                { "${it.nombre} ${it.apellido}".trim() }
            )

            actualizarListaMedicamentosHorario()
        }

        // -------------------------------------------------
        // CATÁLOGO DE MEDICAMENTOS
        // -------------------------------------------------

        viewModel.medicamentosCatalogo.observe(viewLifecycleOwner) { catalogo ->

            mapaMedicamentosHome = catalogo
                ?.associateBy(
                    { it.idMedicamento },
                    { it.nombreMedicamento }
                )
                ?: emptyMap()

            actualizarListaMedicamentosHorario()
        }

        // -------------------------------------------------
        // GRUPOS DE MEDICACIÓN
        // -------------------------------------------------

        viewModel.gruposMedicacion.observe(viewLifecycleOwner) { grupos ->

            if (!grupos.isNullOrEmpty()) {

                // Los grupos deben estar cargados antes
                // de separar las formulaciones por horario.
                viewModel.cargarFormulacionesMedicamentos()

            } else {

                listaMedicamentosManana = emptyList()
                listaMedicamentosTarde = emptyList()
                listaMedicamentosNoche = emptyList()

                actualizarContadoresMedicamentos()
                actualizarListaMedicamentosHorario()
            }
        }

        // -------------------------------------------------
        // MEDICAMENTOS DE LA MAÑANA
        // -------------------------------------------------

        viewModel.medicamentosManana.observe(viewLifecycleOwner) { lista ->

            listaMedicamentosManana = lista ?: emptyList()

            actualizarContadoresMedicamentos()
            actualizarListaMedicamentosHorario()
        }

        // -------------------------------------------------
        // MEDICAMENTOS DE LA TARDE
        // -------------------------------------------------

        viewModel.medicamentosTarde.observe(viewLifecycleOwner) { lista ->

            listaMedicamentosTarde = lista ?: emptyList()

            actualizarContadoresMedicamentos()
            actualizarListaMedicamentosHorario()
        }

        // -------------------------------------------------
        // MEDICAMENTOS DE LA NOCHE
        // -------------------------------------------------

        viewModel.medicamentosNoche.observe(viewLifecycleOwner) { lista ->

            listaMedicamentosNoche = lista ?: emptyList()

            actualizarContadoresMedicamentos()
            actualizarListaMedicamentosHorario()
        }

        // -------------------------------------------------
        // FORMULACIONES COMPLETAS
        // -------------------------------------------------

        viewModel.formulacionesMedicamentos.observe(viewLifecycleOwner) { lista ->

            Log.d(
                "MEDICAMENTOS_HOME",
                "Formulaciones recibidas: ${lista.size}"
            )
        }
    }


    private fun configurarRecyclerMedicamentos() {

        medicamentoAdapter = MedicamentoCuidadorAdapter()

        binding.rvMedicamentosHorario.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = medicamentoAdapter
            setHasFixedSize(false)
        }
    }

    private fun obtenerHorarioActual(): HorarioMedicamento {

        val horaActual = java.util.Calendar.getInstance()
            .get(java.util.Calendar.HOUR_OF_DAY)

        return when {

            horaActual >= 19 || horaActual < 6 ->
                HorarioMedicamento.NOCHE

            horaActual >= 12 ->
                HorarioMedicamento.TARDE

            else ->
                HorarioMedicamento.MANANA
        }
    }

    private fun configurarBotonesHorarioMedicamentos() {

        binding.btnHorarioManana.setOnClickListener {
            horarioSeleccionadoManualmente = true
            horarioMedicamentoActual = HorarioMedicamento.MANANA
            actualizarListaMedicamentosHorario()
        }

        binding.btnHorarioTarde.setOnClickListener {
            horarioSeleccionadoManualmente = true
            horarioMedicamentoActual = HorarioMedicamento.TARDE
            actualizarListaMedicamentosHorario()
        }

        binding.btnHorarioNoche.setOnClickListener {
            horarioSeleccionadoManualmente = true
            horarioMedicamentoActual = HorarioMedicamento.NOCHE
            actualizarListaMedicamentosHorario()
        }
    }


    private fun actualizarListaMedicamentosHorario() {

        // -------------------------------------------------
        // COLOR SEGÚN HORARIO
        // -------------------------------------------------

        val color = when (horarioMedicamentoActual) {

            HorarioMedicamento.MANANA ->
                Color.parseColor("#FFF3D6")

            HorarioMedicamento.TARDE ->
                Color.parseColor("#E8F1FF")

            HorarioMedicamento.NOCHE ->
                Color.parseColor("#EEE9FF")
        }

        binding.layoutListaMedicamentos.backgroundTintList =
            android.content.res.ColorStateList.valueOf(color)

        if (!::medicamentoAdapter.isInitialized) return

        val idsPacientesSeleccionados = viewModel
            .pacientesSeleccionadosHome
            .value
            .orEmpty()
            .mapNotNull { it.idPaciente }
            .toSet()

        val listaHorario = when (horarioMedicamentoActual) {

            HorarioMedicamento.MANANA ->
                listaMedicamentosManana

            HorarioMedicamento.TARDE ->
                listaMedicamentosTarde

            HorarioMedicamento.NOCHE ->
                listaMedicamentosNoche
        }

        val listaFiltrada = listaHorario.filter { medicamento ->
            medicamento.idPaciente in idsPacientesSeleccionados
        }

        medicamentoAdapter.actualizarLista(
            nuevaLista = listaFiltrada,
            nuevosMedicamentos = mapaMedicamentosHome,
            nuevosPacientes = mapaPacientesHome
        )

        when (horarioMedicamentoActual) {

            HorarioMedicamento.MANANA -> {

                binding.tvTituloHorarioActivo.text =
                    "Medicamentos - Mañana"

                binding.tvRangoHorario.text =
                    "06:00am – 12:00pm"
            }

            HorarioMedicamento.TARDE -> {

                binding.tvTituloHorarioActivo.text =
                    "Medicamentos - Tarde"

                binding.tvRangoHorario.text =
                    "12:00pm – 07:00pm"
            }

            HorarioMedicamento.NOCHE -> {

                binding.tvTituloHorarioActivo.text =
                    "Medicamentos - Noche"

                binding.tvRangoHorario.text =
                    "07:00pm – 06:00am"
            }
        }

        // -------------------------------------------------
        // MOSTRAR LISTA
        // -------------------------------------------------

        binding.rvMedicamentosHorario.visibility =
            View.VISIBLE

        actualizarFlechaMedicamentos()
    }


    private fun actualizarContadoresMedicamentos() {

        val idsPacientesSeleccionados = viewModel
            .pacientesSeleccionadosHome
            .value
            .orEmpty()
            .mapNotNull { it.idPaciente }
            .toSet()

        val cantidadManana = listaMedicamentosManana.count {
            it.idPaciente in idsPacientesSeleccionados
        }

        val cantidadTarde = listaMedicamentosTarde.count {
            it.idPaciente in idsPacientesSeleccionados
        }

        val cantidadNoche = listaMedicamentosNoche.count {
            it.idPaciente in idsPacientesSeleccionados
        }

        binding.tvCantidadManana.text =
            cantidadManana.toString()

        binding.tvCantidadTarde.text =
            cantidadTarde.toString()

        binding.tvCantidadNoche.text =
            cantidadNoche.toString()
    }

    // =====================================================
    // HORARIO AUTOMÁTICO DE MEDICAMENTOS
    // =====================================================

    private var horarioSeleccionadoManualmente = false

    private val handlerHorarioMedicamentos =
        android.os.Handler(android.os.Looper.getMainLooper())

    private val actualizarHorarioMedicamentosRunnable =
        object : Runnable {

            override fun run() {

                if (!isAdded || view == null) return

                actualizarHorarioAutomatico()

                handlerHorarioMedicamentos.postDelayed(
                    this,
                    60_000L
                )
            }
        }

    /* Actualiza automáticamente el horario de medicamentos segun la hora real del dispositivo
     * No utiliza barras ni indicadores de progreso */
    private fun actualizarHorarioAutomatico() {

        val horarioReal = obtenerHorarioActual()

        if (!horarioSeleccionadoManualmente &&
            horarioMedicamentoActual != horarioReal
        ) {

            horarioMedicamentoActual = horarioReal

            actualizarListaMedicamentosHorario()
        }
    }


    // =====================================================
    // OBSERVAR PACIENTES SELECCIONADOS
    // =====================================================

    private fun observarDatos() {

        viewModel.pacientesSeleccionadosHome.observe(viewLifecycleOwner) {
                listaSeleccionados ->

            val lista = listaSeleccionados ?: emptyList()
            listaOriginalPacientes = lista

            actualizarListaMedicamentosHorario()
            actualizarContadoresMedicamentos()

            val textoBusqueda =
                binding.etSearchPaciente.text.toString()

            if (textoBusqueda.isNotEmpty()) {
                filtrarLista(textoBusqueda)
            } else {
                homeAdapter.actualizarLista(lista)
            }
        }
    }

    // =====================================================
// RESTAURAR PACIENTES AL VOLVER A ABRIR HOME
// =====================================================

    private fun restaurarPacientesSeleccionadosHome() {
        val preferencias = requireContext()
            .getSharedPreferences("SESION", Context.MODE_PRIVATE)

        val idUsuario = preferencias.getInt("ID_USUARIO", -1)

        if (idUsuario <= 0) {
            Log.e("HOME_PACIENTES", "No se encontró el ID del cuidador")
            return
        }

        Log.d(
            "HOME_PACIENTES",
            "Restaurando pacientes seleccionados del cuidador $idUsuario"
        )

        viewModel.cargarPacientesParaSeleccion(
            idUsuario,
            preferencias
        )
    }

    // =====================================================
    // BUSCADOR
    // =====================================================

    private fun setupBuscador() {

        binding.etSearchPaciente.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filtrarLista(s?.toString() ?: "")
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.GINGERBREAD)
    private fun filtrarLista(texto: String) {

        val consulta = texto.trim().lowercase(Locale.ROOT)

        if (consulta.isEmpty()) {
            homeAdapter.actualizarLista(listaOriginalPacientes)
        } else {

            val listaFiltrada = listaOriginalPacientes.filter { paciente ->

                val nombreCompleto =
                    "${paciente.nombre} ${paciente.apellido}"
                        .lowercase(Locale.ROOT)

                val documento =
                    paciente.numeroDocumento?.lowercase(Locale.ROOT) ?: ""

                val habitacion =
                    paciente.habitacion?.toString()?.lowercase(Locale.ROOT) ?: ""

                nombreCompleto.contains(consulta) ||
                        documento.contains(consulta) ||
                        habitacion.contains(consulta)
            }

            homeAdapter.actualizarLista(listaFiltrada)
        }
    }

    // =====================================================
    // REVISIÓN AUTOMÁTICA DEL TURNO
    // =====================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private fun iniciarRevisionAutomaticaTurno() {

        trabajoRevisionTurno?.cancel()

        trabajoRevisionTurno = viewLifecycleOwner.lifecycleScope.launch {

            while (isActive) {
                revisarTurnoParaMostrarBoton()
                delay(10_000L)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun revisarTurnoParaMostrarBoton() {

        try {
            val preferencias = requireContext()
                .getSharedPreferences("SESION", Context.MODE_PRIVATE)

            val idUsuario = preferencias.getInt("ID_USUARIO", -1)

            if (idUsuario <= 0) {
                binding.contenedorExitTurno.visibility = View.GONE
                return
            }

            val api = RetrofitClient.apiService
            val asignaciones = api.getAsignacionesTurno()
            val turnos = api.getTurnos()

            val ahora = LocalDateTime.now()
            val hoy = ahora.toLocalDate()

            val asignacionesActivas = asignaciones.filter { asignacion ->

                asignacion.id_usuario == idUsuario &&
                        !asignacion.estado.equals("Finalizado", true) &&
                        !asignacion.estado.equals("Inactivo", true)
            }

            val debeMostrar = MODO_PRUEBA_CIERRE ||
                    asignacionesActivas.any { asignacion ->

                        val fechaAsignacion = asignacion.fecha
                            ?.take(10)
                            ?.let { texto ->
                                runCatching {
                                    LocalDate.parse(texto)
                                }.getOrNull()
                            }

                        val turno = turnos.firstOrNull {
                            it.id_turno == asignacion.id_turno
                        }

                        val horaInicio = turno?.hora_inicio
                            ?.let { parsearHora(it) }

                        val horaFin = turno?.hora_fin
                            ?.let { parsearHora(it) }

                        if (
                            fechaAsignacion == null ||
                            turno == null ||
                            horaInicio == null ||
                            horaFin == null
                        ) {
                            false
                        } else {

                            val finTurno = fechaFinTurno(
                                fechaAsignacion,
                                horaInicio,
                                horaFin
                            )

                            !ahora.isBefore(finTurno.minusMinutes(10))
                        }
                    }

            binding.contenedorExitTurno.visibility =
                if (debeMostrar) View.VISIBLE else View.GONE

            Log.d(
                "CIERRE_TURNO",
                "Cuidador=$idUsuario, mostrarBoton=$debeMostrar, " +
                        "modoPrueba=$MODO_PRUEBA_CIERRE, " +
                        "asignaciones=${asignaciones.size}, fecha=$hoy"
            )

        } catch (e: Exception) {

            Log.e(
                "CIERRE_TURNO",
                "Error revisando el turno para mostrar el botón",
                e
            )
        }
    }

    // =====================================================
    // PARSEO DE HORAS
    // =====================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parsearHora(texto: String): LocalTime? {

        val formatos = listOf(
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT),
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
        )

        return formatos.firstNotNullOfOrNull { formato ->
            runCatching {
                LocalTime.parse(texto.trim(), formato)
            }.getOrNull()
        }
    }

    // =====================================================
    // FECHA Y HORA FINAL DEL TURNO
    // =====================================================

    @RequiresApi(Build.VERSION_CODES.O)
    private fun fechaFinTurno(
        fechaAsignacion: LocalDate,
        horaInicio: LocalTime,
        horaFin: LocalTime
    ): LocalDateTime {

        val fechaFin = if (horaFin.isBefore(horaInicio)) {
            fechaAsignacion.plusDays(1)
        } else {
            fechaAsignacion
        }

        return LocalDateTime.of(fechaFin, horaFin)
    }

    // =====================================================
    // DESTRUIR VISTA
    // =====================================================

    override fun onDestroyView() {
        trabajoRevisionTurno?.cancel()
        trabajoRevisionTurno = null

        super.onDestroyView()
        _binding = null
    }
}
