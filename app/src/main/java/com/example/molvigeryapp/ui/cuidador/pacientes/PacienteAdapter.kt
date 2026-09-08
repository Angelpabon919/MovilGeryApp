package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteBinding

class PacienteAdapter(
    private val onItemClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    private var listaOriginal: List<Paciente> = emptyList()
    private var listaDiferida: List<Paciente> = emptyList()

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaOriginal = nuevaLista
        listaDiferida = nuevaLista
        notifyDataSetChanged()
    }

    fun filtrar(texto: String) {
        listaDiferida = if (texto.trim().isEmpty()) {
            listaOriginal
        } else {
            listaOriginal.filter { paciente ->
                val nombreCompleto = "${paciente.nombre ?: ""} ${paciente.apellido ?: ""}".lowercase()
                nombreCompleto.contains(texto.lowercase().trim())
            }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val binding = ItemPacienteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PacienteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        val paciente = listaDiferida[position]
        holder.bind(paciente)
    }

    override fun getItemCount(): Int = listaDiferida.size

    inner class PacienteViewHolder(private val binding: ItemPacienteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {
            val nombre = paciente.nombre ?: ""
            val apellido = paciente.apellido ?: ""
            val nombreCompleto = "$nombre $apellido".trim()
            
            binding.tvNombrePaciente.text = if (nombreCompleto.isNotEmpty()) {
                nombreCompleto
            } else {
                "Sin nombre registrado"
            }

            binding.root.setOnClickListener {
                onItemClick(paciente)
            }
        }
    }
}