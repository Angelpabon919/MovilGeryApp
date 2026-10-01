package com.example.molvigeryapp.ui.encargado.pacientes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentPerfilPacienteEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class PerfilPacienteEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding:
            FragmentPerfilPacienteEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =========================================================
    // REPOSITORY
    // =========================================================

    private val repository =
        PacienteRepository()


    // =========================================================
    // PACIENTE
    // =========================================================

    private var paciente: Paciente? = null

    private var idPaciente: Int = 0


    // =========================================================
    // CARGA
    // =========================================================

    private var cargandoPaciente = false


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        idPaciente =
            arguments?.getInt(
                "idPaciente",
                0
            ) ?: 0
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
            FragmentPerfilPacienteEncargadoBinding.inflate(
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

        configurarBotonVolver()

        cargarPaciente()
    }


    // =========================================================
    // CARGAR PACIENTE DESDE API
    // =========================================================

    private fun cargarPaciente() {

        if (idPaciente <= 0) {

            mostrarPacienteNoDisponible()

            return
        }


        if (cargandoPaciente) {
            return
        }


        cargandoPaciente = true

        mostrarCargando()


        viewLifecycleOwner.lifecycleScope.launch {

            try {

                // =================================================
                // CONSULTAR API
                // =================================================

                val pacienteApi =
                    repository.obtenerPacientePorId(
                        idPaciente
                    )


                // =================================================
                // COMPROBAR QUE LA VISTA SIGUE EXISTIENDO
                // =================================================

                val bindingActual =
                    _binding ?: return@launch


                // =================================================
                // GUARDAR PACIENTE
                // =================================================

                paciente =
                    pacienteApi


                // =================================================
                // MOSTRAR INFORMACIÓN
                // =================================================

                mostrarInformacionPaciente(
                    pacienteApi
                )


            } catch (
                e: CancellationException
            ) {

                throw e


            } catch (
                e: Exception
            ) {

                Log.e(
                    "PERFIL_PACIENTE",
                    "Error al obtener paciente $idPaciente",
                    e
                )


                val bindingActual =
                    _binding ?: return@launch


                mostrarPacienteNoDisponible()


                val contexto =
                    context


                if (contexto != null) {

                    Toast.makeText(
                        contexto,
                        "No se pudo cargar la información del paciente",
                        Toast.LENGTH_SHORT
                    ).show()
                }


            } finally {

                cargandoPaciente = false

                ocultarCargando()
            }
        }
    }


    // =========================================================
    // MOSTRAR CARGANDO
    // =========================================================

    private fun mostrarCargando() {

        val bindingActual =
            _binding ?: return


        bindingActual
            .progressBarPerfilPaciente
            .visibility =
            View.VISIBLE


        bindingActual
            .scrollPerfilPaciente
            .visibility =
            View.GONE
    }


    // =========================================================
    // OCULTAR CARGANDO
    // =========================================================

    private fun ocultarCargando() {

        val bindingActual =
            _binding ?: return


        bindingActual
            .progressBarPerfilPaciente
            .visibility =
            View.GONE


        bindingActual
            .scrollPerfilPaciente
            .visibility =
            View.VISIBLE
    }


    // =========================================================
    // MOSTRAR INFORMACIÓN
    // =========================================================

    private fun mostrarInformacionPaciente(
        pacienteActual: Paciente
    ) {

        val bindingActual =
            _binding ?: return


        // =====================================================
        // INFORMACIÓN PRINCIPAL
        // =====================================================

        val nombreCompleto =
            "${pacienteActual.nombre} ${pacienteActual.apellido}"
                .trim()


        bindingActual
            .txtNombrePerfilPaciente
            .text =
            nombreCompleto.ifBlank {
                "Paciente sin nombre"
            }


        bindingActual
            .txtEstadoPerfilPaciente
            .text =
            if (pacienteActual.estado) {
                "Activo"
            } else {
                "Inactivo"
            }


        // =====================================================
        // INFORMACIÓN PERSONAL
        // =====================================================

        bindingActual
            .txtDocumentoPerfilPaciente
            .text =
            construirDocumento(
                pacienteActual
            )


        bindingActual
            .txtFechaNacimientoPerfilPaciente
            .text =
            pacienteActual.fechaNacimiento
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "No registrada"


        bindingActual
            .txtGeneroPerfilPaciente
            .text =
            pacienteActual.genero
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "No registrado"


        // =====================================================
        // INFORMACIÓN MÉDICA
        // =====================================================

        bindingActual
            .txtGrupoSanguineoPerfilPaciente
            .text =
            construirGrupoSanguineo(
                pacienteActual
            )


        bindingActual
            .txtEpsPerfilPaciente
            .text =
            pacienteActual.eps
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "No registrada"


        bindingActual
            .txtSedePerfilPaciente
            .text =
            pacienteActual.sede
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: "No registrada"


        // =====================================================
        // UBICACIÓN
        // =====================================================

        bindingActual
            .txtHabitacionPerfilPaciente
            .text =
            pacienteActual.habitacion
                ?.toString()
                ?: "No registrada"


        bindingActual
            .txtCamaPerfilPaciente
            .text =
            pacienteActual.cama
                ?.toString()
                ?: "No registrada"


        // =====================================================
        // FECHA DE INGRESO
        // =====================================================

        mostrarFechaIngreso(
            pacienteActual.fechaIngreso
        )
    }


    // =========================================================
    // FECHA DE INGRESO
    // =========================================================

    private fun mostrarFechaIngreso(
        fechaIngreso: String?
    ) {

        val bindingActual =
            _binding ?: return


        if (fechaIngreso.isNullOrBlank()) {

            bindingActual
                .txtFechaIngresoPerfilPaciente
                .text =
                "No registrada"


            bindingActual
                .txtTiempoIngresoPerfilPaciente
                .text =
                "Fecha de ingreso no disponible"

            return
        }


        try {

            val fecha =
                parsearFecha(
                    fechaIngreso
                )


            if (fecha == null) {

                bindingActual
                    .txtFechaIngresoPerfilPaciente
                    .text =
                    "Fecha no disponible"


                bindingActual
                    .txtTiempoIngresoPerfilPaciente
                    .text =
                    "No se pudo calcular el tiempo de ingreso"

                return
            }


            // =================================================
            // FECHA FORMATEADA
            // =================================================

            val formatoSalida =
                SimpleDateFormat(
                    "d 'de' MMMM 'de' yyyy",
                    Locale("es", "CO")
                )


            bindingActual
                .txtFechaIngresoPerfilPaciente
                .text =
                formatoSalida.format(
                    fecha
                )


            // =================================================
            // TIEMPO TRANSCURRIDO
            // =================================================

            val ahora =
                Date()


            val diferencia =
                ahora.time -
                        fecha.time


            val dias =
                TimeUnit.MILLISECONDS.toDays(
                    diferencia
                )


            bindingActual
                .txtTiempoIngresoPerfilPaciente
                .text =
                when {

                    dias < 0 -> {
                        "Fecha de ingreso pendiente"
                    }

                    dias == 0L -> {
                        "Paciente ingresado hoy"
                    }

                    dias == 1L -> {
                        "Paciente ingresado hace 1 día"
                    }

                    else -> {
                        "Paciente ingresado hace $dias días"
                    }
                }


        } catch (
            e: Exception
        ) {

            Log.e(
                "PERFIL_PACIENTE",
                "Error al formatear fecha de ingreso",
                e
            )


            bindingActual
                .txtFechaIngresoPerfilPaciente
                .text =
                "Fecha no disponible"


            bindingActual
                .txtTiempoIngresoPerfilPaciente
                .text =
                "No se pudo calcular el tiempo de ingreso"
        }
    }


    // =========================================================
    // PARSEAR FECHA
    // =========================================================

    private fun parsearFecha(
        fechaTexto: String
    ): Date? {

        val formatos =
            listOf(

                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",

                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

                "yyyy-MM-dd'T'HH:mm:ssXXX",

                "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",

                "yyyy-MM-dd'T'HH:mm:ss'Z'",

                "yyyy-MM-dd"
            )


        for (formato in formatos) {

            try {

                val parser =
                    SimpleDateFormat(
                        formato,
                        Locale.US
                    )


                parser.timeZone =
                    TimeZone.getTimeZone(
                        "UTC"
                    )


                val fecha =
                    parser.parse(
                        fechaTexto
                    )


                if (fecha != null) {
                    return fecha
                }

            } catch (
                _: Exception
            ) {
                // Intentar siguiente formato
            }
        }


        return null
    }


    // =========================================================
    // DOCUMENTO
    // =========================================================

    private fun construirDocumento(
        paciente: Paciente
    ): String {

        val tipo =
            paciente.tipoDocumento
                ?.takeIf {
                    it.isNotBlank()
                }


        val numero =
            paciente.numeroDocumento
                ?.takeIf {
                    it.isNotBlank()
                }


        return when {

            tipo != null &&
                    numero != null ->
                "$tipo $numero"

            numero != null ->
                numero

            tipo != null ->
                tipo

            else ->
                "No registrado"
        }
    }


    // =========================================================
    // GRUPO SANGUÍNEO
    // =========================================================

    private fun construirGrupoSanguineo(
        paciente: Paciente
    ): String {

        val grupo =
            paciente.grupoSanguineo
                ?.takeIf {
                    it.isNotBlank()
                }


        val rh =
            paciente.rh
                ?.takeIf {
                    it.isNotBlank()
                }


        return when {

            grupo != null &&
                    rh != null ->
                "$grupo$rh"

            grupo != null ->
                grupo

            rh != null ->
                rh

            else ->
                "No registrado"
        }
    }


    // =========================================================
    // PACIENTE NO DISPONIBLE
    // =========================================================

    private fun mostrarPacienteNoDisponible() {

        val bindingActual =
            _binding ?: return


        bindingActual
            .txtNombrePerfilPaciente
            .text =
            "Paciente no disponible"


        bindingActual
            .txtEstadoPerfilPaciente
            .text =
            "Sin información"


        bindingActual
            .txtDocumentoPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtFechaNacimientoPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtGeneroPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtGrupoSanguineoPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtEpsPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtSedePerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtHabitacionPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtCamaPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtFechaIngresoPerfilPaciente
            .text =
            "No disponible"


        bindingActual
            .txtTiempoIngresoPerfilPaciente
            .text =
            "No disponible"
    }


    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding
            .btnVolverPerfilPaciente
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
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