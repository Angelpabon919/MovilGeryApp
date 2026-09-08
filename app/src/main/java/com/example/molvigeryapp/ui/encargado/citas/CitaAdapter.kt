package com.example.molvigeryapp.ui.encargado.citas

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.databinding.ItemCitaEncargadoBinding

class CitaAdapter(
    private var listaCitas: List<Cita>,
    private val onCitaClick: (Cita) -> Unit
) : RecyclerView.Adapter<CitaAdapter.CitaViewHolder>() {


    // =====================================================
    // VIEW HOLDER
    // =====================================================

    inner class CitaViewHolder(
        private val binding: ItemCitaEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {


        fun bind(cita: Cita) {

            binding.txtNombrePacienteCita.text =
                cita.nombrePaciente


            binding.txtUbicacionPacienteCita.text =
                "Habitación ${
                    cita.habitacion ?: "N/A"
                } · Cama ${
                    cita.cama ?: "N/A"
                }"


            binding.txtTipoCita.text =
                cita.tipoCita


            binding.txtEspecialidadCita.text =
                cita.especialidad


            binding.txtFechaCita.text =
                cita.fecha


            binding.txtHoraCita.text =
                cita.hora


            binding.txtEstadoCita.text =
                cita.estado


            binding.root.setOnClickListener {

                onCitaClick(cita)
            }
        }
    }


    // =====================================================
    // CREAR VIEW HOLDER
    // =====================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CitaViewHolder {

        val binding =
            ItemCitaEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return CitaViewHolder(binding)
    }


    // =====================================================
    // ASIGNAR DATOS
    // =====================================================

    override fun onBindViewHolder(
        holder: CitaViewHolder,
        position: Int
    ) {

        holder.bind(
            listaCitas[position]
        )
    }


    // =====================================================
    // CANTIDAD
    // =====================================================

    override fun getItemCount(): Int {

        return listaCitas.size
    }


    // =====================================================
    // ACTUALIZAR LISTA
    // =====================================================

    fun actualizarLista(
        nuevaLista: List<Cita>
    ) {

        listaCitas = nuevaLista

        notifyDataSetChanged()
    }
}