package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home_cuidador) {

    private var _binding: FragmentHomeCuidadorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
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
        observarMedicamentos()
        configurarNavegacion()
        configurarBotonNotificaciones()

        configurarBotonExitTurno()
        iniciarRevisionAutomaticaTurno()
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
        binding.layoutHeaderMedicamentos.setOnClickListener {
            isExpanded = !isExpanded

            if (isExpanded) {
                binding.layoutListaMedicamentos.visibility = View.VISIBLE
                binding.btnExpandMedicamentos.setImageResource(
                    R.drawable.ic_arrow_up
                )
            } else {
                binding.layoutListaMedicamentos.visibility = View.GONE
                binding.btnExpandMedicamentos.setImageResource(
                    R.drawable.ic_arrow_down
                )
            }
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

        viewModel.cargarPacientes()
        viewModel.cargarCatalogoMedicamentos()
        viewModel.cargarGruposMedicacion()
        viewModel.cargarFormulacionesMedicamentos()

        viewModel.pacientes.observe(viewLifecycleOwner) { listaPacientes ->

            val mapaPacientes = listaPacientes?.associateBy(
                { it.idPaciente },
                { "${it.nombre} ${it.apellido}".trim() }
            ) ?: emptyMap()

            viewModel.medicamentosCatalogo.observe(viewLifecycleOwner) { catalogo ->

                val mapaMedicamentos = catalogo?.associateBy(
                    { it.idMedicamento },
                    { it.nombreMedicamento }
                ) ?: emptyMap()

                fun mostrarMedicamentos(
                    lista: List<FormulacionMedicamento>
                ): String {

                    if (lista.isEmpty()) {
                        return "Sin medicamentos"
                    }

                    return lista.joinToString("\n\n") { med ->

                        val paciente =
                            mapaPacientes[med.idPaciente]
                                ?: "Paciente ${med.idPaciente}"

                        val medicamento =
                            mapaMedicamentos[med.idMedicamentos]
                                ?: "Medicamento"

                        val horaBase = med.horaAdministrada
                            ?.substringBefore(":")
                            ?.toIntOrNull()

                        val horaActual = java.util.Calendar
                            .getInstance()
                            .get(java.util.Calendar.HOUR_OF_DAY)

                        val horaProgramada = when {
                            med.idGrupo == 2 &&
                                    horaBase != null &&
                                    horaActual == ((horaBase + 12) % 24) -> {
                                (horaBase + 12) % 24
                            }

                            else -> horaBase
                        }

                        val horaMostrar = when {
                            horaProgramada == null -> "Sin hora"
                            horaProgramada == 0 -> "12:00 a. m."
                            horaProgramada < 12 ->
                                "${horaProgramada}:00 a. m."

                            horaProgramada == 12 ->
                                "12:00 p. m."

                            else ->
                                "${horaProgramada - 12}:00 p. m."
                        }

                        "👤 $paciente\n" +
                                "💊 $medicamento\n" +
                                "Dosis: ${med.dosis} - Vía: ${med.via} - Hora: $horaMostrar"
                    }
                }

                fun filtrarSeleccionados(
                    lista: List<FormulacionMedicamento>
                ): List<FormulacionMedicamento> {

                    val seleccionados =
                        viewModel.pacientesSeleccionadosHome.value
                            ?.map { it.idPaciente }
                            ?: emptyList()

                    Log.d(
                        "MEDICAMENTOS_HOME",
                        "TODAS LAS FORMULACIONES: $lista"
                    )

                    Log.d(
                        "MEDICAMENTOS_HOME",
                        "PACIENTES SELECCIONADOS: $seleccionados"
                    )

                    return lista.filter {
                        it.idPaciente in seleccionados
                    }
                }

                val horaActual = java.util.Calendar
                    .getInstance()
                    .get(java.util.Calendar.HOUR_OF_DAY)

                viewModel.medicamentosManana.observe(viewLifecycleOwner) { lista ->

                    if (horaActual in 6..11) {
                        binding.tvMedicamentosManana.text =
                            mostrarMedicamentos(filtrarSeleccionados(lista))
                    } else {
                        binding.tvMedicamentosManana.text = "Sin medicamentos"
                    }
                }

                viewModel.medicamentosTarde.observe(viewLifecycleOwner) { lista ->

                    if (horaActual in 12..17) {
                        binding.tvMedicamentosTarde.text =
                            mostrarMedicamentos(filtrarSeleccionados(lista))
                    } else {
                        binding.tvMedicamentosTarde.text = "Sin medicamentos"
                    }
                }

                viewModel.medicamentosNoche.observe(viewLifecycleOwner) { lista ->

                    if (horaActual >= 18 || horaActual < 6) {
                        binding.tvMedicamentosNoche.text =
                            mostrarMedicamentos(filtrarSeleccionados(lista))
                    } else {
                        binding.tvMedicamentosNoche.text = "Sin medicamentos"
                    }
                }
            }
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

            if (lista.isNotEmpty()) {
                viewModel.cargarFormulacionesMedicamentos()
            }

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