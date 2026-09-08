package com.example.molvigeryapp.ui.encargado.cuidadores

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.databinding.FragmentTurnosCuidadorEncargadoBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TurnosCuidadorEncargadoFragment : Fragment() {

    private var _binding: FragmentTurnosCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: TurnoAdapter

    private val listaTurnos = mutableListOf<Turno>()

    private var tipoTurno = "Diurno"

    private var fechaInicio: Calendar? = null
    private var fechaFin: Calendar? = null

    // ID del turno que estamos editando.
    // Si es null, estamos creando un turno nuevo.
    private var turnoEditandoId: Int? = null

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
        super.onViewCreated(view, savedInstanceState)

        cargarDatosCuidador()

        configurarRecyclerView()

        configurarSeleccionTurno()

        configurarFechas()

        configurarBotonAsignar()

        configurarBotonVolver()
    }

    // =========================================================
    // DATOS DEL CUIDADOR
    // =========================================================

    private fun cargarDatosCuidador() {

        val nombre =
            arguments?.getString("nombre")
                ?: "Cuidador"

        val cargo =
            arguments?.getString("cargo")
                ?: "Cuidador"

        binding.txtNombreCuidadorTurno.text =
            nombre

        binding.txtCargoCuidadorTurno.text =
            cargo
    }

    // =========================================================
    // RECYCLERVIEW
    // =========================================================

    private fun configurarRecyclerView() {

        binding.recyclerTurnos.layoutManager =
            LinearLayoutManager(requireContext())

        adapter = TurnoAdapter(
            listaTurnos,

            onEditar = { turno ->
                editarTurno(turno)
            },

            onEliminar = { turno ->
                eliminarTurno(turno)
            }
        )

        binding.recyclerTurnos.adapter = adapter

        actualizarVisibilidadLista()
    }

    // =========================================================
    // SELECCIÓN DIURNO / NOCTURNO
    // =========================================================

    private fun configurarSeleccionTurno() {

        seleccionarTurnoDiurno()

        binding.cardTurnoDiurno.setOnClickListener {

            seleccionarTurnoDiurno()
        }

        binding.cardTurnoNocturno.setOnClickListener {

            seleccionarTurnoNocturno()
        }
    }

    private fun seleccionarTurnoDiurno() {

        tipoTurno = "Diurno"

        binding.cardTurnoDiurno.setBackgroundResource(
            R.drawable.bg_turno_seleccionado
        )

        binding.cardTurnoNocturno.setBackgroundResource(
            R.drawable.bg_turno_no_seleccionado
        )

        binding.iconoDiurnoSeleccion.setImageResource(
            R.drawable.check_circle
        )

        binding.iconoNocturnoSeleccion.setImageResource(
            R.drawable.radio_button_u
        )

        binding.iconoDiurnoSeleccion.setColorFilter(
            Color.parseColor("#3B5BDB")
        )

        binding.iconoNocturnoSeleccion.setColorFilter(
            Color.parseColor("#98A2B3")
        )

        actualizarResumen()
    }

    private fun seleccionarTurnoNocturno() {

        tipoTurno = "Nocturno"

        binding.cardTurnoDiurno.setBackgroundResource(
            R.drawable.bg_turno_no_seleccionado
        )

        binding.cardTurnoNocturno.setBackgroundResource(
            R.drawable.bg_turno_seleccionado
        )

        binding.iconoDiurnoSeleccion.setImageResource(
            R.drawable.radio_button_u
        )

        binding.iconoNocturnoSeleccion.setImageResource(
            R.drawable.check_circle
        )

        binding.iconoDiurnoSeleccion.setColorFilter(
            Color.parseColor("#98A2B3")
        )

        binding.iconoNocturnoSeleccion.setColorFilter(
            Color.parseColor("#3B5BDB")
        )

        actualizarResumen()
    }

    // =========================================================
    // FECHAS
    // =========================================================

    private fun configurarFechas() {

        binding.btnFechaInicio.setOnClickListener {

            mostrarSelectorFecha(true)
        }

        binding.btnFechaFin.setOnClickListener {

            mostrarSelectorFecha(false)
        }
    }

    private fun mostrarSelectorFecha(
        esFechaInicio: Boolean
    ) {

        val calendario =
            Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->

                val fechaSeleccionada =
                    Calendar.getInstance()

                fechaSeleccionada.set(
                    year,
                    month,
                    dayOfMonth,
                    0,
                    0,
                    0
                )

                fechaSeleccionada.set(
                    Calendar.MILLISECOND,
                    0
                )

                if (esFechaInicio) {

                    fechaInicio =
                        fechaSeleccionada

                    binding.txtFechaInicio.text =
                        formatearFecha(
                            fechaSeleccionada
                        )

                } else {

                    fechaFin =
                        fechaSeleccionada

                    binding.txtFechaFin.text =
                        formatearFecha(
                            fechaSeleccionada
                        )
                }

                calcularDuracion()

            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // =========================================================
    // DURACIÓN
    // =========================================================

    private fun calcularDuracion() {

        val inicio = fechaInicio
        val fin = fechaFin

        if (inicio == null || fin == null) {

            binding.txtDuracion.text =
                "Selecciona las fechas"

            actualizarResumen()

            return
        }

        if (fin.before(inicio)) {

            binding.txtDuracion.text =
                "La fecha final debe ser posterior"

            actualizarResumen()

            return
        }

        val diferencia =
            fin.timeInMillis -
                    inicio.timeInMillis

        val dias =
            (diferencia /
                    (1000L * 60L * 60L * 24L)) + 1L

        binding.txtDuracion.text =
            if (dias == 1L) {
                "1 día"
            } else {
                "$dias días"
            }

        actualizarResumen()
    }

    // =========================================================
    // RESUMEN
    // =========================================================

    private fun actualizarResumen() {

        val inicio = fechaInicio
        val fin = fechaFin

        if (inicio == null || fin == null) {

            binding.txtResumenTurno.text =
                "Selecciona el tipo de turno y las fechas para ver el resumen."

            return
        }

        if (fin.before(inicio)) {

            binding.txtResumenTurno.text =
                "La fecha final no puede ser anterior a la fecha inicial."

            return
        }

        val fechaInicioTexto =
            formatearFecha(inicio)

        val fechaFinTexto =
            formatearFecha(fin)

        val horario =
            if (tipoTurno == "Diurno") {

                "7:00 AM - 7:00 PM"

            } else {

                "7:00 PM - 7:00 AM del día siguiente"
            }

        binding.txtResumenTurno.text =
            "Turno $tipoTurno\n" +
                    "$fechaInicioTexto → $fechaFinTexto\n" +
                    horario
    }

    // =========================================================
    // BOTÓN ASIGNAR / ACTUALIZAR
    // =========================================================

    private fun configurarBotonAsignar() {

        binding.btnAsignarTurno.setOnClickListener {

            if (turnoEditandoId == null) {

                asignarTurno()

            } else {

                actualizarTurno()
            }
        }
    }

    // =========================================================
    // ASIGNAR NUEVO TURNO
    // =========================================================

    private fun asignarTurno() {

        val inicio = fechaInicio
        val fin = fechaFin

        if (inicio == null || fin == null) {

            binding.txtResumenTurno.text =
                "Selecciona las fechas antes de asignar el turno."

            return
        }

        if (fin.before(inicio)) {

            binding.txtResumenTurno.text =
                "La fecha final no puede ser anterior a la fecha inicial."

            return
        }

        val horarioInicio: String
        val horarioFin: String

        if (tipoTurno == "Diurno") {

            horarioInicio = "7:00 AM"
            horarioFin = "7:00 PM"

        } else {

            horarioInicio = "7:00 PM"
            horarioFin = "7:00 AM"
        }

        val dias =
            calcularDias(
                inicio,
                fin
            )

        val duracion =
            formatearDuracion(dias)

        val nuevoTurno =
            Turno(
                id = obtenerSiguienteId(),
                tipo = tipoTurno,
                fechaInicio = formatearFecha(inicio),
                fechaFin = formatearFecha(fin),
                horaInicio = horarioInicio,
                horaFin = horarioFin,
                duracion = duracion,
                estado = "Asignado"
            )

        listaTurnos.add(nuevoTurno)

        adapter.actualizarLista(
            listaTurnos
        )

        actualizarVisibilidadLista()

        limpiarFormulario()
    }

    // =========================================================
    // EDITAR TURNO
    // =========================================================

    private fun editarTurno(
        turno: Turno
    ) {

        turnoEditandoId =
            turno.id

        // Seleccionar tipo
        if (turno.tipo == "Diurno") {

            seleccionarTurnoDiurno()

        } else {

            seleccionarTurnoNocturno()
        }

        // Recuperar fecha inicial
        fechaInicio =
            convertirFechaACalendar(
                turno.fechaInicio
            )

        // Recuperar fecha final
        fechaFin =
            convertirFechaACalendar(
                turno.fechaFin
            )

        // Mostrar fechas
        binding.txtFechaInicio.text =
            turno.fechaInicio

        binding.txtFechaFin.text =
            turno.fechaFin

        // Actualizar duración
        calcularDuracion()

        // Actualizar resumen
        actualizarResumen()

        // Cambiar botón
        binding.btnAsignarTurno.text =
            "Actualizar turno"

        // Llevar el formulario hacia arriba
        binding.scrollTurnos.smoothScrollTo(
            0,
            0
        )
    }

    // =========================================================
    // ACTUALIZAR TURNO
    // =========================================================

    private fun actualizarTurno() {

        val id =
            turnoEditandoId

        val inicio =
            fechaInicio

        val fin =
            fechaFin

        if (id == null) {
            return
        }

        if (inicio == null || fin == null) {

            binding.txtResumenTurno.text =
                "Selecciona las fechas antes de actualizar el turno."

            return
        }

        if (fin.before(inicio)) {

            binding.txtResumenTurno.text =
                "La fecha final no puede ser anterior a la fecha inicial."

            return
        }

        val horarioInicio: String
        val horarioFin: String

        if (tipoTurno == "Diurno") {

            horarioInicio = "7:00 AM"
            horarioFin = "7:00 PM"

        } else {

            horarioInicio = "7:00 PM"
            horarioFin = "7:00 AM"
        }

        val dias =
            calcularDias(
                inicio,
                fin
            )

        val duracion =
            formatearDuracion(dias)

        val posicion =
            listaTurnos.indexOfFirst {
                it.id == id
            }

        if (posicion == -1) {
            return
        }

        val turnoActualizado =
            Turno(
                id = id,
                tipo = tipoTurno,
                fechaInicio = formatearFecha(inicio),
                fechaFin = formatearFecha(fin),
                horaInicio = horarioInicio,
                horaFin = horarioFin,
                duracion = duracion,
                estado = listaTurnos[posicion].estado
            )

        listaTurnos[posicion] =
            turnoActualizado

        adapter.actualizarLista(
            listaTurnos
        )

        actualizarVisibilidadLista()

        limpiarFormulario()
    }

    // =========================================================
    // ELIMINAR TURNO
    // =========================================================

    private fun eliminarTurno(
        turno: Turno
    ) {

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Eliminar turno")
            .setMessage(
                "¿Deseas eliminar el turno ${turno.tipo} " +
                        "del ${turno.fechaInicio} al ${turno.fechaFin}?"
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Eliminar"
            ) { _, _ ->

                listaTurnos.remove(turno)

                adapter.actualizarLista(
                    listaTurnos
                )

                actualizarVisibilidadLista()

                // Si estábamos editando este turno,
                // cancelar el modo edición.
                if (turnoEditandoId == turno.id) {

                    limpiarFormulario()
                }
            }
            .show()
    }

    // =========================================================
    // OBTENER SIGUIENTE ID
    // =========================================================

    private fun obtenerSiguienteId(): Int {

        if (listaTurnos.isEmpty()) {
            return 1
        }

        return listaTurnos.maxOf {
            it.id
        } + 1
    }

    // =========================================================
    // CALCULAR DÍAS
    // =========================================================

    private fun calcularDias(
        inicio: Calendar,
        fin: Calendar
    ): Long {

        val diferencia =
            fin.timeInMillis -
                    inicio.timeInMillis

        return (
                diferencia /
                        (1000L * 60L * 60L * 24L)
                ) + 1L
    }

    // =========================================================
    // FORMATEAR DURACIÓN
    // =========================================================

    private fun formatearDuracion(
        dias: Long
    ): String {

        return if (dias == 1L) {
            "1 día"
        } else {
            "$dias días"
        }
    }

    // =========================================================
    // CONVERTIR FECHA A CALENDAR
    // =========================================================

    private fun convertirFechaACalendar(
        fecha: String
    ): Calendar? {

        return try {

            val partes =
                fecha.split("/")

            if (partes.size != 3) {
                return null
            }

            val dia =
                partes[0].toInt()

            val mes =
                partes[1].toInt() - 1

            val anio =
                partes[2].toInt()

            Calendar.getInstance().apply {

                set(
                    anio,
                    mes,
                    dia,
                    0,
                    0,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        } catch (e: Exception) {

            null
        }
    }

    // =========================================================
    // MOSTRAR / OCULTAR LISTA
    // =========================================================

    private fun actualizarVisibilidadLista() {

        if (listaTurnos.isEmpty()) {

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
    // LIMPIAR FORMULARIO
    // =========================================================

    private fun limpiarFormulario() {

        fechaInicio = null
        fechaFin = null

        turnoEditandoId = null

        binding.txtFechaInicio.text =
            "Seleccionar fecha"

        binding.txtFechaFin.text =
            "Seleccionar fecha"

        binding.txtDuracion.text =
            "Selecciona las fechas"

        binding.txtResumenTurno.text =
            "Selecciona el tipo de turno y las fechas para ver el resumen."

        // Volver al modo de creación
        binding.btnAsignarTurno.text =
            "Asignar turno"

        // Volver a seleccionar Diurno
        seleccionarTurnoDiurno()
    }

    // =========================================================
    // VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding.btnVolverTurnos.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    // =========================================================
    // FORMATO DE FECHA
    // =========================================================

    private fun formatearFecha(
        calendario: Calendar
    ): String {

        val formato =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

        return formato.format(
            calendario.time
        )
    }

    // =========================================================
    // DESTRUIR BINDING
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}