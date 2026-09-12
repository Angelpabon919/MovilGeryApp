package com.example.molvigeryapp.ui.encargado.cuidadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.TurnoUI
import com.example.molvigeryapp.databinding.ItemTurnoEncargadoBinding

class TurnoAdapter(
    private var listaTurnos: List<TurnoUI>,
    private val onEditar: (TurnoUI) -> Unit,
    private val onEliminar: (TurnoUI) -> Unit
) : RecyclerView.Adapter<TurnoAdapter.TurnoViewHolder>() {

    // =========================================================
    // ESTADO DEL ADAPTER
    // =========================================================

    /*
     * false = Turnos asignados
     * true  = Turnos pasados / historial
     */
    private var esHistorial = false

    // =========================================================
    // VIEW HOLDER
    // =========================================================

    inner class TurnoViewHolder(
        private val binding: ItemTurnoEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(turno: TurnoUI) {

            // -------------------------------------------------
            // TIPO DE TURNO
            // -------------------------------------------------

            binding.txtTipoTurno.text =
                "Turno ${turno.tipo}"

            // -------------------------------------------------
            // ESTADO
            // -------------------------------------------------

            binding.txtEstadoTurno.text =
                if (esHistorial) {
                    "Finalizado"
                } else {
                    turno.estado
                }

            // -------------------------------------------------
            // FECHA
            // -------------------------------------------------

            binding.txtFechaTurno.text =
                if (
                    turno.fechaInicio ==
                    turno.fechaFin
                ) {

                    turno.fechaInicio

                } else {

                    "${turno.fechaInicio} - ${turno.fechaFin}"
                }

            // -------------------------------------------------
            // HORARIO
            // -------------------------------------------------

            binding.txtHorarioTurno.text =
                "${turno.horaInicio} - ${turno.horaFin}"

            // -------------------------------------------------
            // DURACIÓN
            // -------------------------------------------------

            binding.txtDuracionTurno.text =
                "Duración: ${turno.duracion}"

            // -------------------------------------------------
            // ICONO DE ESTADO
            // -------------------------------------------------

            binding.iconoEstadoTurno.setImageResource(
                R.drawable.check_circle
            )

            // -------------------------------------------------
            // EDITAR
            // -------------------------------------------------

            if (esHistorial) {

                /*
                 * Los turnos pasados son solamente
                 * para consulta.
                 */

                binding.btnEditarTurno.visibility =
                    View.GONE

            } else {

                binding.btnEditarTurno.visibility =
                    View.VISIBLE

                binding.btnEditarTurno.setOnClickListener {

                    onEditar(turno)
                }
            }

            // -------------------------------------------------
            // ELIMINAR
            // -------------------------------------------------

            if (esHistorial) {

                /*
                 * No permitimos eliminar turnos
                 * que ya forman parte del historial.
                 */

                binding.btnEliminarTurno.visibility =
                    View.GONE

            } else {

                binding.btnEliminarTurno.visibility =
                    View.VISIBLE

                binding.btnEliminarTurno.setOnClickListener {

                    onEliminar(turno)
                }
            }
        }
    }

    // =========================================================
    // CREAR VIEW HOLDER
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TurnoViewHolder {

        val binding =
            ItemTurnoEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return TurnoViewHolder(binding)
    }

    // =========================================================
    // VINCULAR DATOS
    // =========================================================

    override fun onBindViewHolder(
        holder: TurnoViewHolder,
        position: Int
    ) {

        holder.bind(
            listaTurnos[position]
        )
    }

    // =========================================================
    // CANTIDAD
    // =========================================================

    override fun getItemCount(): Int =
        listaTurnos.size

    // =========================================================
    // ACTUALIZAR LISTA
    // =========================================================

    fun actualizarLista(
        nuevaLista: List<TurnoUI>
    ) {

        listaTurnos =
            nuevaLista

        notifyDataSetChanged()
    }

    // =========================================================
    // CAMBIAR MODO
    // =========================================================

    fun establecerModoHistorial(
        historial: Boolean
    ) {

        esHistorial =
            historial

        notifyDataSetChanged()
    }
}