package com.example.molvigeryapp.ui.encargado.cuidadores

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.model.TurnoUI
import com.example.molvigeryapp.data.repository.TurnoRepository
import com.example.molvigeryapp.databinding.DialogEditarTurnoBinding
import com.example.molvigeryapp.databinding.DialogEliminarTurnoBinding
import com.example.molvigeryapp.databinding.FragmentTurnosCuidadorEncargadoBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TurnosCuidadorEncargadoFragment : Fragment() {

    private var _binding: FragmentTurnosCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    private val repository = TurnoRepository()

    private lateinit var adapter: TurnoAdapter

    // =========================================================
    // ID DEL CUIDADOR
    // =========================================================

    private var idUsuario: Int = -1

    private var turnosAsignados: List<TurnoUI> = emptyList()

    private var turnosPasados: List<TurnoUI> = emptyList()

    private var mostrandoHistorial = false

    // =========================================================
    // TURNOS DEL CATÁLOGO
    // =========================================================

    private var turnosDisponibles: List<Turno> = emptyList()

    // =========================================================
    // REGISTROS DE CADA TARJETA
    // =========================================================

    private var registrosPorTarjeta:
            Map<Int, List<RegistroTurno>> = emptyMap()


    companion object {
        private const val TAG = "TURNOS_ENCARGADO"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        idUsuario =
            arguments?.getInt("id_usuario", -1) ?: -1

        Log.d(
            TAG,
            "ID cuidador recibido: $idUsuario"
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentTurnosCuidadorEncargadoBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState
        )
        configurarDatosCuidador()
        configurarBotonVolver()
        configurarRecyclerView()
        configurarSelectorTurnos()
        mostrarTurnosAsignados()
        cargarTurnos()
    }

    // =========================================================
    // DATOS DEL CUIDADOR
    // =========================================================

    private fun configurarDatosCuidador() {

        binding.txtNombreCuidadorTurno.text =
            arguments?.getString("nombre")
                ?: "Cuidador"

        binding.txtCargoCuidadorTurno.text =
            arguments?.getString("cargo")
                ?: "Cuidador"
    }

    // =========================================================
    // BOTON VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding.btnVolverTurnos.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    private fun configurarRecyclerView() {

        adapter =
            TurnoAdapter(
                emptyList(),

                onEditar = { turno ->

                    editarTurno(turno)
                },

                onEliminar = { turno ->

                    eliminarTurno(turno)
                }
            )

        binding.recyclerTurnos.apply {

            layoutManager =
                LinearLayoutManager(
                    requireContext()
                )

            adapter =
                this@TurnosCuidadorEncargadoFragment.adapter

            isNestedScrollingEnabled = false

            setHasFixedSize(false)
        }
    }

    // =========================================================
    // SELECTOR
    // =========================================================

    private fun configurarSelectorTurnos() {

        binding.btnTurnosAsignados.setOnClickListener {

            mostrarTurnosAsignados()
        }

        binding.btnTurnosPasados.setOnClickListener {

            mostrarTurnosPasados()
        }
    }

    // =========================================================
    // MOSTRAR TURNOS ASIGNADOS
    // =========================================================

    private fun mostrarTurnosAsignados() {

        mostrandoHistorial = false

        actualizarSelector()

        adapter.establecerModoHistorial(false)

        adapter.actualizarLista(
            turnosAsignados
        )

        binding.txtInfoTurnos.text =
            "Estos son los turnos actualmente asignados al cuidador."

        binding.txtSinTurnos.text =
            "Este cuidador no tiene próximos turnos asignados."

        actualizarEstadoLista(
            turnosAsignados
        )
    }

    // =========================================================
    // MOSTRAR TURNOS PASADOS
    // =========================================================

    private fun mostrarTurnosPasados() {

        mostrandoHistorial = true

        actualizarSelector()

        adapter.establecerModoHistorial(true)

        adapter.actualizarLista(
            turnosPasados
        )

        binding.txtInfoTurnos.text =
            "Estos son los turnos que ya finalizaron y forman parte del historial."

        binding.txtSinTurnos.text =
            "Este cuidador todavía no tiene turnos pasados."

        actualizarEstadoLista(
            turnosPasados
        )
    }

    // =========================================================
    // ACTUALIZAR SELECTOR
    // =========================================================

    private fun actualizarSelector() {

        if (mostrandoHistorial) {

            binding.btnTurnosAsignados.apply {

                setBackgroundResource(
                    R.drawable.bg_selector_no_seleccionado
                )

                setTextColor(
                    Color.parseColor("#687078")
                )

                setTypeface(
                    null,
                    Typeface.NORMAL
                )
            }

            binding.btnTurnosPasados.apply {

                setBackgroundResource(
                    R.drawable.bg_selector_activo
                )

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        } else {

            binding.btnTurnosAsignados.apply {

                setBackgroundResource(
                    R.drawable.bg_selector_activo
                )

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

            binding.btnTurnosPasados.apply {

                setBackgroundResource(
                    R.drawable.bg_selector_no_seleccionado
                )

                setTextColor(
                    Color.parseColor("#687078")
                )

                setTypeface(
                    null,
                    Typeface.NORMAL
                )
            }
        }
    }

    // =========================================================
    // CARGAR TURNOS
    // =========================================================

    private fun cargarTurnos() {

        if (idUsuario <= 0) {

            Log.e(
                TAG,
                "ID de cuidador inválido: $idUsuario"
            )

            Toast.makeText(
                requireContext(),
                "No se encontró el ID del cuidador.",
                Toast.LENGTH_LONG
            ).show()

            mostrarSinTurnos()

            return
        }

        lifecycleScope.launch {

            try {

                // =================================================
                // OBTENER CATLOGO DE TURNOS
                // =================================================

                turnosDisponibles =
                    repository.obtenerTurnos()

                Log.d(
                    TAG,
                    "Turnos del catálogo: ${turnosDisponibles.size}"
                )

                // =================================================
                // OBTENER ASIGNACIONES
                // =================================================

                val asignaciones =
                    repository.obtenerAsignaciones()

                Log.d(
                    TAG,
                    "Asignaciones recibidas: ${asignaciones.size}"
                )

                // =================================================
                // FILTRAR POR CUIDADOR
                // =================================================

                val asignacionesCuidador =
                    asignaciones.filter { asignacion ->

                        asignacion.id_usuario ==
                                idUsuario
                    }

                // =================================================
                // RELACIONAR ASIGNACIÓN CON TURNO
                // =================================================

                val registros =
                    asignacionesCuidador.mapNotNull { asignacion ->

                        val turnoRelacionado =
                            turnosDisponibles.firstOrNull { turno ->

                                turno.id_turno ==
                                        asignacion.id_turno
                            }

                        if (turnoRelacionado == null) {

                            Log.e(
                                TAG,
                                "No se encontró el turno " +
                                        "${asignacion.id_turno}"
                            )

                            null

                        } else {

                            RegistroTurno(
                                asignacion =
                                    asignacion,

                                turno =
                                    turnoRelacionado
                            )
                        }
                    }

                // =================================================
                // ORDENAR
                // =================================================

                val registrosOrdenados =
                    registros.sortedWith(

                        compareBy(

                            {
                                obtenerFechaOrdenable(
                                    it
                                ).timeInMillis
                            },

                            {
                                normalizarHora(
                                    it.turno.hora_inicio
                                ) ?: ""
                            }
                        )
                    )

                // =================================================
                // AGRUPAR
                // =================================================

                val grupos =
                    agruparTurnos(
                        registrosOrdenados
                    )

                // =================================================
                // CONVERTIR A TARJETAS
                // =================================================

                val listaUI =
                    grupos.map { grupo ->

                        convertirGrupoTurnoUI(
                            grupo
                        )
                    }

                // =================================================
                // GUARDAR REGISTROS DE CADA TARJETA
                // =================================================

                registrosPorTarjeta =
                    grupos.associateBy { grupo ->

                        grupo.mapNotNull { registro ->

                            registro.asignacion
                                .id_asignacion_turno_usuario

                        }.firstOrNull() ?: 0
                    }

                // =================================================
                // SEPARAR ASIGNADOS Y PASADOS
                // =================================================

                separarTurnos(
                    listaUI
                )

                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }

                // =================================================
                // ACTUALIZAR PANTALLA
                // =================================================

                if (mostrandoHistorial) {

                    mostrarTurnosPasados()

                } else {

                    mostrarTurnosAsignados()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "ERROR AL CARGAR TURNOS",
                    e
                )

                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }

                Toast.makeText(
                    requireContext(),
                    "Error al cargar turnos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()

                mostrarSinTurnos()
            }
        }
    }

    // =========================================================
    // AGRUPAR TURNOS
    // =========================================================

    private fun agruparTurnos(
        registros: List<RegistroTurno>
    ): List<List<RegistroTurno>> {

        if (registros.isEmpty()) {
            return emptyList()
        }

        val gruposPorTipo =
            registros.groupBy { registro ->

                val turno =
                    registro.turno

                val tipo =
                    determinarTipoTurno(
                        turno
                    ).trim()
                        .lowercase(
                            Locale.getDefault()
                        )

                val horaInicio =
                    normalizarHora(
                        turno.hora_inicio
                    ) ?: ""

                val horaFin =
                    normalizarHora(
                        turno.hora_fin
                    ) ?: ""

                "$tipo|$horaInicio|$horaFin"
            }

        return gruposPorTipo.values
            .map { grupo ->

                grupo.sortedBy { registro ->

                    obtenerFechaOrdenable(
                        registro
                    ).timeInMillis
                }
            }
            .sortedBy { grupo ->

                obtenerFechaOrdenable(
                    grupo.first()
                ).timeInMillis
            }
    }

    // =========================================================
    // CONVERTIR GRUPO A TurnoUI
    // =========================================================

    private fun convertirGrupoTurnoUI(
        grupo: List<RegistroTurno>
    ): TurnoUI {

        val primero =
            grupo.first()

        val ultimo =
            grupo.last()

        val turnoInicial =
            primero.turno

        val idsTurnos =
            grupo.mapNotNull { registro ->

                registro.turno.id_turno
            }

        val idsAsignaciones =
            grupo.mapNotNull { registro ->

                registro.asignacion
                    .id_asignacion_turno_usuario
            }

        val fechaInicioOriginal =
            obtenerFechaTexto(
                primero
            )

        val fechaFinOriginal =
            obtenerFechaTexto(
                ultimo
            )

        val tipo =
            determinarTipoTurno(
                turnoInicial
            )

        val fechaInicioCalendar =
            obtenerFechaOrdenable(
                primero
            )

        val fechaFinCalendar =
            obtenerFechaOrdenable(
                ultimo
            )

        val diferenciaMillis =
            fechaFinCalendar.timeInMillis -
                    fechaInicioCalendar.timeInMillis

        val cantidadDias =
            ((diferenciaMillis /
                    (1000L * 60L * 60L * 24L))
                .toInt() + 1)
                .coerceAtLeast(1)

        val duracion =
            if (cantidadDias == 1) {

                "1 día"

            } else {

                "$cantidadDias días"
            }

        val estado =
            if (
                primero.asignacion.estado.isBlank()
            ) {

                "Asignado"

            } else {

                primero.asignacion.estado
            }

        return TurnoUI(

            id =
                idsTurnos.firstOrNull()
                    ?: 0,

            idsTurnos =
                idsTurnos,

            idsAsignaciones =
                idsAsignaciones,

            tipo =
                tipo,

            fechaInicio =
                convertirFecha(
                    fechaInicioOriginal
                ),

            fechaFin =
                convertirFecha(
                    fechaFinOriginal
                ),

            horaInicio =
                convertirHora(
                    turnoInicial.hora_inicio
                ),

            horaFin =
                convertirHora(
                    turnoInicial.hora_fin
                ),

            duracion =
                duracion,

            estado =
                estado
        )
    }

    // =========================================================
    // DETERMINAR TIPO
    // =========================================================

    private fun determinarTipoTurno(
        turno: Turno
    ): String {

        if (
            turno.nombre.isNotBlank()
        ) {

            return turno.nombre
        }

        val hora =
            normalizarHora(
                turno.hora_inicio
            )

        if (
            hora.isNullOrBlank()
        ) {

            return "Turno"
        }

        return if (
            hora <= "12:00"
        ) {

            "Diurno"

        } else {

            "Nocturno"
        }
    }

    // =========================================================
    // OBTENER FECHA
    // =========================================================

    private fun obtenerFechaTexto(
        registro: RegistroTurno
    ): String? {

        /* La fecha pertenece a la asignación */

        if (
            !registro.asignacion.fecha.isNullOrBlank()
        ) {

            return extraerSoloFecha(
                registro.asignacion.fecha
            )
        }

        /* Respaldo para datos antiguos */

        if (
            !registro.turno.fecha.isNullOrBlank()
        ) {

            return extraerSoloFecha(
                registro.turno.fecha
            )
        }

        return null
    }

    // =========================================================
    // FECHA ORDENABLE
    // =========================================================

    private fun obtenerFechaOrdenable(
        registro: RegistroTurno
    ): Calendar {

        return convertirFechaCalendar(
            obtenerFechaTexto(
                registro
            )
        )
    }

    // =========================================================
    // EXTRAER FECHA
    // =========================================================

    private fun extraerSoloFecha(
        fecha: String?
    ): String? {

        if (
            fecha.isNullOrBlank()
        ) {
            return null
        }

        if (
            fecha.length >= 10 &&
            fecha[4] == '-' &&
            fecha[7] == '-'
        ) {

            return fecha.substring(
                0,
                10
            )
        }

        val formatos =
            listOf(
                "yyyy-MM-dd",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy"
            )

        for (
        formatoTexto in formatos
        ) {

            try {

                val formato =
                    SimpleDateFormat(
                        formatoTexto,
                        Locale.getDefault()
                    )

                formato.isLenient = false

                val date =
                    formato.parse(
                        fecha
                    )

                if (
                    date != null
                ) {

                    return SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.getDefault()
                    ).format(
                        date
                    )
                }

            } catch (
                _: Exception
            ){
            }
        }

        return null
    }

    // =========================================================
    // NORMALIZAR HORA
    // =========================================================

    private fun normalizarHora(
        hora: String?
    ): String? {

        if (
            hora.isNullOrBlank()
        ) {

            return null
        }

        val valor =
            hora.trim()

        if (
            valor.matches(
                Regex(
                    "^\\d{2}:\\d{2}:\\d{2}$"
                )
            )
        ) {

            return valor.substring(
                0,
                5
            )
        }

        if (
            valor.matches(
                Regex(
                    "^\\d{2}:\\d{2}$"
                )
            )
        ) {

            return valor
        }

        val formatos =
            listOf(
                "HH:mm:ss",
                "HH:mm",
                "hh:mm a"
            )

        for (
        formatoTexto in formatos
        ) {

            try {

                val formato =
                    SimpleDateFormat(
                        formatoTexto,
                        Locale.getDefault()
                    )

                formato.isLenient = false

                val date =
                    formato.parse(
                        valor
                    )

                if (
                    date != null
                ) {

                    return SimpleDateFormat(
                        "HH:mm",
                        Locale.getDefault()
                    ).format(
                        date
                    )
                }

            } catch (
                _: Exception
            ) {
                // Continuar.
            }
        }

        return valor
    }

    // =========================================================
    // SEPARAR TURNOS
    // =========================================================

    private fun separarTurnos(
        lista: List<TurnoUI>
    ) {

        val hoy =
            limpiarHora(
                Calendar.getInstance()
            )

        turnosPasados =
            lista
                .filter { turno ->

                    val estado =
                        turno.estado
                            .trim()
                            .lowercase(
                                Locale.getDefault()
                            )

                    val fechaFin =
                        convertirFechaParaOrden(
                            turno.fechaFin
                        )

                    val estaFinalizado =
                        estado == "finalizado"

                    val fechaYaPaso =
                        fechaFin.before(hoy)

                    estaFinalizado || fechaYaPaso
                }
                .sortedByDescending { turno ->

                    convertirFechaParaOrden(
                        turno.fechaInicio
                    ).timeInMillis
                }

        turnosAsignados =
            lista
                .filter { turno ->

                    val estado =
                        turno.estado
                            .trim()
                            .lowercase(
                                Locale.getDefault()
                            )

                    val fechaFin =
                        convertirFechaParaOrden(
                            turno.fechaFin
                        )

                    val estaFinalizado =
                        estado == "finalizado"

                    val fechaYaPaso =
                        fechaFin.before(hoy)

                    !estaFinalizado && !fechaYaPaso
                }
                .sortedBy { turno ->

                    convertirFechaParaOrden(
                        turno.fechaInicio
                    ).timeInMillis
                }
    }

    // =========================================================
    // ACTUALIZAR ESTADO LISTA
    // =========================================================

    private fun actualizarEstadoLista(
        lista: List<TurnoUI>
    ) {

        if (
            lista.isEmpty()
        ) {

            mostrarSinTurnos()

        } else {

            binding.txtSinTurnos.visibility =
                View.GONE

            binding.recyclerTurnos.visibility =
                View.VISIBLE
        }
    }

    // =========================================================
    // MOSTRAR SIN TURNOS
    // =========================================================

    private fun mostrarSinTurnos() {

        binding.recyclerTurnos.visibility =
            View.GONE

        binding.txtSinTurnos.visibility =
            View.VISIBLE
    }

    // =========================================================
    // EDITAR TURNO
    // =========================================================

    private fun editarTurno(
        turno: TurnoUI
    ) {

        if (
            mostrandoHistorial
        ) {

            Toast.makeText(
                requireContext(),
                "Los turnos pasados no se pueden editar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        /* se busca los registros individuales que forman la tarheta de seleccion */

        val registros =
            obtenerRegistrosDeTarjeta(
                turno
            )

        if (
            registros.isEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "No se encontraron las asignaciones de este turno.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        mostrarDialogoEditar(
            turno,
            registros
        )
    }

    // =========================================================
    // OBTENER REGISTROS DE LA TARJETA
    // =========================================================

    private fun obtenerRegistrosDeTarjeta(
        turno: TurnoUI
    ): List<RegistroTurno> {

        /* Primero intentamos encontrar por cualquiera de los ID de asignacin
         */

        for (
        id in turno.idsAsignaciones
        ) {

            val registros =
                registrosPorTarjeta.entries
                    .firstOrNull { entry ->

                        entry.value.any { registro ->

                            registro.asignacion
                                .id_asignacion_turno_usuario ==
                                    id
                        }

                    }?.value

            if (
                !registros.isNullOrEmpty()
            ) {

                return registros
            }
        }

        return emptyList()
    }

    // =========================================================
    // MOSTRAR DIALOGO DE EDICIÓN
    // =========================================================

    private fun mostrarDialogoEditar(
        turno: TurnoUI,
        registros: List<RegistroTurno>
    ) {
        if (!isAdded) return

        val contexto = requireContext()

        // Conecta dialog_editar_turno
        val dialogBinding =
            DialogEditarTurnoBinding.inflate(layoutInflater)

        val dialog =
            AlertDialog.Builder(contexto)
                .setView(dialogBinding.root)
                .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // Conecta el selector de tipo con l tabla Turnos que es de turnos
        val nombresTurnos =
            turnosDisponibles
                .filter { it.estado }
                .map { it.nombre }
                .filter { it.isNotBlank() }
                .distinct()

        val adapterTurnos = ArrayAdapter(
            contexto,
            android.R.layout.simple_dropdown_item_1line,
            nombresTurnos
        )

        dialogBinding.selectorTipoTurno.setAdapter(adapterTurnos)
        dialogBinding.selectorTipoTurno.setText(turno.tipo, false)
        dialogBinding.selectorTipoTurno.setOnClickListener {
            dialogBinding.selectorTipoTurno.showDropDown()
        }

        // Conecta los campos de fecha con el DatePicker
        dialogBinding.selectorFechaInicio.setText(turno.fechaInicio)
        dialogBinding.selectorFechaInicio.setOnClickListener {
            seleccionarFecha(dialogBinding.selectorFechaInicio)
        }

        dialogBinding.selectorFechaFin.setText(turno.fechaFin)
        dialogBinding.selectorFechaFin.setOnClickListener {
            seleccionarFecha(dialogBinding.selectorFechaFin)
        }

        // Conecta el selector de estado
        val estados =
            listOf(
                turno.estado,
                "Asignado",
                "Pendiente",
                "Cancelado",
                "Finalizado"
            )
                .filter { it.isNotBlank() }
                .distinct()

        val adapterEstados = ArrayAdapter(
            contexto,
            android.R.layout.simple_dropdown_item_1line,
            estados
        )

        dialogBinding.selectorEstadoTurno.setAdapter(adapterEstados)
        dialogBinding.selectorEstadoTurno.setText(turno.estado, false)
        dialogBinding.selectorEstadoTurno.setOnClickListener {
            dialogBinding.selectorEstadoTurno.showDropDown()
        }

        // conecta el diaologo editar
        dialogBinding.btnCancelarEditar.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnGuardarEditar.setOnClickListener {
            val nombreTurno =
                dialogBinding.selectorTipoTurno.text.toString().trim()

            val fechaInicioTexto =
                dialogBinding.selectorFechaInicio.text.toString().trim()

            val fechaFinTexto =
                dialogBinding.selectorFechaFin.text.toString().trim()

            val estado =
                dialogBinding.selectorEstadoTurno.text.toString().trim()

            if (nombreTurno.isBlank()) {
                Toast.makeText(
                    contexto,
                    "Seleccione el tipo de turno.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (
                fechaInicioTexto.isBlank() ||
                fechaInicioTexto == "Sin fecha"
            ) {
                Toast.makeText(
                    contexto,
                    "Seleccione la fecha de inicio.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (
                fechaFinTexto.isBlank() ||
                fechaFinTexto == "Sin fecha"
            ) {
                Toast.makeText(
                    contexto,
                    "Seleccione la fecha de fin.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (estado.isBlank()) {
                Toast.makeText(
                    contexto,
                    "Seleccione el estado.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val fechaInicio = convertirFechaParaApi(fechaInicioTexto)
            val fechaFin = convertirFechaParaApi(fechaFinTexto)

            if (fechaInicio == null || fechaFin == null) {
                Toast.makeText(
                    contexto,
                    "Las fechas seleccionadas no son válidas.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val inicioCalendar = convertirFechaCalendar(fechaInicio)
            val finCalendar = convertirFechaCalendar(fechaFin)

            if (finCalendar.before(inicioCalendar)) {
                Toast.makeText(
                    contexto,
                    "La fecha final no puede ser anterior a la inicial.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val turnoSeleccionado =
                turnosDisponibles.firstOrNull { catalogo ->
                    catalogo.nombre.equals(
                        nombreTurno,
                        ignoreCase = true
                    ) && catalogo.estado
                }

            if (
                turnoSeleccionado == null ||
                turnoSeleccionado.id_turno == null
            ) {
                Toast.makeText(
                    contexto,
                    "No se encontró el turno seleccionado.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            dialog.dismiss()

            guardarEdicionTurno(
                registros = registros,
                turnoSeleccionado = turnoSeleccionado,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                estado = estado
            )
        }

        dialog.show()
    }

    // =========================================================
    // SELECCIONAR FECHA
    // =========================================================

    private fun seleccionarFecha(
        vista: TextView
    ) {

        val fechaActual =
            convertirFechaCalendar(
                convertirFechaParaApi(
                    vista.text.toString()
                )
            )

        val dialog =
            DatePickerDialog(
                requireContext(),

                { _, year, month, dayOfMonth ->

                    val fecha =
                        Calendar.getInstance()

                    fecha.set(
                        Calendar.YEAR,
                        year
                    )

                    fecha.set(
                        Calendar.MONTH,
                        month
                    )

                    fecha.set(
                        Calendar.DAY_OF_MONTH,
                        dayOfMonth
                    )

                    vista.text =
                        SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                        ).format(
                            fecha.time
                        )
                },

                fechaActual.get(
                    Calendar.YEAR
                ),

                fechaActual.get(
                    Calendar.MONTH
                ),

                fechaActual.get(
                    Calendar.DAY_OF_MONTH
                )
            )

        dialog.show()
    }

    // =========================================================
    // GUARDAR EDICIÓN
    // =========================================================

    private fun guardarEdicionTurno(
        registros: List<RegistroTurno>,
        turnoSeleccionado: Turno,
        fechaInicio: String,
        fechaFin: String,
        estado: String
    ) {

        lifecycleScope.launch {

            try {

                val idTurno =
                    turnoSeleccionado.id_turno

                if (
                    idTurno == null
                ) {

                    throw Exception(
                        "El turno seleccionado no tiene ID."
                    )
                }

                // =================================================
                // GENERAR TODAS LAS FECHAS NUEVAS
                // =================================================

                val fechasNuevas =
                    generarRangoFechas(
                        fechaInicio,
                        fechaFin
                    )

                if (
                    fechasNuevas.isEmpty()
                ) {

                    throw Exception(
                        "No se pudo generar el rango de fechas."
                    )
                }

                val asignacionesPorFecha =
                    registros
                        .mapNotNull { registro ->

                            val id =
                                registro.asignacion
                                    .id_asignacion_turno_usuario

                            val fecha =
                                extraerSoloFecha(
                                    registro.asignacion.fecha
                                )

                            if (
                                id != null &&
                                !fecha.isNullOrBlank()
                            ) {

                                fecha to registro.asignacion

                            } else {

                                null
                            }
                        }
                        .toMap()

                for (
                fechaNueva in fechasNuevas
                ) {

                    val asignacionExistente =
                        asignacionesPorFecha[
                            fechaNueva
                        ]

                    if (
                        asignacionExistente != null
                    ) {

                        /* la fecha ya existe solamente se actualiza id_turno, estado
                        el id_usuario se conserva */

                        val asignacionActualizada =
                            asignacionExistente.copy(

                                id_usuario =
                                    asignacionExistente.id_usuario,

                                id_turno =
                                    idTurno,

                                fecha =
                                    fechaNueva,

                                estado =
                                    estado
                            )

                        val idAsignacion =
                            asignacionExistente
                                .id_asignacion_turno_usuario

                        if (
                            idAsignacion == null
                        ) {
                            continue
                        }

                        repository.actualizarAsignacion(
                            idAsignacion,
                            asignacionActualizada
                        )

                        Log.d(
                            TAG,
                            "Asignación actualizada: " +
                                    "$idAsignacion -> $fechaNueva"
                        )

                    } else {

                        /* La fecha es nueva Creamos una nueva
                         asignacin usando EL MISMO cuidador
                         */

                        val idUsuarioCuidador =
                            registros.first()
                                .asignacion
                                .id_usuario

                        val nuevaAsignacion =
                            AsignacionTurnoUsuario(

                                id_asignacion_turno_usuario =
                                    null,

                                id_usuario =
                                    idUsuarioCuidador,

                                id_turno =
                                    idTurno,

                                fecha =
                                    fechaNueva,

                                estado =
                                    estado
                            )

                        repository.crearAsignacion(
                            nuevaAsignacion
                        )

                        Log.d(
                            TAG,
                            "Nueva asignación creada: " +
                                    "$fechaNueva"
                        )
                    }
                }

                val fechasNuevasSet =
                    fechasNuevas.toSet()

                for (
                registro in registros
                ) {

                    val fechaActual =
                        extraerSoloFecha(
                            registro.asignacion.fecha
                        )

                    val idAsignacion =
                        registro.asignacion
                            .id_asignacion_turno_usuario

                    if (
                        !fechaActual.isNullOrBlank() &&
                        !fechasNuevasSet.contains(
                            fechaActual
                        ) &&
                        idAsignacion != null
                    ) {

                        repository.eliminarAsignacion(
                            idAsignacion
                        )

                        Log.d(
                            TAG,
                            "Asignación eliminada: " +
                                    "$idAsignacion -> $fechaActual"
                        )
                    }
                }

                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }

                Toast.makeText(
                    requireContext(),
                    "Turno actualizado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                cargarTurnos()

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "ERROR AL EDITAR TURNO",
                    e
                )

                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }

                Toast.makeText(
                    requireContext(),
                    "Error al actualizar el turno: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // GENERAR RANGO DE FECHAS
    // =========================================================

    private fun generarRangoFechas(
        fechaInicio: String,
        fechaFin: String
    ): List<String> {

        val inicio =
            convertirFechaCalendar(
                fechaInicio
            )

        val fin =
            convertirFechaCalendar(
                fechaFin
            )

        val fechas =
            mutableListOf<String>()

        val actual =
            inicio.clone() as Calendar

        while (
            !actual.after(fin)
        ) {

            fechas.add(
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                ).format(
                    actual.time
                )
            )

            actual.add(
                Calendar.DAY_OF_MONTH,
                1
            )
        }

        return fechas
    }

    // =========================================================
    // CONVERTIR DD/MM/YYYY A YYYY-MM-DD
    // =========================================================

    private fun convertirFechaParaApi(
        fecha: String?
    ): String? {

        if (
            fecha.isNullOrBlank() ||
            fecha == "Sin fecha"
        ) {

            return null
        }

        /*
         * Si ya viene en formato API.
         */

        if (
            fecha.length >= 10 &&
            fecha[4] == '-' &&
            fecha[7] == '-'
        ) {

            return fecha.substring(
                0,
                10
            )
        }

        return try {

            val entrada =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            entrada.isLenient = false

            val salida =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val date =
                entrada.parse(
                    fecha
                )

            if (
                date != null
            ) {

                salida.format(
                    date
                )

            } else {

                null
            }

        } catch (
            _: Exception
        ) {

            null
        }
    }

    // =========================================================
    // CONVERTIR FECHA A DD/MM/YYYY
    // =========================================================

    private fun convertirFecha(
        fecha: String?
    ): String {

        if (
            fecha.isNullOrBlank()
        ) {

            return "Sin fecha"
        }

        return try {

            val entrada =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            entrada.isLenient = false

            val salida =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            val date =
                entrada.parse(
                    fecha
                )

            if (
                date != null
            ) {

                salida.format(
                    date
                )

            } else {

                "Sin fecha"
            }

        } catch (
            _: Exception
        ) {

            "Sin fecha"
        }
    }

    // =========================================================
    // CONVERTIR HORA
    // =========================================================

    private fun convertirHora(
        hora: String?
    ): String {

        if (
            hora.isNullOrBlank()
        ) {

            return "--"
        }

        return try {

            val horaNormalizada =
                normalizarHora(
                    hora
                )

            if (
                horaNormalizada.isNullOrBlank()
            ) {

                return "--"
            }

            val entrada =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            entrada.isLenient = false

            val salida =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                )

            val date =
                entrada.parse(
                    horaNormalizada
                )

            if (
                date != null
            ) {

                salida.format(
                    date
                )

            } else {

                "--"
            }

        } catch (
            _: Exception
        ) {

            "--"
        }
    }

    // =========================================================
    // CONVERTIR FECHA A CALENDAR
    // =========================================================

    private fun convertirFechaCalendar(
        fecha: String?
    ): Calendar {

        val calendar =
            Calendar.getInstance()

        if (
            fecha.isNullOrBlank()
        ) {

            return limpiarHora(
                calendar
            )
        }

        return try {

            val formato =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val date =
                formato.parse(
                    fecha
                )

            if (
                date != null
            ) {

                calendar.time =
                    date
            }

            limpiarHora(
                calendar
            )

        } catch (
            _: Exception
        ) {

            limpiarHora(
                calendar
            )
        }
    }

    // =========================================================
    // CONVERTIR FECHA A CALENDAR
    // =========================================================

    private fun convertirFechaParaOrden(
        fecha: String?
    ): Calendar {

        if (
            fecha.isNullOrBlank() ||
            fecha == "Sin fecha"
        ) {

            return convertirFechaCalendar(
                null
            )
        }

        return try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient = false

            val calendar =
                Calendar.getInstance()

            val date =
                formato.parse(
                    fecha
                )

            if (
                date != null
            ) {

                calendar.time =
                    date
            }

            limpiarHora(
                calendar
            )

        } catch (
            _: Exception
        ) {

            convertirFechaCalendar(
                null
            )
        }
    }

    // =========================================================
    // LIMPIAR HORA
    // =========================================================

    private fun limpiarHora(
        fecha: Calendar
    ): Calendar {

        val resultado =
            fecha.clone() as Calendar

        resultado.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        resultado.set(
            Calendar.MINUTE,
            0
        )

        resultado.set(
            Calendar.SECOND,
            0
        )

        resultado.set(
            Calendar.MILLISECOND,
            0
        )

        return resultado
    }

    // =========================================================
    // ELIMINAR TURNO
    // =========================================================

    private fun eliminarTurno(turno: TurnoUI) {
        if (mostrandoHistorial) {
            Toast.makeText(
                requireContext(),
                "Los turnos pasados no se pueden eliminar.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val registros = obtenerRegistrosDeTarjeta(turno)

        if (registros.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "No se encontraron las asignaciones.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Conecta dialog_eliminar_turno.xml
        val dialogBinding =
            DialogEliminarTurnoBinding.inflate(layoutInflater)

        dialogBinding.txtTipoEliminar.text = turno.tipo
        dialogBinding.txtFechaEliminar.text =
            "${turno.fechaInicio} - ${turno.fechaFin}"

        val dialog =
            AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // Conecta los botones con las acciones del dialogo
        dialogBinding.btnCancelarEliminar.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirmarEliminar.setOnClickListener {
            dialog.dismiss()
            eliminarAsignaciones(registros)
        }

        dialog.show()
    }

    // =========================================================
    // ELIMINAR TODAS LAS ASIGNACIONES DE UNA TARJETA
    // =========================================================

    private fun eliminarAsignaciones(
        registros: List<RegistroTurno>
    ) {

        lifecycleScope.launch {

            try {

                for (
                registro in registros
                ) {

                    val id =
                        registro.asignacion
                            .id_asignacion_turno_usuario

                    if (
                        id != null
                    ) {

                        repository.eliminarAsignacion(
                            id
                        )
                    }
                }

                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }

                Toast.makeText(
                    requireContext(),
                    "Turno eliminado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                cargarTurnos()

            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "ERROR AL ELIMINAR TURNO",
                    e
                )

                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }

                Toast.makeText(
                    requireContext(),
                    "Error al eliminar el turno: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private data class RegistroTurno(

        val asignacion:
        AsignacionTurnoUsuario,

        val turno:
        Turno
    )


    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}
