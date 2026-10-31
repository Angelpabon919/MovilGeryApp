package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.ItemPacienteBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class PacienteAdapter(
    private var listaPacientes: List<Paciente> = emptyList(),
    private val mostrarCheckbox: Boolean = true,
    private val onSeleccionCambiada: ((totalSeleccionados: Int) -> Unit)? = null
) : RecyclerView.Adapter<PacienteAdapter.PacienteViewHolder>() {

    // Mantiene todos los pacientes aunque se aplique un filtro.
    private var listaCompletaPacientes: List<Paciente> = listaPacientes

    inner class PacienteViewHolder(
        private val binding: ItemPacienteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) = with(binding) {

            // NOMBRE COMPLETO
            val nombreCompleto =
                "${paciente.nombre} ${paciente.apellido}".trim()

            tvNombrePaciente.text = nombreCompleto.ifBlank {
                "Paciente sin nombre"
            }

            // HABITACIÓN, CAMA Y EDAD
            val detalles = mutableListOf<String>()

            detalles.add(
                "Habitación ${paciente.habitacion ?: "--"}"
            )

            paciente.cama?.let { cama ->
                detalles.add("Cama $cama")
            }

            calcularEdad(paciente.fechaNacimiento)?.let { edad ->
                detalles.add("$edad años")
            }

            tvDetallesPaciente.text = detalles.joinToString("  •  ")

            // FOTOGRAFÍA DEL PACIENTE
            if (!paciente.foto.isNullOrBlank()) {
                imgAvatar.imageTintList = null

                Glide.with(itemView.context)
                    .load(paciente.foto)
                    .placeholder(R.drawable.person_icon)
                    .error(R.drawable.person_icon)
                    .centerCrop()
                    .into(imgAvatar)
            } else {
                Glide.with(itemView.context).clear(imgAvatar)

                imgAvatar.setImageResource(R.drawable.person_icon)
                imgAvatar.imageTintList = ColorStateList.valueOf(
                    Color.parseColor("#64748B")
                )
            }

            // EPS: SOLO SE MUESTRA SI EXISTE INFORMACIÓN
            val eps = paciente.eps?.trim().orEmpty()

            tvEtiquetaCuidado.isVisible = eps.isNotBlank()

            if (eps.isNotBlank()) {
                tvEtiquetaCuidado.text = "EPS: $eps"
            } else {
                tvEtiquetaCuidado.text = ""
            }

            // ESTADO DEL PACIENTE
            tvEtiquetaRiesgo.isVisible = true

            tvEtiquetaRiesgo.text = if (paciente.estado) {
                "Paciente activo"
            } else {
                "Paciente inactivo"
            }

            tvEtiquetaRiesgo.setBackgroundColor(
                Color.parseColor(
                    if (paciente.estado) "#F5E8D5" else "#E9ECEF"
                )
            )

            tvEtiquetaRiesgo.setTextColor(
                Color.parseColor("#475569")
            )

            layoutEtiquetasCuidado.isVisible = true

            // ESTILO VISUAL DE LA TARJETA
            actualizarEstiloSeleccion(paciente.isSelected)

            // CONFIGURACIÓN DEL CHECKBOX
            cbSeleccionado.visibility = if (mostrarCheckbox) {
                View.VISIBLE
            } else {
                View.GONE
            }

            cbSeleccionado.setOnCheckedChangeListener(null)
            cbSeleccionado.isChecked = paciente.isSelected

            if (mostrarCheckbox) {

                val cambiarSeleccion = View.OnClickListener {
                    val posicion = bindingAdapterPosition

                    if (posicion != RecyclerView.NO_POSITION) {
                        val pacienteActual = listaPacientes.getOrNull(posicion)
                            ?: return@OnClickListener

                        pacienteActual.isSelected =
                            !pacienteActual.isSelected

                        cbSeleccionado.setOnCheckedChangeListener(null)
                        cbSeleccionado.isChecked =
                            pacienteActual.isSelected

                        actualizarEstiloSeleccion(
                            pacienteActual.isSelected
                        )

                        onSeleccionCambiada?.invoke(
                            obtenerCantidadSeleccionados()
                        )
                    }
                }

                itemView.setOnClickListener(cambiarSeleccion)
                cbSeleccionado.setOnClickListener(cambiarSeleccion)

            } else {
                itemView.setOnClickListener(null)
                cbSeleccionado.setOnClickListener(null)
                cbSeleccionado.setOnCheckedChangeListener(null)
            }
        }

        private fun actualizarEstiloSeleccion(seleccionado: Boolean) {
            val tarjeta = binding.cardPaciente

            tarjeta.setCardBackgroundColor(
                Color.parseColor(
                    if (seleccionado) "#EDF3FF" else "#FFFFFF"
                )
            )

            tarjeta.strokeWidth = dpApx(
                if (seleccionado) 2 else 1
            )

            tarjeta.strokeColor = Color.parseColor(
                if (seleccionado) "#3B5BDB" else "#E9ECEF"
            )
        }

        private fun dpApx(dp: Int): Int {
            return (
                    dp * itemView.resources.displayMetrics.density
                    ).toInt()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PacienteViewHolder {
        val binding = ItemPacienteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PacienteViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PacienteViewHolder,
        position: Int
    ) {
        holder.bind(listaPacientes[position])
    }

    override fun getItemCount(): Int = listaPacientes.size

    /**
     * Actualiza la lista completa cuando llegan datos nuevos de la API.
     */
    fun actualizarLista(nuevaLista: List<Paciente>) {
        listaCompletaPacientes = nuevaLista
        listaPacientes = nuevaLista
        notifyDataSetChanged()
    }

    /**
     * Cambia únicamente las tarjetas visibles, sin perder
     * las selecciones de los pacientes ocultos por el filtro.
     */
    fun mostrarListaFiltrada(listaFiltrada: List<Paciente>) {
        listaPacientes = listaFiltrada
        notifyDataSetChanged()
    }

    /**
     * Cuenta las selecciones en la lista completa,
     * no solamente en las tarjetas visibles.
     */
    fun obtenerCantidadSeleccionados(): Int {
        return listaCompletaPacientes.count { paciente ->
            paciente.isSelected
        }
    }

    /**
     * Devuelve los identificadores de todos los pacientes seleccionados.
     */
    fun obtenerIdsSeleccionados(): Set<Int> {
        return listaCompletaPacientes
            .filter { paciente -> paciente.isSelected }
            .mapNotNull { paciente -> paciente.idPaciente }
            .toSet()
    }

    private fun calcularEdad(fechaNacimiento: String?): Int? {
        if (fechaNacimiento.isNullOrBlank()) {
            return null
        }

        val formatos = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )

        val fecha = formatos.firstNotNullOfOrNull { formato ->
            try {
                SimpleDateFormat(
                    formato,
                    Locale.US
                ).apply {
                    isLenient = false
                }.parse(fechaNacimiento.trim())
            } catch (_: Exception) {
                null
            }
        } ?: return null

        return calcularEdadDesdeFecha(fecha)
    }

    private fun calcularEdadDesdeFecha(
        fechaNacimiento: Date
    ): Int? {
        val nacimiento = Calendar.getInstance().apply {
            time = fechaNacimiento
        }

        val hoy = Calendar.getInstance()

        var edad = hoy.get(Calendar.YEAR) -
                nacimiento.get(Calendar.YEAR)

        val aunNoCumpleEsteAno =
            hoy.get(Calendar.DAY_OF_YEAR) <
                    nacimiento.get(Calendar.DAY_OF_YEAR)

        if (aunNoCumpleEsteAno) {
            edad--
        }

        return edad.takeIf { it in 0..125 }
    }
}
