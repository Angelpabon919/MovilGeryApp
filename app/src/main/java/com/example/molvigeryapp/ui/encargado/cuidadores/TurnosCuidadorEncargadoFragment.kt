package com.example.molvigeryapp.ui.encargado.cuidadores

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.model.TurnoUI
import com.example.molvigeryapp.data.repository.TurnoRepository
import com.example.molvigeryapp.databinding.FragmentTurnosCuidadorEncargadoBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TurnosCuidadorEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentTurnosCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    // =========================================================
    // ADAPTER Y REPOSITORY
    // =========================================================

    private lateinit var adapter: TurnoAdapter

    private val repository = TurnoRepository()

    // =========================================================
    // ID DEL CUIDADOR
    // =========================================================

    private var idUsuario: Int = 0

    // =========================================================
    // LISTAS DE TURNOS
    // =========================================================

    /*
     * Turnos que todavía están vigentes:
     * - Hoy
     * - Fechas futuras
     */
    private var turnosAsignados: List<TurnoUI> = emptyList()

    /*
     * Turnos cuya fecha final ya pasó.
     */
    private var turnosPasados: List<TurnoUI> = emptyList()

    // =========================================================
    // ESTADO DE LA PANTALLA
    // =========================================================

    /*
     * false = Turnos asignados
     * true  = Turnos pasados
     */
    private var mostrandoHistorial = false

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Recuperamos el ID del cuidador.
         *
         * Este ID viene desde:
         *
         * Inicio
         *   ↓
         * Cuidador
         *   ↓
         * Detalle del cuidador
         *   ↓
         * Turnos
         */
        idUsuario =
            arguments?.getInt("id_usuario") ?: 0
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
            FragmentTurnosCuidadorEncargadoBinding.inflate(
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

        // =====================================================
        // BOTÓN VOLVER
        // =====================================================

        binding.btnVolverTurnos.setOnClickListener {

            requireActivity()
                .supportFragmentManager
                .popBackStack()
        }

        // =====================================================
        // DATOS DEL CUIDADOR
        // =====================================================

        configurarDatosCuidador()

        // =====================================================
        // RECYCLERVIEW
        // =====================================================

        configurarRecyclerView()

        // =====================================================
        // SELECTOR DE TURNOS
        // =====================================================

        configurarSelectorTurnos()

        /*
         * Al entrar a la pantalla comenzamos
         * mostrando los turnos asignados.
         */
        mostrarTurnosAsignados()

        // =====================================================
        // CARGAR INFORMACIÓN DESDE LA API
        // =====================================================

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
    // CONFIGURAR RECYCLERVIEW
    // =========================================================

    private fun configurarRecyclerView() {

        adapter = TurnoAdapter(
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

            setHasFixedSize(true)

            /*
             * Como el RecyclerView está dentro de un
             * NestedScrollView, desactivamos su scroll
             * interno.
             */
            isNestedScrollingEnabled = false
        }
    }

    // =========================================================
    // CONFIGURAR SELECTOR
    // =========================================================

    private fun configurarSelectorTurnos() {

        // -----------------------------------------------------
        // TURNOS ASIGNADOS
        // -----------------------------------------------------

        binding.btnTurnosAsignados.setOnClickListener {

            mostrarTurnosAsignados()
        }

        // -----------------------------------------------------
        // TURNOS PASADOS
        // -----------------------------------------------------

        binding.btnTurnosPasados.setOnClickListener {

            mostrarTurnosPasados()
        }
    }

    // =========================================================
    // MOSTRAR TURNOS ASIGNADOS
    // =========================================================

    private fun mostrarTurnosAsignados() {

        mostrandoHistorial = false

        // Actualizar apariencia de las pestañas
        actualizarSelector()

        /*
         * El Adapter queda en modo normal.
         *
         * Esto permite:
         * - Editar
         * - Eliminar
         */
        adapter.establecerModoHistorial(false)

        // Mostrar solamente próximos/vigentes
        adapter.actualizarLista(
            turnosAsignados
        )

        // Información de la sección
        binding.txtInfoTurnos.text =
            "Estos son los turnos actualmente asignados al cuidador."

        // Mensaje cuando no existen turnos
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

        // Actualizar apariencia de las pestañas
        actualizarSelector()

        /*
         * Activamos modo historial.
         *
         * El Adapter ocultará:
         * - Editar
         * - Eliminar
         */
        adapter.establecerModoHistorial(true)

        // Mostrar solamente turnos pasados
        adapter.actualizarLista(
            turnosPasados
        )

        // Información de la sección
        binding.txtInfoTurnos.text =
            "Estos son los turnos que ya finalizaron y forman parte del historial."

        // Mensaje cuando no existe historial
        binding.txtSinTurnos.text =
            "Este cuidador todavía no tiene turnos pasados."

        actualizarEstadoLista(
            turnosPasados
        )
    }

    // =========================================================
    // ACTUALIZAR APARIENCIA DEL SELECTOR
    // =========================================================

    private fun actualizarSelector() {

        if (mostrandoHistorial) {

            // =================================================
            // TURNOS ASIGNADOS → NO SELECCIONADO
            // =================================================

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

            // =================================================
            // TURNOS PASADOS → SELECCIONADO
            // =================================================

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

            // =================================================
            // TURNOS ASIGNADOS → SELECCIONADO
            // =================================================

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

            // =================================================
            // TURNOS PASADOS → NO SELECCIONADO
            // =================================================

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
    // MOSTRAR / OCULTAR LISTA
    // =========================================================

    private fun actualizarEstadoLista(
        lista: List<TurnoUI>
    ) {

        if (lista.isEmpty()) {

            binding.txtSinTurnos.visibility =
                View.VISIBLE

            binding.recyclerTurnos.visibility =
                View.GONE

        } else {

            binding.txtSinTurnos.visibility =
                View.GONE

            binding.recyclerTurnos.visibility =
                View.VISIBLE
        }
    }

    // =========================================================
    // CARGAR TURNOS DESDE LA API
    // =========================================================

    private fun cargarTurnos() {

        lifecycleScope.launch {

            try {

                // =================================================
                // OBTENER TURNOS
                // =================================================

                val turnos =
                    repository.obtenerTurnos()

                // =================================================
                // OBTENER ASIGNACIONES
                // =================================================

                val asignaciones =
                    repository.obtenerAsignaciones()

                // =================================================
                // FILTRAR ASIGNACIONES DEL CUIDADOR
                // =================================================

                val asignacionesCuidador =
                    asignaciones.filter {

                        it.id_usuario ==
                                idUsuario
                    }

                // =================================================
                // RELACIONAR ASIGNACIÓN CON TURNO
                // =================================================

                val registros =
                    asignacionesCuidador
                        .mapNotNull { asignacion ->

                            val turno =
                                turnos.find {

                                    it.id_turno ==
                                            asignacion.id_turno
                                }

                            if (turno != null) {

                                RegistroTurno(
                                    asignacion = asignacion,
                                    turno = turno
                                )

                            } else {

                                null
                            }
                        }
                        .sortedBy {

                            convertirFechaCalendar(
                                it.turno.fecha
                            ).timeInMillis
                        }

                // =================================================
                // AGRUPAR TURNOS CONSECUTIVOS
                // =================================================

                val grupos =
                    mutableListOf<
                            MutableList<RegistroTurno>
                            >()

                for (registro in registros) {

                    val grupoAnterior =
                        grupos.lastOrNull()

                    if (
                        grupoAnterior == null ||
                        !puedeAgruparse(
                            grupoAnterior.last(),
                            registro
                        )
                    ) {

                        /*
                         * Comienza un nuevo grupo.
                         */

                        grupos.add(
                            mutableListOf(
                                registro
                            )
                        )

                    } else {

                        /*
                         * Se agrega al grupo anterior.
                         */

                        grupoAnterior.add(
                            registro
                        )
                    }
                }

                // =================================================
                // CONVERTIR GRUPOS A TurnoUI
                // =================================================

                val listaUI =
                    grupos.map { grupo ->

                        val primero =
                            grupo.first()

                        val ultimo =
                            grupo.last()

                        // -------------------------------------------------
                        // IDS DE TURNOS
                        // -------------------------------------------------

                        val idsTurnos =
                            grupo.mapNotNull {

                                it.turno.id_turno
                            }

                        // -------------------------------------------------
                        // IDS DE ASIGNACIONES
                        // -------------------------------------------------

                        val idsAsignaciones =
                            grupo.mapNotNull {

                                it.asignacion
                                    .id_asignacion_turno_usuario
                            }

                        // -------------------------------------------------
                        // TIPO DE TURNO
                        // -------------------------------------------------

                        val tipo =
                            if (
                                primero.turno.hora_inicio ==
                                "07:00:00"
                            ) {

                                "Diurno"

                            } else {

                                "Nocturno"
                            }

                        // -------------------------------------------------
                        // DURACIÓN
                        // -------------------------------------------------

                        val cantidadDias =
                            grupo.size

                        val duracion =
                            if (
                                cantidadDias == 1
                            ) {

                                "1 día"

                            } else {

                                "$cantidadDias días"
                            }

                        // -------------------------------------------------
                        // CREAR MODELO
                        // -------------------------------------------------

                        TurnoUI(

                            id =
                                idsTurnos
                                    .firstOrNull()
                                    ?: 0,

                            idsTurnos =
                                idsTurnos,

                            idsAsignaciones =
                                idsAsignaciones,

                            tipo =
                                tipo,

                            fechaInicio =
                                convertirFecha(
                                    primero.turno.fecha
                                ),

                            fechaFin =
                                convertirFecha(
                                    ultimo.turno.fecha
                                ),

                            horaInicio =
                                convertirHora(
                                    primero.turno.hora_inicio
                                ),

                            horaFin =
                                convertirHora(
                                    primero.turno.hora_fin
                                        ?: ""
                                ),

                            duracion =
                                duracion,

                            estado =
                                primero.asignacion.estado
                        )
                    }

                // =================================================
                // SEPARAR TURNOS
                // =================================================

                separarTurnos(
                    listaUI
                )

                // =================================================
                // RESPETAR PESTAÑA SELECCIONADA
                // =================================================

                if (mostrandoHistorial) {

                    mostrarTurnosPasados()

                } else {

                    mostrarTurnosAsignados()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "No se pudieron cargar los turnos: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // COMPROBAR SI DOS TURNOS PUEDEN AGRUPARSE
    // =========================================================

    private fun puedeAgruparse(
        anterior: RegistroTurno,
        actual: RegistroTurno
    ): Boolean {

        // =====================================================
        // COMPROBAR HORARIO
        // =====================================================

        val mismoHorario =
            anterior.turno.hora_inicio ==
                    actual.turno.hora_inicio &&
                    anterior.turno.hora_fin ==
                    actual.turno.hora_fin

        if (!mismoHorario) {
            return false
        }

        // =====================================================
        // COMPROBAR ESTADO
        // =====================================================

        val mismoEstado =
            anterior.asignacion.estado ==
                    actual.asignacion.estado

        if (!mismoEstado) {
            return false
        }

        // =====================================================
        // COMPROBAR TIPO / NOMBRE DEL TURNO
        // =====================================================

        val mismoTurno =
            anterior.turno.nombre ==
                    actual.turno.nombre

        if (!mismoTurno) {
            return false
        }

        // =====================================================
        // COMPROBAR FECHAS CONSECUTIVAS
        // =====================================================

        val fechaAnterior =
            convertirFechaCalendar(
                anterior.turno.fecha
            )

        val fechaActual =
            convertirFechaCalendar(
                actual.turno.fecha
            )

        /*
         * Creamos una copia de la fecha anterior
         * y le sumamos exactamente un día.
         */

        val diaSiguiente =
            fechaAnterior.clone() as Calendar

        diaSiguiente.add(
            Calendar.DAY_OF_YEAR,
            1
        )

        /*
         * Si el día siguiente coincide con la fecha
         * actual, entonces los turnos son consecutivos.
         */

        return diaSiguiente.timeInMillis ==
                fechaActual.timeInMillis
    }

    // =========================================================
    // SEPARAR TURNOS ASIGNADOS Y PASADOS
    // =========================================================

    private fun separarTurnos(
        lista: List<TurnoUI>
    ) {

        /*
         * Obtenemos la fecha actual sin hora.
         */

        val hoy =
            limpiarHora(
                Calendar.getInstance()
            )

        // =====================================================
        // TURNOS ASIGNADOS
        // =====================================================

        turnosAsignados =
            lista.filter { turno ->

                val fechaFin =
                    convertirFechaParaOrden(
                        turno.fechaFin
                    )

                /*
                 * Si todavía no ha terminado,
                 * permanece como turno asignado.
                 *
                 * También incluye el turno de hoy.
                 */

                !fechaFin.before(hoy)

            }.sortedBy {

                convertirFechaParaOrden(
                    it.fechaInicio
                ).timeInMillis
            }

        // =====================================================
        // TURNOS PASADOS
        // =====================================================

        turnosPasados =
            lista.filter { turno ->

                val fechaFin =
                    convertirFechaParaOrden(
                        turno.fechaFin
                    )

                /*
                 * Si la fecha final ya pasó,
                 * se mueve al historial.
                 */

                fechaFin.before(hoy)

            }.sortedByDescending {

                convertirFechaParaOrden(
                    it.fechaInicio
                ).timeInMillis
            }
    }

    // =========================================================
    // EDITAR TURNO
    // =========================================================

    private fun editarTurno(
        turno: TurnoUI
    ) {

        /*
         * Esta comprobación evita editar accidentalmente
         * un turno del historial.
         */

        if (mostrandoHistorial) {

            Toast.makeText(
                requireContext(),
                "Los turnos pasados no se pueden editar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            requireContext(),
            "Editar turno: ${turno.tipo}",
            Toast.LENGTH_SHORT
        ).show()

        /*
         * AQUÍ IMPLEMENTAREMOS DESPUÉS:
         *
         * - Abrir formulario de edición.
         * - Cargar fecha inicial.
         * - Cargar fecha final.
         * - Cargar tipo.
         * - Actualizar los registros.
         */
    }

    // =========================================================
    // ELIMINAR TURNO
    // =========================================================

    private fun eliminarTurno(
        turno: TurnoUI
    ) {

        /*
         * Los turnos históricos no pueden eliminarse.
         */

        if (mostrandoHistorial) {

            Toast.makeText(
                requireContext(),
                "Los turnos pasados no se pueden eliminar.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        Toast.makeText(
            requireContext(),
            "Eliminar turno: ${turno.tipo}",
            Toast.LENGTH_SHORT
        ).show()

        /*
         * AQUÍ IMPLEMENTAREMOS DESPUÉS:
         *
         * Primero eliminaremos la asignación
         * del cuidador.
         *
         * Después comprobaremos si el turno
         * sigue siendo utilizado por otro cuidador.
         *
         * No eliminaremos directamente el Turno
         * para evitar afectar a otros cuidadores.
         */
    }

    // =========================================================
    // CONVERTIR FECHA API → UI
    // =========================================================

    private fun convertirFecha(
        fecha: String
    ): String {

        return try {

            val entrada =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val salida =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            val date =
                entrada.parse(fecha)

            if (date != null) {

                salida.format(date)

            } else {

                fecha
            }

        } catch (e: Exception) {

            fecha
        }
    }

    // =========================================================
    // CONVERTIR HORA API → UI
    // =========================================================

    private fun convertirHora(
        hora: String
    ): String {

        return try {

            val entrada =
                SimpleDateFormat(
                    "HH:mm:ss",
                    Locale.getDefault()
                )

            val salida =
                SimpleDateFormat(
                    "hh:mm a",
                    Locale.getDefault()
                )

            val date =
                entrada.parse(hora)

            if (date != null) {

                salida.format(date)

            } else {

                hora
            }

        } catch (e: Exception) {

            hora
        }
    }

    // =========================================================
    // CONVERTIR FECHA PARA ORDENAMIENTO
    // =========================================================

    private fun convertirFechaParaOrden(
        fecha: String
    ): Calendar {

        val formato =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

        val calendar =
            Calendar.getInstance()

        try {

            val date =
                formato.parse(fecha)

            if (date != null) {

                calendar.time =
                    date
            }

        } catch (e: Exception) {

            // Se conserva la fecha actual
        }

        return limpiarHora(
            calendar
        )
    }

    // =========================================================
    // CONVERTIR FECHA API → CALENDAR
    // =========================================================

    private fun convertirFechaCalendar(
        fecha: String
    ): Calendar {

        val formato =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        val calendar =
            Calendar.getInstance()

        try {

            val date =
                formato.parse(fecha)

            if (date != null) {

                calendar.time =
                    date
            }

        } catch (e: Exception) {

            // Se conserva la fecha actual
        }

        return limpiarHora(
            calendar
        )
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
    // MODELO INTERNO
    // =========================================================

    private data class RegistroTurno(

        val asignacion:
        AsignacionTurnoUsuario,

        val turno:
        Turno
    )

    // =========================================================
    // DESTRUIR BINDING
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}