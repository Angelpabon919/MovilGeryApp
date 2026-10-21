package com.example.molvigeryapp.ui.encargado.asignarturno

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Usuario
import com.example.molvigeryapp.databinding.ItemCuidadorResumenAsignacionBinding

class CuidadorResumenAsignacionAdapter(
    private var cuidadores: List<Usuario>
) : RecyclerView.Adapter<CuidadorResumenAsignacionAdapter.CuidadorViewHolder>() {

    companion object {
        private const val AZUL = "#3B5BDB"
        private const val TEXTO = "#1D2939"
        private const val GRIS = "#667085"
    }

    inner class CuidadorViewHolder(
        private val binding: ItemCuidadorResumenAsignacionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cuidador: Usuario) {

            val nombreCompleto =
                "${cuidador.nombres} ${cuidador.apellidos}".trim()

            binding.txtNombreCuidadorResumen.text =
                nombreCompleto.ifBlank { "Cuidador" }

            binding.txtDocumentoCuidadorResumen.text =
                if (cuidador.numeroDocumento.isNullOrBlank()) {
                    "Documento no disponible"
                } else {
                    "Documento: ${cuidador.numeroDocumento}"
                }

            binding.txtEstadoCuidadorResumen.text = "Seleccionado"

            binding.txtNombreCuidadorResumen.setTextColor(
                Color.parseColor(TEXTO)
            )

            binding.txtDocumentoCuidadorResumen.setTextColor(
                Color.parseColor(GRIS)
            )

            binding.txtEstadoCuidadorResumen.setTextColor(
                Color.parseColor(AZUL)
            )

            val inicial = nombreCompleto
                .trim()
                .firstOrNull()
                ?.uppercaseChar()
                ?: 'C'

            Glide.with(binding.imgCuidadorResumen)
                .load(cuidador.foto)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .placeholder(R.drawable.persona_encargado)
                .error(R.drawable.persona_encargado)
                .fallback(R.drawable.persona_encargado)
                .centerCrop()
                .into(binding.imgCuidadorResumen)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CuidadorViewHolder {

        val binding =
            ItemCuidadorResumenAsignacionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return CuidadorViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CuidadorViewHolder,
        position: Int
    ) {
        holder.bind(cuidadores[position])
    }

    override fun getItemCount(): Int = cuidadores.size

    fun actualizarLista(nuevaLista: List<Usuario>) {

        val clavesActuales = cuidadores.map {
            "${it.idUsuario}|${it.foto}"
        }

        val clavesNuevas = nuevaLista.map {
            "${it.idUsuario}|${it.foto}"
        }

        if (clavesActuales == clavesNuevas) {
            return
        }

        cuidadores = nuevaLista
        notifyDataSetChanged()
    }
}