package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.ImagenEventoAdverso
import com.example.molvigeryapp.data.model.TipoEmergencia
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.FragmentEventosAdversosCierreTurnoBinding
import com.example.molvigeryapp.databinding.ItemEventoAdversoCierreBinding
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventosAdversosCierreTurnoFragment : Fragment(
    R.layout.fragment_eventos_adversos_cierre_turno
) {

    private var _binding: FragmentEventosAdversosCierreTurnoBinding? = null
    private val binding get() = _binding!!

    private var idAsignacionTurno: Int = -1

    companion object {
        private const val ARG_ID_ASIGNACION = "id_asignacion_turno_usuario"

        fun newInstance(idAsignacion: Int) =
            EventosAdversosCierreTurnoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_ASIGNACION, idAsignacion)
                }
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentEventosAdversosCierreTurnoBinding.bind(view)

        idAsignacionTurno =
            arguments?.getInt(ARG_ID_ASIGNACION, -1) ?: -1

        binding.btnVolverEventosCierre.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (idAsignacionTurno <= 0) {
            mostrarEstado("No se encontró la asignación del turno.")
            return
        }

        cargarEventosAdversos()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun cargarEventosAdversos() {
        viewLifecycleOwner.lifecycleScope.launch {

            binding.progresoEventosCierre.visibility = View.VISIBLE
            binding.tvEstadoEventosCierre.visibility = View.GONE
            binding.contenedorEventosCierre.removeAllViews()

            try {
                val api = RetrofitClient.apiService

                // 1. Buscar la asignación del turno.
                val asignaciones = api.getAsignacionesTurno()

                val asignacion = asignaciones.firstOrNull {
                    it.id_asignacion_turno_usuario == idAsignacionTurno
                }

                if (asignacion == null) {
                    mostrarEstado("No se encontró el turno seleccionado.")
                    return@launch
                }

                // 2. Validar el cuidador de la sesión.
                val preferencias = requireContext()
                    .getSharedPreferences(
                        "SESION",
                        android.content.Context.MODE_PRIVATE
                    )

                val idUsuarioSesion =
                    preferencias.getInt("ID_USUARIO", -1)

                val idCuidador = asignacion.id_usuario

                if (idUsuarioSesion <= 0 || idCuidador != idUsuarioSesion) {
                    mostrarEstado(
                        "No se pudo verificar el cuidador asignado a este turno."
                    )
                    return@launch
                }

                // 3. Obtener la fecha del turno.
                val fechaTurno = asignacion.fecha?.take(10)

                // 4. Consultar las bitácoras del cuidador.
                val bitacoras = api.getBitacoras().filter { bitacora ->
                    bitacora.idUsuario == idCuidador &&
                            fechaDe(bitacora.fechaHora) == fechaTurno
                }

                if (bitacoras.isEmpty()) {
                    mostrarEstado(
                        "No hay bitácoras registradas para este cuidador en la fecha del turno."
                    )
                    return@launch
                }

                // 5. Obtener los identificadores de las bitácoras.
                val idsBitacoras = bitacoras
                    .mapNotNull { it.idBitacora }
                    .toSet()

                // 6. Consultar los eventos asociados a esas bitácoras.
                val eventos = api.getEventosAdversos().filter { evento ->
                    evento.idBitacora != null &&
                            evento.idBitacora in idsBitacoras
                }

                if (eventos.isEmpty()) {
                    mostrarEstado(
                        "No se encontraron eventos adversos registrados para este turno."
                    )
                    return@launch
                }

                // 7. Consultar información complementaria.
                val tiposEmergencia = api.getTiposEmergencia()
                val imagenes = api.getImagenesEventoAdverso()
                val pacientes = api.getPacientes()

                // 8. Mostrar los eventos en las tarjetas XML.
                mostrarEventos(
                    eventos = eventos,
                    bitacoras = bitacoras,
                    tiposEmergencia = tiposEmergencia,
                    imagenes = imagenes,
                    pacientes = pacientes
                )

            } catch (e: Exception) {

                mostrarEstado(
                    "No fue posible cargar los eventos. Verifica tu conexión e inténtalo nuevamente."
                )

                Toast.makeText(
                    requireContext(),
                    "Error al consultar eventos adversos: ${e.message ?: "error desconocido"}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                if (_binding != null) {
                    binding.progresoEventosCierre.visibility = View.GONE
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun mostrarEventos(
        eventos: List<EventoAdverso>,
        bitacoras: List<Bitacora>,
        tiposEmergencia: List<TipoEmergencia>,
        imagenes: List<ImagenEventoAdverso>,
        pacientes: List<Paciente>
    ) {

        binding.contenedorEventosCierre.removeAllViews()
        binding.tvEstadoEventosCierre.visibility = View.GONE

        // Relacionar bitácoras por identificador.
        val bitacorasPorId = bitacoras
            .mapNotNull { bitacora ->
                bitacora.idBitacora?.let { it to bitacora }
            }
            .toMap()

        // Relacionar tipos de emergencia por identificador.
        val tiposPorId = tiposEmergencia
            .mapNotNull { tipo ->
                tipo.idTipoEmergencia?.let { it to tipo }
            }
            .toMap()

        // Relacionar pacientes por identificador.
        val pacientesPorId = pacientes
            .mapNotNull { paciente ->
                paciente.idPaciente?.let { it to paciente }
            }
            .toMap()

        eventos.forEach { evento ->

            // Buscar la bitácora relacionada.
            val bitacora = evento.idBitacora?.let {
                bitacorasPorId[it]
            }

            // Buscar el tipo de emergencia.
            val tipo = evento.idTipoEmergencia?.let {
                tiposPorId[it]
            }

            // Buscar el paciente.
            val paciente = bitacora?.idPaciente?.let {
                pacientesPorId[it]
            }

            val nombrePaciente = paciente?.let {
                "${it.nombre} ${it.apellido}".trim()
            }?.takeIf {
                it.isNotBlank()
            } ?: "No identificado"

            // Identificar el evento para buscar sus imágenes.
            val idEvento = evento.idEventoAdverso
                ?: evento.idEvento?.toIntOrNull()

            val imagenesEvento = imagenes.filter { imagen ->
                idEvento != null &&
                        imagen.idEventoAdverso == idEvento
            }

            // Inflar la tarjeta XML.
            val itemBinding = ItemEventoAdversoCierreBinding.inflate(
                layoutInflater,
                binding.contenedorEventosCierre,
                false
            )

            // Fecha del evento en horario colombiano.
            itemBinding.tvFechaEvento.text =
                "Fecha: ${formatearFechaColombia(evento.fechaHora)}"

            // Descripción del evento.
            itemBinding.tvDescripcionEvento.text =
                evento.descripcion.ifBlank {
                    "Sin descripción registrada."
                }

            // Tipo de emergencia.
            itemBinding.tvTipoEmergencia.text =
                tipo?.nombre ?: "Tipo no disponible"

            // Nivel de emergencia.
            if (tipo != null) {
                itemBinding.tvNivelEmergencia.visibility = View.VISIBLE
                itemBinding.tvNivelEmergencia.text =
                    "Nivel: ${tipo.nivel.ifBlank { "No especificado" }}"

                if (tipo.descripcion.isNotBlank()) {
                    itemBinding.tvDescripcionTipo.visibility = View.VISIBLE
                    itemBinding.tvDescripcionTipo.text = tipo.descripcion
                } else {
                    itemBinding.tvDescripcionTipo.visibility = View.GONE
                }
            } else {
                itemBinding.tvNivelEmergencia.visibility = View.GONE
                itemBinding.tvDescripcionTipo.visibility = View.GONE
            }

            // Acciones realizadas.
            if (evento.accionesRealizadas.isNotBlank()) {
                itemBinding.tvAccionesRealizadas.visibility = View.VISIBLE
                itemBinding.tvAccionesRealizadas.text =
                    "Acciones realizadas\n${evento.accionesRealizadas}"
            } else {
                itemBinding.tvAccionesRealizadas.visibility = View.GONE
            }

            // Nombre del paciente.
            itemBinding.tvPacienteEvento.text =
                "Paciente: $nombrePaciente"

            // Identificador de la bitácora.
            itemBinding.tvBitacoraEvento.text =
                "Bitácora: ${bitacora?.idBitacora ?: "No disponible"}"

            // Evidencias fotográficas.
            if (imagenesEvento.isNotEmpty()) {

                itemBinding.tvTituloEvidencias.visibility = View.VISIBLE
                itemBinding.tvTituloEvidencias.text =
                    "Evidencias fotográficas"

                itemBinding.contenedorEvidenciasEvento.visibility =
                    View.VISIBLE

                imagenesEvento.forEach { imagen ->

                    val boton = Button(requireContext()).apply {

                        text = if (imagen.descripcion.isNotBlank()) {
                            "Ver imagen: ${imagen.descripcion}"
                        } else {
                            "Ver imagen registrada"
                        }

                        isAllCaps = false

                        setOnClickListener {
                            abrirImagen(imagen.urlImagen)
                        }

                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            topMargin = 6
                        }
                    }

                    itemBinding.contenedorEvidenciasEvento.addView(boton)
                }

            } else {
                itemBinding.tvTituloEvidencias.visibility = View.VISIBLE
                itemBinding.tvTituloEvidencias.text =
                    "Sin imágenes adjuntas."

                itemBinding.contenedorEvidenciasEvento.visibility =
                    View.GONE
            }

            // Agregar la tarjeta al contenedor principal.
            binding.contenedorEventosCierre.addView(itemBinding.root)
        }
    }

    private fun abrirImagen(url: String) {

        if (url.isBlank()) {
            Toast.makeText(
                requireContext(),
                "La imagen no tiene una URL disponible.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "No se pudo abrir la imagen.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun mostrarEstado(mensaje: String) {
        if (_binding == null) return

        binding.progresoEventosCierre.visibility = View.GONE
        binding.tvEstadoEventosCierre.text = mensaje
        binding.tvEstadoEventosCierre.visibility = View.VISIBLE
    }

    // Obtener la fecha de la bitácora para compararla con la fecha del turno.
    private fun fechaDe(fechaHora: String): String {
        return fechaHora.take(10)
    }

    // Convertir la fecha UTC a la zona horaria de Colombia.
    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatearFechaColombia(fechaHora: String): String {

        if (fechaHora.isBlank()) return "No disponible"

        return try {

            val zonaColombia = ZoneId.of("America/Bogota")

            val fechaInstant = try {
                Instant.parse(fechaHora)
            } catch (e: Exception) {
                try {
                    OffsetDateTime.parse(fechaHora).toInstant()
                } catch (e: Exception) {
                    LocalDateTime.parse(fechaHora)
                        .toInstant(ZoneOffset.UTC)
                }
            }

            val formato = DateTimeFormatter.ofPattern(
                "dd/MM/yyyy · hh:mm a",
                Locale("es", "CO")
            )

            fechaInstant
                .atZone(zonaColombia)
                .format(formato)

        } catch (e: Exception) {
            "Fecha no disponible"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}