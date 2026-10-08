package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.model.Usuario
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.data.repository.TurnoRepository
import com.example.molvigeryapp.data.repository.UsuarioRepository
import com.example.molvigeryapp.databinding.FragmentDetalleCuidadorEncBinding
import com.example.molvigeryapp.ui.encargado.bitacora.BitacoraEncargadoFragment
import com.example.molvigeryapp.ui.encargado.pacientes.PacientesCuidadorEncargadoFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DetalleCuidadorEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentDetalleCuidadorEncBinding? = null

    private val binding
        get() = requireNotNull(_binding)


    // =====================================================
    // REPOSITORIOS
    // =====================================================

    private val usuarioRepository by lazy {
        UsuarioRepository()
    }

    private val pacienteRepository by lazy {
        PacienteRepository()
    }

    private val turnoRepository by lazy {
        TurnoRepository()
    }


    // =====================================================
    // CONTROL DE CARGA
    // =====================================================

    /**
     * Evita que se ejecuten dos cargas simultáneamente.
     */
    private var cargandoDatos = false

    /**
     * Indica si la primera carga terminó.
     *
     * Durante la primera carga:
     * - Se muestra el ProgressBar.
     * - Se ocultan las opciones.
     *
     * Después de la primera carga:
     * - Las opciones permanecen visibles.
     * - Las actualizaciones no provocan parpadeos.
     */
    private var primeraCargaCompletada = false


    // =====================================================
    // CONSTANTES
    // =====================================================

    companion object {

        private const val TAG =
            "DETALLE_CUIDADOR"

        private const val INTERVALO_ACTUALIZACION =
            2_000L

        private const val ESTADO_FINALIZADO =
            "finalizado"

        private const val ESTADO_CANCELADO =
            "cancelado"
    }


    // =====================================================
    // MODELO TEMPORAL PARA CARGAR INFORMACIÓN
    // =====================================================

    private data class ResultadoCarga(

        val usuario: Usuario?,

        val asignacionesPacientes:
        List<AsignacionPacienteCuidador>,

        val asignacionesTurnos:
        List<AsignacionTurnoUsuario>,

        val turnosCatalogo:
        List<Turno>
    )


    // =====================================================
    // MODELO INTERNO PARA TARJETAS DE TURNO
    // =====================================================

    private data class RegistroTurnoDetalle(

        val asignacion:
        AsignacionTurnoUsuario,

        val turno:
        Turno
    )


    // =====================================================
    // CREAR VISTA
    // =====================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDetalleCuidadorEncBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // =====================================================
    // VISTA CREADA
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        configurarBotonVolver()

        configurarNavegacion()

        iniciarActualizacionAutomatica()
    }


    // =====================================================
    // OBTENER ID DEL CUIDADOR
    // =====================================================

    private fun obtenerIdUsuario(): Int {

        return arguments?.getInt(
            "id_usuario",
            0
        ) ?: 0
    }


    // =====================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =====================================================

    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // =================================================
                // OBTENER ID
                // =================================================

                val idUsuario =
                    obtenerIdUsuario()


                if (idUsuario == 0) {

                    mostrarMensaje(
                        "No se encontró el cuidador seleccionado.",
                        Toast.LENGTH_LONG
                    )

                    return@repeatOnLifecycle
                }


                // =================================================
                // PRIMERA CARGA INMEDIATA
                // =================================================

                cargarInformacionReal(
                    idUsuario = idUsuario,
                    mostrarCargaInicial = true
                )


                // =================================================
                // ACTUALIZACIÓN CADA 2 SEGUNDOS
                // =================================================

                while (isActive) {

                    delay(
                        INTERVALO_ACTUALIZACION
                    )


                    if (!isActive) {
                        break
                    }


                    cargarInformacionReal(
                        idUsuario = idUsuario,
                        mostrarCargaInicial = false
                    )
                }
            }
        }
    }


    // =====================================================
    // CARGAR INFORMACIÓN REAL
    // =====================================================

    private suspend fun cargarInformacionReal(
        idUsuario: Int,
        mostrarCargaInicial: Boolean
    ) {

        // =================================================
        // VALIDAR VISTA
        // =================================================

        if (_binding == null) {
            return
        }


        // =================================================
        // EVITAR PETICIONES SIMULTÁNEAS
        // =================================================

        if (cargandoDatos) {
            return
        }


        cargandoDatos = true


        // =================================================
        // MOSTRAR LOADING SOLO EN PRIMERA CARGA
        // =================================================

        if (
            mostrarCargaInicial &&
            !primeraCargaCompletada
        ) {

            mostrarCargandoInicial()
        }


        try {

            /*
             * Las consultas son independientes.
             * Se ejecutan en paralelo para evitar
             * esperar innecesariamente una por una.
             */

            val resultado =
                coroutineScope {

                    // =============================================
                    // USUARIO
                    // =============================================

                    val usuarioDeferred =
                        async<Usuario?> {

                            usuarioRepository
                                .obtenerUsuarioPorId(
                                    idUsuario
                                )
                        }


                    // =============================================
                    // ASIGNACIONES PACIENTE-CUIDADOR
                    // =============================================

                    val pacientesDeferred =
                        async<List<AsignacionPacienteCuidador>> {

                            pacienteRepository
                                .obtenerAsignacionesPacienteCuidador()
                        }


                    // =============================================
                    // ASIGNACIONES DE TURNOS
                    // =============================================

                    val asignacionesTurnosDeferred =
                        async<List<AsignacionTurnoUsuario>> {

                            RetrofitClient
                                .apiService
                                .getAsignacionesTurno()
                        }


                    // =============================================
                    // CATÁLOGO DE TURNOS
                    // =============================================

                    val turnosCatalogoDeferred =
                        async<List<Turno>> {

                            turnoRepository
                                .obtenerTurnos()
                        }


                    // =============================================
                    // ESPERAR RESULTADOS
                    // =============================================

                    ResultadoCarga(

                        usuario =
                            usuarioDeferred.await(),

                        asignacionesPacientes =
                            pacientesDeferred.await(),

                        asignacionesTurnos =
                            asignacionesTurnosDeferred.await(),

                        turnosCatalogo =
                            turnosCatalogoDeferred.await()
                    )
                }


            // =================================================
            // COMPROBAR CORRUTINA Y VISTA
            // =================================================

            if (
                !currentCoroutineContext().isActive ||
                _binding == null
            ) {

                return
            }


            // =================================================
            // APLICAR INFORMACIÓN DEL CUIDADOR
            // =================================================

            aplicarDatosUsuario(
                resultado.usuario
            )


            // =================================================
            // CONTAR PACIENTES
            // =================================================

            val cantidadPacientes =
                contarPacientesDelCuidador(
                    asignaciones =
                        resultado.asignacionesPacientes,

                    idUsuario =
                        idUsuario
                )


            binding
                .txtCantidadPacientes
                .text =
                cantidadPacientes.toString()


            // =================================================
            // FILTRAR TURNOS DEL CUIDADOR
            // =================================================

            val turnosDelCuidador =
                resultado.asignacionesTurnos
                    .filter { asignacion ->

                        asignacion.id_usuario ==
                                idUsuario
                    }


            // =================================================
            // CONTAR TARJETAS ACTIVAS
            // =================================================

            val cantidadTarjetasActivas =
                contarTarjetasActivas(

                    asignaciones =
                        turnosDelCuidador,

                    turnosCatalogo =
                        resultado.turnosCatalogo
                )


            binding
                .txtCantidadTurnos
                .text =
                cantidadTarjetasActivas.toString()


            // =================================================
            // FINALIZAR PRIMERA CARGA
            // =================================================

            if (
                !primeraCargaCompletada
            ) {

                primeraCargaCompletada =
                    true

                mostrarCargaCompletada()
            }


        } catch (
            e: CancellationException
        ) {

            /*
             * Cancelación normal cuando el Fragment
             * sale de pantalla.
             */

            throw e


        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error cargando información del cuidador",
                e
            )


            if (
                !currentCoroutineContext().isActive ||
                _binding == null
            ) {

                return
            }


            /*
             * Si la primera carga falla, mostramos
             * las opciones para que la pantalla no
             * quede bloqueada.
             *
             * Las siguientes actualizaciones seguirán
             * intentando obtener los datos de la API.
             */

            if (
                !primeraCargaCompletada
            ) {

                primeraCargaCompletada =
                    true

                mostrarCargaCompletada()

                mostrarMensaje(
                    "No se pudo cargar la información del cuidador.",
                    Toast.LENGTH_LONG
                )
            }

            /*
             * Si falla una actualización posterior,
             * NO ocultamos las opciones.
             *
             * Los datos anteriores permanecen visibles.
             */
        }


        // =================================================
        // LIBERAR CONTROL DE CARGA
        // =================================================

        finally {

            cargandoDatos =
                false
        }
    }


    // =====================================================
    // APLICAR DATOS DEL USUARIO
    // =====================================================

    private fun aplicarDatosUsuario(
        usuario: Usuario?
    ) {

        if (usuario == null) {
            return
        }


        // =================================================
        // NOMBRE
        // =================================================

        val nombreCompleto =
            "${usuario.nombres} ${usuario.apellidos}"
                .trim()


        binding
            .txtNombreCuidador
            .text =
            nombreCompleto


        // =================================================
        // CARGO
        // =================================================

        val cargo =
            arguments
                ?.getString("cargo")
                .orEmpty()


        binding
            .txtCargoCuidador
            .text =
            cargo


        // =================================================
        // ID
        // =================================================

        binding
            .txtIdCuidador
            .text =
            usuario.idUsuario.toString()


        // =================================================
        // ESTADO
        // =================================================

        aplicarEstadoUsuario(
            usuario
        )
    }


    // =====================================================
    // APLICAR ESTADO DEL USUARIO
    // =====================================================

    private fun aplicarEstadoUsuario(
        usuario: Usuario?
    ) {

        if (usuario == null) {
            return
        }


        val estadoTexto =
            if (usuario.estado) {

                "Activo"

            } else {

                "Inactivo"
            }


        binding
            .txtEstadoCuidador
            .text =
            estadoTexto


        binding
            .txtEstadoResumen
            .text =
            estadoTexto
    }


    // =====================================================
    // MOSTRAR CARGA INICIAL
    // =====================================================

    private fun mostrarCargandoInicial() {
    }

    private fun mostrarCargaCompletada() {
    }


    // =====================================================
    // MOSTRAR MENSAJE
    // =====================================================

    private fun mostrarMensaje(
        mensaje: String,
        duracion: Int
    ) {

        val contexto =
            context ?: return


        Toast.makeText(
            contexto,
            mensaje,
            duracion
        ).show()
    }


    // =====================================================
    // CONTAR PACIENTES
    // =====================================================

    private fun contarPacientesDelCuidador(
        asignaciones:
        List<AsignacionPacienteCuidador>,

        idUsuario: Int
    ): Int {

        val pacientes =
            asignaciones.mapNotNull { asignacion ->

                try {

                    val campoUsuario =
                        asignacion::class.java
                            .declaredFields
                            .firstOrNull { campo ->

                                campo.name ==
                                        "idUsuario"
                            }


                    val campoPaciente =
                        asignacion::class.java
                            .declaredFields
                            .firstOrNull { campo ->

                                campo.name ==
                                        "idPaciente"
                            }


                    if (
                        campoUsuario == null ||
                        campoPaciente == null
                    ) {

                        null

                    } else {

                        campoUsuario.isAccessible =
                            true

                        campoPaciente.isAccessible =
                            true


                        val usuarioAsignado =
                            campoUsuario.get(
                                asignacion
                            ) as? Int


                        val paciente =
                            campoPaciente.get(
                                asignacion
                            ) as? Int


                        if (
                            usuarioAsignado ==
                            idUsuario
                        ) {

                            paciente

                        } else {

                            null
                        }
                    }

                } catch (
                    _: Exception
                ) {

                    null
                }
            }
                .distinct()


        return pacientes.size
    }


    // =====================================================
    // CONTAR TARJETAS ACTIVAS
    // =====================================================

    private fun contarTarjetasActivas(
        asignaciones:
        List<AsignacionTurnoUsuario>,

        turnosCatalogo:
        List<Turno>
    ): Int {

        if (
            asignaciones.isEmpty() ||
            turnosCatalogo.isEmpty()
        ) {

            return 0
        }


        val registros =
            asignaciones.mapNotNull { asignacion ->

                val turnoRelacionado =
                    turnosCatalogo.firstOrNull { turno ->

                        turno.id_turno ==
                                asignacion.id_turno
                    }


                if (
                    turnoRelacionado == null
                ) {

                    null

                } else {

                    RegistroTurnoDetalle(

                        asignacion =
                            asignacion,

                        turno =
                            turnoRelacionado
                    )
                }
            }


        if (
            registros.isEmpty()
        ) {

            return 0
        }


        // =================================================
        // REGISTROS CON GRUPO
        // =================================================

        val registrosConGrupo =
            registros.filter { registro ->

                !registro
                    .asignacion
                    .id_grupo_asignacion
                    .isNullOrBlank()
            }


        val gruposModernos =
            registrosConGrupo.groupBy { registro ->

                registro
                    .asignacion
                    .id_grupo_asignacion!!
                    .trim()
            }


        // =================================================
        // REGISTROS LEGACY
        // =================================================

        val registrosLegacy =
            registros.filter { registro ->

                registro
                    .asignacion
                    .id_grupo_asignacion
                    .isNullOrBlank()
            }


        var tarjetasActivas =
            0


        // =================================================
        // GRUPOS MODERNOS
        // =================================================

        gruposModernos.values.forEach { grupo ->

            if (
                tarjetaEstaActiva(
                    grupo
                )
            ) {

                tarjetasActivas++
            }
        }


        // =================================================
        // GRUPOS LEGACY
        // =================================================

        if (
            registrosLegacy.isNotEmpty()
        ) {

            val registrosOrdenados =
                registrosLegacy.sortedWith(

                    compareBy(

                        {
                            obtenerFechaOrdenable(
                                it.asignacion
                            )
                                ?.timeInMillis
                                ?: Long.MAX_VALUE
                        },

                        {
                            normalizarHora(
                                it.turno.hora_inicio
                            )
                                ?: ""
                        }
                    )
                )


            val gruposLegacy =
                registrosOrdenados.groupBy { registro ->

                    val tipo =
                        determinarTipoTurno(
                            registro.turno
                        )
                            .trim()
                            .lowercase(
                                Locale.getDefault()
                            )


                    val horaInicio =
                        normalizarHora(
                            registro.turno.hora_inicio
                        )
                            ?: ""


                    val horaFin =
                        normalizarHora(
                            registro.turno.hora_fin
                        )
                            ?: ""


                    "$tipo|$horaInicio|$horaFin"
                }


            gruposLegacy.values.forEach { grupo ->

                val ordenados =
                    grupo.sortedBy { registro ->

                        obtenerFechaOrdenable(
                            registro.asignacion
                        )
                            ?.timeInMillis
                            ?: Long.MAX_VALUE
                    }


                var bloqueActual =
                    mutableListOf<RegistroTurnoDetalle>()


                var fechaAnterior:
                        Calendar? = null


                ordenados.forEach { registro ->

                    val fechaActual =
                        obtenerFechaOrdenable(
                            registro.asignacion
                        )


                    if (
                        bloqueActual.isEmpty()
                    ) {

                        bloqueActual.add(
                            registro
                        )

                        fechaAnterior =
                            fechaActual

                        return@forEach
                    }


                    val esDiaConsecutivo =
                        fechaAnterior != null &&
                                fechaActual != null &&
                                esDiaSiguiente(
                                    fechaAnterior,
                                    fechaActual
                                )


                    if (
                        esDiaConsecutivo
                    ) {

                        bloqueActual.add(
                            registro
                        )

                    } else {

                        if (
                            tarjetaEstaActiva(
                                bloqueActual
                            )
                        ) {

                            tarjetasActivas++
                        }


                        bloqueActual =
                            mutableListOf()


                        bloqueActual.add(
                            registro
                        )
                    }


                    fechaAnterior =
                        fechaActual
                }


                if (
                    bloqueActual.isNotEmpty() &&
                    tarjetaEstaActiva(
                        bloqueActual
                    )
                ) {

                    tarjetasActivas++
                }
            }
        }


        return tarjetasActivas
    }


    // =====================================================
    // SABER SI UNA TARJETA ESTÁ ACTIVA
    // =====================================================

    private fun tarjetaEstaActiva(
        grupo:
        List<RegistroTurnoDetalle>
    ): Boolean {

        if (
            grupo.isEmpty()
        ) {

            return false
        }


        // =================================================
        // ESTADO FINALIZADO / CANCELADO
        // =================================================

        val tieneEstadoFinalizadoOCancelado =
            grupo.any { registro ->

                val estado =
                    registro
                        .asignacion
                        .estado
                        .trim()
                        .lowercase(
                            Locale.getDefault()
                        )


                estado ==
                        ESTADO_FINALIZADO ||

                        estado ==
                        ESTADO_CANCELADO
            }


        if (
            tieneEstadoFinalizadoOCancelado
        ) {

            return false
        }


        // =================================================
        // FECHA FINAL
        // =================================================

        val fechaFin =
            grupo
                .mapNotNull { registro ->

                    obtenerFechaOrdenable(
                        registro.asignacion
                    )
                }
                .maxByOrNull { fecha ->

                    fecha.timeInMillis
                }


        if (
            fechaFin == null
        ) {

            return false
        }


        // =================================================
        // FECHA ACTUAL
        // =================================================

        val hoy =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }


        return !fechaFin.before(
            hoy
        )
    }


    // =====================================================
    // DETERMINAR TIPO DE TURNO
    // =====================================================

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


    // =====================================================
    // OBTENER FECHA ORDENABLE
    // =====================================================

    private fun obtenerFechaOrdenable(
        asignacion:
        AsignacionTurnoUsuario
    ): Calendar? {

        val fechaOriginal =
            asignacion.fecha


        if (
            fechaOriginal.isNullOrBlank()
        ) {

            return null
        }


        val fecha =
            if (
                fechaOriginal.length >= 10 &&
                fechaOriginal[4] == '-' &&
                fechaOriginal[7] == '-'
            ) {

                fechaOriginal.substring(
                    0,
                    10
                )

            } else {

                convertirFechaParaApi(
                    fechaOriginal
                )
            }


        if (
            fecha.isNullOrBlank()
        ) {

            return null
        }


        return try {

            val formato =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )


            formato.isLenient =
                false


            val date =
                formato.parse(
                    fecha
                )


            if (
                date == null
            ) {

                null

            } else {

                Calendar.getInstance().apply {

                    time =
                        date


                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )


                    set(
                        Calendar.MINUTE,
                        0
                    )


                    set(
                        Calendar.SECOND,
                        0
                    )


                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }
            }

        } catch (
            _: Exception
        ) {

            null
        }
    }


    // =====================================================
    // SABER SI UNA FECHA ES EL DÍA SIGUIENTE
    // =====================================================

    private fun esDiaSiguiente(
        anterior: Calendar,
        actual: Calendar
    ): Boolean {

        val siguiente =
            anterior.clone() as Calendar


        siguiente.add(
            Calendar.DAY_OF_YEAR,
            1
        )


        return siguiente.timeInMillis ==
                actual.timeInMillis
    }


    // =====================================================
    // NORMALIZAR HORA
    // =====================================================

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


        // =================================================
        // HH:mm:ss
        // =================================================

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


        // =================================================
        // HH:mm
        // =================================================

        if (
            valor.matches(
                Regex(
                    "^\\d{2}:\\d{2}$"
                )
            )
        ) {

            return valor
        }


        // =================================================
        // OTROS FORMATOS
        // =================================================

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


                formato.isLenient =
                    false


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
                    )
                        .format(
                            date
                        )
                }

            } catch (
                _: Exception
            ) {

                // Probar siguiente formato.
            }
        }


        return valor
    }


    // =====================================================
    // CONVERTIR FECHA A API
    // =====================================================

    private fun convertirFechaParaApi(
        fecha: String?
    ): String? {

        if (
            fecha.isNullOrBlank()
        ) {

            return null
        }


        // =================================================
        // YA ESTÁ EN FORMATO API
        // =================================================

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


        // =================================================
        // DD/MM/YYYY → YYYY-MM-DD
        // =================================================

        return try {

            val entrada =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )


            entrada.isLenient =
                false


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


    // =====================================================
    // BOTÓN VOLVER
    // =====================================================

    private fun configurarBotonVolver() {

        binding
            .btnVolver
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }


    // =====================================================
    // NAVEGACIÓN
    // =====================================================

    private fun configurarNavegacion() {

        // =================================================
        // EVENTOS
        // =================================================

        binding
            .moduloEventos
            .setOnClickListener {

                abrirPantalla(
                    EventosCuidadorEncargadoFragment()
                )
            }


        // =================================================
        // TURNOS
        // =================================================

        binding
            .moduloTurnos
            .setOnClickListener {

                abrirPantalla(
                    TurnosCuidadorEncargadoFragment()
                )
            }


        // =================================================
        // PACIENTES
        // =================================================

        binding
            .moduloPacientes
            .setOnClickListener {

                abrirPantalla(
                    PacientesCuidadorEncargadoFragment()
                )
            }


        // =================================================
        // BITÁCORA
        // =================================================

        binding
            .moduloBitacora
            .setOnClickListener {

                abrirPantalla(
                    BitacoraEncargadoFragment()
                )
            }
    }


    // =====================================================
    // ABRIR PANTALLA
    // =====================================================

    private fun abrirPantalla(
        fragment: Fragment
    ) {

        val bindingActual =
            _binding ?: return


        val datos =
            Bundle().apply {

                // =============================================
                // ID DEL CUIDADOR
                // =============================================

                putInt(
                    "id_usuario",
                    obtenerIdUsuario()
                )


                // =============================================
                // NOMBRE
                // =============================================

                putString(
                    "nombre",
                    bindingActual
                        .txtNombreCuidador
                        .text
                        .toString()
                )


                // =============================================
                // CARGO
                // =============================================

                putString(
                    "cargo",
                    bindingActual
                        .txtCargoCuidador
                        .text
                        .toString()
                )


                // =============================================
                // ESTADO
                // =============================================

                putString(
                    "estado",
                    bindingActual
                        .txtEstadoCuidador
                        .text
                        .toString()
                )


                // =============================================
                // PACIENTES
                // =============================================

                putInt(
                    "pacientes",
                    bindingActual
                        .txtCantidadPacientes
                        .text
                        .toString()
                        .toIntOrNull()
                        ?: 0
                )
            }


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


    // =====================================================
    // DESTRUIR VISTA
    // =====================================================

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }
}