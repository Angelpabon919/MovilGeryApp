package com.example.molvigeryapp.ui.encargado.cuidadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.databinding.ItemEventoAdversoEncargadoBinding

data class EventoAdversoUI(
    val evento: EventoAdverso,
    val nombrePaciente: String
)

class EventoAdversoAdapter(
    private var lista: List<EventoAdversoUI>,
    private val onClick: (EventoAdversoUI) -> Unit
) : RecyclerView.Adapter<EventoAdversoAdapter.ViewHolder>() {

    class ViewHolder(
        val binding: ItemEventoAdversoEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemEventoAdversoEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int =
        lista.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = lista[position]
        val evento = item.evento

        holder.binding.txtTituloEvento.text =
            "Evento adverso"

        holder.binding.txtPacienteEvento.text =
            "Paciente: ${item.nombrePaciente}"

        holder.binding.txtFechaEvento.text =
            evento.fechaHora.ifBlank {
                "Fecha no disponible"
            }

        holder.binding.txtDescripcionEvento.text =
            if (evento.descripcion.isBlank()) {
                "Sin descripción."
            } else {
                evento.descripcion
            }

        holder.binding.txtAccionesEvento.text =
            if (evento.accionesRealizadas.isBlank()) {
                "Acciones realizadas: No registradas."
            } else {
                "Acciones realizadas: ${evento.accionesRealizadas}"
            }

        holder.binding.txtEstadoEvento.text =
            evento.estado.ifBlank {
                "Sin estado"
            }

        holder.binding.root.setOnClickListener {
            onClick(item)
        }
    }

    fun actualizarLista(
        nuevaLista: List<EventoAdversoUI>
    ) {

        lista = nuevaLista

        notifyDataSetChanged()
    }
}