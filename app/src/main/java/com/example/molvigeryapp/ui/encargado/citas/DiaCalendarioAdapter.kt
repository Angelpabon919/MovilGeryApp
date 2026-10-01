package com.example.molvigeryapp.ui.encargado.citas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.DiaCalendario
import com.example.molvigeryapp.databinding.ItemDiaCalendarioEncargadoBinding

class DiaCalendarioAdapter(
    private var dias: List<DiaCalendario>,
    private val onDiaClick: (DiaCalendario) -> Unit
) : RecyclerView.Adapter<DiaCalendarioAdapter.DiaViewHolder>() {

    inner class DiaViewHolder(
        private val binding: ItemDiaCalendarioEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dia: DiaCalendario) {

            // =================================================
            // ESPACIO VACÍO
            // =================================================

            if (dia.dia == null) {

                binding.txtDiaCalendario.text = ""

                binding.txtDiaCalendario.background = null

                binding.txtDiaCalendario.setTextColor(
                    Color.TRANSPARENT
                )

                binding.root.setOnClickListener(null)

                binding.root.isClickable = false

                return
            }


            // =================================================
            // DÍA
            // =================================================

            binding.txtDiaCalendario.text =
                dia.dia.toString()


            // =================================================
            // FECHA ANTERIOR
            // =================================================

            if (dia.esAnterior) {

                binding.txtDiaCalendario.setBackgroundResource(
                    R.drawable.bg_dia_calendario_anterior_encargado
                )

                binding.txtDiaCalendario.setTextColor(
                    Color.rgb(
                        203,
                        208,
                        217
                    )
                )

                binding.root.setOnClickListener(null)

                binding.root.isClickable = false

                binding.root.isFocusable = false

                return
            }


            // =================================================
            // FECHA SELECCIONADA
            // =================================================

            if (dia.esSeleccionado) {

                binding.txtDiaCalendario.setBackgroundResource(
                    R.drawable.bg_dia_calendario_seleccionado_encargado
                )

                binding.txtDiaCalendario.setTextColor(
                    Color.WHITE
                )

            }

            // =================================================
            // HOY
            // =================================================

            else if (dia.esHoy) {

                binding.txtDiaCalendario.setBackgroundResource(
                    R.drawable.bg_dia_calendario_hoy_encargado
                )

                binding.txtDiaCalendario.setTextColor(
                    Color.rgb(
                        59,
                        91,
                        219
                    )
                )

            }

            // =================================================
            // FECHA FUTURA
            // =================================================

            else {

                binding.txtDiaCalendario.setBackgroundResource(
                    R.drawable.bg_dia_calendario_encargado
                )

                binding.txtDiaCalendario.setTextColor(
                    Color.rgb(
                        29,
                        41,
                        57
                    )
                )
            }


            // =================================================
            // CLICK
            // =================================================

            binding.root.isClickable = true

            binding.root.isFocusable = true

            binding.root.setOnClickListener {

                onDiaClick(dia)
            }
        }
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiaViewHolder {

        val binding =
            ItemDiaCalendarioEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return DiaViewHolder(binding)
    }


    override fun onBindViewHolder(
        holder: DiaViewHolder,
        position: Int
    ) {

        holder.bind(
            dias[position]
        )
    }


    override fun getItemCount(): Int =
        dias.size


    fun actualizar(
        nuevaLista: List<DiaCalendario>
    ) {

        dias = nuevaLista

        notifyDataSetChanged()
    }
}