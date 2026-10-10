package com.example.molvigeryapp.ui.encargado.asignarturno

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.databinding.ItemCuidadorConflictoTurnoBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CuidadorConflictoTurnoAdapter(
    private var conflictos: List<AsignarTurnoEncargadoFragment.ConflictoTurno>
) : RecyclerView.Adapter<CuidadorConflictoTurnoAdapter.ConflictoViewHolder>() {

    inner class ConflictoViewHolder(
        private val binding: ItemCuidadorConflictoTurnoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(conflicto: AsignarTurnoEncargadoFragment.ConflictoTurno) {
            val cuidador = conflicto.cuidador
            val nombre = "${cuidador.nombres} ${cuidador.apellidos}".trim()

            binding.txtNombreCuidadorConflicto.text =
                nombre.ifBlank { "Cuidador" }
            binding.txtEstadoCuidadorConflicto.text =
                "Ya tiene un turno asignado"

            val detalles = conflicto.detalles
                .distinctBy { "${it.turno.id_turno}|${it.fecha}" }
                .sortedBy { it.fecha }

            // Si el cuidador tiene más de un tipo de turno que se solapa,
            // se muestran los horarios distintos en la misma tarjeta.
            val turnos = detalles
                .distinctBy { it.turno.id_turno }
                .map { detalle ->
                    val turno = detalle.turno
                    val nombreTurno = turno.nombre.ifBlank { "Turno" }
                    val inicio = turno.hora_inicio?.takeIf { it.isNotBlank() } ?: "--"
                    val fin = turno.hora_fin?.takeIf { it.isNotBlank() } ?: "--"
                    "$nombreTurno · $inicio - $fin"
                }

            binding.txtDetalleTurnoConflicto.text =
                "Turno: ${turnos.joinToString("; ").ifBlank { "--" }}"

            val fechas = detalles.map { it.fecha }.distinct().sorted()
            val fechaTexto = when (fechas.size) {
                0 -> "--"
                1 -> formatearFecha(fechas.first())
                else -> "${formatearFecha(fechas.first())} a ${formatearFecha(fechas.last())}"
            }

            binding.txtFechaTurnoConflicto.text = "Fecha: $fechaTexto"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConflictoViewHolder {
        val binding = ItemCuidadorConflictoTurnoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ConflictoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ConflictoViewHolder, position: Int) {
        holder.bind(conflictos[position])
    }

    override fun getItemCount(): Int = conflictos.size

    fun actualizarLista(nuevaLista: List<AsignarTurnoEncargadoFragment.ConflictoTurno>) {
        if (claveLista(conflictos) == claveLista(nuevaLista)) return
        conflictos = nuevaLista
        notifyDataSetChanged()
    }

    private fun claveLista(
        lista: List<AsignarTurnoEncargadoFragment.ConflictoTurno>
    ): List<String> = lista.map { conflicto ->
        val detalles = conflicto.detalles
            .map { "${it.turno.id_turno}|${it.fecha}" }
            .distinct()
            .sorted()
            .joinToString(",")
        "${conflicto.cuidador.idUsuario}|$detalles"
    }

    private fun formatearFecha(fecha: String): String {
        if (fecha.isBlank()) return "--"
        return try {
            val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val salida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            salida.format(entrada.parse(fecha.take(10))!!)
        } catch (_: Exception) {
            fecha
        }
    }
}
