package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.FragmentCitasCierreTurnoBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class CitasCierreTurnoFragment : Fragment() {

    private var _binding: FragmentCitasCierreTurnoBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_ID_ASIGNACION =
            "id_asignacion_turno_usuario"

        private const val TAG = "DEBUG_CITAS"

        fun newInstance(idAsignacion: Int) =
            CitasCierreTurnoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_ASIGNACION, idAsignacion)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCitasCierreTurnoBinding.inflate(
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

        binding.listaCitasCierre.layoutManager =
            LinearLayoutManager(requireContext())

        binding.btnVolverCitasCierre.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cargarCitas()
    }

    private fun cargarCitas() {
        val idAsignacion =
            arguments?.getInt(ARG_ID_ASIGNACION, -1) ?: -1

        if (idAsignacion <= 0) {
            mostrarError("No se recibió la asignación del turno.")
            return
        }

        binding.progresoCitasCierre.visibility = View.VISIBLE
        binding.tvSinCitasCierre.visibility = View.GONE
        binding.listaCitasCierre.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.apiService

                // 1. Obtener la asignación del turno
                val asignacionesTurno = api.getAsignacionesTurno()

                val asignacionTurno = asignacionesTurno.firstOrNull {
                    it.id_asignacion_turno_usuario == idAsignacion
                }

                if (asignacionTurno == null) {
                    mostrarError("No se encontró la asignación del turno.")
                    return@launch
                }

                val fechaTurno =
                    asignacionTurno.fecha?.take(10).orEmpty()

                val idCuidador = asignacionTurno.id_usuario

                Log.d(TAG, "ID asignación: $idAsignacion")
                Log.d(TAG, "ID cuidador: $idCuidador")
                Log.d(TAG, "Fecha del turno: $fechaTurno")

                if (fechaTurno.isBlank()) {
                    mostrarError("El turno no tiene una fecha válida.")
                    return@launch
                }

                binding.tvFechaCitasCierre.text =
                    "Fecha: ${formatearFecha(fechaTurno)}"

                // 2. Obtener las asignaciones paciente-cuidador
                val respuestaAsignaciones =
                    api.getAsignacionesPacienteCuidador()

                if (!respuestaAsignaciones.isSuccessful) {
                    mostrarError("No se pudieron consultar los pacientes asignados.")
                    return@launch
                }

                val todasLasAsignaciones =
                    respuestaAsignaciones.body().orEmpty()

                // 3. Filtrar pacientes asignados activos al cuidador
                // Se conserva temporalmente el filtro probado,
                // sin fecha_fin, por la inconsistencia encontrada.
                val pacientesAsignados = todasLasAsignaciones
                    .filter { asignacion ->
                        asignacion.idUsuario == idCuidador &&
                                asignacion.estado.equals(
                                    "Activo",
                                    ignoreCase = true
                                )
                    }
                    .map { it.idPaciente }
                    .toSet()

                Log.d(
                    TAG,
                    "Pacientes activos del cuidador: $pacientesAsignados"
                )

                // 4. Consultar los pacientes para obtener sus nombres
                val listaPacientes: List<Paciente> = api.getPacientes()

                val nombresPacientes = listaPacientes
                    .filter { it.idPaciente != null }
                    .associate { paciente ->
                        val nombreCompleto = listOf(
                            paciente.nombre.trim(),
                            paciente.apellido.trim()
                        )
                            .filter { it.isNotBlank() }
                            .joinToString(" ")

                        itIdPaciente(paciente) to nombreCompleto
                    }

                Log.d(
                    TAG,
                    "Nombres de pacientes cargados: ${nombresPacientes.size}"
                )

                // 5. Consultar las citas
                val respuestaCitas = api.getCitas()

                if (!respuestaCitas.isSuccessful) {
                    mostrarError("No se pudieron consultar las citas.")
                    return@launch
                }

                val todasLasCitas = respuestaCitas.body().orEmpty()

                Log.d(
                    TAG,
                    "Total de citas recibidas: ${todasLasCitas.size}"
                )

                // 6. Filtrar por paciente activo y fecha del turno
                val citas = todasLasCitas
                    .filter { cita ->
                        val pacienteAsignado =
                            cita.idPaciente != null &&
                                    cita.idPaciente in pacientesAsignados

                        val fechaCoincide =
                            cita.fecha.take(10) == fechaTurno

                        pacienteAsignado && fechaCoincide
                    }
                    .sortedWith(
                        compareBy<Cita> { it.fecha }
                            .thenBy { it.hora }
                    )

                Log.d(TAG, "Citas después del filtro: ${citas.size}")

                // 7. Agregar el nombre obtenido del endpoint de pacientes
                val citasConNombre = citas.map { cita ->
                    val nombreDesdePacientes =
                        cita.idPaciente?.let { nombresPacientes[it] }
                            ?.takeIf { it.isNotBlank() }

                    val nombreDesdeCita =
                        cita.nombrePaciente
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }

                    cita.copy(
                        nombrePaciente =
                            nombreDesdePacientes ?: nombreDesdeCita
                    )
                }

                // 8. Contadores
                val finalizadas = citasConNombre.count {
                    it.estado.equals("Finalizada", ignoreCase = true)
                }

                val programadas = citasConNombre.count {
                    it.estado.equals("Programada", ignoreCase = true)
                }

                binding.tvTotalCitasCierre.text =
                    "${citasConNombre.size} citas · " +
                            "$programadas programadas · " +
                            "$finalizadas finalizadas"

                // 9. Mostrar tarjetas
                binding.listaCitasCierre.adapter =
                    CitasAdapter(citasConNombre)

                if (citasConNombre.isEmpty()) {
                    binding.tvSinCitasCierre.text =
                        "No hay citas para los pacientes asignados en esta fecha."

                    binding.tvSinCitasCierre.visibility = View.VISIBLE
                    binding.listaCitasCierre.visibility = View.GONE
                } else {
                    binding.tvSinCitasCierre.visibility = View.GONE
                    binding.listaCitasCierre.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error consultando las citas", e)

                if (isAdded) {
                    mostrarError("No se pudieron cargar las citas.")
                }
            } finally {
                _binding?.progresoCitasCierre?.visibility = View.GONE
            }
        }
    }

    // Devuelve el ID del paciente para construir el mapa de nombres.
    private fun itIdPaciente(paciente: Paciente): Int {
        return paciente.idPaciente
            ?: throw IllegalStateException("Paciente sin ID")
    }

    private fun mostrarError(mensaje: String) {
        val b = _binding ?: return

        b.tvTotalCitasCierre.text = mensaje
        b.tvSinCitasCierre.text = mensaje
        b.tvSinCitasCierre.visibility = View.VISIBLE
        b.listaCitasCierre.visibility = View.GONE

        Toast.makeText(
            requireContext(),
            mensaje,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val entrada = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

            val salida = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            val date = entrada.parse(fecha)

            if (date != null) salida.format(date) else fecha
        } catch (_: Exception) {
            fecha
        }
    }

    private class CitasAdapter(
        private val citas: List<Cita>
    ) : RecyclerView.Adapter<CitasAdapter.CitaViewHolder>() {

        class CitaViewHolder(
            val contenedor: LinearLayout
        ) : RecyclerView.ViewHolder(contenedor)

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): CitaViewHolder {
            val tarjeta = LinearLayout(parent.context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(18, 18, 18, 18)
                setBackgroundColor(Color.WHITE)
                elevation = 4f

                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }
            }

            return CitaViewHolder(tarjeta)
        }

        override fun onBindViewHolder(
            holder: CitaViewHolder,
            position: Int
        ) {
            val cita = citas[position]
            val contenedor = holder.contenedor

            contenedor.removeAllViews()

            val nombrePaciente = cita.nombrePaciente
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "Nombre no disponible"

            agregarTexto(
                contenedor,
                nombrePaciente,
                17f,
                "#25324B",
                true
            )

            agregarTexto(
                contenedor,
                "Especialidad: ${cita.obtenerEspecialidad()}",
                14f,
                "#475467"
            )

            agregarTexto(
                contenedor,
                "Fecha: ${cita.fecha.take(10)}",
                14f,
                "#475467"
            )

            agregarTexto(
                contenedor,
                "Hora: ${cita.hora.ifBlank { "No disponible" }}",
                14f,
                "#475467"
            )

            agregarTexto(
                contenedor,
                "Lugar: ${cita.lugar ?: "No especificado"}",
                14f,
                "#475467"
            )

            agregarTexto(
                contenedor,
                "Estado: ${cita.estado.ifBlank { "No especificado" }}",
                14f,
                when {
                    cita.estado.equals("Finalizada", true) ->
                        "#218838"

                    cita.estado.equals("Programada", true) ->
                        "#3B5DBB"

                    else -> "#667085"
                },
                true
            )

            if (!cita.motivo.isNullOrBlank()) {
                agregarTexto(
                    contenedor,
                    "Motivo: ${cita.motivo}",
                    14f,
                    "#475467"
                )
            }

            if (!cita.observaciones.isNullOrBlank()) {
                agregarTexto(
                    contenedor,
                    "Observaciones: ${cita.observaciones}",
                    14f,
                    "#475467"
                )
            }
        }

        private fun agregarTexto(
            contenedor: LinearLayout,
            texto: String,
            tamano: Float,
            color: String,
            negrita: Boolean = false
        ) {
            val vista = TextView(contenedor.context).apply {
                text = texto
                textSize = tamano
                setTextColor(Color.parseColor(color))
                gravity = Gravity.START

                if (negrita) {
                    setTypeface(null, Typeface.BOLD)
                }

                setPadding(0, 4, 0, 4)
            }

            contenedor.addView(vista)
        }

        override fun getItemCount(): Int = citas.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}