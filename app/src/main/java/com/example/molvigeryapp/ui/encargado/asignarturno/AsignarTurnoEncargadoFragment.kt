package com.example.molvigeryapp.ui.encargado.asignarturno

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
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
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit


class AsignarTurnoEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentAsignarTurnoEncargadoBinding? = null
    private val binding get() = _binding!!


    // =========================================================
    // CALENDARIO
    // =========================================================

    private val meses = mutableListOf<Calendar>()

    private var mesMostrado: Calendar =
        Calendar.getInstance()

    private var fechaInicio: Calendar? = null

    private var fechaFin: Calendar? = null


    // =========================================================
    // TIPO DE TURNO
    // =========================================================

    private var tipoTurnoSeleccionado: String? = null

    private var horaInicioSeleccionada: String = ""

    private var horaFinSeleccionada: String = ""


    // =========================================================
    // CUIDADORES
    // =========================================================

    private var cuidadoresSeleccionados: List<Usuario> =
        emptyList()

    private lateinit var cuidadorAdapter:
            CuidadorAsignarTurnoAdapter


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

        configurarNavegacion()

        configurarSelectorTurno()

        configurarCalendario()

        configurarCuidadores()

        configurarBotonAsignar()

        cargarCuidadores()
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

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeEncargadoFragment()
                    )
                    .commit()
            },


            onAsignarTurno = {
                // Ya estamos en esta pantalla
            },


            onCitas = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CitasEncargadoFragment()
                    )
                    .commit()
            },


            onPerfil = {

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

        val tiposTurno =
            listOf(
                "Diurno",
                "Nocturno"
            )


        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                tiposTurno
            )


        binding.selectorTipoTurno
            .setAdapter(adapter)


        binding.selectorTipoTurno
            .setOnItemClickListener { _, _, position, _ ->

                seleccionarTipoTurno(
                    tiposTurno[position]
                )
            }
    }


    // =========================================================
    // SELECCIONAR TIPO DE TURNO
    // =========================================================

    private fun seleccionarTipoTurno(
        tipo: String
    ) {

        tipoTurnoSeleccionado =
            tipo


        when (tipo) {

            "Diurno" -> {

                horaInicioSeleccionada =
                    "07:00:00"

                horaFinSeleccionada =
                    "19:00:00"
            }


            "Nocturno" -> {

                horaInicioSeleccionada =
                    "19:00:00"

                horaFinSeleccionada =
                    "07:00:00"
            }
        }


        // =====================================================
        // INFORMACIÓN DEL TURNO
        // =====================================================

        binding.txtNombreTurnoSeleccionado.text =
            "Turno $tipo"


        binding.txtHorarioTurnoSeleccionado.text =
            "$horaInicioSeleccionada - $horaFinSeleccionada"


        binding.cardInformacionTurno.visibility =
            View.VISIBLE


        /*
         * IMPORTANTE:
         *
         * Volvemos a comprobar las fechas porque
         * el usuario puede haberlas seleccionado
         * antes de elegir el tipo de turno.
         */

        actualizarResumenFechas()


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


        /*
         * Mes actual + próximos 11 meses.
         */

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

        val layoutManager =
            LinearLayoutManager(
                requireContext(),
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

                seleccionarMes(mes)
            }


        binding.recyclerMeses.adapter =
            adapter


        /*
         * Centramos el mes actual solamente
         * cuando se carga la pantalla.
         */

        binding.recyclerMeses.post {

            centrarMesSeleccionado()
        }
    }


    // =========================================================
    // SELECCIONAR MES
    // =========================================================

    private fun seleccionarMes(
        mes: Calendar
    ) {

        mesMostrado =
            mes.clone() as Calendar


        val adapter =
            binding.recyclerMeses.adapter
                    as? MesCalendarioAdapter


        adapter?.actualizarSeleccion(
            mesMostrado
        )


        /*
         * Cargamos los días del nuevo mes.
         */

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

        val layoutManager =
            LinearLayoutManager(
                requireContext(),
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


        /*
         * El centrado solamente se hace cuando
         * cargamos el mes.
         *
         * Al seleccionar un día NO se vuelve
         * a ejecutar.
         */

        binding.recyclerDias.post {

            centrarFechaEnCalendario(
                mes = mes,
                layoutManager = layoutManager
            )
        }
    }


    // =========================================================
    // GENERAR TODOS LOS DÍAS DEL MES
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


        /*
         * NO eliminamos los días pasados.
         *
         * Todos los días del mes se muestran.
         *
         * DiaCalendarioAdapter se encarga de
         * mostrar los días pasados como
         * no disponibles.
         */

        return dias
    }


    // =========================================================
    // SELECCIONAR / DESELECCIONAR FECHA
    // =========================================================

    private fun seleccionarFecha(
        fecha: Calendar
    ) {

        val fechaSeleccionada =
            limpiarHora(fecha)


        // =====================================================
        // CASO 1: NO EXISTE FECHA DE INICIO
        // =====================================================

        if (fechaInicio == null) {

            fechaInicio =
                fechaSeleccionada

            fechaFin = null
        }


        // =====================================================
        // CASO 2: EXISTE INICIO PERO NO FINAL
        // =====================================================

        else if (fechaFin == null) {

            /*
             * Si toca nuevamente el día de inicio,
             * lo deseleccionamos.
             */

            if (
                esMismaFecha(
                    fechaSeleccionada,
                    fechaInicio
                )
            ) {

                fechaInicio = null

                fechaFin = null

            } else {

                /*
                 * Si selecciona una fecha anterior
                 * al inicio, intercambiamos las fechas.
                 */

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
        }


        // =====================================================
        // CASO 3: YA EXISTE UN RANGO
        // =====================================================

        else {

            /*
             * Al seleccionar otra fecha cuando
             * ya existe un rango, comenzamos
             * una nueva selección.
             */

            fechaInicio =
                fechaSeleccionada

            fechaFin = null
        }


        /*
         * SOLAMENTE actualizamos la apariencia.
         *
         * NO centramos el RecyclerView.
         */

        actualizarSeleccionVisual()


        actualizarResumenFechas()


        actualizarEstadoBotonAsignar()
    }


    // =========================================================
    // ACTUALIZAR SELECCIÓN VISUAL
    // =========================================================

    private fun actualizarSeleccionVisual() {

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
    // CENTRAR FECHA AL CARGAR EL MES
    // =========================================================

    private fun centrarFechaEnCalendario(
        mes: Calendar,
        layoutManager: LinearLayoutManager
    ) {

        /*
         * Si existe una fecha de inicio y pertenece
         * al mes actual, centramos esa fecha.
         *
         * Si estamos en el mes actual y todavía
         * no hay selección, centramos el día actual.
         */

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


        // =====================================================
        // POSICIÓN
        // =====================================================

        val posicion =
            fechaObjetivo.get(
                Calendar.DAY_OF_MONTH
            ) - 1


        // =====================================================
        // ANCHO DEL DÍA
        // =====================================================

        val anchoDia =
            dpApx(66)


        binding.recyclerDias.post {

            val anchoRecycler =
                binding.recyclerDias.width


            if (anchoRecycler <= 0) {
                return@post
            }


            // =================================================
            // PADDING LATERAL
            // =================================================

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


            // =================================================
            // CENTRAR
            // =================================================

            /*
             * El padding ya permite que el elemento
             * quede centrado.
             *
             * Por eso el offset es 0.
             */

            layoutManager.scrollToPositionWithOffset(
                posicion,
                0
            )
        }
    }


    // =========================================================
    // CENTRAR MES SELECCIONADO
    // =========================================================

    private fun centrarMesSeleccionado() {

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

        // =====================================================
        // NO EXISTE FECHA DE INICIO
        // =====================================================

        if (fechaInicio == null) {

            binding.cardResumenFechas.visibility =
                View.GONE


            binding.seccionCuidadoresAsignar.visibility =
                View.GONE


            return
        }


        // =====================================================
        // MOSTRAR RESUMEN
        // =====================================================

        binding.cardResumenFechas.visibility =
            View.VISIBLE


        binding.txtFechaInicioSeleccionada.text =
            formatoFechaVisible.format(
                fechaInicio!!.time
            )


        // =====================================================
        // FALTA FECHA FINAL
        // =====================================================

        if (fechaFin == null) {

            binding.txtFechaFinSeleccionada.text =
                "Selecciona la fecha final"


            binding.txtDuracionSeleccionada.text =
                "Pendiente"


            /*
             * No mostramos cuidadores porque
             * todavía falta la fecha final.
             */

            binding.seccionCuidadoresAsignar.visibility =
                View.GONE


            return
        }


        // =====================================================
        // MOSTRAR FECHA FINAL
        // =====================================================

        binding.txtFechaFinSeleccionada.text =
            formatoFechaVisible.format(
                fechaFin!!.time
            )


        // =====================================================
        // CALCULAR DURACIÓN
        // =====================================================

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


        // =====================================================
        // COMPROBAR SI YA ESTÁ TODO COMPLETO
        // =====================================================

        val datosCompletos =
            tipoTurnoSeleccionado != null &&
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


                actualizarEstadoBotonAsignar()
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

        lifecycleScope.launch {

            try {

                val usuarios =
                    RetrofitClient.apiService
                        .getUsuarios()


                val cuidadores =
                    usuarios.filter {

                        it.idRol == 5 &&
                                it.estado
                    }


                cuidadorAdapter
                    .actualizarLista(
                        cuidadores
                    )

            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
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

        val puedeAsignar =
            tipoTurnoSeleccionado != null &&
                    fechaInicio != null &&
                    fechaFin != null &&
                    cuidadoresSeleccionados.isNotEmpty()


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
    // ASIGNAR TURNO
    // =========================================================

    private fun asignarTurno() {

        if (
            tipoTurnoSeleccionado == null ||
            fechaInicio == null ||
            fechaFin == null ||
            cuidadoresSeleccionados.isEmpty()
        ) {

            Toast.makeText(
                requireContext(),
                "Completa todos los datos del turno",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        lifecycleScope.launch {

            try {

                binding.btnAsignarTurnoGlobal
                    .isEnabled = false


                val inicio =
                    fechaInicio!!
                        .clone() as Calendar


                val fin =
                    fechaFin!!
                        .clone() as Calendar


                val calendario =
                    inicio.clone() as Calendar


                // =================================================
                // CREAR TURNOS POR CADA DÍA
                // =================================================

                while (
                    !calendario.after(fin)
                ) {

                    val fecha =
                        formatoFecha.format(
                            calendario.time
                        )


                    val turno =
                        Turno(

                            fecha = fecha,

                            hora_inicio =
                                horaInicioSeleccionada,

                            hora_fin =
                                horaFinSeleccionada,

                            estado = true,

                            nombre =
                                "Turno $tipoTurnoSeleccionado",

                            descripcion =
                                "Turno asignado desde GerIApp"
                        )


                    // =================================================
                    // CREAR TURNO
                    // =================================================

                    val turnoCreado =
                        RetrofitClient.apiService
                            .crearTurno(turno)


                    val idTurno =
                        turnoCreado.id_turno


                    if (idTurno == null) {

                        throw Exception(
                            "La API no devolvió el ID del turno"
                        )
                    }


                    // =================================================
                    // ASIGNAR A CADA CUIDADOR
                    // =================================================

                    for (
                    cuidador
                    in cuidadoresSeleccionados
                    ) {

                        val idUsuario =
                            cuidador.idUsuario


                        if (idUsuario == null) {
                            continue
                        }


                        val asignacion =
                            AsignacionTurnoUsuario(

                                id_usuario =
                                    idUsuario,

                                id_turno =
                                    idTurno,

                                fecha =
                                    fecha,

                                estado =
                                    "Asignado"
                            )


                        RetrofitClient.apiService
                            .crearAsignacionTurno(
                                asignacion
                            )
                    }


                    calendario.add(
                        Calendar.DAY_OF_MONTH,
                        1
                    )
                }


                // =================================================
                // ÉXITO
                // =================================================

                Toast.makeText(
                    requireContext(),
                    "Turno asignado correctamente",
                    Toast.LENGTH_SHORT
                ).show()


                limpiarFormulario()


            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
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

        tipoTurnoSeleccionado = null

        horaInicioSeleccionada = ""

        horaFinSeleccionada = ""

        fechaInicio = null

        fechaFin = null

        cuidadoresSeleccionados =
            emptyList()


        binding.selectorTipoTurno.setText(
            "",
            false
        )


        binding.cardInformacionTurno.visibility =
            View.GONE


        binding.cardResumenFechas.visibility =
            View.GONE


        binding.seccionCuidadoresAsignar.visibility =
            View.GONE


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


    // =========================================================
    // DP A PX
    // =========================================================

    private fun dpApx(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }
}