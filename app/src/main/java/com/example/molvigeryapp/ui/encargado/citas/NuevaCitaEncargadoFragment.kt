package com.example.molvigeryapp.ui.encargado.citas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.model.CrearCitaRequest
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.CitasRepository
import com.example.molvigeryapp.databinding.FragmentNuevaCitaEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class NuevaCitaEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentNuevaCitaEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =========================================================
    // DATOS
    // =========================================================

    private var paciente: Paciente? = null


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {

        private const val TAG =
            "NUEVA_CITA_ENCARGADO"

        private const val ESTADO_PROGRAMADA =
            "Programada"
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
            FragmentNuevaCitaEncargadoBinding.inflate(
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

        recibirPaciente()

        mostrarPaciente()

        configurarTipoCita()
        configurarEspecialidad()
        configurarBotonVolver()
        configurarFecha()
        configurarHora()
        configurarBotonGuardar()

        binding.txtEstadoCita.text =
            ESTADO_PROGRAMADA

        mostrarFormulario()
    }


    // =========================================================
    // RECIBIR PACIENTE
    // =========================================================

    private fun recibirPaciente() {

        val idPaciente =
            arguments?.getInt(
                "idPaciente",
                -1
            ) ?: -1

        val nombre =
            arguments?.getString(
                "nombrePaciente"
            )

        val apellido =
            arguments?.getString(
                "apellidoPaciente"
            )

        val habitacion =
            arguments?.getInt(
                "habitacionPaciente",
                -1
            ) ?: -1

        val cama =
            arguments?.getInt(
                "camaPaciente",
                -1
            ) ?: -1


        paciente =
            Paciente(

                idPaciente =
                    if (idPaciente == -1) {
                        null
                    } else {
                        idPaciente
                    },

                nombre =
                    nombre ?: "",

                apellido =
                    apellido ?: "",

                habitacion =
                    if (habitacion == -1) {
                        null
                    } else {
                        habitacion
                    },

                cama =
                    if (cama == -1) {
                        null
                    } else {
                        cama
                    }
            )
    }


    // =========================================================
    // MOSTRAR PACIENTE
    // =========================================================

    private fun mostrarPaciente() {

        val pacienteSeleccionado =
            paciente

        if (pacienteSeleccionado == null) {

            mostrarError(
                "No se pudo cargar la información del paciente."
            )

            return
        }


        val nombreCompleto =
            "${pacienteSeleccionado.nombre} ${pacienteSeleccionado.apellido}"
                .trim()


        binding.txtPacienteCita.text =
            if (nombreCompleto.isBlank()) {
                "Paciente"
            } else {
                nombreCompleto
            }


        val habitacion =
            pacienteSeleccionado.habitacion
                ?.toString()
                ?: "N/A"

        val cama =
            pacienteSeleccionado.cama
                ?.toString()
                ?: "N/A"


        binding.txtHabitacionCita.text =
            "Habitación $habitacion · Cama $cama"
    }


    // =========================================================
    // TIPO DE CITA
    // =========================================================

    private fun configurarTipoCita() {

        val contexto =
            context ?: return


        val tipos =
            listOf(
                "Seleccionar tipo de cita",
                "Consulta médica",
                "Control",
                "Valoración",
                "Examen",
                "Seguimiento",
                "Urgencia"
            )


        val adapterSpinner =
            ArrayAdapter(
                contexto,
                android.R.layout.simple_spinner_item,
                tipos
            )


        adapterSpinner.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        /*
         * El popup personalizado se configura
         * directamente en el XML mediante:
         *
         * android:popupBackground="@drawable/bg_spinner_popup"
         *
         * Por eso NO lo configuramos nuevamente aquí.
         */

        binding.spinnerTipoCita.adapter =
            adapterSpinner
    }


    // =========================================================
    // ESPECIALIDAD
    // =========================================================

    private fun configurarEspecialidad() {

        val contexto =
            context ?: return


        val especialidades =
            listOf(
                "Seleccionar especialidad",
                "Medicina general",
                "Geriatría",
                "Neurología",
                "Psicología",
                "Psiquiatría",
                "Enfermería"
            )


        val adapterSpinner =
            ArrayAdapter(
                contexto,
                android.R.layout.simple_spinner_item,
                especialidades
            )


        adapterSpinner.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        /*
         * El popup personalizado se configura
         * directamente en el XML.
         */

        binding.spinnerEspecialidad.adapter =
            adapterSpinner
    }


    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding.btnVolverNuevaCita.setOnClickListener {

            if (!isAdded) {
                return@setOnClickListener
            }

            parentFragmentManager.popBackStack()
        }
    }


    // =========================================================
    // SELECCIONAR FECHA
    // =========================================================

    private fun configurarFecha() {

        binding.containerFechaCita.setOnClickListener {

            if (_binding == null) {
                return@setOnClickListener
            }


            /*
             * Si ya existe una fecha seleccionada,
             * el calendario se abrirá en esa fecha.
             *
             * Si todavía no hay fecha,
             * se abrirá en el mes actual.
             */

            val fechaActual =
                binding.txtFechaCita.text
                    .toString()
                    .trim()
                    .takeIf {

                        it.isNotBlank() &&
                                it != "Seleccionar fecha"
                    }


            val calendario =
                CalendarioPersonalizadoDialog.newInstance(

                    fechaInicial =
                        fechaActual

                ) { fechaSeleccionada ->


                    /*
                     * Verificamos que la vista todavía exista
                     * antes de modificar el binding.
                     */

                    if (_binding == null) {
                        return@newInstance
                    }


                    binding.txtFechaCita.text =
                        fechaSeleccionada
                }


            /*
             * El calendario pertenece a este Fragment,
             * por eso utilizamos childFragmentManager.
             */

            calendario.show(
                childFragmentManager,
                "CalendarioPersonalizado"
            )
        }
    }


    // =========================================================
    // SELECCIONAR HORA
    // =========================================================

    private fun configurarHora() {

        binding.containerHoraCita.setOnClickListener {

            if (_binding == null) return@setOnClickListener

            val horaActual =
                binding.txtHoraCita.text
                    .toString()
                    .trim()
                    .takeIf {
                        it.isNotBlank() &&
                                it != "Seleccionar hora"
                    }

            val selectorHora =
                SelectorHoraPersonalizadoDialog.newInstance(
                    horaInicial = horaActual
                ) { horaSeleccionada ->

                    if (_binding == null) return@newInstance

                    binding.txtHoraCita.text =
                        horaSeleccionada
                }

            selectorHora.show(
                childFragmentManager,
                "SelectorHoraPersonalizado"
            )
        }
    }


    // =========================================================
    // BOTÓN GUARDAR
    // =========================================================

    private fun configurarBotonGuardar() {

        binding.btnGuardarCita.setOnClickListener {

            guardarCita()
        }
    }


    // =========================================================
    // GUARDAR CITA
    // =========================================================

    private fun guardarCita() {

        val contexto =
            context ?: return


        // -----------------------------------------------------
        // PACIENTE
        // -----------------------------------------------------

        val pacienteSeleccionado =
            paciente


        if (pacienteSeleccionado == null) {

            Toast.makeText(
                contexto,
                "No se encontró el paciente",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val idPaciente =
            pacienteSeleccionado.idPaciente


        if (idPaciente == null) {

            Toast.makeText(
                contexto,
                "El paciente no tiene un ID válido",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // -----------------------------------------------------
        // TIPO DE CITA
        // -----------------------------------------------------

        val tipoCita =
            binding.spinnerTipoCita
                .selectedItem
                ?.toString()
                ?.trim()
                .orEmpty()


        if (
            tipoCita.isBlank() ||
            tipoCita == "Seleccionar tipo de cita"
        ) {

            Toast.makeText(
                contexto,
                "Selecciona el tipo de cita",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // -----------------------------------------------------
        // ESPECIALIDAD
        // -----------------------------------------------------

        val especialidad =
            binding.spinnerEspecialidad
                .selectedItem
                ?.toString()
                ?.trim()
                .orEmpty()


        if (
            especialidad.isBlank() ||
            especialidad == "Seleccionar especialidad"
        ) {

            Toast.makeText(
                contexto,
                "Selecciona la especialidad",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // -----------------------------------------------------
        // MOTIVO
        // -----------------------------------------------------

        val motivo =
            binding.edtMotivoCita
                .text
                .toString()
                .trim()


        if (motivo.isBlank()) {

            Toast.makeText(
                contexto,
                "Escribe el motivo de la cita",
                Toast.LENGTH_SHORT
            ).show()

            binding.edtMotivoCita.requestFocus()

            return
        }


        // -----------------------------------------------------
        // LUGAR
        // -----------------------------------------------------

        val lugar =
            binding.edtLugarCita
                .text
                .toString()
                .trim()


        if (lugar.isBlank()) {

            Toast.makeText(
                contexto,
                "Escribe el lugar de la cita",
                Toast.LENGTH_SHORT
            ).show()

            binding.edtLugarCita.requestFocus()

            return
        }


        // -----------------------------------------------------
        // FECHA
        // -----------------------------------------------------

        val fecha =
            binding.txtFechaCita
                .text
                .toString()
                .trim()


        if (
            fecha.isBlank() ||
            fecha == "Seleccionar fecha"
        ) {

            Toast.makeText(
                contexto,
                "Selecciona una fecha",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // -----------------------------------------------------
        // HORA
        // -----------------------------------------------------

        val hora =
            binding.txtHoraCita
                .text
                .toString()
                .trim()


        if (
            hora.isBlank() ||
            hora == "Seleccionar hora"
        ) {

            Toast.makeText(
                contexto,
                "Selecciona una hora",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // -----------------------------------------------------
        // OBSERVACIONES
        // -----------------------------------------------------

        val observaciones =
            binding.edtObservacionesCita
                .text
                .toString()
                .trim()


        // -----------------------------------------------------
        // MOTIVO PARA API
        // -----------------------------------------------------

        /*
         * Mantenemos el formato utilizado
         * actualmente por tu backend:
         *
         * Tipo - Especialidad: Motivo
         */

        val motivoApi =
            "$tipoCita - $especialidad: $motivo"


        // -----------------------------------------------------
        // FECHA PARA API
        // -----------------------------------------------------

        val fechaApi =
            convertirFechaParaApi(
                fecha
            )


        // -----------------------------------------------------
        // FECHA DE REGISTRO
        // -----------------------------------------------------

        val fechaRegistro =
            obtenerFechaRegistro()


        // -----------------------------------------------------
        // ID ÚNICO
        // -----------------------------------------------------

        val idCita =
            "CITA-${UUID.randomUUID()}"


        // -----------------------------------------------------
        // REQUEST
        // -----------------------------------------------------

        val request =
            CrearCitaRequest(

                idCita =
                    idCita,

                fecha =
                    fechaApi,

                hora =
                    hora,

                lugar =
                    lugar,

                motivo =
                    motivoApi,

                estado =
                    ESTADO_PROGRAMADA,

                observaciones =
                    observaciones,

                fechaRegistro =
                    fechaRegistro,

                idPaciente =
                    idPaciente,

                idUsuario =
                    null
            )


        enviarCita(
            request
        )
    }


    // =========================================================
    // ENVIAR CITA A LA API
    // =========================================================

    private fun enviarCita(
        request: CrearCitaRequest
    ) {

        mostrarCargando(
            "Guardando cita..."
        )


        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val resultado =
                    CitasRepository.crearCita(
                        request
                    )


                /*
                 * La vista pudo destruirse mientras
                 * esperábamos la respuesta.
                 */

                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }


                resultado
                    .onSuccess {

                        if (
                            !isAdded ||
                            _binding == null
                        ) {
                            return@onSuccess
                        }


                        val contexto =
                            context
                                ?: return@onSuccess


                        Toast.makeText(
                            contexto,
                            "Cita creada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()


                        parentFragmentManager
                            .popBackStack()
                    }

                    .onFailure { error ->

                        if (
                            !isAdded ||
                            _binding == null
                        ) {
                            return@onFailure
                        }


                        Log.e(
                            TAG,
                            "Error creando la cita",
                            error
                        )


                        mostrarFormulario()


                        val contexto =
                            context
                                ?: return@onFailure


                        Toast.makeText(
                            contexto,
                            "No se pudo crear la cita: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }


            } catch (
                e: CancellationException
            ) {

                /*
                 * Cancelación normal causada por
                 * destrucción/navegación del Fragment.
                 */

                throw e


            } catch (
                e: Exception
            ) {

                Log.e(
                    TAG,
                    "Error inesperado creando la cita",
                    e
                )


                if (
                    !isAdded ||
                    _binding == null
                ) {
                    return@launch
                }


                mostrarFormulario()


                val contexto =
                    context
                        ?: return@launch


                Toast.makeText(
                    contexto,
                    "No se pudo crear la cita.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // MOSTRAR CARGANDO
    // =========================================================

    private fun mostrarCargando(
        mensaje: String
    ) {

        if (_binding == null) {
            return
        }


        binding.txtLoadingNuevaCita.text =
            mensaje


        binding.loadingNuevaCita.visibility =
            View.VISIBLE


        binding.contenedorErrorNuevaCita.visibility =
            View.GONE


        binding.scrollNuevaCita.visibility =
            View.GONE


        binding.btnGuardarCita.isEnabled =
            false
    }


    // =========================================================
    // MOSTRAR FORMULARIO
    // =========================================================

    private fun mostrarFormulario() {

        if (_binding == null) {
            return
        }


        binding.loadingNuevaCita.visibility =
            View.GONE


        binding.contenedorErrorNuevaCita.visibility =
            View.GONE


        binding.scrollNuevaCita.visibility =
            View.VISIBLE


        binding.btnGuardarCita.isEnabled =
            true


        binding.btnGuardarCita.text =
            "GUARDAR CITA"
    }


    // =========================================================
    // MOSTRAR ERROR
    // =========================================================

    private fun mostrarError(
        mensaje: String
    ) {

        if (_binding == null) {
            return
        }


        binding.loadingNuevaCita.visibility =
            View.GONE


        binding.scrollNuevaCita.visibility =
            View.GONE


        binding.contenedorErrorNuevaCita.visibility =
            View.VISIBLE


        binding.txtErrorNuevaCita.text =
            mensaje
    }


    // =========================================================
    // CONVERTIR FECHA PARA API
    // =========================================================

    private fun convertirFechaParaApi(
        fecha: String
    ): String {

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                ).apply {

                    isLenient =
                        false
                }


            val formatoSalida =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )


            val fechaConvertida =
                formatoEntrada.parse(
                    fecha
                )


            if (fechaConvertida != null) {

                formatoSalida.format(
                    fechaConvertida
                )

            } else {

                fecha
            }


        } catch (
            e: Exception
        ) {

            Log.e(
                TAG,
                "Error convirtiendo fecha: $fecha",
                e
            )

            fecha
        }
    }


    // =========================================================
    // FECHA DE REGISTRO
    // =========================================================

    private fun obtenerFechaRegistro(): String {

        val formato =
            SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.getDefault()
            )


        return formato.format(
            Calendar.getInstance().time
        )
    }



    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}