package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente

class PacienteAdapter(
    private var listaPacientes: List<Paciente> = emptyList(),
    private val mostrarCheckbox: Boolean = true,
    private val onSeleccionCambiada: ((totalSeleccionados: Int) -> Unit)? = null
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    inner class PacienteViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        val imgAvatar: ImageView=
            itemView.findViewById(R.id.imgAvatar)

        val tvNombre: TextView =
            itemView.findViewById(R.id.tvNombrePaciente)

        val tvDetalles: TextView =
            itemView.findViewById(R.id.tvDetallesPaciente)

        val cbSeleccionado: CheckBox =
            itemView.findViewById(R.id.cbSeleccionado)

        fun bind(paciente: Paciente) {

            val nombreCompleto =
                "${paciente.nombre ?: ""} ${paciente.apellido ?: ""}".trim()

            tvNombre.text =
                if (nombreCompleto.isNotEmpty()) {
                    nombreCompleto
                } else {
                    "Paciente sin nombre"
                }

            val habitacion = paciente.habitacion?.toString() ?: "--"
            val documento = paciente.numeroDocumento ?: "--"

            tvDetalles.text =
                "Habitación $habitacion • Doc: $documento"

            //=============================
            //FOTO DEL PACIENTE
            //=============================
            if (!paciente.foto.isNullOrBlank()){
                imgAvatar.imageTintList = null

                Glide.with(itemView.context)
                    .load(paciente.foto)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .centerCrop()
                    .into(imgAvatar)
            }else{
                Glide.with(itemView.context).clear(imgAvatar)
                imgAvatar.setImageResource(R.drawable.person_icon)
                imgAvatar.imageTintList=
                    ColorStateList.valueOf(Color.parseColor("#2563EB"))

            }

            if (mostrarCheckbox) {

                cbSeleccionado.visibility = View.VISIBLE

                // Evitar que el cambio visual active otro listener.
                cbSeleccionado.setOnCheckedChangeListener(null)

                // Restaurar el estado guardado en el objeto Paciente.
                cbSeleccionado.isChecked = paciente.isSelected

                val cambiarSeleccion = View.OnClickListener {

                    val posicion = bindingAdapterPosition

                    if (posicion != RecyclerView.NO_POSITION) {

                        val pacienteActual = listaPacientes[posicion]

                        pacienteActual.isSelected =
                            !pacienteActual.isSelected

                        cbSeleccionado.setOnCheckedChangeListener(null)

                        cbSeleccionado.isChecked =
                            pacienteActual.isSelected

                        onSeleccionCambiada?.invoke(
                            obtenerCantidadSeleccionados()
                        )
                    }
                }

                itemView.setOnClickListener(cambiarSeleccion)
                cbSeleccionado.setOnClickListener(cambiarSeleccion)

            } else {

                cbSeleccionado.visibility = View.GONE

                itemView.setOnClickListener(null)
                cbSeleccionado.setOnClickListener(null)
                cbSeleccionado.setOnCheckedChangeListener(null)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.item_paciente,
                parent,
                false
            )

        return PacienteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PacienteViewHolder,
        position: Int
    ) {
        holder.bind(listaPacientes[position])
    }

    override fun getItemCount(): Int = listaPacientes.size

    fun actualizarLista(nuevaLista: List<Paciente>) {

        listaPacientes = nuevaLista

        notifyDataSetChanged()
    }

    fun obtenerCantidadSeleccionados(): Int {

        return listaPacientes.count { it.isSelected }
    }

    fun obtenerIdsSeleccionados(): Set<Int> {

        return listaPacientes
            .filter { it.isSelected }
            .mapNotNull { it.idPaciente }
            .toSet()
    }
}