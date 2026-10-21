
package com.example.molvigeryapp.ui.encargado.notificaciones

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.databinding.ItemNotificacionEncargadoBinding
import com.example.molvigeryapp.utils.FechaNotificacionUtils

class NotificacionAdapter(
    private var lista: List<Notificacion>,
    private val onClick: (Notificacion) -> Unit
) : RecyclerView.Adapter<NotificacionAdapter.NotificacionViewHolder>() {

    private val handler = Handler(Looper.getMainLooper())

    private val actualizarTiempos = object : Runnable {
        override fun run() {
            if (itemCount > 0) {
                notifyItemRangeChanged(
                    0,
                    itemCount,
                    PAYLOAD_TIEMPO
                )
            }

            handler.postDelayed(
                this,
                INTERVALO_ACTUALIZACION
            )
        }
    }

    companion object {
        private const val TAG = "FECHA_NOTIFICACION"
        private const val INTERVALO_ACTUALIZACION = 60_000L
        private const val PAYLOAD_TIEMPO = "actualizar_tiempo"
    }

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
            binding.txtTipoNotificacion.text = notificacion.tipo
            binding.txtTituloNotificacion.text = notificacion.titulo
            binding.txtDetalleNotificacion.text = notificacion.detalle

            actualizarFecha(notificacion)

            // INDICADOR DE NO LEÍDA
            binding.indicadorNoLeida.visibility =
                if (notificacion.leida) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            // ACCESIBILIDAD
            binding.root.contentDescription = buildString {
                append(notificacion.tipo)
                append(". ")
                append(notificacion.titulo)
                append(". ")
                append(notificacion.detalle)
                append(". ")
                append(binding.txtFechaNotificacion.text)
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

        fun actualizarSoloFecha(notificacion: Notificacion) {
            actualizarFecha(notificacion)
        }

        private fun actualizarFecha(notificacion: Notificacion) {
            val fechaOriginal = notificacion.fechaHora

            android.util.Log.d(
                "DEBUG_FECHA",
                "ID=${notificacion.idNotificacion} | " +
                        "fechaHora=${notificacion.fechaHora} | " +
                        "textoCalculado=${
                            FechaNotificacionUtils.parsearFecha(notificacion.fechaHora)
                                ?.let {
                                    FechaNotificacionUtils.tiempoTranscurrido(it)
                                } ?: "Fecha no disponible"
                        }"
            )

            val fecha = FechaNotificacionUtils.parsearFecha(fechaOriginal)

            binding.txtFechaNotificacion.text =
                if (fecha != null) {
                    FechaNotificacionUtils.tiempoTranscurrido(fecha)
                } else {
                    "Error al interpretar fechaHora"
                }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificacionViewHolder {
        val binding = ItemNotificacionEncargadoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return NotificacionViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: NotificacionViewHolder,
        position: Int
    ) {
        holder.bind(lista[position])
    }

    override fun onBindViewHolder(
        holder: NotificacionViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_TIEMPO)) {
            holder.actualizarSoloFecha(lista[position])
        } else {
            holder.bind(lista[position])
        }
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<Notificacion>) {
        if (lista == nuevaLista) return

        // Se conserva el orden que entrega el repositorio.
        lista = nuevaLista.toList()
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(
        recyclerView: RecyclerView
    ) {
        super.onAttachedToRecyclerView(recyclerView)

        handler.removeCallbacks(actualizarTiempos)
        handler.postDelayed(
            actualizarTiempos,
            INTERVALO_ACTUALIZACION
        )
    }

    override fun onDetachedFromRecyclerView(
        recyclerView: RecyclerView
    ) {
        handler.removeCallbacks(actualizarTiempos)
        super.onDetachedFromRecyclerView(recyclerView)
    }
}
