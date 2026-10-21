package com.example.molvigeryapp.ui.encargado.citas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteCitaBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.molvigeryapp.R

class PacienteCitaAdapter(
    private var listaPacientes: List<Paciente>,
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacienteCitaAdapter.PacienteViewHolder>() {

    inner class PacienteViewHolder(
        private val binding: ItemPacienteCitaBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {

            binding.txtNombrePaciente.text =
                "${paciente.nombre} ${paciente.apellido}"

            binding.txtHabitacionPaciente.text =
                "Habitación ${paciente.habitacion ?: "N/A"} · Cama ${paciente.cama ?: "N/A"}"

            binding.txtEpsPaciente.text =
                "EPS: ${paciente.eps ?: "No registrada"}"

            // Cargar fotografía del paciente desde la API
            Glide.with(binding.imgPaciente)
                .load(paciente.foto)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .placeholder(R.drawable.person_icon)
                .error(R.drawable.person_icon)
                .fallback(R.drawable.person_icon)
                .centerCrop()
                .into(binding.imgPaciente)

            binding.root.setOnClickListener {
                onPacienteClick(paciente)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {

        val binding = ItemPacienteCitaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PacienteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PacienteViewHolder,
        position: Int
    ) {
        holder.bind(listaPacientes[position])
    }

    override fun getItemCount(): Int {
        return listaPacientes.size
    }

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaPacientes = nuevaLista
        notifyDataSetChanged()
    }
}