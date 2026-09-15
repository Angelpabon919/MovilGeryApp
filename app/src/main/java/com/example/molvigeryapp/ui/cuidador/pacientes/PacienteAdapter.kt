package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteBinding

class PacienteAdapter(
    private val onItemClick: (Paciente) -> Unit,
    private val onSelectionToggle: (Paciente) -> Unit
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    private var listaOriginal: List<Paciente> = emptyList()
    private var listaFiltrada: List<Paciente> = emptyList()

    var modoSeleccion: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaOriginal = nuevaLista
        listaFiltrada = nuevaLista
        notifyDataSetChanged()
    }

    fun filtrar(texto: String) {
        listaFiltrada = if (texto.trim().isEmpty()) {
            listaOriginal
        } else {
            listaOriginal.filter { paciente ->
                val nombreCompleto = "${paciente.nombre} ${paciente.apellido}".lowercase()
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
        val paciente = listaFiltrada[position]
        holder.bind(paciente)
    }

    override fun getItemCount(): Int = listaFiltrada.size

    inner class PacienteViewHolder(private val binding: ItemPacienteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {
            binding.tvNombrePaciente.text = "${paciente.nombre} ${paciente.apellido}".trim()

            if (modoSeleccion) {
                binding.cbSeleccionar.visibility = View.VISIBLE
                binding.icChevron.visibility = View.GONE
                
                binding.cbSeleccionar.setOnCheckedChangeListener(null)
                binding.cbSeleccionar.isChecked = paciente.isSelected
                
                binding.cbSeleccionar.setOnCheckedChangeListener { _, isChecked ->
                    onSelectionToggle(paciente)
                }
                
                binding.root.setOnClickListener {
                    binding.cbSeleccionar.isChecked = !binding.cbSeleccionar.isChecked
                }
            } else {
                binding.cbSeleccionar.visibility = View.GONE
                binding.icChevron.visibility = View.VISIBLE
                
                binding.root.setOnClickListener {
                    onItemClick(paciente)
                }
            }
        }
    }
}
