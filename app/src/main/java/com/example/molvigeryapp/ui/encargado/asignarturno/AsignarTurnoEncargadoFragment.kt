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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
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

    // Evita reconstruir el selector en cada refresco de la API.
    private var claveTurnosMostrados: List<String> = emptyList()


    // =========================================================
    // CUIDADORES
    // =========================================================

    private var cuidadoresSeleccionados: List<Usuario> =
        emptyList()

    /**
     * IDs actualmente seleccionados. Se usa para que el refresco de 2 segundos
     * no vuelva a consultar conflictos si la selección realmente no cambió.
     */
    private var idsCuidadoresSeleccionados: Set<Int> =
        emptySet()

    private lateinit var cuidadorAdapter:
            CuidadorAsignarTurnoAdapter

    private lateinit var resumenCuidadoresAdapter:
            CuidadorResumenAsignacionAdapter

    private lateinit var conflictoCuidadoresAdapter:
            CuidadorConflictoTurnoAdapter


    // =========================================================
    // REFRESCO AUTOMÁTICO
    // =========================================================

    private var refrescoAutomaticoJob: Job? =
        null

    private var refrescandoDatos =
        false


    // =========================================================
    // VALIDACIÓN DE CONFLICTOS
    // =========================================================

    private var validacionConflictosJob: Job? =
        null

    private var hayConflictosTurno =
        false

    private var verificandoConflictos =
        false

    private var errorVerificacionConflictos =
        false


    // =========================================================
    // CUIDADOR INICIAL
    // =========================================================

    private var idCuidadorInicial: Int? =
        null

    private var cuidadorInicialAplicado =
        false


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
    // MODELO INTERNO DE CONFLICTO
    // =========================================================

    data class DetalleConflictoTurno(
        val turno: Turno,
        val fecha: String
    )

    data class ConflictoTurno(
        val cuidador: Usuario,
        val detalles: List<DetalleConflictoTurno>
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

        // =====================================================
        // WINDOW INSETS
        // =====================================================

        WindowInsetsEncargado.aplicar(
            root = binding.root,
            contenido = binding.scrollAsignarTurno,
            menuInferior = binding.bottomNavigationAsignarTurno
        )


        // =====================================================
        // CUIDADOR RECIBIDO POR ARGUMENTOS
        // =====================================================

        idCuidadorInicial =
            arguments
                ?.getInt("id_usuario")
                ?.takeIf { it > 0 }


        // =====================================================
        // CONFIGURACIONES
        // =====================================================

        configurarNavegacion()

        configurarSelectorTurno()

        configurarCalendario()

        configurarCuidadores()

        configurarResumenCuidadores()

        configurarConflictos()

        configurarBotonAsignar()

        configurarEstadoInicial()


        // =====================================================
        // CONTADOR DE NOTIFICACIONES
        // =====================================================

        ContadorNotificaciones.iniciar(
            fragment = this,
            badge = binding.txtNotificacionesAsignarTurno
        )


        // =====================================================
        // REFRESCO AUTOMÁTICO
        // =====================================================

        iniciarRefrescoAutomatico()
    }


    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        refrescoAutomaticoJob?.cancel()
        validacionConflictosJob?.cancel()

        refrescoAutomaticoJob = null
        validacionConflictosJob = null

        if (_binding != null) {
            binding.recyclerCuidadoresAsignar.adapter = null
            binding.recyclerCuidadoresSeleccionados.adapter = null
            binding.recyclerCuidadoresConConflicto.adapter = null
        }

        _binding = null

        super.onDestroyView()
    }


    // =========================================================
    // ESTADO INICIAL
    // =========================================================

    private fun configurarEstadoInicial() {

        actualizarProgreso()

        actualizarResumenFechas()

        actualizarResumenAsignacion()

        actualizarEstadoBotonAsignar()
    }


    // =========================================================
    // RESUMEN DE CUIDADORES
    // =========================================================

    private fun configurarResumenCuidadores() {

        resumenCuidadoresAdapter =
            CuidadorResumenAsignacionAdapter(
                cuidadores = emptyList()
            )

        binding.recyclerCuidadoresSeleccionados.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerCuidadoresSeleccionados.adapter =
            resumenCuidadoresAdapter
    }


    // =========================================================
    // CONFLICTOS
    // =========================================================

    private fun configurarConflictos() {

        conflictoCuidadoresAdapter =
            CuidadorConflictoTurnoAdapter(
                conflictos = emptyList()
            )

        binding.recyclerCuidadoresConConflicto.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerCuidadoresConConflicto.adapter =
            conflictoCuidadoresAdapter
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
    // SELECTOR DE TURNO
    // =========================================================

    private fun configurarSelectorTurno() {

        binding.selectorTipoTurno.setText(
            "",
            false
        )

        binding.selectorTipoTurno
            .setCompoundDrawablesRelative(
                null,
                null,
                null,
                null
            )

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
    // REFRESCO AUTOMÁTICO
    // =========================================================

    private fun iniciarRefrescoAutomatico() {

        refrescoAutomaticoJob?.cancel()

        refrescoAutomaticoJob =
            viewLifecycleOwner.lifecycleScope.launch {

                viewLifecycleOwner.repeatOnLifecycle(
                    Lifecycle.State.STARTED
                ) {

                    while (true) {

                        refrescarDatosDesdeApi()

                        delay(2_000L)
                    }
                }
            }
    }


    // =========================================================
    // REFRESCAR DATOS DESDE API
    // =========================================================

    private suspend fun refrescarDatosDesdeApi() {

        if (refrescandoDatos) {
            return
        }

        refrescandoDatos = true

        try {

            if (_binding == null) {
                return
            }

            // =================================================
            // ESTADO DE CARGA
            // =================================================

            if (
                turnosDisponibles.isEmpty() &&
                cuidadorAdapter.itemCount == 0
            ) {

                binding.txtEstadoPasoAsignarTurno.text =
                    "Cargando turnos y cuidadores..."
            }


            // =================================================
            // CONSULTAS
            // =================================================

            val turnos =
                RetrofitClient.apiService
                    .getTurnos()

            val usuarios =
                RetrofitClient.apiService
                    .getUsuarios()


            if (_binding == null) {
                return
            }


            // =================================================
            // TURNOS ACTIVOS
            // =================================================

            turnosDisponibles =
                turnos.filter {
                    it.estado
                }

            actualizarListaTurnos()


            // =================================================
            // CUIDADORES ACTIVOS
            // =================================================

            val cuidadores =
                usuarios.filter {

                    it.idRol == 5 &&
                            it.estado
                }

            cuidadorAdapter.actualizarLista(
                cuidadores
            )


            // =================================================
            // SELECCIONAR CUIDADOR INICIAL
            // =================================================

            if (
                !cuidadorInicialAplicado &&
                idCuidadorInicial != null &&
                cuidadores.any {
                    it.idUsuario ==
                            idCuidadorInicial
                }
            ) {

                cuidadorAdapter.seleccionarCuidador(
                    idCuidadorInicial!!
                )

                cuidadorInicialAplicado =
                    true
            }


            // =================================================
            // ACTUALIZAR PROGRESO
            // =================================================

            actualizarProgreso()

            actualizarResumenAsignacion()

            actualizarEstadoBotonAsignar()

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            android.util.Log.e(
                "ASIGNAR_TURNO",
                "Error durante refresco automático",
                e
            )

            if (_binding == null) {
                return
            }

            /*
             * No mostramos Toast cada 2 segundos.
             * El error se deja visible en el estado del paso.
             */

            if (
                turnosDisponibles.isEmpty()
            ) {

                binding.txtEstadoPasoAsignarTurno.text =
                    "No se pudieron cargar los datos."
            }

        } finally {

            refrescandoDatos = false
        }
    }


    // =========================================================
    // ACTUALIZAR LISTA DE TURNOS
    // =========================================================

    private fun actualizarListaTurnos() {

        if (_binding == null) {
            return
        }

        val claveActual = turnosDisponibles.map { turno ->
            "${turno.id_turno}|${turno.nombre}|${turno.hora_inicio}|${turno.hora_fin}|${turno.estado}"
        }

        // El refresco automático ocurre cada 2 segundos. Si los turnos no
        // cambiaron, no se vuelve a asignar el adapter ni se mueve la pantalla.
        if (claveActual == claveTurnosMostrados) {
            return
        }
        claveTurnosMostrados = claveActual

        val nombres =
            turnosDisponibles.map { turno ->

                turno.nombre.ifBlank {
                    "Turno ${turno.id_turno}"
                }
            }


        val adapter =
            object : ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombres
            ) {

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

                    configurarVistaTurno(
                        textView =
                            textView,
                        turno =
                            turnosDisponibles[position]
                    )

                    return textView
                }


                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val turno =
                        turnosDisponibles[position]


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
                        textView =
                            textView,
                        turno =
                            turno
                    )


                    contenedor.addView(
                        textView
                    )


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


        binding.selectorTipoTurno
            .setAdapter(adapter)


        // =====================================================
        // CONSERVAR TURNO SELECCIONADO
        // =====================================================

        turnoSeleccionado?.let { seleccionado ->

            val actualizado =
                turnosDisponibles.firstOrNull {

                    it.id_turno ==
                            seleccionado.id_turno
                }


            if (actualizado != null) {

                turnoSeleccionado =
                    actualizado

                binding.selectorTipoTurno.setText(
                    actualizado.nombre,
                    false
                )

                binding.txtNombreTurnoSeleccionado.text =
                    actualizado.nombre

                binding.txtHorarioTurnoSeleccionado.text =
                    "${actualizado.hora_inicio} - " +
                            "${actualizado.hora_fin ?: "Sin hora final"}"
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


        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
            icono,
            null,
            null,
            null
        )


        textView.compoundDrawablePadding =
            dpApx(12)


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


        textView.text =
            turno.nombre.ifBlank {
                "Turno ${turno.id_turno}"
            }
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


        binding.selectorTipoTurno.setText(
            turno.nombre,
            false
        )


        val icono =
            obtenerIconoTurno(
                turno
            )


        binding.selectorTipoTurno
            .setCompoundDrawablesRelativeWithIntrinsicBounds(
                icono,
                null,
                null,
                null
            )


        binding.selectorTipoTurno
            .compoundDrawablePadding =
            dpApx(10)


        binding.selectorTipoTurno.setTextColor(
            Color.parseColor(
                "#1D2939"
            )
        )


        binding.txtNombreTurnoSeleccionado.text =
            turno.nombre


        binding.txtHorarioTurnoSeleccionado.text =
            "${turno.hora_inicio} - " +
                    "${turno.hora_fin ?: "Sin hora final"}"


        binding.cardInformacionTurno.visibility =
            View.VISIBLE


        actualizarResumenFechas()

        actualizarProgreso()

        actualizarConflictosEnTiempoReal()

        actualizarEstadoBotonAsignar()
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

            meses.add(
                mes
            )
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

        actualizarProgreso()

        actualizarConflictosEnTiempoReal()

        actualizarEstadoBotonAsignar()
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

            actualizarProgreso()

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

            actualizarProgreso()

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


        binding.seccionCuidadoresAsignar.visibility =
            if (datosCompletos) {
                View.VISIBLE
            } else {
                View.GONE
            }


        actualizarProgreso()
    }


    // =========================================================
    // CONFIGURAR CUIDADORES
    // =========================================================

    private fun configurarCuidadores() {

        cuidadorAdapter =
            CuidadorAsignarTurnoAdapter(
                cuidadores = emptyList()
            ) { seleccionados ->

                val nuevosIds =
                    seleccionados
                        .mapNotNull { it.idUsuario }
                        .toSet()

                val seleccionCambio =
                    nuevosIds != idsCuidadoresSeleccionados

                cuidadoresSeleccionados =
                    seleccionados

                idsCuidadoresSeleccionados =
                    nuevosIds

                actualizarResumenAsignacion()

                if (seleccionCambio) {
                    actualizarConflictosEnTiempoReal()
                }

                actualizarProgreso()
                actualizarEstadoBotonAsignar()
            }


        binding.recyclerCuidadoresAsignar.layoutManager =
            LinearLayoutManager(requireContext())


        binding.recyclerCuidadoresAsignar.adapter =
            cuidadorAdapter
    }


    // =========================================================
    // RESUMEN DE ASIGNACIÓN
    // =========================================================

    private fun actualizarResumenAsignacion() {

        if (_binding == null) {
            return
        }


        val cuidadores =
            cuidadoresSeleccionados


        if (cuidadores.isEmpty()) {

            binding.cardResumenAsignacionTurno.visibility =
                View.GONE

            actualizarProgreso()

            return
        }


        binding.cardResumenAsignacionTurno.visibility =
            View.VISIBLE


        binding.txtCantidadCuidadoresSeleccionados.text =
            if (cuidadores.size == 1) {

                "1 cuidador seleccionado"

            } else {

                "${cuidadores.size} cuidadores seleccionados"
            }


        // =====================================================
        // TURNO
        // =====================================================

        turnoSeleccionado?.let { turno ->

            binding.txtResumenTurno.text =
                "${turno.nombre} · " +
                        "${turno.hora_inicio} - " +
                        "${turno.hora_fin ?: "Sin hora final"}"
        }


        // =====================================================
        // FECHA
        // =====================================================

        if (
            fechaInicio != null &&
            fechaFin != null
        ) {

            val inicio =
                formatoFechaVisible.format(
                    fechaInicio!!.time
                )

            val fin =
                formatoFechaVisible.format(
                    fechaFin!!.time
                )


            binding.txtResumenFecha.text =
                if (inicio == fin) {

                    inicio

                } else {

                    "$inicio - $fin"
                }
        }


        // =====================================================
        // LISTA
        // =====================================================

        resumenCuidadoresAdapter.actualizarLista(
            cuidadores
        )


        actualizarProgreso()
    }


    // =========================================================
    // PROGRESO
    // =========================================================

    private fun actualizarProgreso() {

        if (_binding == null) {
            return
        }


        val tieneTurno =
            turnoSeleccionado != null


        val tieneFecha =
            fechaInicio != null &&
                    fechaFin != null


        val tieneCuidador =
            cuidadoresSeleccionados.isNotEmpty()


        // =====================================================
        // PASO 1
        // =====================================================

        actualizarIndicadorPaso(
            indicador =
                binding.indicadorPaso1AsignarTurno,

            numero =
                binding.txtNumeroPaso1AsignarTurno,

            texto =
                binding.txtPaso1AsignarTurno,

            numeroPaso = "1",

            activo =
                true,

            completado =
                tieneTurno
        )


        // =====================================================
        // PASO 2
        // =====================================================

        actualizarIndicadorPaso(
            indicador =
                binding.indicadorPaso2AsignarTurno,

            numero =
                binding.txtNumeroPaso2AsignarTurno,

            texto =
                binding.txtPaso2AsignarTurno,

            numeroPaso = "2",

            activo =
                tieneTurno,

            completado =
                tieneFecha
        )


        // =====================================================
        // PASO 3
        // =====================================================

        actualizarIndicadorPaso(
            indicador =
                binding.indicadorPaso3AsignarTurno,

            numero =
                binding.txtNumeroPaso3AsignarTurno,

            texto =
                binding.txtPaso3AsignarTurno,

            numeroPaso = "3",

            activo =
                tieneFecha,

            completado =
                tieneCuidador
        )


        // =====================================================
        // ESTADO
        // =====================================================

        binding.txtEstadoPasoAsignarTurno.text =
            when {

                refrescandoDatos &&
                        turnosDisponibles.isEmpty() ->
                    "Cargando turnos y cuidadores..."

                errorVerificacionConflictos ->
                    "No se pudo verificar la disponibilidad."

                hayConflictosTurno ->
                    "Hay cuidadores con conflicto."

                verificandoConflictos ->
                    "Verificando disponibilidad..."

                !tieneTurno ->
                    "Selecciona un turno para comenzar."

                !tieneFecha ->
                    "Ahora selecciona la fecha."

                !tieneCuidador ->
                    "Selecciona uno o varios cuidadores."

                else ->
                    "Todo listo para asignar el turno."
            }
    }


    // =========================================================
    // INDICADOR DE PASO
    // =========================================================

    private fun actualizarIndicadorPaso(
        indicador: View,
        numero: TextView,
        texto: TextView,
        numeroPaso: String,
        activo: Boolean,
        completado: Boolean
    ) {

        when {
            completado -> {
                indicador.setBackgroundResource(
                    R.drawable.bg_paso_completado
                )
                numero.text = "✓"
                numero.setTextColor(Color.WHITE)
                texto.setTextColor(
                    Color.parseColor("#3B5BDB")
                )
            }

            activo -> {
                indicador.setBackgroundResource(
                    R.drawable.bg_paso_activo
                )
                numero.text = numeroPaso
                numero.setTextColor(
                    Color.parseColor("#3B5BDB")
                )
                texto.setTextColor(
                    Color.parseColor("#3B5BDB")
                )
            }

            else -> {
                indicador.setBackgroundResource(
                    R.drawable.bg_paso_inactivo
                )
                numero.text = numeroPaso
                numero.setTextColor(
                    Color.parseColor("#98A2B3")
                )
                texto.setTextColor(
                    Color.parseColor("#98A2B3")
                )
            }
        }
    }

    // =========================================================
    // CONFIGURAR BOTÓN
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
    // OBTENER CONFLICTOS
    // =========================================================

    private suspend fun obtenerConflictosTurno(
        turnoNuevo: Turno,
        fechasSeleccionadas: List<String>,
        cuidadores: List<Usuario>
    ): List<ConflictoTurno> {

        if (
            fechasSeleccionadas.isEmpty() ||
            cuidadores.isEmpty()
        ) {
            return emptyList()
        }


        // =====================================================
        // CONSULTAR ASIGNACIONES
        // =====================================================

        val asignaciones =
            RetrofitClient.apiService
                .getAsignacionesTurno()


        // =====================================================
        // CONSULTAR TURNOS
        // =====================================================

        val turnos =
            RetrofitClient.apiService
                .getTurnos()


        val mapaTurnos =
            turnos.associateBy {
                it.id_turno
            }


        val hoy =
            formatoFecha.format(
                Calendar.getInstance().time
            )


        val conflictosPorCuidador =
            linkedMapOf<Int, MutableList<DetalleConflictoTurno>>()


        // =====================================================
        // RECORRER CUIDADORES
        // =====================================================

        for (cuidador in cuidadores) {

            val idUsuario =
                cuidador.idUsuario
                    ?: continue


            val asignacionesCuidador =
                asignaciones.filter {

                    it.id_usuario ==
                            idUsuario
                }


            // =================================================
            // RECORRER ASIGNACIONES
            // =================================================

            for (asignacion in asignacionesCuidador) {

                // =============================================
                // ESTADO
                // =============================================

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


                // =============================================
                // FECHA
                // =============================================

                val fechaExistente =
                    asignacion.fecha
                        ?.take(10)
                        ?: continue


                // =============================================
                // TURNOS PASADOS
                // =============================================

                if (
                    fechaExistente < hoy
                ) {
                    continue
                }


                // =============================================
                // FECHA SELECCIONADA
                // =============================================

                if (
                    fechaExistente !in
                    fechasSeleccionadas
                ) {
                    continue
                }


                // =============================================
                // TURNO EXISTENTE
                // =============================================

                val turnoExistente =
                    mapaTurnos[
                        asignacion.id_turno
                    ]
                        ?: continue


                // =============================================
                // COMPROBAR SOLAPAMIENTO
                // =============================================

                val seSolapan =
                    turnosSeSolapan(
                        turnoNuevo,
                        turnoExistente
                    )


                if (!seSolapan) {
                    continue
                }


                // =============================================
                // GUARDAR CONFLICTO
                // =============================================

                val detalles = conflictosPorCuidador.getOrPut(idUsuario) {
                    mutableListOf()
                }

                val yaRegistrado = detalles.any { detalle ->
                    detalle.turno.id_turno == turnoExistente.id_turno &&
                            detalle.fecha == fechaExistente
                }

                if (!yaRegistrado) {
                    detalles.add(
                        DetalleConflictoTurno(
                            turno = turnoExistente,
                            fecha = fechaExistente
                        )
                    )
                }
            }
        }


        return cuidadores.mapNotNull { cuidador ->
            val idUsuario = cuidador.idUsuario ?: return@mapNotNull null
            val detalles = conflictosPorCuidador[idUsuario]
                ?.sortedBy { it.fecha }
                ?: return@mapNotNull null

            if (detalles.isEmpty()) {
                null
            } else {
                ConflictoTurno(
                    cuidador = cuidador,
                    detalles = detalles
                )
            }
        }
    }


    // =========================================================
    // COMPARAR HORARIOS
    // =========================================================

    private fun turnosSeSolapan(
        turnoNuevo: Turno,
        turnoExistente: Turno
    ): Boolean {

        val inicioNuevo =
            convertirHoraAMinutos(
                turnoNuevo.hora_inicio
            )


        val finNuevo =
            convertirHoraAMinutos(
                turnoNuevo.hora_fin
            )


        val inicioExistente =
            convertirHoraAMinutos(
                turnoExistente.hora_inicio
            )


        val finExistente =
            convertirHoraAMinutos(
                turnoExistente.hora_fin
            )


        /*
         * Si alguno de los horarios no se puede interpretar,
         * no asumimos que el cuidador está libre.
         */

        if (
            inicioNuevo == null ||
            finNuevo == null ||
            inicioExistente == null ||
            finExistente == null
        ) {

            return true
        }


        val finNuevoNormalizado =
            if (
                finNuevo <= inicioNuevo
            ) {

                finNuevo + 24 * 60

            } else {

                finNuevo
            }


        val finExistenteNormalizado =
            if (
                finExistente <= inicioExistente
            ) {

                finExistente + 24 * 60

            } else {

                finExistente
            }


        return inicioNuevo <
                finExistenteNormalizado &&

                inicioExistente <
                finNuevoNormalizado
    }


    // =========================================================
    // CONVERTIR HORA A MINUTOS
    // =========================================================

    private fun convertirHoraAMinutos(
        hora: String?
    ): Int? {

        if (hora.isNullOrBlank()) {
            return null
        }


        return try {

            val partes =
                hora.trim()
                    .split(":")


            if (partes.size < 2) {
                return null
            }


            val horas =
                partes[0]
                    .toInt()


            val minutos =
                partes[1]
                    .take(2)
                    .toInt()


            (horas * 60) +
                    minutos

        } catch (_: Exception) {

            null
        }
    }


    // =========================================================
    // VALIDAR CONFLICTOS EN TIEMPO REAL
    // =========================================================

    private fun actualizarConflictosEnTiempoReal() {

        validacionConflictosJob?.cancel()


        hayConflictosTurno =
            false

        errorVerificacionConflictos =
            false

        verificandoConflictos =
            false


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

            actualizarProgreso()

            actualizarEstadoBotonAsignar()

            return
        }


        verificandoConflictos =
            true


        binding.txtConflictosTurno.visibility =
            View.VISIBLE


        binding.txtConflictosTurno.text =
            "Verificando disponibilidad..."


        actualizarProgreso()

        actualizarEstadoBotonAsignar()


        validacionConflictosJob =
            viewLifecycleOwner.lifecycleScope.launch {

                try {

                    val conflictos =
                        obtenerConflictosTurno(
                            turnoNuevo =
                                turno,

                            fechasSeleccionadas =
                                fechas,

                            cuidadores =
                                cuidadoresSeleccionados
                        )


                    if (_binding == null) {
                        return@launch
                    }


                    verificandoConflictos =
                        false


                    errorVerificacionConflictos =
                        false


                    hayConflictosTurno =
                        conflictos.isNotEmpty()


                    if (
                        conflictos.isEmpty()
                    ) {

                        ocultarConflictos()

                    } else {

                        mostrarConflictos(
                            conflictos
                        )
                    }


                    actualizarProgreso()

                    actualizarEstadoBotonAsignar()

                } catch (
                    e: CancellationException
                ) {

                    throw e

                } catch (
                    e: Exception
                ) {

                    android.util.Log.e(
                        "ASIGNAR_TURNO",
                        "Error al verificar conflictos",
                        e
                    )


                    if (_binding == null) {
                        return@launch
                    }


                    verificandoConflictos =
                        false


                    errorVerificacionConflictos =
                        true


                    hayConflictosTurno =
                        false


                    binding.txtConflictosTurno.visibility =
                        View.VISIBLE


                    binding.txtConflictosTurno.text =
                        "No se pudo verificar la disponibilidad. Intenta nuevamente."


                    actualizarProgreso()

                    actualizarEstadoBotonAsignar()
                }
            }
    }


    // =========================================================
    // MOSTRAR CONFLICTOS
    // =========================================================

    private fun mostrarConflictos(
        conflictos: List<ConflictoTurno>
    ) {

        if (_binding == null) {
            return
        }


        binding.cardResumenAsignacionTurno.visibility =
            View.VISIBLE


        binding.contenedorConflictosAsignacion.visibility =
            View.VISIBLE


        val cantidad =
            conflictos.size


        binding.txtMensajeConflictosAsignacion.text =
            if (cantidad == 1) {

                "Se encontró 1 cuidador con conflicto de horario. Debes resolverlo antes de asignar."

            } else {

                "Se encontraron $cantidad cuidadores con conflictos de horario. Debes resolverlos antes de asignar."
            }


        conflictoCuidadoresAdapter
            .actualizarLista(
                conflictos
            )


        binding.txtConflictosTurno.visibility =
            View.GONE
    }


    // =========================================================
    // OCULTAR CONFLICTOS
    // =========================================================

    private fun ocultarConflictos() {

        if (_binding == null) {
            return
        }


        binding.txtConflictosTurno.text =
            ""


        binding.txtConflictosTurno.visibility =
            View.GONE


        binding.contenedorConflictosAsignacion.visibility =
            View.GONE


        binding.txtMensajeConflictosAsignacion.text =
            ""


        conflictoCuidadoresAdapter
            .actualizarLista(
                emptyList()
            )
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

            Toast.makeText(
                context,
                "Completa todos los datos del turno.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =====================================================
        // VALIDAR ESTADO DE CONFLICTOS
        // =====================================================

        if (
            verificandoConflictos
        ) {

            Toast.makeText(
                context,
                "Espera mientras se verifica la disponibilidad.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        if (
            errorVerificacionConflictos
        ) {

            Toast.makeText(
                context,
                "No se pudo verificar la disponibilidad.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        if (
            hayConflictosTurno
        ) {

            Toast.makeText(
                context,
                "No puedes asignar porque existen conflictos de horario.",
                Toast.LENGTH_LONG
            ).show()

            return
        }


        // =====================================================
        // VALIDAR ID DEL TURNO
        // =====================================================

        val idTurno =
            turno.id_turno


        if (idTurno == null) {

            Toast.makeText(
                context,
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

            Toast.makeText(
                context,
                "No se encontraron fechas válidas.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =====================================================
        // LANZAR ASIGNACIÓN
        // =====================================================

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                if (_binding == null) {
                    return@launch
                }


                binding.btnAsignarTurnoGlobal
                    .isEnabled = false


                binding.txtEstadoPasoAsignarTurno.text =
                    "Verificando nuevamente y asignando..."


                // =================================================
                // VALIDACIÓN FINAL
                // =================================================

                val conflictos =
                    obtenerConflictosTurno(
                        turnoNuevo =
                            turno,

                        fechasSeleccionadas =
                            fechasSeleccionadas,

                        cuidadores =
                            cuidadoresSeleccionados
                    )


                if (
                    conflictos.isNotEmpty()
                ) {

                    hayConflictosTurno =
                        true


                    mostrarConflictos(
                        conflictos
                    )


                    actualizarProgreso()

                    actualizarEstadoBotonAsignar()

                    return@launch
                }


                // =================================================
                // GRUPO DE ASIGNACIÓN
                // =================================================

                val idGrupoAsignacion =
                    UUID.randomUUID().toString()


                // =================================================
                // CREAR ASIGNACIONES
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


                if (_binding == null) {
                    return@launch
                }


                Toast.makeText(
                    context,
                    "Turno asignado correctamente.",
                    Toast.LENGTH_SHORT
                ).show()


                limpiarFormulario()

            } catch (
                e: CancellationException
            ) {

                throw e

            } catch (
                e: Exception
            ) {

                android.util.Log.e(
                    "ASIGNAR_TURNO",
                    "Error al asignar turno",
                    e
                )


                if (_binding == null) {
                    return@launch
                }


                Toast.makeText(
                    context,
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


        turnoSeleccionado =
            null


        fechaInicio =
            null


        fechaFin =
            null


        cuidadoresSeleccionados =
            emptyList()

        idsCuidadoresSeleccionados =
            emptySet()

        validacionConflictosJob?.cancel()


        hayConflictosTurno =
            false


        verificandoConflictos =
            false


        errorVerificacionConflictos =
            false


        ocultarConflictos()


        // =====================================================
        // SELECTOR
        // =====================================================

        binding.selectorTipoTurno.setText(
            "",
            false
        )


        binding.selectorTipoTurno
            .setCompoundDrawablesRelative(
                null,
                null,
                null,
                null
            )


        // =====================================================
        // INFORMACIÓN DEL TURNO
        // =====================================================

        binding.cardInformacionTurno.visibility =
            View.GONE


        // =====================================================
        // FECHAS
        // =====================================================

        binding.cardResumenFechas.visibility =
            View.GONE


        // =====================================================
        // CUIDADORES
        // =====================================================

        binding.seccionCuidadoresAsignar.visibility =
            View.GONE


        // =====================================================
        // RESUMEN
        // =====================================================

        binding.cardResumenAsignacionTurno.visibility =
            View.GONE


        // =====================================================
        // LIMPIAR SELECCIÓN
        // =====================================================

        cuidadorAdapter.limpiarSeleccion()


        actualizarSeleccionVisual()

        actualizarProgreso()

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
