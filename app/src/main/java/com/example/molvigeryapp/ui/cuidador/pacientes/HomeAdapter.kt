package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.ImageView
import com.bumptech.glide.Glide

class HomeAdapter(
    private var listaPacientes: List<Paciente> = emptyList(),
    private val onItemClick: ((Paciente) -> Unit)? = null
) : RecyclerView.Adapter<HomeAdapter.HomeViewHolder>() {

    inner class HomeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val imgAvatar: ImageView=itemView.findViewById(R.id.imgAvatar)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombrePaciente)
        val tvDetalles: TextView = itemView.findViewById(R.id.tvDetallesPaciente)
        val cbSeleccionado: View? = itemView.findViewById(R.id.cbSeleccionado)

        fun bind(paciente: Paciente) {
            val nombreCompleto = "${paciente.nombre} ${paciente.apellido}".trim()
            tvNombre.text = if (nombreCompleto.isNotEmpty()) nombreCompleto else "Paciente sin nombre"

            val hab = paciente.habitacion?.toString() ?: "--"
            val doc = paciente.numeroDocumento ?: "--"
            tvDetalles.text = "Habitación $hab • Doc: $doc"

            if(!paciente.foto.isNullOrBlank()){
                imgAvatar.imageTintList=null

                Glide.with(itemView.context)
                    .load(paciente.foto)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .centerCrop()
                    .into(imgAvatar)
            }else{
                Glide.with(itemView.context)
                    .clear(imgAvatar)

                imgAvatar.setImageResource(
                    R.drawable.person_icon
                )
                imgAvatar.imageTintList=
                    ColorStateList.valueOf(
                        Color.parseColor("#2563EB")
                    )
            }

            // Ocultar el CheckBox permanentemente para la vista de lectura
            cbSeleccionado?.visibility = View.GONE

            itemView.setOnClickListener {
                onItemClick?.invoke(paciente)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paciente, parent, false)
        return HomeViewHolder(view)
    }

    override fun onBindViewHolder(holder: HomeViewHolder, position: Int) {
        holder.bind(listaPacientes[position])
    }

    override fun getItemCount(): Int = listaPacientes.size

    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaPacientes = nuevaLista
        notifyDataSetChanged()
    }
}