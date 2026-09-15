package com.example.molvigeryapp.ui.cuidador.pacientes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Recomendacion

class RecomendacionesAdapter(
    private var lista: List<Recomendacion> = emptyList(),
    private val onGuardarClick: ((Recomendacion) -> Unit)? = null
) : RecyclerView.Adapter<RecomendacionesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cbHidratarM: CheckBox = view.findViewById(R.id.cbHidratarM)
        val cbHidratarT: CheckBox = view.findViewById(R.id.cbHidratarT)
        val cbHidratarN: CheckBox = view.findViewById(R.id.cbHidratarN)

        val cbAlimM: CheckBox = view.findViewById(R.id.cbAlimM)
        val cbAlimT: CheckBox = view.findViewById(R.id.cbAlimT)
        val cbAlimN: CheckBox = view.findViewById(R.id.cbAlimN)
        val etViaAlimentacion: EditText = view.findViewById(R.id.etViaAlimentacion)

        val etPrevencionCaidas: EditText = view.findViewById(R.id.etPrevencionCaidas)

        val cbTerFisM: CheckBox = view.findViewById(R.id.cbTerFisM)
        val cbTerFisT: CheckBox = view.findViewById(R.id.cbTerFisT)
        val cbTerFisSegun: CheckBox = view.findViewById(R.id.cbTerFisSegun)

        val cbTerRespM: CheckBox = view.findViewById(R.id.cbTerRespM)
        val cbTerRespT: CheckBox = view.findViewById(R.id.cbTerRespT)
        val cbTerRespSegun: CheckBox = view.findViewById(R.id.cbTerRespSegun)

        val etActividadOcupacional: EditText = view.findViewById(R.id.etActividadOcupacional)

        val rgCorteUnas: RadioGroup = view.findViewById(R.id.rgCorteUnas)
        val rbUnasQuincenal: RadioButton = view.findViewById(R.id.rbUnasQuincenal)
        val rbUnasSemanal: RadioButton = view.findViewById(R.id.rbUnasSemanal)

        val rgCorteCabello: RadioGroup = view.findViewById(R.id.rgCorteCabello)
        val rbCabelloQuincenal: RadioButton = view.findViewById(R.id.rbCabelloQuincenal)
        val rbCabelloMensual: RadioButton = view.findViewById(R.id.rbCabelloMensual)

        val cbOralM: CheckBox = view.findViewById(R.id.cbOralM)
        val cbOralT: CheckBox = view.findViewById(R.id.cbOralT)
        val cbOralN: CheckBox = view.findViewById(R.id.cbOralN)

        val btnGuardar: Button = view.findViewById(R.id.btnGuardarRecomendacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recomendacion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        // HIDRATAR PIEL
        val hp = item.hidratarPiel?.uppercase() ?: ""
        holder.cbHidratarM.isChecked = hp.contains("M")
        holder.cbHidratarT.isChecked = hp.contains("T")
        holder.cbHidratarN.isChecked = hp.contains("N")

        // ASISTIR ALIMENTACIÓN
        val aa = item.asistirAlimentacion?.uppercase() ?: ""
        holder.cbAlimM.isChecked = aa.contains("M")
        holder.cbAlimT.isChecked = aa.contains("T")
        holder.cbAlimN.isChecked = aa.contains("N")
        holder.etViaAlimentacion.setText(item.viaAlimentacion ?: "")

        // PREVENCIÓN DE CAÍDAS
        holder.etPrevencionCaidas.setText(item.prevencionCaidas ?: "")

        // TERAPIAS FÍSICAS
        val tf = item.terapiasFisicas?.uppercase() ?: ""
        holder.cbTerFisM.isChecked = tf.contains("M")
        holder.cbTerFisT.isChecked = tf.contains("T")
        holder.cbTerFisSegun.isChecked = tf.contains("SEGUN")

        // TERAPIA RESPIRATORIA
        val tr = item.terapiaRespiratoria?.uppercase() ?: ""
        holder.cbTerRespM.isChecked = tr.contains("M")
        holder.cbTerRespT.isChecked = tr.contains("T")
        holder.cbTerRespSegun.isChecked = tr.contains("SEGUN")

        // ACTIVIDAD OCUPACIONAL
        holder.etActividadOcupacional.setText(item.actividadOcupacional ?: "")

        // CORTE DE UÑAS
        val cu = item.corteUnas?.uppercase() ?: ""
        if (cu.contains("QUINCENAL")) holder.rbUnasQuincenal.isChecked = true
        else if (cu.contains("SEMANAL")) holder.rbUnasSemanal.isChecked = true

        // CORTE DE CABELLO
        val cc = item.corteCabello?.uppercase() ?: ""
        if (cc.contains("QUINCENAL")) holder.rbCabelloQuincenal.isChecked = true
        else if (cc.contains("MENSUAL")) holder.rbCabelloMensual.isChecked = true

        // HIGIENE ORAL
        val ho = item.higieneOral?.uppercase() ?: ""
        holder.cbOralM.isChecked = ho.contains("M")
        holder.cbOralT.isChecked = ho.contains("T")
        holder.cbOralN.isChecked = ho.contains("N")

        holder.btnGuardar.setOnClickListener {
            // Helpers para construir los strings a guardar
            fun buildMTN(m: Boolean, t: Boolean, n: Boolean): String {
                val list = mutableListOf<String>()
                if (m) list.add("M")
                if (t) list.add("T")
                if (n) list.add("N")
                return list.joinToString(", ")
            }

            fun buildSegun(m: Boolean, t: Boolean, segun: Boolean): String {
                val list = mutableListOf<String>()
                if (m) list.add("M")
                if (t) list.add("T")
                if (segun) list.add("SEGÚN NECESIDAD")
                return list.joinToString(", ")
            }

            // Crear el objeto con lo que seleccionó el usuario
            val recomendacionEditada = item.copy(
                hidratarPiel = buildMTN(holder.cbHidratarM.isChecked, holder.cbHidratarT.isChecked, holder.cbHidratarN.isChecked),
                asistirAlimentacion = buildMTN(holder.cbAlimM.isChecked, holder.cbAlimT.isChecked, holder.cbAlimN.isChecked),
                viaAlimentacion = holder.etViaAlimentacion.text.toString(),
                prevencionCaidas = holder.etPrevencionCaidas.text.toString(),
                terapiasFisicas = buildSegun(holder.cbTerFisM.isChecked, holder.cbTerFisT.isChecked, holder.cbTerFisSegun.isChecked),
                terapiaRespiratoria = buildSegun(holder.cbTerRespM.isChecked, holder.cbTerRespT.isChecked, holder.cbTerRespSegun.isChecked),
                actividadOcupacional = holder.etActividadOcupacional.text.toString(),
                corteUnas = if (holder.rbUnasQuincenal.isChecked) "Quincenal" else if (holder.rbUnasSemanal.isChecked) "Semanal" else null,
                corteCabello = if (holder.rbCabelloQuincenal.isChecked) "Quincenal" else if (holder.rbCabelloMensual.isChecked) "Mensual" else null,
                higieneOral = buildMTN(holder.cbOralM.isChecked, holder.cbOralT.isChecked, holder.cbOralN.isChecked)
            )

            // 1. Notificar callback para guardar en BD/Backend
            onGuardarClick?.invoke(recomendacionEditada)

            // 2. Limpiar todos los elementos visuales inmediatamente
            limpiarCampos(holder)

            Toast.makeText(holder.itemView.context, "Recomendación guardada exitosamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun limpiarCampos(holder: ViewHolder) {
        holder.cbHidratarM.isChecked = false
        holder.cbHidratarT.isChecked = false
        holder.cbHidratarN.isChecked = false

        holder.cbAlimM.isChecked = false
        holder.cbAlimT.isChecked = false
        holder.cbAlimN.isChecked = false
        holder.etViaAlimentacion.setText("")

        holder.etPrevencionCaidas.setText("")

        holder.cbTerFisM.isChecked = false
        holder.cbTerFisT.isChecked = false
        holder.cbTerFisSegun.isChecked = false

        holder.cbTerRespM.isChecked = false
        holder.cbTerRespT.isChecked = false
        holder.cbTerRespSegun.isChecked = false

        holder.etActividadOcupacional.setText("")

        holder.rgCorteUnas.clearCheck()
        holder.rgCorteCabello.clearCheck()

        holder.cbOralM.isChecked = false
        holder.cbOralT.isChecked = false
        holder.cbOralN.isChecked = false
    }

    override fun getItemCount(): Int = lista.size

    fun actualizarLista(nuevaLista: List<Recomendacion>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}