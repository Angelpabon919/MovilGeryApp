package com.example.molvigeryapp.ui.encargado.asignarturno

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Usuario
import com.example.molvigeryapp.databinding.ItemCuidadorAsignarTurnoBinding

class CuidadorAsignarTurnoAdapter(
    private var cuidadores: List<Usuario>,
    private val onSeleccionChanged: (List<Usuario>) -> Unit
) : RecyclerView.Adapter<CuidadorAsignarTurnoAdapter.CuidadorViewHolder>() {

    companion object {
        private const val AZUL = "#3B5BDB"
        private const val GRIS = "#8492A6"
        private const val TEXTO = "#1F2937"
    }

    private val seleccionados = mutableSetOf<Int>()

    inner class CuidadorViewHolder(
        private val binding: ItemCuidadorAsignarTurnoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cuidador: Usuario) {
            val nombreCompleto =
                "${cuidador.nombres} ${cuidador.apellidos}".trim()

            binding.txtNombreCuidador.text =
                nombreCompleto.ifBlank { "Cuidador" }

            // Icono predeterminado hasta incorporar la fotografía real.
            binding.iconoCuidador.setImageResource(
                R.drawable.persona_encargado
            )

            binding.txtEstadoCuidador.text =
                if (cuidador.estado) "Activo" else "Inactivo"

            binding.txtEstadoCuidador.setTextColor(
                Color.parseColor(if (cuidador.estado) AZUL else GRIS)
            )

            actualizarEstadoVisual(cuidador)

            // Un toque en la tarjeta cambia la selección.
            binding.root.setOnClickListener {
                cambiarSeleccion(cuidador)
            }

            // Evita que el clic del checkbox burbujee a la tarjeta
            // y provoque una segunda modificación de la selección.
            binding.checkCuidador.setOnClickListener {
                cambiarSeleccion(cuidador)
            }
        }

        private fun actualizarEstadoVisual(cuidador: Usuario) {
            val id = cuidador.idUsuario
            val seleccionado = id != null && seleccionados.contains(id)

            binding.checkCuidador.setOnCheckedChangeListener(null)
            binding.checkCuidador.isChecked = seleccionado

            binding.txtNombreCuidador.setTextColor(
                Color.parseColor(if (seleccionado) AZUL else TEXTO)
            )

            binding.root.setBackgroundResource(
                if (seleccionado) {
                    R.drawable.bg_cuidador_seleccionado
                } else {
                    R.drawable.bg_cuidador_no_seleccionado
                }
            )
        }

        private fun cambiarSeleccion(cuidador: Usuario) {
            val id = cuidador.idUsuario ?: return

            if (seleccionados.contains(id)) {
                seleccionados.remove(id)
            } else {
                seleccionados.add(id)
            }

            val posicion = bindingAdapterPosition

            if (posicion != RecyclerView.NO_POSITION) {
                notifyItemChanged(posicion)
            }

            notificarSeleccion()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CuidadorViewHolder {
        val binding = ItemCuidadorAsignarTurnoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return CuidadorViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: CuidadorViewHolder,
        position: Int
    ) {
        holder.bind(cuidadores[position])
    }

    override fun getItemCount(): Int = cuidadores.size

    // Actualizar datos de la API sin reconstruir la lista
    // cuando los cuidadores y sus propiedades relevantes no cambian.
    fun actualizarLista(nuevaLista: List<Usuario>) {
        val listaCambio =
            cuidadores.size != nuevaLista.size ||
                    cuidadores.map { obtenerClave(it) } !=
                    nuevaLista.map { obtenerClave(it) }

        val idsActuales = nuevaLista.mapNotNull { it.idUsuario }.toSet()
        seleccionados.retainAll(idsActuales)

        cuidadores = nuevaLista.toList()

        if (!listaCambio) return

        notifyDataSetChanged()
        notificarSeleccion()
    }

    private fun obtenerClave(cuidador: Usuario): String {
        return listOf(
            cuidador.idUsuario,
            cuidador.nombres,
            cuidador.apellidos,
            cuidador.estado,
            cuidador.idRol
        ).joinToString("|")
    }

    private fun notificarSeleccion() {
        val listaSeleccionados = cuidadores.filter { cuidador ->
            val id = cuidador.idUsuario
            id != null && seleccionados.contains(id)
        }

        onSeleccionChanged(listaSeleccionados)
    }

    fun obtenerIdsSeleccionados(): List<Int> =
        seleccionados.toList()

    fun seleccionarCuidador(idUsuario: Int) {
        val existe = cuidadores.any { it.idUsuario == idUsuario }
        if (!existe) return

        if (seleccionados.add(idUsuario)) {
            notifyDataSetChanged()
            notificarSeleccion()
        }
    }

    fun limpiarSeleccion() {
        if (seleccionados.isEmpty()) {
            notificarSeleccion()
            return
        }

        seleccionados.clear()
        notifyDataSetChanged()
        notificarSeleccion()
    }
}