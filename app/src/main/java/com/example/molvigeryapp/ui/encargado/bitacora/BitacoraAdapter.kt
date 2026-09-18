package com.example.molvigeryapp.ui.encargado.bitacora

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemEventoBitacoraEncargadoBinding
import java.text.SimpleDateFormat
import java.util.Locale

class BitacoraAdapter(
    private var listaBitacoras: List<Bitacora>,
    private var listaPacientes: List<Paciente>
) : RecyclerView.Adapter<BitacoraAdapter.BitacoraViewHolder>() {

    // =========================================================
    // VIEW HOLDER
    // =========================================================

    inner class BitacoraViewHolder(
        private val binding: ItemEventoBitacoraEncargadoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(bitacora: Bitacora) {

            // -------------------------------------------------
            // TIPO DE REGISTRO
            // -------------------------------------------------

            binding.txtTipoEventoBitacora.text =
                obtenerTituloRegistro(bitacora.tipoRegistro)

            // -------------------------------------------------
            // DESCRIPCIÓN
            // -------------------------------------------------

            binding.txtDescripcionEventoBitacora.text =
                bitacora.descripcion.ifBlank {
                    "Sin descripción"
                }

            // -------------------------------------------------
            // PACIENTE
            // -------------------------------------------------

            val paciente = obtenerPaciente(
                bitacora.idPaciente
            )

            val nombrePaciente = paciente?.let {
                "${it.nombre} ${it.apellido}"
            } ?: "Paciente no encontrado"

            binding.txtPacienteEventoBitacora.text =
                "Paciente: $nombrePaciente"

            // -------------------------------------------------
            // FECHA Y HORA
            // -------------------------------------------------

            binding.txtFechaEventoBitacora.text =
                formatearFecha(bitacora.fechaHora)

            // -------------------------------------------------
            // ESTADO
            // -------------------------------------------------

            if (bitacora.estado) {

                binding.iconoEstadoBitacora
                    .setImageResource(R.drawable.check_circle)

                binding.txtEstadoEventoBitacora.text =
                    "Actividad registrada"

            } else {

                binding.iconoEstadoBitacora
                    .setImageResource(R.drawable.check_circle)

                binding.txtEstadoEventoBitacora.text =
                    "Registro inactivo"
            }
        }
    }

    // =========================================================
    // CREAR VIEW HOLDER
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BitacoraViewHolder {

        val binding =
            ItemEventoBitacoraEncargadoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )

        return BitacoraViewHolder(binding)
    }

    // =========================================================
    // VINCULAR DATOS
    // =========================================================

    override fun onBindViewHolder(
        holder: BitacoraViewHolder,
        position: Int
    ) {

        holder.bind(
            listaBitacoras[position]
        )
    }

    // =========================================================
    // CANTIDAD DE ELEMENTOS
    // =========================================================

    override fun getItemCount(): Int =
        listaBitacoras.size

    // =========================================================
    // ACTUALIZAR DATOS
    // =========================================================

    fun actualizarDatos(
        nuevasBitacoras: List<Bitacora>,
        nuevosPacientes: List<Paciente>
    ) {

        listaBitacoras = nuevasBitacoras
        listaPacientes = nuevosPacientes

        notifyDataSetChanged()
    }

    // =========================================================
    // BUSCAR PACIENTE
    // =========================================================

    private fun obtenerPaciente(
        idPaciente: Int?
    ): Paciente? {

        if (idPaciente == null) {
            return null
        }

        return listaPacientes.find {
            it.idPaciente == idPaciente
        }
    }

    // =========================================================
    // TÍTULO DEL REGISTRO
    // =========================================================

    private fun obtenerTituloRegistro(
        tipoRegistro: String
    ): String {

        return when {

            tipoRegistro.equals(
                "actividad",
                ignoreCase = true
            ) -> "Registro de actividad"

            tipoRegistro.equals(
                "signos vitales",
                ignoreCase = true
            ) -> "Signos vitales"

            tipoRegistro.equals(
                "evento adverso",
                ignoreCase = true
            ) -> "Evento adverso"

            else -> {

                if (tipoRegistro.isBlank()) {
                    "Registro de bitácora"
                } else {
                    tipoRegistro
                }
            }
        }
    }

    // =========================================================
    // FORMATEAR FECHA
    // =========================================================

    private fun formatearFecha(
        fechaOriginal: String
    ): String {

        if (fechaOriginal.isBlank()) {
            return "Fecha no disponible"
        }

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.getDefault()
                )

            val formatoSalida =
                SimpleDateFormat(
                    "dd/MM/yyyy · hh:mm a",
                    Locale.getDefault()
                )

            val fecha =
                formatoEntrada.parse(fechaOriginal)

            fecha?.let {
                formatoSalida.format(it)
            } ?: fechaOriginal

        } catch (e: Exception) {

            fechaOriginal
        }
    }
}