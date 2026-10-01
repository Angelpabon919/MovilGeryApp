package com.example.molvigeryapp.ui.encargado.asignarturno

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.model.Usuario
import com.example.molvigeryapp.databinding.FragmentAsignarTurnoEncargadoBinding
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.WindowInsetsEncargado
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.ContadorNotificaciones
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Job
import java.util.UUID

class AsignarTurnoEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentAsignarTurnoEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =========================================================
    // CALENDARIO
    // =========================================================

    private val meses =
        mutableListOf<Calendar>()

    private var mesMostrado: Calendar =
        Calendar.getInstance()

    private var fechaInicio: Calendar? = null

    private var fechaFin: Calendar? = null


    // =========================================================
    // TURNOS
    // =========================================================

    private var turnosDisponibles: List<Turno> =
        emptyList()

    private var turnoSeleccionado: Turno? =
        null


    // =========================================================
    // CUIDADORES
    // =========================================================

    private var cuidadoresSeleccionados: List<Usuario> =
        emptyList()

    private lateinit var cuidadorAdapter:
            CuidadorAsignarTurnoAdapter


    // =========================================================
    // CUIDADOR INICIAL
    // =========================================================

    private var idCuidadorInicial: Int? = null

    // =========================================================
    // VALIDACIÓN DE CONFLICTOS
    // =========================================================

    private var validacionConflictosJob: Job? = null

    private var hayConflictosTurno = false

    private var verificandoConflictos = false

    private var errorVerificacionConflictos = false


    // =========================================================
    // FORMATO DE FECHAS
    // =========================================================

    private val formatoFecha =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

    private val formatoFechaVisible =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )


    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentAsignarTurnoEncargadoBinding.inflate(
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
            contenido = binding.scrollAsignarTurno,
            menuInferior = binding.bottomNavigationAsignarTurno
        )

        idCuidadorInicial =
            arguments?.getInt("id_usuario")
                ?.takeIf { it > 0 }


        configurarNavegacion()

        configurarSelectorTurno()

        configurarCalendario()

        configurarCuidadores()

        configurarBotonAsignar()

        cargarTurnos()

        cargarCuidadores()


        // =====================================================
        // CONTADOR DE NOTIFICACIONES
        // =====================================================

        ContadorNotificaciones.iniciar(
            fragment = this,
            badge = binding.txtNotificacionesAsignarTurno
        )
    }


    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }


    // =========================================================
    // NAVEGACIÓN
    // =========================================================

    private fun configurarNavegacion() {

        NavegacionEncargado.configurar(

            navInicio =
                binding.navInicioAsignarTurno,

            iconInicio =
                binding.iconInicioAsignarTurno,

            textInicio =
                binding.textInicioAsignarTurno,


            navAsignarTurno =
                binding.navAsignarTurnoAsignarTurno,

            iconAsignarTurno =
                binding.iconAsignarTurnoAsignarTurno,

            textAsignarTurno =
                binding.textAsignarTurnoAsignarTurno,


            navCitas =
                binding.navCitasAsignarTurno,

            iconCitas =
                binding.iconCitasAsignarTurno,

            textCitas =
                binding.textCitasAsignarTurno,


            navPerfil =
                binding.navPerfilAsignarTurno,

            iconPerfil =
                binding.iconPerfilAsignarTurno,

            textPerfil =
                binding.textPerfilAsignarTurno,


            pantallaActual =
                NavegacionEncargado.Pantalla.ASIGNAR_TURNO,


            onInicio = {

                if (!isAdded) {
                    return@configurar
                }

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeEncargadoFragment()
                    )
                    .commit()
            },


            onAsignarTurno = {
                // Ya estamos en esta pantalla.
            },


            onCitas = {

                if (!isAdded) {
                    return@configurar
                }

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CitasEncargadoFragment()
                    )
                    .commit()
            },


            onPerfil = {

                if (!isAdded) {
                    return@configurar
                }

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        PerfilEncargadoFragment()
                    )
                    .commit()
            }
        )
    }


    // =========================================================
    // SELECTOR DE TIPO DE TURNO
    // =========================================================

    private fun configurarSelectorTurno() {

        // Estado inicial del selector.
        binding.selectorTipoTurno.setText(
            "",
            false
        )


        // Al iniciar no debe aparecer ningún icono.
        binding.selectorTipoTurno
            .setCompoundDrawablesRelative(
                null,
                null,
                null,
                null
            )


        // =====================================================
        // CUANDO SE SELECCIONA UN TURNO
        // =====================================================

        binding.selectorTipoTurno
            .setOnItemClickListener { _, _, position, _ ->

                if (
                    position >= 0 &&
                    position < turnosDisponibles.size
                ) {

                    seleccionarTurno(
                        turnosDisponibles[position]
                    )
                }
            }
    }


    // =========================================================
    // CARGAR TURNOS
    // =========================================================

    private fun cargarTurnos() {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val turnos =
                    RetrofitClient.apiService
                        .getTurnos()


                // =================================================
                // COMPROBAR VISTA
                // =================================================

                if (_binding == null) {
                    return@launch
                }


                // =================================================
                // FILTRAR TURNOS ACTIVOS
                // =================================================

                turnosDisponibles =
                    turnos.filter {
                        it.estado
                    }


                if (turnosDisponibles.isEmpty()) {

                    val contexto =
                        context ?: return@launch

                    Toast.makeText(
                        contexto,
                        "No hay turnos disponibles en el sistema.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }


                // =================================================
                // CREAR ADAPTADOR PERSONALIZADO
                // =================================================

                val adapter =
                    object : ArrayAdapter<String>(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        turnosDisponibles.map { turno ->

                            turno.nombre.ifBlank {
                                "Turno ${turno.id_turno}"
                            }

                        }
                    ) {

                        // =================================================
                        // VISTA QUE QUEDA EN EL SELECTOR
                        // =================================================

                        override fun getView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {

                            val textView =
                                super.getView(
                                    position,
                                    convertView,
                                    parent
                                ) as TextView


                            val turno =
                                turnosDisponibles[position]


                            configurarVistaTurno(
                                textView = textView,
                                turno = turno
                            )


                            return textView
                        }


                        // =================================================
                        // VISTA DEL DESPLEGABLE
                        // =================================================

                        override fun getDropDownView(
                            position: Int,
                            convertView: View?,
                            parent: ViewGroup
                        ): View {

                            val turno =
                                turnosDisponibles[position]


                            // =================================================
                            // CONTENEDOR DE LA OPCIÓN
                            // =================================================

                            val contenedor =
                                LinearLayout(
                                    requireContext()
                                ).apply {

                                    orientation =
                                        LinearLayout.VERTICAL

                                    layoutParams =
                                        ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )

                                    setBackgroundColor(
                                        Color.TRANSPARENT
                                    )
                                }


                            // =================================================
                            // TEXTO DEL TURNO
                            // =================================================

                            val textView =
                                TextView(
                                    requireContext()
                                )


                            textView.layoutParams =
                                LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    dpApx(62)
                                )


                            configurarVistaTurno(
                                textView = textView,
                                turno = turno
                            )


                            contenedor.addView(
                                textView
                            )


                            // =================================================
                            // LÍNEA DIVISORIA
                            // =================================================

                            if (
                                position <
                                turnosDisponibles.lastIndex
                            ) {

                                val divider =
                                    View(
                                        requireContext()
                                    )


                                val parametros =
                                    LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        dpApx(1)
                                    )


                                parametros.leftMargin =
                                    dpApx(20)

                                parametros.rightMargin =
                                    dpApx(20)


                                divider.layoutParams =
                                    parametros


                                divider.setBackgroundColor(
                                    Color.parseColor(
                                        "#E4E7EC"
                                    )
                                )


                                contenedor.addView(
                                    divider
                                )
                            }


                            return contenedor
                        }
                    }


                // =================================================
                // ASIGNAR ADAPTADOR
                // =================================================

                binding.selectorTipoTurno
                    .setAdapter(adapter)


            } catch (e: CancellationException) {

                // La pantalla fue abandonada.
                // La cancelación es normal.
                throw e

            } catch (e: Exception) {

                android.util.Log.e(
                    "ASIGNAR_TURNO",
                    "Error al cargar los turnos",
                    e
                )


                val contexto =
                    context ?: return@launch


                if (_binding == null) {
                    return@launch
                }


                Toast.makeText(
                    contexto,
                    "No se pudieron cargar los turnos",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // CONFIGURAR VISUALMENTE EL TURNO
    // =========================================================

    private fun configurarVistaTurno(
        textView: TextView,
        turno: Turno
    ) {

        val icono =
            obtenerIconoTurno(
                turno
            )


        // =====================================================
        // ICONO
        // =====================================================

        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            icono,
            null,
            null,
            null
        )


        // =====================================================
        // ESPACIO ENTRE ICONO Y TEXTO
        // =====================================================

        textView.compoundDrawablePadding =
            dpApx(12)


        // =====================================================
        // ESTILO
        // =====================================================

        textView.setTextColor(
            Color.parseColor(
                "#1D2939"
            )
        )


        textView.textSize =
            14f


        textView.gravity =
            Gravity.CENTER_VERTICAL


        textView.setPadding(
            dpApx(16),
            dpApx(12),
            dpApx(16),
            dpApx(12)
        )
    }


    // =========================================================
    // OBTENER ICONO DEL TURNO
    // =========================================================

    private fun obtenerIconoTurno(
        turno: Turno
    ): Drawable? {

        val nombre =
            turno.nombre
                .trim()
                .lowercase(
                    Locale.getDefault()
                )


        return when {

            nombre.contains("diurno") -> {

                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.sun
                )
            }


            nombre.contains("nocturno") -> {

                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.moon
                )
            }


            else -> {

                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.clock
                )
            }
        }
    }


    // =========================================================
    // SELECCIONAR TURNO
    // =========================================================

    private fun seleccionarTurno(
        turno: Turno
    ) {

        turnoSeleccionado =
            turno


        // =====================================================
        // MOSTRAR NOMBRE EN EL MISMO SELECTOR
        // =====================================================

        binding.selectorTipoTurno.setText(
            turno.nombre,
            false
        )


        // =====================================================
        // OBTENER ICONO
        // =====================================================

        val icono =
            obtenerIconoTurno(
                turno
            )


        // =====================================================
        // MOSTRAR ICONO DENTRO DEL SELECTOR
        // =====================================================

        binding.selectorTipoTurno
            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                icono,
                null,
                null,
                null
            )


        // =====================================================
        // ESPACIO ENTRE ICONO Y TEXTO
        // =====================================================

        binding.selectorTipoTurno
            .compoundDrawablePadding =
            dpApx(10)


        // =====================================================
        // COLOR DEL TEXTO
        // =====================================================

        binding.selectorTipoTurno.setTextColor(
            Color.parseColor(
                "#1D2939"
            )
        )


        // =====================================================
        // INFORMACIÓN DEL TURNO
        // =====================================================

        binding.txtNombreTurnoSeleccionado.text =
            turno.nombre


        binding.txtHorarioTurnoSeleccionado.text =
            "${turno.hora_inicio} - ${turno.hora_fin ?: "Sin hora final"}"


        binding.cardInformacionTurno.visibility =
            View.VISIBLE


        // =====================================================
        // ACTUALIZAR ESTADO
        // =====================================================

        actualizarResumenFechas()

        actualizarConflictosEnTiempoReal()
    }


    // =========================================================
    // CONFIGURAR CALENDARIO
    // =========================================================

    private fun configurarCalendario() {

        mesMostrado =
            Calendar.getInstance()

        generarMeses()

        configurarRecyclerMeses()

        mostrarMes(
            mesMostrado
        )
    }


    // =========================================================
    // GENERAR MESES
    // =========================================================

    private fun generarMeses() {

        meses.clear()

        val hoy =
            Calendar.getInstance()

        hoy.set(
            Calendar.DAY_OF_MONTH,
            1
        )


        for (i in 0 until 12) {

            val mes =
                hoy.clone() as Calendar

            mes.add(
                Calendar.MONTH,
                i
            )

            meses.add(mes)
        }
    }


    // =========================================================
    // RECYCLER MESES
    // =========================================================

    private fun configurarRecyclerMeses() {

        val contexto =
            context ?: return


        val layoutManager =
            LinearLayoutManager(
                contexto,
                LinearLayoutManager.HORIZONTAL,
                false
            )


        binding.recyclerMeses.layoutManager =
            layoutManager


        val adapter =
            MesCalendarioAdapter(
                meses = meses,
                mesSeleccionado = mesMostrado
            ) { mes ->

                seleccionarMes(
                    mes
                )
            }


        binding.recyclerMeses.adapter =
            adapter


        binding.recyclerMeses.post {

            if (_binding == null) {
                return@post
            }


            centrarMesSeleccionado()
        }
    }


    // =========================================================
    // SELECCIONAR MES
    // =========================================================

    private fun seleccionarMes(
        mes: Calendar
    ) {

        if (_binding == null) {
            return
        }


        mesMostrado =
            mes.clone() as Calendar


        val adapter =
            binding.recyclerMeses.adapter
                    as? MesCalendarioAdapter


        adapter?.actualizarSeleccion(
            mesMostrado
        )


        mostrarMes(
            mesMostrado
        )
    }


    // =========================================================
    // MOSTRAR MES
    // =========================================================

    private fun mostrarMes(
        mes: Calendar
    ) {

        if (_binding == null) {
            return
        }


        val contexto =
            context ?: return


        val layoutManager =
            LinearLayoutManager(
                contexto,
                LinearLayoutManager.HORIZONTAL,
                false
            )


        binding.recyclerDias.layoutManager =
            layoutManager


        val dias =
            generarDiasDelMes(
                mes
            )


        val adapter =
            DiaCalendarioAdapter(
                dias = dias,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin
            ) { fecha ->

                seleccionarFecha(
                    fecha
                )
            }


        binding.recyclerDias.adapter =
            adapter


        binding.recyclerDias.post {

            if (_binding == null) {
                return@post
            }


            centrarFechaEnCalendario(
                mes = mes,
                layoutManager = layoutManager
            )
        }
    }


    // =========================================================
    // GENERAR DÍAS
    // =========================================================

    private fun generarDiasDelMes(
        mes: Calendar
    ): List<Calendar> {

        val dias =
            mutableListOf<Calendar>()


        val calendario =
            mes.clone() as Calendar


        calendario.set(
            Calendar.DAY_OF_MONTH,
            1
        )


        val cantidadDias =
            calendario.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )


        for (dia in 1..cantidadDias) {

            val fecha =
                calendario.clone() as Calendar


            fecha.set(
                Calendar.DAY_OF_MONTH,
                dia
            )


            dias.add(
                fecha
            )
        }


        return dias
    }


    // =========================================================
    // SELECCIONAR FECHA
    // =========================================================

    private fun seleccionarFecha(
        fecha: Calendar
    ) {

        val fechaSeleccionada =
            limpiarHora(
                fecha
            )


        if (fechaInicio == null) {

            fechaInicio =
                fechaSeleccionada

            fechaFin = null

        } else if (fechaFin == null) {

            if (
                esMismaFecha(
                    fechaSeleccionada,
                    fechaInicio
                )
            ) {

                fechaFin =
                    fechaInicio?.clone()
                            as Calendar

            } else {

                if (
                    fechaSeleccionada
                        .before(fechaInicio)
                ) {

                    fechaFin =
                        fechaInicio

                    fechaInicio =
                        fechaSeleccionada

                } else {

                    fechaFin =
                        fechaSeleccionada
                }
            }

        } else {

            fechaInicio =
                fechaSeleccionada

            fechaFin = null
        }


        actualizarSeleccionVisual()

        actualizarResumenFechas()

        actualizarConflictosEnTiempoReal()
    }


    // =========================================================
    // ACTUALIZAR SELECCIÓN VISUAL
    // =========================================================

    private fun actualizarSeleccionVisual() {

        if (_binding == null) {
            return
        }


        val adapter =
            binding.recyclerDias.adapter
                    as? DiaCalendarioAdapter
                ?: return


        adapter.actualizarSeleccion(
            fechaInicio,
            fechaFin
        )
    }


    // =========================================================
    // CENTRAR FECHA
    // =========================================================

    private fun centrarFechaEnCalendario(
        mes: Calendar,
        layoutManager: LinearLayoutManager
    ) {

        if (_binding == null) {
            return
        }


        val fechaObjetivo =
            when {

                fechaInicio != null &&
                        mismoMes(
                            fechaInicio!!,
                            mes
                        ) -> {

                    fechaInicio!!
                        .clone() as Calendar
                }


                mismoMes(
                    Calendar.getInstance(),
                    mes
                ) -> {

                    Calendar.getInstance()
                }


                else -> {

                    mes.clone() as Calendar
                }
            }


        val posicion =
            fechaObjetivo.get(
                Calendar.DAY_OF_MONTH
            ) - 1


        val anchoDia =
            dpApx(66)


        binding.recyclerDias.post {

            if (_binding == null) {
                return@post
            }


            val anchoRecycler =
                binding.recyclerDias.width


            if (anchoRecycler <= 0) {
                return@post
            }


            val paddingLateral =
                (
                        (anchoRecycler - anchoDia) / 2
                        ).coerceAtLeast(0)


            binding.recyclerDias.setPadding(
                paddingLateral,
                0,
                paddingLateral,
                0
            )


            layoutManager.scrollToPositionWithOffset(
                posicion,
                0
            )
        }
    }


    // =========================================================
    // CENTRAR MES
    // =========================================================

    private fun centrarMesSeleccionado() {

        if (_binding == null) {
            return
        }


        val layoutManager =
            binding.recyclerMeses.layoutManager
                    as? LinearLayoutManager
                ?: return


        val posicion =
            meses.indexOfFirst { mes ->

                mismoMes(
                    mes,
                    mesMostrado
                )
            }


        if (posicion == -1) {
            return
        }


        binding.recyclerMeses.post {

            if (_binding == null) {
                return@post
            }


            val anchoMes =
                dpApx(82)


            val anchoRecycler =
                binding.recyclerMeses.width


            if (anchoRecycler <= 0) {
                return@post
            }


            val paddingLateral =
                (
                        (anchoRecycler - anchoMes) / 2
                        ).coerceAtLeast(0)


            binding.recyclerMeses.setPadding(
                paddingLateral,
                0,
                paddingLateral,
                0
            )


            layoutManager.scrollToPositionWithOffset(
                posicion,
                0
            )
        }
    }


    // =========================================================
    // COMPARAR MESES
    // =========================================================

    private fun mismoMes(
        fecha1: Calendar,
        fecha2: Calendar
    ): Boolean {

        return fecha1.get(Calendar.YEAR) ==
                fecha2.get(Calendar.YEAR) &&

                fecha1.get(Calendar.MONTH) ==
                fecha2.get(Calendar.MONTH)
    }


    // =========================================================
    // COMPARAR FECHAS
    // =========================================================

    private fun esMismaFecha(
        fecha1: Calendar,
        fecha2: Calendar?
    ): Boolean {

        if (fecha2 == null) {
            return false
        }


        return fecha1.get(Calendar.YEAR) ==
                fecha2.get(Calendar.YEAR) &&

                fecha1.get(Calendar.MONTH) ==
                fecha2.get(Calendar.MONTH) &&

                fecha1.get(Calendar.DAY_OF_MONTH) ==
                fecha2.get(Calendar.DAY_OF_MONTH)
    }


    // =========================================================
    // RESUMEN DE FECHAS
    // =========================================================

    private fun actualizarResumenFechas() {

        if (_binding == null) {
            return
        }


        if (fechaInicio == null) {

            binding.cardResumenFechas.visibility =
                View.GONE

            binding.seccionCuidadoresAsignar.visibility =
                View.GONE

            return
        }


        binding.cardResumenFechas.visibility =
            View.VISIBLE


        binding.txtFechaInicioSeleccionada.text =
            formatoFechaVisible.format(
                fechaInicio!!.time
            )


        if (fechaFin == null) {

            binding.txtFechaFinSeleccionada.text =
                "Selecciona la fecha final"


            binding.txtDuracionSeleccionada.text =
                "Pendiente"


            binding.seccionCuidadoresAsignar.visibility =
                View.GONE

            return
        }


        binding.txtFechaFinSeleccionada.text =
            formatoFechaVisible.format(
                fechaFin!!.time
            )


        val diferencia =
            fechaFin!!.timeInMillis -
                    fechaInicio!!.timeInMillis


        val dias =
            TimeUnit.MILLISECONDS
                .toDays(diferencia) + 1


        binding.txtDuracionSeleccionada.text =
            if (dias == 1L) {

                "1 día"

            } else {

                "$dias días"
            }


        val datosCompletos =
            turnoSeleccionado != null &&
                    fechaInicio != null &&
                    fechaFin != null


        if (datosCompletos) {

            binding.seccionCuidadoresAsignar.visibility =
                View.VISIBLE

        } else {

            binding.seccionCuidadoresAsignar.visibility =
                View.GONE
        }
    }


    // =========================================================
    // CONFIGURAR CUIDADORES
    // =========================================================

    private fun configurarCuidadores() {

        cuidadorAdapter =
            CuidadorAsignarTurnoAdapter(
                cuidadores = emptyList()
            ) { seleccionados ->

                cuidadoresSeleccionados =
                    seleccionados

                actualizarConflictosEnTiempoReal()
            }


        binding.recyclerCuidadoresAsignar.layoutManager =
            LinearLayoutManager(
                requireContext()
            )


        binding.recyclerCuidadoresAsignar.adapter =
            cuidadorAdapter
    }


    // =========================================================
    // CARGAR CUIDADORES
    // =========================================================

    private fun cargarCuidadores() {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val usuarios =
                    RetrofitClient.apiService
                        .getUsuarios()


                if (_binding == null) {
                    return@launch
                }


                val cuidadores =
                    usuarios.filter {

                        it.idRol == 5 &&
                                it.estado
                    }


                cuidadorAdapter
                    .actualizarLista(
                        cuidadores
                    )


                idCuidadorInicial?.let { idUsuario ->

                    if (_binding == null) {
                        return@launch
                    }


                    cuidadorAdapter
                        .seleccionarCuidador(
                            idUsuario
                        )
                }


            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                android.util.Log.e(
                    "ASIGNAR_TURNO",
                    "Error al cargar cuidadores",
                    e
                )


                val contexto =
                    context ?: return@launch


                if (_binding == null) {
                    return@launch
                }


                Toast.makeText(
                    contexto,
                    "No se pudieron cargar los cuidadores",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // CONFIGURAR BOTÓN ASIGNAR
    // =========================================================

    private fun configurarBotonAsignar() {

        binding.btnAsignarTurnoGlobal
            .setOnClickListener {

                asignarTurno()
            }


        actualizarEstadoBotonAsignar()
    }


    // =========================================================
    // ESTADO DEL BOTÓN
    // =========================================================

    private fun actualizarEstadoBotonAsignar() {

        if (_binding == null) {
            return
        }


        val puedeAsignar =
            turnoSeleccionado != null &&
                    fechaInicio != null &&
                    fechaFin != null &&
                    cuidadoresSeleccionados.isNotEmpty() &&
                    !hayConflictosTurno &&
                    !verificandoConflictos &&
                    !errorVerificacionConflictos


        binding.btnAsignarTurnoGlobal.isEnabled =
            puedeAsignar


        binding.btnAsignarTurnoGlobal.alpha =
            if (puedeAsignar) {
                1f
            } else {
                0.5f
            }
    }


    // =========================================================
    // NORMALIZAR TIPO DE TURNO
    // =========================================================

    private fun normalizarTipoTurno(
        turno: Turno
    ): String {

        val nombre =
            turno.nombre
                .trim()
                .lowercase(
                    Locale.getDefault()
                )

        return when {

            nombre.contains("diurno") ->
                "diurno"

            nombre.contains("nocturno") ->
                "nocturno"

            else ->
                nombre
        }
    }


    // =========================================================
    // OBTENER FECHAS SELECCIONADAS
    // =========================================================

    private fun obtenerFechasSeleccionadas():
            List<String> {

        val inicio =
            fechaInicio
                ?: return emptyList()


        val fin =
            fechaFin
                ?: return emptyList()


        val fechas =
            mutableListOf<String>()


        val calendario =
            inicio.clone() as Calendar


        while (
            !calendario.after(fin)
        ) {

            fechas.add(
                formatoFecha.format(
                    calendario.time
                )
            )


            calendario.add(
                Calendar.DAY_OF_MONTH,
                1
            )
        }


        return fechas
    }


    // =========================================================
// OBTENER CONFLICTOS DE TURNOS
// =========================================================

    private suspend fun obtenerConflictosTurno(
        tipoTurnoSeleccionado: String,
        fechasSeleccionadas: List<String>,
        cuidadores: List<Usuario>
    ): Map<String, List<String>> {

        val asignaciones =
            RetrofitClient.apiService
                .getAsignacionesTurno()

        val turnos =
            RetrofitClient.apiService
                .getTurnos()

        val mapaTipos =
            turnos.associate { turno ->

                turno.id_turno to
                        normalizarTipoTurno(turno)
            }

        val hoy =
            formatoFecha.format(
                Calendar.getInstance().time
            )

        val conflictos =
            mutableMapOf<String, MutableList<String>>()

        for (cuidador in cuidadores) {

            val idUsuario =
                cuidador.idUsuario
                    ?: continue

            val nombreCuidador =
                "${cuidador.nombres} ${cuidador.apellidos}"
                    .trim()
                    .ifBlank {
                        "Cuidador"
                    }

            val asignacionesCuidador =
                asignaciones.filter { asignacion ->

                    asignacion.id_usuario ==
                            idUsuario
                }

            for (asignacion in asignacionesCuidador) {

                // =================================================
                // IGNORAR ESTADOS QUE NO BLOQUEAN
                // =================================================

                val estado =
                    asignacion.estado
                        .trim()
                        .lowercase(
                            Locale.getDefault()
                        )

                val estadoNoBloquea =
                    estado in setOf(
                        "cancelado",
                        "cancelada",
                        "finalizado",
                        "finalizada",
                        "completado",
                        "completada"
                    )

                if (estadoNoBloquea) {
                    continue
                }

                // =================================================
                // OBTENER FECHA
                // =================================================

                val fechaExistente =
                    asignacion.fecha
                        ?.take(10)
                        ?: continue

                // =================================================
                // LOS TURNOS PASADOS NO BLOQUEAN
                // =================================================

                if (fechaExistente < hoy) {
                    continue
                }

                // =================================================
                // OBTENER TIPO
                // =================================================

                val tipoExistente =
                    mapaTipos[
                        asignacion.id_turno
                    ]
                        ?: continue

                // =================================================
                // COMPARAR TIPO
                // =================================================

                if (
                    tipoExistente !=
                    tipoTurnoSeleccionado
                ) {
                    continue
                }

                // =================================================
                // COMPARAR FECHA
                // =================================================

                if (
                    fechaExistente !in
                    fechasSeleccionadas
                ) {
                    continue
                }

                // =================================================
                // GUARDAR CONFLICTO
                // =================================================

                conflictos
                    .getOrPut(
                        nombreCuidador
                    ) {
                        mutableListOf()
                    }
                    .add(
                        fechaExistente
                    )
            }
        }

        return conflictos
    }


    // =========================================================
    // VALIDAR CONFLICTOS EN TIEMPO REAL
    // =========================================================

    private fun actualizarConflictosEnTiempoReal() {

        validacionConflictosJob?.cancel()

        hayConflictosTurno = false
        errorVerificacionConflictos = false
        verificandoConflictos = false

        if (_binding == null) {
            return
        }

        val turno =
            turnoSeleccionado

        val fechas =
            obtenerFechasSeleccionadas()

        if (
            turno == null ||
            fechas.isEmpty() ||
            cuidadoresSeleccionados.isEmpty()
        ) {

            ocultarConflictos()

            actualizarEstadoBotonAsignar()

            return
        }

        verificandoConflictos = true

        binding.txtConflictosTurno.visibility =
            View.VISIBLE

        binding.txtConflictosTurno.text =
            "Verificando disponibilidad de los cuidadores..."

        actualizarEstadoBotonAsignar()

        validacionConflictosJob =
            viewLifecycleOwner.lifecycleScope.launch {

                try {

                    val conflictos =
                        obtenerConflictosTurno(
                            tipoTurnoSeleccionado =
                                normalizarTipoTurno(turno),

                            fechasSeleccionadas =
                                fechas,

                            cuidadores =
                                cuidadoresSeleccionados
                        )

                    if (_binding == null) {
                        return@launch
                    }

                    verificandoConflictos = false

                    hayConflictosTurno =
                        conflictos.isNotEmpty()

                    errorVerificacionConflictos =
                        false

                    if (conflictos.isEmpty()) {

                        ocultarConflictos()

                    } else {

                        mostrarConflictos(
                            conflictos
                        )
                    }

                    actualizarEstadoBotonAsignar()

                } catch (e: CancellationException) {

                    throw e

                } catch (e: Exception) {

                    android.util.Log.e(
                        "ASIGNAR_TURNO",
                        "Error al verificar conflictos",
                        e
                    )

                    if (_binding == null) {
                        return@launch
                    }

                    verificandoConflictos = false

                    errorVerificacionConflictos =
                        true

                    hayConflictosTurno = false

                    binding.txtConflictosTurno.visibility =
                        View.VISIBLE

                    binding.txtConflictosTurno.text =
                        "No se pudo verificar la disponibilidad. Intenta nuevamente."

                    actualizarEstadoBotonAsignar()
                }
            }
    }

    // =========================================================
// MOSTRAR CONFLICTOS
// =========================================================

    private fun mostrarConflictos(
        conflictos: Map<String, List<String>>
    ) {

        if (_binding == null) {
            return
        }

        val mensaje =
            buildString {

                append(
                    "Hay conflictos con los turnos seleccionados:\n\n"
                )

                conflictos.forEach { (nombre, fechas) ->

                    append("• ")
                    append(nombre)
                    append(" ya tiene ")
                    append(
                        turnoSeleccionado?.nombre
                            ?.ifBlank {
                                "este turno"
                            }
                            ?: "este turno"
                    )
                    append(" asignado el ")

                    val fechasVisibles =
                        fechas
                            .distinct()
                            .sorted()
                            .map { fecha ->

                                try {

                                    formatoFechaVisible.format(
                                        formatoFecha.parse(
                                            fecha
                                        )!!
                                    )

                                } catch (_: Exception) {

                                    fecha
                                }
                            }

                    append(
                        fechasVisibles.joinToString(
                            separator = ", "
                        )
                    )

                    append(".\n")
                }
            }

        binding.txtConflictosTurno.text =
            mensaje

        binding.txtConflictosTurno.visibility =
            View.VISIBLE
    }

    // =========================================================
    // OCULTAR CONFLICTOS
    // =========================================================

    private fun ocultarConflictos() {

        if (_binding == null) {
            return
        }

        binding.txtConflictosTurno.text = ""

        binding.txtConflictosTurno.visibility =
            View.GONE
    }


    // =========================================================
    // ASIGNAR TURNO
    // =========================================================

    private fun asignarTurno() {

        // =====================================================
        // VALIDAR INFORMACIÓN
        // =====================================================

        val turno =
            turnoSeleccionado


        if (
            turno == null ||
            fechaInicio == null ||
            fechaFin == null ||
            cuidadoresSeleccionados.isEmpty()
        ) {

            val contexto =
                context ?: return


            Toast.makeText(
                contexto,
                "Completa todos los datos del turno",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        // =====================================================
        // VALIDAR ID
        // =====================================================

        val idTurno =
            turno.id_turno


        if (idTurno == null) {

            val contexto =
                context ?: return


            Toast.makeText(
                contexto,
                "El turno seleccionado no tiene un ID válido.",
                Toast.LENGTH_LONG
            ).show()


            return
        }


        // =====================================================
        // OBTENER FECHAS
        // =====================================================

        val fechasSeleccionadas =
            obtenerFechasSeleccionadas()


        if (
            fechasSeleccionadas.isEmpty()
        ) {

            val contexto =
                context ?: return


            Toast.makeText(
                contexto,
                "No se encontraron fechas válidas.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        viewLifecycleOwner.lifecycleScope.launch {

            try {

                if (_binding == null) {
                    return@launch
                }


                binding.btnAsignarTurnoGlobal
                    .isEnabled = false


                // =================================================
                // TIPO DE TURNO
                // =================================================

                val tipoTurno =
                    normalizarTipoTurno(
                        turno
                    )


                // =================================================
                // VALIDAR DUPLICADOS
                // =================================================

                val conflictos =
                    obtenerConflictosTurno(
                        tipoTurnoSeleccionado =
                            tipoTurno,

                        fechasSeleccionadas =
                            fechasSeleccionadas,

                        cuidadores =
                            cuidadoresSeleccionados
                    )

                if (conflictos.isNotEmpty()) {

                    hayConflictosTurno = true

                    mostrarConflictos(
                        conflictos
                    )

                    actualizarEstadoBotonAsignar()

                    return@launch
                }

                // =====================================================
                // ID DEL GRUPO DE ASIGNACIÓN
                // =====================================================

                val idGrupoAsignacion =
                    UUID.randomUUID().toString()


                // =================================================
                // CREAR ASIGNACIONES NUEVAS
                // =================================================

                for (
                fecha in fechasSeleccionadas
                ) {

                    for (
                    cuidador in cuidadoresSeleccionados
                    ) {

                        val idUsuario =
                            cuidador.idUsuario
                                ?: continue


                        val asignacion =
                            AsignacionTurnoUsuario(

                                id_usuario =
                                    idUsuario,

                                id_turno =
                                    idTurno,

                                fecha =
                                    fecha,

                                estado =
                                    "Asignado",

                                id_grupo_asignacion =
                                    idGrupoAsignacion
                            )


                        RetrofitClient.apiService
                            .crearAsignacionTurno(
                                asignacion
                            )
                    }
                }


                // =================================================
                // COMPROBAR QUE LA VISTA SIGUE EXISTIENDO
                // =================================================

                if (_binding == null) {
                    return@launch
                }


                val contexto =
                    context ?: return@launch


                Toast.makeText(
                    contexto,
                    "Turno asignado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()


                limpiarFormulario()


            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                android.util.Log.e(
                    "ASIGNAR_TURNO",
                    "Error al asignar turno",
                    e
                )


                val contexto =
                    context ?: return@launch


                if (_binding == null) {
                    return@launch
                }


                Toast.makeText(
                    contexto,
                    "Error al asignar el turno: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()


                actualizarEstadoBotonAsignar()
            }
        }
    }


    // =========================================================
    // LIMPIAR FORMULARIO
    // =========================================================

    private fun limpiarFormulario() {

        if (_binding == null) {
            return
        }


        turnoSeleccionado = null

        fechaInicio = null

        fechaFin = null

        cuidadoresSeleccionados =
            emptyList()

        validacionConflictosJob?.cancel()

        hayConflictosTurno = false

        verificandoConflictos = false

        errorVerificacionConflictos = false

        ocultarConflictos()


        // =====================================================
        // RESTABLECER SELECTOR
        // =====================================================

        binding.selectorTipoTurno.setText(
            "",
            false
        )


        // Quitar el icono del turno seleccionado.

        binding.selectorTipoTurno
            .setCompoundDrawablesRelative(
                null,
                null,
                null,
                null
            )


        // =====================================================
        // OCULTAR INFORMACIÓN
        // =====================================================

        binding.cardInformacionTurno.visibility =
            View.GONE


        binding.cardResumenFechas.visibility =
            View.GONE


        binding.seccionCuidadoresAsignar.visibility =
            View.GONE


        // =====================================================
        // LIMPIAR CUIDADORES
        // =====================================================

        cuidadorAdapter
            .limpiarSeleccion()


        actualizarSeleccionVisual()

        actualizarEstadoBotonAsignar()
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


    private fun dpApx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }
}