package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.databinding.ItemPacienteBinding
import kotlin.collections.filter
import com.example.molvigeryapp.data.model.Paciente


class PacienteAdapter(
    private var listaOriginal: List<Paciente> = emptyList(),
    private val onPacienteClick: (Paciente) -> Unit
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    private var listaFiltrada: List<Paciente> = listaOriginal

    inner class PacienteViewHolder(val binding: ItemPacienteBinding) :
        RecyclerView.ViewHolder(binding.root)

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
        with(holder.binding) {
            tvNombrePaciente.text = "${paciente.nombre} ${paciente.apellido}"

            root.setOnClickListener {
                onPacienteClick(paciente)
            }
        }
    }

    override fun getItemCount(): Int = listaFiltrada.size

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaOriginal = nuevaLista
        listaFiltrada = nuevaLista
        notifyDataSetChanged()
    }

    fun filtrar(texto: String) {
        listaFiltrada = if (texto.isEmpty()) {
            listaOriginal
        } else {
            listaOriginal.filter { paciente ->
                "${paciente.nombre} ${paciente.apellido}".lowercase().contains(texto.lowercase())
            }
        }
        notifyDataSetChanged()
    }
}