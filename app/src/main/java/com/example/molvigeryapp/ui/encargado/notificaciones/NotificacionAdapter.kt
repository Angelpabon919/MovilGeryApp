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

    // =====================================================
    // VIEW HOLDER
    // =====================================================

    inner class NotificacionViewHolder(
        private val binding: ItemNotificacionEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notificacion: Notificacion) {

            // =================================================
            // IMAGEN DE LA NOTIFICACIÓN
            // =================================================

            binding.imgIconoNotificacion.setImageResource(
                notificacion.icono
            )


            // =================================================
            // INFORMACIÓN DE LA NOTIFICACIÓN
            // =================================================

            binding.txtTipoNotificacion.text =
                notificacion.tipo

            binding.txtTituloNotificacion.text =
                notificacion.titulo

            binding.txtDetalleNotificacion.text =
                notificacion.detalle

            binding.txtFechaNotificacion.text =
                notificacion.fecha


            // =================================================
            // INDICADOR DE NOTIFICACIÓN NO LEÍDA
            // =================================================

            binding.indicadorNoLeida.visibility =
                if (notificacion.leida) {
                    View.GONE
                } else {
                    View.VISIBLE
                }


            // =================================================
            // CLICK EN LA NOTIFICACIÓN
            // =================================================

            binding.root.setOnClickListener {

                onClick(notificacion)
            }
        }
    }


    // =====================================================
    // CREAR VIEW HOLDER
    // =====================================================

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


    // =====================================================
    // CONECTAR DATOS CON EL ITEM
    // =====================================================

    override fun onBindViewHolder(
        holder: NotificacionViewHolder,
        position: Int
    ) {

        holder.bind(lista[position])
    }


    // =====================================================
    // CANTIDAD DE ELEMENTOS
    // =====================================================

    override fun getItemCount(): Int {

        return lista.size
    }


    // =====================================================
    // ACTUALIZAR LISTA
    // =====================================================

    fun actualizarLista(
        nuevaLista: List<Notificacion>
    ) {

        lista = nuevaLista

        notifyDataSetChanged()
    }
}