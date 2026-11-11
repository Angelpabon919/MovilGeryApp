package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.databinding.ItemMedicamentoCuidadorBinding
import java.util.Locale

class MedicamentoCuidadorAdapter :
    RecyclerView.Adapter<MedicamentoCuidadorAdapter.MedicamentoViewHolder>() {

    private var listaMedicamentos: List<FormulacionMedicamento> = emptyList()

    private var mapaMedicamentos: Map<Int, String> = emptyMap()
    private var mapaPacientes: Map<Int, String> = emptyMap()

    inner class MedicamentoViewHolder(
        private val binding: ItemMedicamentoCuidadorBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(medicamento: FormulacionMedicamento) {

            // Nombre del medicamento
            binding.tvNombreMedicamento.text =
                medicamento.idMedicamentos?.let { id ->
                    mapaMedicamentos[id]
                } ?: "Medicamento no identificado"

            // Dosis y vía
            val dosis = medicamento.dosis?.takeIf { it.isNotBlank() }
                ?: "Sin dosis registrada"

            val via = medicamento.via?.takeIf { it.isNotBlank() }
                ?: "Sin vía registrada"

            binding.tvDosisMedicamento.text =
                "$dosis · Vía $via"

            // Nombre del paciente
            binding.tvPacienteMedicamento.text =
                medicamento.idPaciente?.let { id ->
                    mapaPacientes[id]
                }?.let { nombre ->
                    "Paciente: $nombre"
                } ?: "Paciente no identificado"

            // Hora programada
            binding.tvHoraMedicamento.text =
                formatearHora(medicamento.horaAdministrada)
        }

        private fun formatearHora(hora: String?): String {

            if (hora.isNullOrBlank()) {
                return "--:--"
            }

            val partes = hora.split(":")
            val hora24 = partes.getOrNull(0)?.toIntOrNull()
                ?: return hora

            val minutos = partes.getOrNull(1)?.take(2)
                ?: "00"

            val hora12 = when {
                hora24 == 0 -> 12
                hora24 > 12 -> hora24 - 12
                else -> hora24
            }

            val periodo = if (hora24 < 12) "AM" else "PM"

            return String.format(
                Locale.getDefault(),
                "%02d:%s %s",
                hora12,
                minutos,
                periodo
            )
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MedicamentoViewHolder {

        val binding = ItemMedicamentoCuidadorBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return MedicamentoViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MedicamentoViewHolder,
        position: Int
    ) {
        holder.bind(listaMedicamentos[position])
    }

    override fun getItemCount(): Int =
        listaMedicamentos.size

    fun actualizarLista(
        nuevaLista: List<FormulacionMedicamento>,
        nuevosMedicamentos: Map<Int, String>,
        nuevosPacientes: Map<Int, String>
    ) {

        listaMedicamentos = nuevaLista
        mapaMedicamentos = nuevosMedicamentos
        mapaPacientes = nuevosPacientes

        notifyDataSetChanged()
    }
}
