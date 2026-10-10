package com.example.molvigeryapp.ui.encargado.asignarturno

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
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

            binding.txtInicialCuidador.text = inicial.toString()
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

        // Comparamos los ID de los cuidadores actuales
        // con los que acaba de entregar la API
        val idsActuales = cuidadores.mapNotNull {
            it.idUsuario
        }

        val idsNuevos = nuevaLista.mapNotNull {
            it.idUsuario
        }

        // Si la lista es exactamente la misma,
        // no reconstruimos el RecyclerView
        if (idsActuales == idsNuevos) {
            return
        }

        // Solo actualizamos visualmente cuando realmente
        // hubo un cambio en los cuidadores.
        cuidadores = nuevaLista

        notifyDataSetChanged()
    }
}