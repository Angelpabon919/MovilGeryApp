package com.example.molvigeryapp.ui.encargado.asignarturno

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.ItemDiaCalendarioBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DiaCalendarioAdapter(
    private val dias: List<Calendar>,
    private var fechaInicio: Calendar?,
    private var fechaFin: Calendar?,
    private val onFechaSeleccionada: (Calendar) -> Unit
) : RecyclerView.Adapter<DiaCalendarioAdapter.DiaViewHolder>() {

    companion object {
        private const val AZUL = "#3B5BDB"
        private const val GRIS = "#98A2B3"
        private const val TEXTO = "#1D2939"
        private const val BLANCO = "#FFFFFF"
        private const val GRIS_CLARO = "#E9ECEF"
        private const val TEXTO_DESHABILITADO = "#B8BEC8"
    }

    private val formatoDia =
        SimpleDateFormat(
            "EEE",
            Locale("es", "ES")
        )

    inner class DiaViewHolder(
        private val binding: ItemDiaCalendarioBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dia: Calendar) {

            binding.txtNumeroDia.text =
                dia.get(Calendar.DAY_OF_MONTH).toString()

            val nombreDia =
                formatoDia
                    .format(dia.time)
                    .replaceFirstChar {
                        it.uppercase()
                    }

            binding.txtNombreDia.text =
                nombreDia

            actualizarEstadoVisual(dia)

            binding.root.setOnClickListener {

                /*
                 * Los días anteriores a hoy no se pueden
                 * seleccionar.
                 */
                if (esFechaPasada(dia)) {
                    return@setOnClickListener
                }

                onFechaSeleccionada(
                    dia.clone() as Calendar
                )
            }
        }

        private fun actualizarEstadoVisual(
            dia: Calendar
        ) {

            // =================================================
            // FECHA PASADA
            // =================================================

            if (esFechaPasada(dia)) {

                binding.txtNumeroDia.setBackgroundResource(
                    R.drawable.bg_dia_calendario_no_disponible
                )

                binding.txtNumeroDia.setTextColor(
                    Color.parseColor(
                        TEXTO_DESHABILITADO
                    )
                )

                binding.txtNumeroDia.setTypeface(
                    null,
                    Typeface.NORMAL
                )

                binding.txtNombreDia.setTextColor(
                    Color.parseColor(
                        TEXTO_DESHABILITADO
                    )
                )

                binding.txtNombreDia.setTypeface(
                    null,
                    Typeface.NORMAL
                )

                binding.root.alpha = 0.8f

                return
            }

            // =================================================
            // FECHA INICIO / FECHA FIN
            // =================================================

            val esInicio =
                esMismaFecha(
                    dia,
                    fechaInicio
                )

            val esFin =
                esMismaFecha(
                    dia,
                    fechaFin
                )

            val estaEnRango =
                estaDentroDelRango(
                    dia
                )

            // =================================================
            // INICIO O FIN SELECCIONADO
            // =================================================

            if (esInicio || esFin) {

                binding.txtNumeroDia.setBackgroundResource(
                    R.drawable.bg_dia_calendario_seleccionado
                )

                binding.txtNumeroDia.setTextColor(
                    Color.parseColor(
                        BLANCO
                    )
                )

                binding.txtNumeroDia.setTypeface(
                    null,
                    Typeface.BOLD
                )

                binding.txtNombreDia.setTextColor(
                    Color.parseColor(
                        AZUL
                    )
                )

                binding.txtNombreDia.setTypeface(
                    null,
                    Typeface.BOLD
                )

                binding.root.alpha = 1f

                return
            }

            // =================================================
            // DÍA DENTRO DEL RANGO
            // =================================================

            if (estaEnRango) {

                binding.txtNumeroDia.setBackgroundResource(
                    R.drawable.bg_dia_calendario_rango
                )

                binding.txtNumeroDia.setTextColor(
                    Color.parseColor(
                        AZUL
                    )
                )

                binding.txtNumeroDia.setTypeface(
                    null,
                    Typeface.BOLD
                )

                binding.txtNombreDia.setTextColor(
                    Color.parseColor(
                        GRIS
                    )
                )

                binding.txtNombreDia.setTypeface(
                    null,
                    Typeface.NORMAL
                )

                binding.root.alpha = 1f

                return
            }

            // =================================================
            // DÍA NORMAL DISPONIBLE
            // =================================================

            binding.txtNumeroDia.setBackgroundResource(
                R.drawable.bg_dia_calendario
            )

            binding.txtNumeroDia.setTextColor(
                Color.parseColor(
                    TEXTO
                )
            )

            binding.txtNumeroDia.setTypeface(
                null,
                Typeface.BOLD
            )

            binding.txtNombreDia.setTextColor(
                Color.parseColor(
                    GRIS
                )
            )

            binding.txtNombreDia.setTypeface(
                null,
                Typeface.NORMAL
            )

            binding.root.alpha = 1f
        }
    }

    // =========================================================
    // CREAR VIEW HOLDER
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DiaViewHolder {

        val binding =
            ItemDiaCalendarioBinding.inflate(
                LayoutInflater.from(
                    parent.context
                ),
                parent,
                false
            )

        return DiaViewHolder(
            binding
        )
    }

    // =========================================================
    // VINCULAR DATOS
    // =========================================================

    override fun onBindViewHolder(
        holder: DiaViewHolder,
        position: Int
    ) {

        holder.bind(
            dias[position]
        )
    }

    // =========================================================
    // CANTIDAD DE ELEMENTOS
    // =========================================================

    override fun getItemCount(): Int =
        dias.size

    // =========================================================
    // ACTUALIZAR SELECCIÓN
    // =========================================================

    fun actualizarSeleccion(
        nuevaFechaInicio: Calendar?,
        nuevaFechaFin: Calendar?
    ) {

        fechaInicio =
            nuevaFechaInicio
                ?.clone() as? Calendar

        fechaFin =
            nuevaFechaFin
                ?.clone() as? Calendar

        notifyDataSetChanged()
    }

    // =========================================================
    // COMPROBAR MISMA FECHA
    // =========================================================

    private fun esMismaFecha(
        fecha1: Calendar,
        fecha2: Calendar?
    ): Boolean {

        if (fecha2 == null) {
            return false
        }

        return fecha1.get(Calendar.YEAR) ==
                fecha2.get(Calendar.YEAR) &&

                fecha1.get(Calendar.MONTH) ==
                fecha2.get(Calendar.MONTH) &&

                fecha1.get(Calendar.DAY_OF_MONTH) ==
                fecha2.get(Calendar.DAY_OF_MONTH)
    }

    // =========================================================
    // COMPROBAR SI ESTÁ DENTRO DEL RANGO
    // =========================================================

    private fun estaDentroDelRango(
        fecha: Calendar
    ): Boolean {

        if (
            fechaInicio == null ||
            fechaFin == null
        ) {
            return false
        }

        val fechaActual =
            limpiarHora(fecha)

        val inicio =
            limpiarHora(
                fechaInicio!!
            )

        val fin =
            limpiarHora(
                fechaFin!!
            )

        return fechaActual.after(inicio) &&
                fechaActual.before(fin)
    }

    // =========================================================
    // COMPROBAR FECHA PASADA
    // =========================================================

    private fun esFechaPasada(
        fecha: Calendar
    ): Boolean {

        val hoy =
            limpiarHora(
                Calendar.getInstance()
            )

        val fechaComparar =
            limpiarHora(fecha)

        return fechaComparar.before(hoy)
    }

    // =========================================================
    // LIMPIAR HORA
    // =========================================================

    private fun limpiarHora(
        fecha: Calendar
    ): Calendar {

        val resultado =
            fecha.clone() as Calendar

        resultado.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        resultado.set(
            Calendar.MINUTE,
            0
        )

        resultado.set(
            Calendar.SECOND,
            0
        )

        resultado.set(
            Calendar.MILLISECOND,
            0
        )

        return resultado
    }
}