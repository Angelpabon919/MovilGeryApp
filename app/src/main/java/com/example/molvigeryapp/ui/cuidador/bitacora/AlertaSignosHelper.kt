package com.example.molvigeryapp.ui.cuidador.bitacora

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.google.android.material.button.MaterialButton

private data class ItemAlertaSigno(
    val titulo: String,
    val valorMedido: String? = null,
    val referencia: String? = null
)

/**
 * Muestra un diálogo modal cuando el backend detecta signos vitales
 * fuera de los rangos personalizados del paciente.
 */
internal fun Fragment.mostrarAlertaClinicaSignos(
    mensajeAlerta: String?,
    alConfirmar: () -> Unit
) {
    if (!isAdded) return

    val context = requireContext()
    val inflater = LayoutInflater.from(context)
    val vistaDialogo = inflater.inflate(R.layout.dialog_alerta_signos_vitales, null)

    val svLista = vistaDialogo.findViewById<ScrollView>(R.id.svListaAlertasSignos)
    val llLista = vistaDialogo.findViewById<LinearLayout>(R.id.llListaAlertasSignos)
    val btnEntendido = vistaDialogo.findViewById<MaterialButton>(R.id.btnEntendidoAlerta)

    val mensaje = mensajeAlerta?.takeIf { it.isNotBlank() }
        ?: "Se detectaron signos vitales fuera de los rangos normales del paciente."

    val items = parsearAlertasSignos(mensaje)

    items.forEachIndexed { index, item ->
        val fila = inflater.inflate(R.layout.item_alerta_signo_vital, llLista, false)
        val tvTitulo = fila.findViewById<TextView>(R.id.tvTituloSignoAlerta)
        val tvDetalle = fila.findViewById<TextView>(R.id.tvDetalleSignoAlerta)

        tvTitulo.text = item.titulo

        if (item.valorMedido != null || item.referencia != null) {
            tvDetalle.visibility = View.VISIBLE
            tvDetalle.text = construirDetalleEstilizado(item.valorMedido, item.referencia)
        } else {
            tvDetalle.visibility = View.GONE
        }

        if (index == items.lastIndex) {
            (fila.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = 0
        }

        llLista.addView(fila)
    }

    val dialog = AlertDialog.Builder(context)
        .setView(vistaDialogo)
        .setCancelable(false)
        .create()

    btnEntendido.setOnClickListener {
        dialog.dismiss()
        if (isAdded) {
            alConfirmar()
        }
    }

    dialog.setOnShowListener {
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val alturaPantalla = resources.displayMetrics.heightPixels
        val maxAlturaLista = (alturaPantalla * 0.45f).toInt()
        svLista.post {
            if (svLista.height > maxAlturaLista) {
                svLista.layoutParams = svLista.layoutParams.apply {
                    height = maxAlturaLista
                }
                svLista.requestLayout()
            }
        }
    }

    dialog.show()
}

private fun parsearAlertasSignos(mensajeRaw: String): List<ItemAlertaSigno> {
    var cuerpo = mensajeRaw.trim()

    val idxDosPuntos = cuerpo.indexOf(':')
    if (idxDosPuntos != -1) {
        val prefijo = cuerpo.substring(0, idxDosPuntos).trim()
        if (prefijo.startsWith("ALERTA", ignoreCase = true) ||
            prefijo.contains("fuera de rango", ignoreCase = true)
        ) {
            cuerpo = cuerpo.substring(idxDosPuntos + 1).trim()
        }
    }

    cuerpo = cuerpo.trimEnd('.')

    val segmentos = dividirFueraDeParentesis(cuerpo)
        .map { it.trim().trimEnd('.') }
        .filter { it.isNotEmpty() }

    if (segmentos.isEmpty()) {
        return listOf(ItemAlertaSigno(titulo = mensajeRaw.trim()))
    }

    val patronDetalle = Regex("""^(.+?)\s*\((.+)\)$""")

    return segmentos.map { segmento ->
        val coincidencia = patronDetalle.matchEntire(segmento)
        if (coincidencia != null) {
            val titulo = coincidencia.groupValues[1].trim()
            val detalleInterior = coincidencia.groupValues[2].trim()
            val partes = detalleInterior.split(';').map { it.trim() }.filter { it.isNotEmpty() }

            when {
                partes.size >= 2 -> {
                    val medido = partes[0]
                    val ref = partes.drop(1).joinToString(" · ") { capitalizarPrimera(it) }
                    ItemAlertaSigno(titulo = titulo, valorMedido = medido, referencia = ref)
                }
                partes.size == 1 -> {
                    ItemAlertaSigno(titulo = titulo, valorMedido = partes[0])
                }
                else -> ItemAlertaSigno(titulo = segmento)
            }
        } else {
            ItemAlertaSigno(titulo = segmento)
        }
    }
}

private fun dividirFueraDeParentesis(texto: String): List<String> {
    val resultado = mutableListOf<String>()
    val actual = StringBuilder()
    var profundidad = 0

    for (c in texto) {
        when (c) {
            '(' -> {
                profundidad++
                actual.append(c)
            }
            ')' -> {
                if (profundidad > 0) profundidad--
                actual.append(c)
            }
            ';', '\n' -> {
                if (profundidad == 0) {
                    resultado.add(actual.toString())
                    actual.clear()
                } else {
                    actual.append(c)
                }
            }
            else -> actual.append(c)
        }
    }

    if (actual.isNotEmpty()) {
        resultado.add(actual.toString())
    }

    return resultado
}

private fun capitalizarPrimera(texto: String): String {
    if (texto.isEmpty()) return texto
    return texto.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

private fun construirDetalleEstilizado(
    valorMedido: String?,
    referencia: String?
): CharSequence {
    val builder = SpannableStringBuilder()

    if (!valorMedido.isNullOrBlank()) {
        val inicio = builder.length
        builder.append(valorMedido)
        builder.setSpan(
            StyleSpan(Typeface.BOLD),
            inicio,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        builder.setSpan(
            ForegroundColorSpan(Color.parseColor("#C62828")),
            inicio,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    if (!referencia.isNullOrBlank()) {
        if (builder.isNotEmpty()) {
            builder.append("  •  ")
        }
        builder.append(referencia)
    }

    return builder
}
