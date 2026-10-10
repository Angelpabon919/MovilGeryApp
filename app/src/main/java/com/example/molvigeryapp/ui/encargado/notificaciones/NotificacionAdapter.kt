package com.example.molvigeryapp.ui.encargado.notificaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.databinding.ItemNotificacionEncargadoBinding

class NotificacionAdapter(
    private var lista: List<Notificacion>,
    private val onClick: (Notificacion) -> Unit
) : RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder>() {

    // ============================================================
    // VIEW HOLDER
    // ============================================================

    inner class NotificacionViewHolder(
        private val binding: ItemNotificacionEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notificacion: Notificacion) {

            // ICONO
            if (notificacion.icono != 0) {
                binding.imgIconoNotificacion.setImageResource(
                    notificacion.icono
                )
            } else {
                binding.imgIconoNotificacion.setImageDrawable(null)
            }

            // INFORMACIÓN
            binding.txtTipoNotificacion.text =
                notificacion.tipo

            binding.txtTituloNotificacion.text =
                notificacion.titulo

            binding.txtDetalleNotificacion.text =
                notificacion.detalle

            binding.txtFechaNotificacion.text =
                notificacion.fecha

            // INDICADOR DE NO LEÍDA
            binding.indicadorNoLeida.visibility =
                if (notificacion.leida) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            // ACCESIBILIDAD
            binding.root.contentDescription =
                buildString {
                    append(notificacion.tipo)
                    append(". ")
                    append(notificacion.titulo)
                    append(". ")
                    append(notificacion.detalle)
                    append(". ")
                    append(notificacion.fecha)
                    append(
                        if (notificacion.leida) {
                            ". Leída"
                        } else {
                            ". No leída"
                        }
                    )
                }

            // CLIC EN LA TARJETA
            binding.root.setOnClickListener {
                onClick(notificacion)
            }
        }
    }

    // ============================================================
    // CREAR VIEW HOLDER
    // ============================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificacionViewHolder {

        val binding =
            ItemNotificacionEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return NotificacionViewHolder(binding)
    }

    // ============================================================
    // CONECTAR DATOS
    // ============================================================

    override fun onBindViewHolder(
        holder: NotificacionViewHolder,
        position: Int
    ) {
        holder.bind(lista[position])
    }

    // ============================================================
    // CANTIDAD
    // ============================================================

    override fun getItemCount(): Int =
        lista.size

    // ============================================================
    // ACTUALIZAR LISTA
    // ============================================================

    fun actualizarLista(
        nuevaLista: List<Notificacion>
    ) {
        if (lista == nuevaLista) return

        lista = nuevaLista.toList()
        notifyDataSetChanged()
    }
}