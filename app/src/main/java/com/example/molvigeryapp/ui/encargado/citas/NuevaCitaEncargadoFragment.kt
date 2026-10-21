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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.molvigeryapp.R

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
    // MODO EDICIÓN
    // =========================================================

    private var modoEdicion = false
    private var idCitaEditar: String? = null
    private var citaOriginal: com.example.molvigeryapp.data.model.Cita? = null


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

        //detecta si se abrrio para editar
        modoEdicion = arguments?.getBoolean("modoEdicion", false) ?: false

        idCitaEditar = arguments?.getString("idCita")

        configurarModoFormulario()
        mostrarPaciente()

        configurarTipoCita()
        configurarEspecialidad()
        configurarBotonVolver()
        configurarFecha()
        configurarHora()
        configurarBotonGuardar()

        binding.txtEstadoCita.text =
            ESTADO_PROGRAMADA

        if (modoEdicion) {
            viewLifecycleOwner.lifecycleScope.launch {
                cargarCitaParaEditar()
            }
        } else {
            mostrarPaciente()
            mostrarFormulario()
        }

        if (!modoEdicion) {
            viewLifecycleOwner.lifecycleScope.launch {
                cargarPacienteCompleto()
            }
        }

    }

    // =========================================================
    // CARGAR FOTO DEL PACIENTE
    // =========================================================

    private fun cargarFotoPaciente(pacienteActual: Paciente) {
        val bindingActual = _binding ?: return

        Glide.with(bindingActual.imgPacienteCita)
            .load(pacienteActual.foto?.takeIf { it.isNotBlank() })
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(R.drawable.persona_encargado)
            .error(R.drawable.persona_encargado)
            .fallback(R.drawable.persona_encargado)
            .centerCrop()
            .into(bindingActual.imgPacienteCita)
    }

    // =========================================================
    // FUNCION PARA CONSULTAR LA CITA
    // =========================================================


    private suspend fun cargarCitaParaEditar() {
        val id = idCitaEditar?.takeIf { it.isNotBlank() }

        if (id == null) {
            mostrarError("No se recibió el identificador de la cita.")
            return
        }

        try {
            mostrarCargando("Cargando cita para editar...")

            val citas = CitasRepository.obtenerCitasDesdeApi()

            if (_binding == null) return

            val cita = citas.firstOrNull {
                it.id == id || it.idCita == id
            }

            if (cita == null) {
                mostrarError("No se encontró la cita que deseas editar.")
                return
            }

            citaOriginal = cita

            // Recuperar el paciente real asociado a la cita.
            val pacientes = com.example.molvigeryapp.data.api.RetrofitClient
                .apiService.getPacientes()

            if (_binding == null) return

            paciente = pacientes.firstOrNull {
                it.idPaciente == cita.idPaciente
            }

            if (paciente == null) {
                mostrarError("No se encontró el paciente de esta cita.")
                return
            }

            // Mostrar información del paciente.
            mostrarPaciente()

            // Recuperar tipo, especialidad y motivo.
            val motivoCompleto = cita.motivo.orEmpty()
            val partesTipo = motivoCompleto.split(" - ", limit = 2)

            val tipo = partesTipo.getOrNull(0)?.trim().orEmpty()
            val resto = partesTipo.getOrNull(1).orEmpty()
            val partesEspecialidad = resto.split(":", limit = 2)

            val especialidad = partesEspecialidad
                .getOrNull(0)?.trim().orEmpty()

            val motivo = partesEspecialidad
                .getOrNull(1)?.trim().orEmpty()

            seleccionarSpinner(binding.spinnerTipoCita, tipo)
            seleccionarSpinner(binding.spinnerEspecialidad, especialidad)

            binding.edtMotivoCita.setText(motivo)
            binding.edtLugarCita.setText(cita.lugar.orEmpty())
            binding.edtObservacionesCita.setText(
                cita.observaciones.orEmpty()
            )

            binding.txtFechaCita.text = convertirFechaVisible(cita.fecha)
            binding.txtHoraCita.text = cita.hora

            binding.txtEstadoCita.text = cita.estado

            mostrarFormulario()

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando cita para editar", e)

            if (_binding != null) {
                mostrarError("No se pudo cargar la cita para editar.")
            }
        }
    }

    private fun seleccionarSpinner(
        spinner: android.widget.Spinner,
        valor: String
    ) {
        val adapter = spinner.adapter ?: return

        for (i in 0 until adapter.count) {
            if (adapter.getItem(i)?.toString()
                    ?.equals(valor, ignoreCase = true) == true
            ) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun convertirFechaVisible(fecha: String): String {
        return try {
            val entrada = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            ).apply {
                isLenient = false
            }

            val salida = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            val fechaParseada = entrada.parse(fecha)
            fechaParseada?.let { salida.format(it) } ?: fecha
        } catch (e: Exception) {
            fecha
        }
    }

    // =========================================================
    // CAMBIAR TITULO Y BOTON PARA FORMULARIO EDITAR CITA
    // =========================================================

    private fun configurarModoFormulario() {
        if (_binding == null) return

        if (modoEdicion) {
            binding.txtTituloNuevaCita.text = "Editar cita"
            binding.txtSubtituloNuevaCita.text =
                "Modifica la información de la cita programada."
            binding.btnGuardarCita.text = "GUARDAR CAMBIOS"
        } else {
            binding.txtTituloNuevaCita.text = "Nueva cita"
            binding.txtSubtituloNuevaCita.text =
                "Completa la información de la cita médica."
            binding.btnGuardarCita.text = "GUARDAR CITA"
        }
    }


    // =========================================================
    // RECIBIR PACIENTE
    // =========================================================


    private fun recibirPaciente() {
        val idPaciente = arguments?.getInt("idPaciente", -1) ?: -1

        val nombre = arguments?.getString("nombrePaciente").orEmpty()
        val apellido = arguments?.getString("apellidoPaciente").orEmpty()
        val habitacion = arguments?.getInt("habitacionPaciente", -1) ?: -1
        val cama = arguments?.getInt("camaPaciente", -1) ?: -1

        paciente = Paciente(
            idPaciente = idPaciente.takeIf { it > 0 },
            nombre = nombre,
            apellido = apellido,
            habitacion = habitacion.takeIf { it >= 0 },
            cama = cama.takeIf { it >= 0 }
        )
    }



    private suspend fun cargarPacienteCompleto() {
        val id = paciente?.idPaciente ?: return

        try {
            val pacientes = com.example.molvigeryapp.data.api
                .RetrofitClient.apiService.getPacientes()

            if (_binding == null) return

            val pacienteApi = pacientes.firstOrNull {
                it.idPaciente == id
            }

            if (pacienteApi != null) {
                paciente = pacienteApi
                mostrarPaciente()
            } else {
                Log.w(TAG, "No se encontró el paciente $id en la API")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo cargar la fotografía del paciente", e)
        }
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

        // Cargar fotografía del paciente
        cargarFotoPaciente(pacienteSeleccionado)


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
        val contexto = context ?: return

        // PACIENTE
        val pacienteSeleccionado = paciente

        if (pacienteSeleccionado == null) {
            Toast.makeText(contexto, "No se encontró el paciente", Toast.LENGTH_SHORT).show()
            return
        }

        val idPaciente = pacienteSeleccionado.idPaciente

        if (idPaciente == null) {
            Toast.makeText(contexto, "El paciente no tiene un ID válido", Toast.LENGTH_SHORT).show()
            return
        }

        // TIPO DE CITA
        val tipoCita = binding.spinnerTipoCita.selectedItem
            ?.toString()?.trim().orEmpty()

        if (tipoCita.isBlank() || tipoCita == "Seleccionar tipo de cita") {
            Toast.makeText(contexto, "Selecciona el tipo de cita", Toast.LENGTH_SHORT).show()
            return
        }

        // ESPECIALIDAD
        val especialidad = binding.spinnerEspecialidad.selectedItem
            ?.toString()?.trim().orEmpty()

        if (especialidad.isBlank() || especialidad == "Seleccionar especialidad") {
            Toast.makeText(contexto, "Selecciona la especialidad", Toast.LENGTH_SHORT).show()
            return
        }

        // MOTIVO
        val motivo = binding.edtMotivoCita.text.toString().trim()

        if (motivo.isBlank()) {
            Toast.makeText(contexto, "Escribe el motivo de la cita", Toast.LENGTH_SHORT).show()
            binding.edtMotivoCita.requestFocus()
            return
        }

        // LUGAR
        val lugar = binding.edtLugarCita.text.toString().trim()

        if (lugar.isBlank()) {
            Toast.makeText(contexto, "Escribe el lugar de la cita", Toast.LENGTH_SHORT).show()
            binding.edtLugarCita.requestFocus()
            return
        }

        // FECHA
        val fecha = binding.txtFechaCita.text.toString().trim()

        if (fecha.isBlank() || fecha == "Seleccionar fecha") {
            Toast.makeText(contexto, "Selecciona una fecha", Toast.LENGTH_SHORT).show()
            return
        }

        // HORA
        val hora = binding.txtHoraCita.text.toString().trim()

        if (hora.isBlank() || hora == "Seleccionar hora") {
            Toast.makeText(contexto, "Selecciona una hora", Toast.LENGTH_SHORT).show()
            return
        }

        // OBSERVACIONES
        val observaciones = binding.edtObservacionesCita.text.toString().trim()

        // MOTIVO PARA LA API
        val motivoApi = "$tipoCita - $especialidad: $motivo"

        // FECHA PARA LA API
        val fechaApi = convertirFechaParaApi(fecha)

        // CONSERVAR LOS DATOS ORIGINALES AL EDITAR
        val citaAnterior = if (modoEdicion) citaOriginal else null

        if (modoEdicion && citaAnterior == null) {
            Toast.makeText(
                contexto,
                "No se cargaron los datos originales de la cita",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val identificador = if (modoEdicion) {
            idCitaEditar?.takeIf { it.isNotBlank() }
                ?: citaAnterior?.idCita
                ?: citaAnterior?.id
        } else {
            "CITA-${UUID.randomUUID()}"
        }

        if (identificador.isNullOrBlank()) {
            Toast.makeText(contexto, "No se encontró el identificador de la cita", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CrearCitaRequest(
            idCita = identificador,
            fecha = fechaApi,
            hora = hora,
            lugar = lugar,
            motivo = motivoApi,
            estado = citaAnterior?.estado ?: ESTADO_PROGRAMADA,
            observaciones = observaciones,
            fechaRegistro = citaAnterior?.fechaRegistro ?: obtenerFechaRegistro(),
            idPaciente = citaAnterior?.idPaciente ?: idPaciente,
            idUsuario = citaAnterior?.idUsuario
        )

        enviarCita(request)
    }



    // =========================================================
    // ENVIAR CITA A LA API
    // =========================================================


    private fun enviarCita(request: CrearCitaRequest) {
        mostrarCargando(
            if (modoEdicion) "Actualizando cita..." else "Guardando cita..."
        )

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resultado = if (modoEdicion) {
                    val id = idCitaEditar
                        ?.takeIf { it.isNotBlank() }
                        ?: citaOriginal?.idCita
                        ?: citaOriginal?.id
                        ?: throw IllegalStateException(
                            "No se encontró el identificador de la cita."
                        )

                    CitasRepository.actualizarCita(id, request)
                } else {
                    CitasRepository.crearCita(request)
                }

                if (!isAdded || _binding == null) {
                    return@launch
                }

                resultado
                    .onSuccess {
                        if (!isAdded || _binding == null) {
                            return@onSuccess
                        }

                        val contexto = context ?: return@onSuccess

                        val mensaje = if (modoEdicion) {
                            "Cita actualizada correctamente"
                        } else {
                            "Cita creada correctamente"
                        }

                        Toast.makeText(
                            contexto,
                            mensaje,
                            Toast.LENGTH_SHORT
                        ).show()

                        parentFragmentManager.popBackStack()
                    }
                    .onFailure { error ->
                        if (!isAdded || _binding == null) {
                            return@onFailure
                        }

                        Log.e(
                            TAG,
                            if (modoEdicion) {
                                "Error actualizando la cita"
                            } else {
                                "Error creando la cita"
                            },
                            error
                        )

                        mostrarFormulario()

                        val contexto = context ?: return@onFailure

                        val accion = if (modoEdicion) {
                            "actualizar"
                        } else {
                            "crear"
                        }

                        Toast.makeText(
                            contexto,
                            "No se pudo $accion la cita: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    if (modoEdicion) {
                        "Error inesperado actualizando la cita"
                    } else {
                        "Error inesperado creando la cita"
                    },
                    e
                )

                if (!isAdded || _binding == null) {
                    return@launch
                }

                mostrarFormulario()

                val contexto = context ?: return@launch

                Toast.makeText(
                    contexto,
                    if (modoEdicion) {
                        "No se pudo actualizar la cita."
                    } else {
                        "No se pudo crear la cita."
                    },
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