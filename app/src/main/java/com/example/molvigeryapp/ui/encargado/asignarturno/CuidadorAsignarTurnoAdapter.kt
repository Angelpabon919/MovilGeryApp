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


    // =========================================================
    // CUIDADORES SELECCIONADOS
    // =========================================================

    /*
     * Guardamos los ID de los cuidadores seleccionados.
     *
     * Al ser un MutableSet podemos tener:
     *
     * Cuidador A
     * Cuidador B
     * Cuidador C
     *
     * sin duplicados.
     */

    private val seleccionados =
        mutableSetOf<Int>()


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    inner class CuidadorViewHolder(
        private val binding: ItemCuidadorAsignarTurnoBinding
    ) : RecyclerView.ViewHolder(binding.root) {


        // =====================================================
        // BIND
        // =====================================================

        fun bind(
            cuidador: Usuario
        ) {

            binding.txtNombreCuidador.text =
                "${cuidador.nombres} ${cuidador.apellidos}"


            // =================================================
            // ESTADO
            // =================================================

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


            // =================================================
            // ESTADO VISUAL
            // =================================================

            actualizarEstadoVisual(
                cuidador
            )


            // =================================================
            // CLICK EN LA TARJETA
            // =================================================

            binding.root.setOnClickListener {

                cambiarSeleccion(
                    cuidador
                )
            }


            // =================================================
            // CLICK EN CHECKBOX
            // =================================================

            binding.checkCuidador.setOnClickListener {

                cambiarSeleccion(
                    cuidador
                )
            }
        }


        // =====================================================
        // ACTUALIZAR ESTADO VISUAL
        // =====================================================

        private fun actualizarEstadoVisual(
            cuidador: Usuario
        ) {

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


        // =====================================================
        // CAMBIAR SELECCIÓN
        // =====================================================

        private fun cambiarSeleccion(
            cuidador: Usuario
        ) {

            /*
             * Necesitamos el ID del usuario porque
             * este ID será enviado al backend.
             */

            val id =
                cuidador.idUsuario
                    ?: return


            if (
                seleccionados.contains(id)
            ) {

                // Ya estaba seleccionado → quitarlo.

                seleccionados.remove(id)

            } else {

                // No estaba seleccionado → agregarlo.

                seleccionados.add(id)
            }


            // =================================================
            // ACTUALIZAR VISUAL
            // =================================================

            val posicion =
                bindingAdapterPosition


            if (
                posicion != RecyclerView.NO_POSITION
            ) {

                notifyItemChanged(
                    posicion
                )
            }


            // =================================================
            // AVISAR AL FRAGMENT
            // =================================================

            obtenerSeleccionados()
        }
    }


    // =========================================================
    // CREAR VIEW HOLDER
    // =========================================================

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


    // =========================================================
    // VINCULAR VIEW HOLDER
    // =========================================================

    override fun onBindViewHolder(
        holder: CuidadorViewHolder,
        position: Int
    ) {

        holder.bind(
            cuidadores[position]
        )
    }


    // =========================================================
    // CANTIDAD
    // =========================================================

    override fun getItemCount(): Int =
        cuidadores.size


    // =========================================================
    // ACTUALIZAR LISTA
    // =========================================================

    fun actualizarLista(
        nuevaLista: List<Usuario>
    ) {

        cuidadores =
            nuevaLista


        /*
         * Conservamos solamente los IDs seleccionados
         * que todavía existen en la lista.
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


    // =========================================================
    // OBTENER CUIDADORES SELECCIONADOS
    // =========================================================

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


    // =========================================================
    // OBTENER IDS SELECCIONADOS
    // =========================================================

    fun obtenerIdsSeleccionados(): List<Int> =
        seleccionados.toList()


    // =========================================================
    // SELECCIONAR CUIDADOR AUTOMÁTICAMENTE
    // =========================================================

    /*
     * Se utiliza cuando entramos desde:
     *
     * Ver turnos
     *      ↓
     * Asignar turno
     *
     * El cuidador desde el que venimos queda
     * seleccionado inicialmente.
     *
     * IMPORTANTE:
     *
     * Esto NO elimina la posibilidad de seleccionar
     * otros cuidadores.
     */

    fun seleccionarCuidador(
        idUsuario: Int
    ) {

        /*
         * Comprobamos que el cuidador exista
         * actualmente en la lista.
         */

        val existe =
            cuidadores.any {
                it.idUsuario == idUsuario
            }


        if (!existe) {
            return
        }



        seleccionados.add(
            idUsuario
        )



        notifyDataSetChanged()


        /*
         * Informamos al Fragment de la nueva
         * lista de seleccionados.
         */

        obtenerSeleccionados()
    }


    fun limpiarSeleccion() {

        seleccionados.clear()

        notifyDataSetChanged()

        obtenerSeleccionados()
    }
}