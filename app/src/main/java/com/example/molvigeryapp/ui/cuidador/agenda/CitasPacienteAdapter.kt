package com.example.molvigeryapp.ui.cuidador.agenda

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.databinding.ItemCitaPacienteBinding

class CitasPacienteAdapter(
    private val onCitaClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitasPacienteAdapter.CitaViewHolder>() {

    private val citas = mutableListOf<Cita>()

    fun submitList(nuevaLista: List<Cita>) {
        citas.clear()
        citas.addAll(nuevaLista)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CitaViewHolder {

        val binding =
            ItemCitaPacienteBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return CitaViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CitaViewHolder,
        position: Int
    ) {
        holder.bind(citas[position])
    }

    override fun getItemCount(): Int {
        return citas.size
    }

    inner class CitaViewHolder(
        private val binding: ItemCitaPacienteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cita: Cita) {

            binding.tvFechaCita.text =
                if (cita.fecha.isNotBlank()) {
                    cita.fecha
                } else {
                    "Fecha no registrada"
                }

            binding.tvHoraCita.text =
                if (cita.hora.isNotBlank()) {
                    cita.hora
                } else {
                    "Hora no registrada"
                }

            binding.tvMotivoCita.text =
                if (!cita.motivo.isNullOrBlank()) {
                    cita.motivo
                } else {
                    "Sin motivo registrado"
                }

            binding.tvLugarCita.text =
                if (!cita.lugar.isNullOrBlank()) {
                    cita.lugar
                } else {
                    "Lugar no registrado"
                }

            binding.tvEstadoCita.text =
                if (cita.estado.isNotBlank()) {
                    cita.estado
                } else {
                    "Estado no registrado"
                }

            val completada =
                cita.estado.equals(
                    "completada",
                    ignoreCase = true
                )

            if (completada) {

                binding.cardCitaPaciente.alpha = 0.65f

                binding.tvEstadoCita.text =
                    "Completada"

                binding.tvEstadoCita.alpha = 0.8f

            } else {

                binding.cardCitaPaciente.alpha = 1f

                binding.tvEstadoCita.alpha = 1f
            }

            binding.root.setOnClickListener {
                onCitaClick(cita)
            }
        }
    }
}