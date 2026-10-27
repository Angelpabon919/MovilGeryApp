package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.ImagenEventoAdverso
import com.example.molvigeryapp.data.model.TipoEmergencia
import com.example.molvigeryapp.databinding.FragmentEventosAdversosCierreTurnoBinding
import kotlinx.coroutines.launch

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

    private fun cargarEventosAdversos() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progresoEventosCierre.visibility = View.VISIBLE
            binding.tvEstadoEventosCierre.visibility = View.GONE
            binding.contenedorEventosCierre.removeAllViews()

            try {
                val api = RetrofitClient.apiService

                // Buscar la asignación del turno seleccionado.
                val asignaciones = api.getAsignacionesTurno()

                val asignacion = asignaciones.firstOrNull {
                    it.id_asignacion_turno_usuario == idAsignacionTurno
                }

                if (asignacion == null) {
                    mostrarEstado("No se encontró el turno seleccionado.")
                    return@launch
                }

                val preferencias = requireContext()
                    .getSharedPreferences("SESION", android.content.Context.MODE_PRIVATE)

                val idUsuarioSesion =
                    preferencias.getInt("ID_USUARIO", -1)

                val idCuidador = asignacion.id_usuario

                if (idUsuarioSesion <= 0 || idCuidador != idUsuarioSesion) {
                    mostrarEstado(
                        "No se pudo verificar el cuidador asignado a este turno."
                    )
                    return@launch
                }

                val fechaTurno = asignacion.fecha?.take(10)

                // Consultar las bitácoras del cuidador para la fecha del turno.
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

                val idsBitacoras = bitacoras
                    .mapNotNull { it.idBitacora }
                    .toSet()

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

                val tiposEmergencia = api.getTiposEmergencia()
                val imagenes = api.getImagenesEventoAdverso()

                mostrarEventos(
                    eventos = eventos,
                    bitacoras = bitacoras,
                    tiposEmergencia = tiposEmergencia,
                    imagenes = imagenes
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

    private fun mostrarEventos(
        eventos: List<EventoAdverso>,
        bitacoras: List<Bitacora>,
        tiposEmergencia: List<TipoEmergencia>,
        imagenes: List<ImagenEventoAdverso>
    ) {
        binding.contenedorEventosCierre.removeAllViews()
        binding.tvEstadoEventosCierre.visibility = View.GONE

        val bitacorasPorId = bitacoras
            .mapNotNull { bitacora ->
                bitacora.idBitacora?.let { it to bitacora }
            }
            .toMap()

        val tiposPorId = tiposEmergencia
            .mapNotNull { tipo ->
                tipo.idTipoEmergencia?.let { it to tipo }
            }
            .toMap()

        eventos.forEach { evento ->
            val bitacora = evento.idBitacora?.let {
                bitacorasPorId[it]
            }

            val tipo = evento.idTipoEmergencia?.let {
                tiposPorId[it]
            }

            val idEvento = evento.idEventoAdverso
                ?: evento.idEvento?.toIntOrNull()

            val imagenesEvento = imagenes.filter { imagen ->
                idEvento != null &&
                        imagen.idEventoAdverso == idEvento
            }

            val tarjeta = crearTarjeta()
            tarjeta.addView(
                crearTexto(
                    "Evento adverso",
                    18f,
                    "#2459C4",
                    negrita = true
                )
            )

            tarjeta.addView(
                crearTexto(
                    "Fecha: ${evento.fechaHora.ifBlank { "No disponible" }}",
                    13f,
                    "#64748B"
                )
            )

            tarjeta.addView(espacio(10))

            tarjeta.addView(
                crearTexto("Descripción", 14f, "#172B4D", true)
            )
            tarjeta.addView(
                crearTexto(
                    evento.descripcion.ifBlank { "Sin descripción registrada." },
                    15f,
                    "#334155"
                )
            )

            tarjeta.addView(espacio(10))

            tarjeta.addView(
                crearTexto("Tipo de emergencia", 14f, "#172B4D", true)
            )
            tarjeta.addView(
                crearTexto(
                    tipo?.nombre ?: "Tipo no disponible",
                    15f,
                    "#334155",
                    negrita = true
                )
            )

            if (tipo != null) {
                tarjeta.addView(
                    crearTexto(
                        "Nivel: ${tipo.nivel.ifBlank { "No especificado" }}",
                        14f,
                        "#B45309",
                        true
                    )
                )

                if (tipo.descripcion.isNotBlank()) {
                    tarjeta.addView(
                        crearTexto(tipo.descripcion, 14f, "#64748B")
                    )
                }
            }

            if (evento.accionesRealizadas.isNotBlank()) {
                tarjeta.addView(espacio(10))
                tarjeta.addView(
                    crearTexto("Acciones realizadas", 14f, "#172B4D", true)
                )
                tarjeta.addView(
                    crearTexto(evento.accionesRealizadas, 15f, "#334155")
                )
            }

            tarjeta.addView(espacio(10))
            tarjeta.addView(
                crearTexto(
                    "Paciente: ${bitacora?.idPaciente?.let { "ID $it" } ?: "No identificado"}",
                    14f,
                    "#475569"
                )
            )

            tarjeta.addView(
                crearTexto(
                    "Bitácora: ${bitacora?.idBitacora ?: "No disponible"}",
                    13f,
                    "#64748B"
                )
            )

            if (imagenesEvento.isNotEmpty()) {
                tarjeta.addView(espacio(12))
                tarjeta.addView(
                    crearTexto("Evidencias fotográficas", 14f, "#172B4D", true)
                )

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
                    }

                    tarjeta.addView(boton)
                }
            } else {
                tarjeta.addView(espacio(8))
                tarjeta.addView(
                    crearTexto(
                        "Sin imágenes adjuntas.",
                        13f,
                        "#64748B"
                    )
                )
            }

            val separador = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
                ).apply {
                    topMargin = 8
                    bottomMargin = 8
                }
                setBackgroundColor(Color.parseColor("#E2E8F0"))
            }

            binding.contenedorEventosCierre.addView(tarjeta)
            binding.contenedorEventosCierre.addView(separador)
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

    private fun crearTarjeta(): LinearLayout {
        return LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)

            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 20f
                setStroke(1, Color.parseColor("#E2E8F0"))
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
            }
        }
    }

    private fun crearTexto(
        texto: String,
        tamano: Float,
        color: String,
        negrita: Boolean = false
    ): TextView {
        return TextView(requireContext()).apply {
            text = texto
            textSize = tamano
            setTextColor(Color.parseColor(color))
            if (negrita) {
                setTypeface(typeface, Typeface.BOLD)
            }
            setPadding(0, 3, 0, 3)
        }
    }

    private fun espacio(alto: Int): View {
        return View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                1,
                alto
            )
        }
    }

    private fun fechaDe(fechaHora: String): String {
        return fechaHora.take(10)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}