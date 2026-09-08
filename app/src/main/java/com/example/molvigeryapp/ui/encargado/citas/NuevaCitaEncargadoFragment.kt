package com.example.molvigeryapp.ui.encargado.citas

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.CitasRepository
import com.example.molvigeryapp.databinding.FragmentNuevaCitaEncargadoBinding
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment
import java.util.Calendar


class NuevaCitaEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentNuevaCitaEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =====================================================
    // PACIENTE SELECCIONADO
    // =====================================================

    private var paciente: Paciente? = null


    // =====================================================
    // CREAR VISTA
    // =====================================================

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


    // =====================================================
    // VISTA CREADA
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        recibirPaciente()

        mostrarPaciente()

        configurarTipoCita()

        configurarEspecialidad()

        configurarBotonVolver()

        configurarFecha()

        configurarHora()

        configurarNotificaciones()

        configurarBotonGuardar()
    }


    // =====================================================
    // RECIBIR PACIENTE
    // =====================================================

    private fun recibirPaciente() {

        val idPaciente =
            arguments?.getInt(
                "idPaciente",
                -1
            )

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
            )

        val cama =
            arguments?.getInt(
                "camaPaciente",
                -1
            )


        paciente = Paciente(

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


    // =====================================================
    // MOSTRAR PACIENTE
    // =====================================================

    private fun mostrarPaciente() {

        val pacienteSeleccionado =
            paciente ?: return


        binding.txtPacienteCita.text =
            "${pacienteSeleccionado.nombre} ${pacienteSeleccionado.apellido}"


        binding.txtHabitacionCita.text =
            "Habitación ${
                pacienteSeleccionado.habitacion ?: "N/A"
            } · Cama ${
                pacienteSeleccionado.cama ?: "N/A"
            }"
    }


    // =====================================================
    // TIPO DE CITA
    // =====================================================

    private fun configurarTipoCita() {

        val tipos = listOf(

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
                requireContext(),
                android.R.layout.simple_spinner_item,
                tipos
            )


        adapterSpinner.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        binding.spinnerTipoCita.adapter =
            adapterSpinner
    }


    // =====================================================
    // ESPECIALIDAD
    // =====================================================

    private fun configurarEspecialidad() {

        val especialidades = listOf(

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
                requireContext(),
                android.R.layout.simple_spinner_item,
                especialidades
            )


        adapterSpinner.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        binding.spinnerEspecialidad.adapter =
            adapterSpinner
    }


    // =====================================================
    // BOTÓN VOLVER
    // =====================================================

    private fun configurarBotonVolver() {

        binding.btnVolverNuevaCita.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }


    // =====================================================
    // SELECCIONAR FECHA
    // =====================================================

    private fun configurarFecha() {

        binding.containerFechaCita.setOnClickListener {

            val calendario =
                Calendar.getInstance()


            val year =
                calendario.get(Calendar.YEAR)

            val month =
                calendario.get(Calendar.MONTH)

            val day =
                calendario.get(Calendar.DAY_OF_MONTH)


            val datePicker =
                DatePickerDialog(

                    requireContext(),

                    { _, selectedYear, selectedMonth, selectedDay ->

                        val fecha =
                            String.format(
                                "%02d/%02d/%04d",
                                selectedDay,
                                selectedMonth + 1,
                                selectedYear
                            )

                        binding.txtFechaCita.text =
                            fecha
                    },

                    year,
                    month,
                    day
                )


            datePicker.show()
        }
    }


    // =====================================================
    // SELECCIONAR HORA
    // =====================================================

    private fun configurarHora() {

        binding.containerHoraCita.setOnClickListener {

            val calendario =
                Calendar.getInstance()


            val hour =
                calendario.get(Calendar.HOUR_OF_DAY)

            val minute =
                calendario.get(Calendar.MINUTE)


            val timePicker =
                TimePickerDialog(

                    requireContext(),

                    { _, selectedHour, selectedMinute ->

                        val hora =
                            String.format(
                                "%02d:%02d",
                                selectedHour,
                                selectedMinute
                            )

                        binding.txtHoraCita.text =
                            hora
                    },

                    hour,
                    minute,
                    true
                )


            timePicker.show()
        }
    }


    // =====================================================
    // NOTIFICACIONES
    // =====================================================

    private fun configurarNotificaciones() {

        binding.btnNotificacionesNuevaCita.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    NotificacionesEncargadoFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }


    // =====================================================
    // GUARDAR CITA
    // =====================================================

    private fun configurarBotonGuardar() {

        binding.btnGuardarCita.setOnClickListener {

            guardarCita()
        }
    }


    // =====================================================
    // GUARDAR CITA
    // =====================================================

    private fun guardarCita() {

        val pacienteSeleccionado =
            paciente


        if (pacienteSeleccionado == null) {

            Toast.makeText(
                requireContext(),
                "No se encontró el paciente",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =================================================
        // TIPO
        // =================================================

        val tipoCita =
            binding.spinnerTipoCita
                .selectedItem
                ?.toString()
                ?: ""


        if (
            tipoCita.isEmpty() ||
            tipoCita == "Seleccionar tipo de cita"
        ) {

            Toast.makeText(
                requireContext(),
                "Selecciona el tipo de cita",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =================================================
        // ESPECIALIDAD
        // =================================================

        val especialidad =
            binding.spinnerEspecialidad
                .selectedItem
                ?.toString()
                ?: ""


        if (
            especialidad.isEmpty() ||
            especialidad == "Seleccionar especialidad"
        ) {

            Toast.makeText(
                requireContext(),
                "Selecciona la especialidad",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =================================================
        // FECHA
        // =================================================

        val fecha =
            binding.txtFechaCita.text
                .toString()


        if (
            fecha.isEmpty() ||
            fecha == "Seleccionar fecha"
        ) {

            Toast.makeText(
                requireContext(),
                "Selecciona una fecha",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =================================================
        // HORA
        // =================================================

        val hora =
            binding.txtHoraCita.text
                .toString()


        if (
            hora.isEmpty() ||
            hora == "Seleccionar hora"
        ) {

            Toast.makeText(
                requireContext(),
                "Selecciona una hora",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =================================================
        // OBSERVACIONES
        // =================================================

        val observaciones =
            binding.edtObservacionesCita.text
                .toString()
                .trim()


        // =================================================
        // CREAR CITA
        // =================================================

        val cita =
            Cita(

                id = 0,

                idPaciente =
                    pacienteSeleccionado.idPaciente,

                nombrePaciente =
                    "${pacienteSeleccionado.nombre} ${pacienteSeleccionado.apellido}",

                habitacion =
                    pacienteSeleccionado.habitacion,

                cama =
                    pacienteSeleccionado.cama,

                tipoCita =
                    tipoCita,

                especialidad =
                    especialidad,

                fecha =
                    fecha,

                hora =
                    hora,

                observaciones =
                    observaciones,

                estado =
                    "PROGRAMADA"
            )


        // =================================================
        // GUARDAR TEMPORALMENTE
        // =================================================

        CitasRepository.agregarCita(
            cita
        )


        Toast.makeText(
            requireContext(),
            "Cita creada correctamente",
            Toast.LENGTH_SHORT
        ).show()


        // =================================================
        // VOLVER A CITAS
        // =================================================

        parentFragmentManager.popBackStack()
    }


    // =====================================================
    // DESTRUIR BINDING
    // =====================================================

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}