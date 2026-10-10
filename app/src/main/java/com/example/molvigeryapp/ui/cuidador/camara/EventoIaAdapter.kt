
package com.example.molvigeryapp.ui.cuidador.camara

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Camara
import com.example.molvigeryapp.data.model.EventoIa
import com.example.molvigeryapp.data.model.Habitacion
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.TipoEventoIa
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class EventoIaAdapter(
    private val onEventoClick: (EventoIa) -> Unit
) : RecyclerView.Adapter<EventoIaAdapter.EventoViewHolder>() {

    // Lista de eventos obtenidos desde eventos_ia/
    private var listaEventos = listOf<EventoIa>()

    // Tipos obtenidos desde tipos_evento_ia/
    private var tiposEventos = mapOf<Int, TipoEventoIa>()

    private var pacientes = mapOf<Int, Paciente>()

    private var camaras = mapOf<Int, Camara>()

    private var habitaciones = mapOf<Int, Habitacion>()


    // =====================================================
    // ACTUALIZAR LISTA DE EVENTOS
    // =====================================================

    fun actualizarEventos(nuevaLista: List<EventoIa>) {
        listaEventos = nuevaLista
        notifyDataSetChanged()
    }

    // =====================================================
    // ACTUALIZAR TIPOS DE EVENTOS
    // =====================================================

    fun actualizarTiposEventos(nuevosTipos: List<TipoEventoIa>) {
        tiposEventos = nuevosTipos.associateBy {
            it.id_tipo_evento
        }

        notifyDataSetChanged()
    }
    fun actualizarPacientes(nuevosPacientes: List<Paciente>) {

        pacientes = nuevosPacientes
            .filter { it.idPaciente != null }
            .associateBy { it.idPaciente!! }

        // Refrescar las tarjetas cuando lleguen los nombres
        notifyDataSetChanged()
    }
    // =====================================================
// ACTUALIZAR CÁMARAS
// =====================================================

    fun actualizarCamaras(nuevasCamaras: List<Camara>) {

        camaras = nuevasCamaras.associateBy {
            it.idCamara
        }

        // Actualizar las tarjetas con los nombres reales
        notifyDataSetChanged()
    }
    // =====================================================
// ACTUALIZAR HABITACIONES
// =====================================================

    fun actualizarHabitaciones(nuevasHabitaciones: List<Habitacion>) {

        habitaciones = nuevasHabitaciones.associateBy {
            it.idHabitacion
        }

        // Refrescar las tarjetas con los números reales
        notifyDataSetChanged()
    }
    // =====================================================
    // CREAR TARJETAS
    // =====================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventoViewHolder {

        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_evento, parent, false)

        return EventoViewHolder(vista)
    }

    // =====================================================
    // MOSTRAR INFORMACIÓN EN CADA TARJETA
    // =====================================================

    override fun onBindViewHolder(
        holder: EventoViewHolder,
        position: Int
    ) {
        holder.bind(listaEventos[position])
    }

    override fun getItemCount(): Int = listaEventos.size

    // =====================================================
    // VIEWHOLDER
    // =====================================================

    inner class EventoViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvTipoEvento: TextView =
            itemView.findViewById(R.id.tvTipoEvento)

        private val tvRiesgoEvento: TextView =
            itemView.findViewById(R.id.tvRiesgoEvento)

        private val tvFechaEvento: TextView =
            itemView.findViewById(R.id.tvFechaEvento)

        private val tvPacienteEvento: TextView =
            itemView.findViewById(R.id.tvPacienteEvento)

        private val tvHabitacionEvento: TextView =
            itemView.findViewById(R.id.tvHabitacionEvento)

        private val tvCamaraEvento: TextView =
            itemView.findViewById(R.id.tvCamaraEvento)

        private val tvConfianzaEvento: TextView =
            itemView.findViewById(R.id.tvConfianzaEvento)

        private val tvEstadoEvento: TextView =
            itemView.findViewById(R.id.tvEstadoEvento)

        private val btnDetalleEvento: TextView =
            itemView.findViewById(R.id.btnDetalleEvento)

        // =================================================
        // ASIGNAR DATOS DEL EVENTO
        // =================================================

        fun bind(evento: EventoIa) {

            // Buscar el tipo correspondiente desde la API
            val tipoEvento = tiposEventos[evento.id_tipo_evento]

            // Nombre obtenido desde tipos_evento_ia/
            val nombreEvento = tipoEvento?.nombre
                ?: "Tipo de evento #${evento.id_tipo_evento}"

            // Nivel de riesgo obtenido desde la API
            val riesgo = tipoEvento?.nivel_riesgo
                ?.lowercase(Locale.ROOT)
                ?: "desconocido"

            // Nombre del evento
            tvTipoEvento.text = nombreEvento

            // Nivel de riesgo
            tvRiesgoEvento.text = riesgo.uppercase(Locale.ROOT)

            // Fecha y hora convertida a Colombia
            tvFechaEvento.text =
                convertirFechaColombia(evento.fecha_hora)

            // Identificación del paciente
            val paciente = pacientes[evento.id_paciente]
            val nombreCompleto = paciente?.let{
                "${it.nombre} ${it.apellido}".trim()
            }.orEmpty()

            tvPacienteEvento.text =
                    if (nombreCompleto.isNotEmpty()){
                        nombreCompleto
                    }else{
                        "paciente ID ${evento.id_paciente}"
                    }
            // =====================================================
            // NÚMERO REAL DE LA HABITACIÓN
            // =====================================================

            val habitacion = habitaciones[evento.id_habitacion]

            val numeroHabitacion = habitacion?.numero
                ?.trim()
                .orEmpty()

            tvHabitacionEvento.text =
                if (numeroHabitacion.isNotEmpty()) {
                    "Habitación $numeroHabitacion"
                } else {
                    "Hab. ${evento.id_habitacion}"
                }



            // =====================================================
            // NOMBRE REAL DE LA CÁMARA
            // =====================================================

            val camara = camaras[evento.id_camara]

            val nombreCamara = camara?.nombre
                ?.trim()
                .orEmpty()

            tvCamaraEvento.text =
                if (nombreCamara.isNotEmpty()) {
                    nombreCamara
                } else {
                    "Cam. ${evento.id_camara}"
                }


            // Porcentaje de confianza
            tvConfianzaEvento.text =
                "IA: ${evento.confianza} %"

            // Estado del evento
            tvEstadoEvento.text = evento.estado

            // =============================================
            // COLORES SEGÚN NIVEL DE RIESGO
            // =============================================

            val colorRiesgo = when (riesgo) {

                "bajo" ->
                    Color.parseColor("#059669")

                "medio" ->
                    Color.parseColor("#D97706")

                "alto" ->
                    Color.parseColor("#EA580C")

                "critico", "crítico" ->
                    Color.parseColor("#DC2626")

                else -> Color.GRAY
            }

            tvRiesgoEvento.setTextColor(colorRiesgo)

            // =============================================
            // CLIC PARA VER DETALLES
            // =============================================

            itemView.setOnClickListener {
                onEventoClick(evento)
            }

            btnDetalleEvento.setOnClickListener {
                onEventoClick(evento)
            }
        }
    }

    // =====================================================
    // CONVERTIR FECHA UTC A HORA DE COLOMBIA
    // =====================================================

    private fun convertirFechaColombia(
        fechaUtc: String
    ): String {

        return try {

            val entrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
            )

            entrada.timeZone =
                TimeZone.getTimeZone("UTC")

            val salida = SimpleDateFormat(
                "dd MMM yyyy · HH:mm:ss",
                Locale("es", "CO")
            )

            salida.timeZone =
                TimeZone.getTimeZone("America/Bogota")

            val fecha = entrada.parse(
                fechaUtc.substring(0, 19)
            )

            if (fecha != null) {
                "${salida.format(fecha)} (UTC-5)"
            } else {
                fechaUtc
            }

        } catch (e: Exception) {
            fechaUtc
        }
    }
}
