package com.example.molvigeryapp.ui.encargado.asignarturno

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.ItemMesCalendarioBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MesCalendarioAdapter(
    private val meses: List<Calendar>,
    private var mesSeleccionado: Calendar,
    private val onMesSeleccionado: (Calendar) -> Unit
) : RecyclerView.Adapter<MesCalendarioAdapter.MesViewHolder>() {

    // =========================================================
    // COLORES GER IAPP
    // =========================================================

    companion object {

        private const val AZUL = "#3B5BDB"
        private const val GRIS = "#98A2B3"
    }


    // =========================================================
    // FORMATO DEL MES
    // =========================================================

    private val formatoMes =
        SimpleDateFormat(
            "MMMM",
            Locale("es", "ES")
        )


    // =========================================================
    // VIEW HOLDER
    // =========================================================

    inner class MesViewHolder(
        private val binding: ItemMesCalendarioBinding
    ) : RecyclerView.ViewHolder(binding.root) {


        // =====================================================
        // MOSTRAR MES
        // =====================================================

        fun bind(mes: Calendar) {

            val nombreMes =
                formatoMes.format(mes.time)
                    .replaceFirstChar {
                        it.uppercase()
                    }


            binding.txtMes.text =
                nombreMes


            actualizarEstadoVisual(mes)


            // =================================================
            // CLICK EN EL MES
            // =================================================

            binding.root.setOnClickListener {

                onMesSeleccionado(
                    mes.clone() as Calendar
                )
            }
        }


        // =====================================================
        // ESTADO VISUAL DEL MES
        // =====================================================

        private fun actualizarEstadoVisual(
            mes: Calendar
        ) {

            val seleccionado =
                mismoMes(
                    mes,
                    mesSeleccionado
                )


            if (seleccionado) {

                binding.txtMes.setTextColor(
                    Color.parseColor(AZUL)
                )

                binding.txtMes.setTypeface(
                    null,
                    Typeface.BOLD
                )

            } else {

                binding.txtMes.setTextColor(
                    Color.parseColor(GRIS)
                )

                binding.txtMes.setTypeface(
                    null,
                    Typeface.NORMAL
                )
            }
        }
    }


    // =========================================================
    // CREAR VIEWHOLDER
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MesViewHolder {

        val binding =
            ItemMesCalendarioBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return MesViewHolder(binding)
    }


    // =========================================================
    // ASIGNAR DATOS
    // =========================================================

    override fun onBindViewHolder(
        holder: MesViewHolder,
        position: Int
    ) {

        holder.bind(
            meses[position]
        )
    }


    // =========================================================
    // CANTIDAD DE ELEMENTOS
    // =========================================================

    override fun getItemCount(): Int =
        meses.size


    // =========================================================
    // ACTUALIZAR MES SELECCIONADO
    // =========================================================

    fun actualizarSeleccion(
        nuevoMes: Calendar
    ) {

        mesSeleccionado =
            nuevoMes.clone() as Calendar


        notifyDataSetChanged()
    }


    // =========================================================
    // COMPARAR DOS MESES
    // =========================================================

    private fun mismoMes(
        fecha1: Calendar,
        fecha2: Calendar
    ): Boolean {

        return fecha1.get(Calendar.YEAR) ==
                fecha2.get(Calendar.YEAR) &&

                fecha1.get(Calendar.MONTH) ==
                fecha2.get(Calendar.MONTH)
    }
}