package com.example.molvigeryapp.ui.encargado.bitacora

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.EstadoEvento
import com.example.molvigeryapp.data.model.EventoBitacora
import com.example.molvigeryapp.data.model.TipoEvento
import com.example.molvigeryapp.databinding.ItemEventoBitacoraEncargadoBinding

class EventoBitacoraAdapter(
    private var listaEventos: List<EventoBitacora>,
    private val onEventoClick: (EventoBitacora) -> Unit
) : RecyclerView.Adapter<EventoBitacoraAdapter.EventoViewHolder>() {

    inner class EventoViewHolder(
        private val binding: ItemEventoBitacoraEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(evento: EventoBitacora) {

            binding.txtNombrePacienteEvento.text = evento.paciente

            binding.txtDescripcionEvento.text = evento.descripcion

            binding.txtCuidadorEvento.text =
                "Cuidador: ${evento.cuidador}"

            binding.txtHoraEvento.text =
                "${evento.fecha} · ${evento.hora}"

            configurarTipoEvento(evento.tipo)
            configurarEstado(evento.estado)

            binding.txtVerEvento.setOnClickListener {
                onEventoClick(evento)
            }

            binding.root.setOnClickListener {
                onEventoClick(evento)
            }
        }

        private fun configurarTipoEvento(tipo: TipoEvento) {

            when (tipo) {

                TipoEvento.EVENTO_ADVERSO -> {
                    binding.txtTipoEvento.text = "EVENTO ADVERSO"

                    binding.txtTipoEvento.setTextColor(
                        Color.parseColor("#E76F51")
                    )

                    binding.viewIndicadorEvento.background =
                        ContextCompat.getDrawable(
                            binding.root.context,
                            R.drawable.bg_indicador_evento
                        )
                }

                TipoEvento.ACTIVIDAD -> {
                    binding.txtTipoEvento.text = "ACTIVIDAD"

                    binding.txtTipoEvento.setTextColor(
                        Color.parseColor("#3B5BDB")
                    )

                    binding.viewIndicadorEvento.background =
                        crearIndicador("#3B5BDB")
                }

                TipoEvento.OBSERVACION -> {
                    binding.txtTipoEvento.text = "OBSERVACIÓN"

                    binding.txtTipoEvento.setTextColor(
                        Color.parseColor("#F59F00")
                    )

                    binding.viewIndicadorEvento.background =
                        crearIndicador("#F59F00")
                }
            }
        }

        private fun configurarEstado(estado: EstadoEvento) {

            when (estado) {

                EstadoEvento.PENDIENTE -> {
                    binding.txtEstadoEvento.text = "PENDIENTE"

                    binding.txtEstadoEvento.setTextColor(
                        Color.parseColor("#E76F51")
                    )

                    binding.txtEstadoEvento.background =
                        ContextCompat.getDrawable(
                            binding.root.context,
                            R.drawable.bg_estado_evento
                        )
                }

                EstadoEvento.REVISADO -> {
                    binding.txtEstadoEvento.text = "REVISADO"

                    binding.txtEstadoEvento.setTextColor(
                        Color.parseColor("#3B5BDB")
                    )

                    binding.txtEstadoEvento.background =
                        crearFondoEstado("#EAF0FF")
                }
            }
        }

        private fun crearIndicador(color: String): android.graphics.drawable.GradientDrawable {

            return android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.parseColor(color))
            }
        }

        private fun crearFondoEstado(
            color: String
        ): android.graphics.drawable.GradientDrawable {

            return android.graphics.drawable.GradientDrawable().apply {
                shape =
                    android.graphics.drawable.GradientDrawable.RECTANGLE

                cornerRadius = 30f

                setColor(Color.parseColor(color))
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): EventoViewHolder {

        val binding =
            ItemEventoBitacoraEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return EventoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: EventoViewHolder,
        position: Int
    ) {
        holder.bind(listaEventos[position])
    }

    override fun getItemCount(): Int {
        return listaEventos.size
    }

    fun actualizarLista(
        nuevaLista: List<EventoBitacora>
    ) {
        listaEventos = nuevaLista
        notifyDataSetChanged()
    }
}