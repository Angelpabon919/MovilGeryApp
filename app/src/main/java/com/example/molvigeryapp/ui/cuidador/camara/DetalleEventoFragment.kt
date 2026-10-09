
package com.example.molvigeryapp.ui.cuidador.camara

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.EventoIa
import com.example.molvigeryapp.data.model.TipoEventoIa
import com.example.molvigeryapp.data.repository.EventoIaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetalleEventoFragment : Fragment() {

    private val repository = EventoIaRepository()

    private var idEventoSeleccionado = -1

    companion object {

        private const val ARG_ID_EVENTO = "id_evento"

        fun newInstance(idEvento: Int): DetalleEventoFragment {
            return DetalleEventoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_EVENTO, idEvento)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        idEventoSeleccionado =
            arguments?.getInt(ARG_ID_EVENTO, -1) ?: -1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_detalle_evento,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Regresar al listado
        view.findViewById<View>(
            R.id.btnVolverDetalle
        ).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        if (idEventoSeleccionado == -1) {
            mostrarError(view, "No se recibió el ID del evento")
            return
        }

        cargarDetalle(view)
    }

    // =====================================================
    // CONSULTAR LOS DATOS DESDE EL BACKEND
    // =====================================================

    private fun cargarDetalle(view: View) {

        viewLifecycleOwner.lifecycleScope.launch {

            // 1. CONSULTAR EVENTOS
            val evento: EventoIa

            try {
                val respuesta = repository.obtenerEventos()

                if (!respuesta.isSuccessful) {
                    mostrarError(
                        view,
                        "Error al consultar evento: ${respuesta.code()}"
                    )
                    return@launch
                }

                val encontrado = respuesta.body()
                    .orEmpty()
                    .find {
                        it.id_evento == idEventoSeleccionado
                    }

                if (encontrado == null) {
                    mostrarError(
                        view,
                        "No se encontró el evento seleccionado"
                    )
                    return@launch
                }

                evento = encontrado

                mostrarDatosEvento(view, evento)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mostrarError(
                    view,
                    "No fue posible consultar el evento"
                )
                return@launch
            }

            // 2. CONSULTAR TIPO Y DESCRIPCIÓN
            try {
                val respuestaTipos =
                    repository.obtenerTiposEventos()

                if (respuestaTipos.isSuccessful) {

                    val tipo = respuestaTipos.body()
                        .orEmpty()
                        .find {
                            it.id_tipo_evento ==
                                    evento.id_tipo_evento
                        }

                    mostrarTipoEvento(view, tipo)

                } else {
                    mostrarTipoEvento(view, null)
                }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                mostrarTipoEvento(view, null)
            }

            // 3. CONSULTAR EVIDENCIAS
            cargarEvidencia(view, evento.id_evento)
        }
    }

    // =====================================================
    // MOSTRAR INFORMACIÓN DEL EVENTO
    // =====================================================


    private fun mostrarDatosEvento(
        view: View,
        evento: EventoIa
    ) {

        view.findViewById<TextView>(
            R.id.tvIdEventoDetalle
        ).text = "#${evento.id_evento}"

        view.findViewById<TextView>(
            R.id.tvPacienteDetalle
        ).text = "ID ${evento.id_paciente}"

        view.findViewById<TextView>(
            R.id.tvHabitacionDetalle
        ).text = "Habitación ${evento.id_habitacion}"

        view.findViewById<TextView>(
            R.id.tvCamaraDetalle
        ).text = "Cámara ${evento.id_camara}"

        view.findViewById<TextView>(
            R.id.tvFechaHoraDetalle
        ).text = convertirFechaColombia(evento.fecha_hora)

        view.findViewById<TextView>(
            R.id.tvConfianzaDetalle
        ).text = "${evento.confianza} %"

        view.findViewById<TextView>(
            R.id.tvEstadoDetalle
        ).text = evento.estado.replaceFirstChar {
            it.uppercase()
        }

        val confianza = evento.confianza
            .toString()
            .toDoubleOrNull()
            ?.coerceIn(0.0, 100.0) ?: 0.0

        view.findViewById<ProgressBar>(
            R.id.progressConfianzaDetalle
        ).progress = confianza.toInt()
    }


    // =====================================================
    // MOSTRAR TIPO, RIESGO Y DESCRIPCIÓN
    // =====================================================

    private fun mostrarTipoEvento(
        view: View,
        tipo: TipoEventoIa?
    ) {

        val tvNombre = view.findViewById<TextView>(
            R.id.tvNombreDetalle
        )

        val tvRiesgo = view.findViewById<TextView>(
            R.id.tvRiesgoDetalle
        )

        val tvDescripcion = view.findViewById<TextView>(
            R.id.tvDescripcionDetalle
        )

        if (tipo == null) {

            tvNombre.text = "Tipo de evento no disponible"
            tvRiesgo.text = "Riesgo no disponible"
            tvDescripcion.text =
                "No se pudo obtener la descripción del evento."

            return
        }

        // Datos reales de tipos_evento_ia/
        tvNombre.text = tipo.nombre

        val riesgo = tipo.nivel_riesgo.lowercase(Locale.ROOT)

        tvRiesgo.text = "RIESGO ${riesgo.uppercase(Locale.ROOT)}"

        tvDescripcion.text = tipo.descripcion

        val color = when (riesgo) {
            "bajo" -> "#059669"
            "medio" -> "#D97706"
            "alto" -> "#EA580C"
            "critico", "crítico" -> "#DC2626"
            else -> "#64748B"
        }

        tvRiesgo.setTextColor(Color.parseColor(color))
    }

    // =====================================================
    // CONSULTAR Y MOSTRAR EVIDENCIA
    // =====================================================

    private suspend fun cargarEvidencia(
        view: View,
        idEvento: Int
    ) {

        val imgEvidencia = view.findViewById<ImageView>(
            R.id.imgEvidenciaDetalle
        )

        val tvSinEvidencia = view.findViewById<TextView>(
            R.id.tvSinEvidencia
        )

        val progress = view.findViewById<ProgressBar>(
            R.id.progressEvidencia
        )

        progress.visibility = View.VISIBLE
        imgEvidencia.visibility = View.GONE
        tvSinEvidencia.visibility = View.GONE

        try {

            val respuesta = repository.obtenerEvidencias()

            if (!respuesta.isSuccessful) {
                tvSinEvidencia.text =
                    "No se pudieron consultar las evidencias."
                tvSinEvidencia.visibility = View.VISIBLE
                return
            }

            // Buscar la imagen correspondiente al evento
            val evidencia = respuesta.body()
                .orEmpty()
                .filter {
                    it.id_evento == idEvento &&
                            it.tipo.equals("imagen", ignoreCase = true) &&
                            it.url.isNotBlank()
                }
                .maxByOrNull { it.fecha_hora }

            if (evidencia == null) {

                tvSinEvidencia.text =
                    "Este evento no tiene evidencia disponible."

                tvSinEvidencia.visibility = View.VISIBLE
                return
            }

            // Cargar imagen real desde su URL
            tvSinEvidencia.visibility = View.GONE

            Glide.with(this@DetalleEventoFragment)
                .load(evidencia.url)
                .into(imgEvidencia)

            imgEvidencia.visibility = View.VISIBLE

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {

            tvSinEvidencia.text =
                "No fue posible cargar la evidencia."

            tvSinEvidencia.visibility = View.VISIBLE

        } finally {
            progress.visibility = View.GONE
        }
    }

    // =====================================================
    // CONVERTIR FECHA UTC A HORA COLOMBIANA
    // =====================================================

    private fun convertirFechaColombia(
        fechaUtc: String
    ): String {

        return try {

            val formatoEntrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
            )

            formatoEntrada.timeZone =
                TimeZone.getTimeZone("UTC")

            val formatoSalida = SimpleDateFormat(
                "dd/MM/yyyy · HH:mm:ss",
                Locale("es", "CO")
            )

            formatoSalida.timeZone =
                TimeZone.getTimeZone("America/Bogota")

            val fecha = formatoEntrada.parse(
                fechaUtc.substring(0, 19)
            )

            if (fecha != null) {
                "${formatoSalida.format(fecha)} (Colombia)"
            } else {
                fechaUtc
            }

        } catch (e: Exception) {
            fechaUtc
        }
    }

    // =====================================================
    // MOSTRAR ERROR
    // =====================================================

    private fun mostrarError(
        view: View,
        mensaje: String
    ) {
        view.findViewById<TextView>(
            R.id.tvNombreDetalle
        ).text = mensaje

        view.findViewById<TextView>(
            R.id.tvDescripcionDetalle
        ).text = "Información no disponible."

        view.findViewById<TextView>(
            R.id.tvSinEvidencia
        ).text = "Evidencia no disponible."
    }
}
