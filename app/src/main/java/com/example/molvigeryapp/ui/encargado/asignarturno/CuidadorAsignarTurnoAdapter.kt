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
        private const val GRIS = "#98A2B3"
        private const val TEXTO = "#1D2939"
    }

    // Guardamos únicamente IDs que realmente existan.
    private val seleccionados =
        mutableSetOf<Int>()

    inner class CuidadorViewHolder(
        private val binding: ItemCuidadorAsignarTurnoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cuidador: Usuario) {

            binding.txtNombreCuidador.text =
                "${cuidador.nombres} ${cuidador.apellidos}"

            if (cuidador.estado) {

                binding.txtEstadoCuidador.text =
                    "Activo"

                binding.txtEstadoCuidador.setTextColor(
                    Color.parseColor(AZUL)
                )

            } else {

                binding.txtEstadoCuidador.text =
                    "Inactivo"

                binding.txtEstadoCuidador.setTextColor(
                    Color.parseColor(GRIS)
                )
            }

            actualizarEstadoVisual(
                cuidador
            )

            binding.root.setOnClickListener {

                cambiarSeleccion(
                    cuidador
                )
            }

            binding.checkCuidador.setOnClickListener {

                cambiarSeleccion(
                    cuidador
                )
            }
        }

        private fun actualizarEstadoVisual(
            cuidador: Usuario
        ) {

            /*
             * idUsuario es Int?, por eso comprobamos
             * que tenga un valor antes de utilizarlo.
             */

            val id =
                cuidador.idUsuario

            val seleccionado =
                id != null &&
                        seleccionados.contains(id)

            if (seleccionado) {

                binding.checkCuidador.isChecked =
                    true

                binding.txtNombreCuidador.setTextColor(
                    Color.parseColor(AZUL)
                )

                binding.root.setBackgroundResource(
                    R.drawable.bg_cuidador_seleccionado
                )

            } else {

                binding.checkCuidador.isChecked =
                    false

                binding.txtNombreCuidador.setTextColor(
                    Color.parseColor(TEXTO)
                )

                binding.root.setBackgroundResource(
                    R.drawable.bg_cuidador_no_seleccionado
                )
            }
        }

        private fun cambiarSeleccion(
            cuidador: Usuario
        ) {

            /*
             * idUsuario puede ser null.
             *
             * Si no existe un ID no podemos seleccionar
             * este cuidador porque la API necesita el ID
             * del usuario para crear la asignación.
             */

            val id =
                cuidador.idUsuario
                    ?: return

            if (
                seleccionados.contains(id)
            ) {

                seleccionados.remove(id)

            } else {

                seleccionados.add(id)
            }

            val posicion =
                bindingAdapterPosition

            if (
                posicion != RecyclerView.NO_POSITION
            ) {

                notifyItemChanged(
                    posicion
                )
            }

            obtenerSeleccionados()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CuidadorViewHolder {

        val binding =
            ItemCuidadorAsignarTurnoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return CuidadorViewHolder(
            binding
        )
    }

    override fun onBindViewHolder(
        holder: CuidadorViewHolder,
        position: Int
    ) {

        holder.bind(
            cuidadores[position]
        )
    }

    override fun getItemCount(): Int =
        cuidadores.size

    fun actualizarLista(
        nuevaLista: List<Usuario>
    ) {

        cuidadores =
            nuevaLista

        /*
         * Conservamos solamente las selecciones
         * que todavía existen en la nueva lista.
         */

        val idsActuales =
            nuevaLista
                .mapNotNull {
                    it.idUsuario
                }
                .toSet()

        seleccionados.retainAll(
            idsActuales
        )

        notifyDataSetChanged()

        obtenerSeleccionados()
    }

    private fun obtenerSeleccionados() {

        val listaSeleccionados =
            cuidadores.filter { cuidador ->

                val id =
                    cuidador.idUsuario

                id != null &&
                        seleccionados.contains(id)
            }

        onSeleccionChanged(
            listaSeleccionados
        )
    }

    fun obtenerIdsSeleccionados(): List<Int> =
        seleccionados.toList()

    fun limpiarSeleccion() {

        seleccionados.clear()

        notifyDataSetChanged()

        obtenerSeleccionados()
    }
}