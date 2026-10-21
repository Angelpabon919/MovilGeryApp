package com.example.molvigeryapp.ui.encargado.cuidadores

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.data.model.Cuidador
import com.example.molvigeryapp.databinding.ItemCuidadorEncargadoBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.molvigeryapp.R

class CuidadorAdapter(
    private var lista: List<Cuidador>,
    private val onClick: (Cuidador) -> Unit
) : RecyclerView.Adapter<CuidadorAdapter.ViewHolder>() {

    // ==========================================
    // VIEW HOLDER
    // ==========================================

    class ViewHolder(
        val binding: ItemCuidadorEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root)


    // ==========================================
    // CREAR TARJETA
    // ==========================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding =
            ItemCuidadorEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return ViewHolder(binding)
    }


    // ==========================================
    // CANTIDAD DE ELEMENTOS
    // ==========================================

    override fun getItemCount(): Int =
        lista.size


    // ==========================================
    // MOSTRAR DATOS
    // ==========================================

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val cuidador = lista[position]

        holder.binding.txtNombre.text =
            cuidador.nombre

        holder.binding.txtCargo.text =
            cuidador.cargo

        holder.binding.txtEstado.text =
            cuidador.estado

        holder.binding.txtPacientes.text =
            "${cuidador.pacientes} pacientes"


        // FOTO DEL CUIDADOR
        Glide.with(holder.itemView)
            .load(cuidador.foto)
            .placeholder(R.drawable.persona_encargado)
            .error(R.drawable.persona_encargado)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .centerCrop()
            .into(holder.binding.imgCuidador)



        // ==========================================
        // CLICK EN LA TARJETA
        // ==========================================

        holder.binding.root.setOnClickListener {
            onClick(cuidador)
        }
    }


    // ==========================================
    // ACTUALIZAR LISTA
    // ==========================================

    fun actualizarLista(
        nuevaLista: List<Cuidador>
    ) {

        lista = nuevaLista

        notifyDataSetChanged()
    }
}