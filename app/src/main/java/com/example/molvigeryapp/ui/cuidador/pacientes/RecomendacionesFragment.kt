package com.example.molvigeryapp.ui.cuidador.pacientes

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R

class RecomendacionesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recomendaciones, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarInteracciones(view)
    }

    private fun configurarInteracciones(view: View) {
        // 1. Hidratar la Piel
        val btnHidratarManana = view.findViewById<TextView>(R.id.btnHidratarManana)
        val btnHidratarNoche = view.findViewById<TextView>(R.id.btnHidratarNoche)

        btnHidratarManana?.setOnClickListener { toggleSeleccionMultiple(it as TextView) }
        btnHidratarNoche?.setOnClickListener { toggleSeleccionMultiple(it as TextView) }

        // 2. Asistir Alimentación
        val btnAlimentacionManana = view.findViewById<TextView>(R.id.btnAlimentacionManana)
        val btnAlimentacionTarde = view.findViewById<TextView>(R.id.btnAlimentacionTarde)
        val btnAlimentacionNoche = view.findViewById<TextView>(R.id.btnAlimentacionNoche)
        
        val opcionesAlimentacion = listOfNotNull(btnAlimentacionManana, btnAlimentacionTarde, btnAlimentacionNoche)
        opcionesAlimentacion.forEach { opcion ->
            opcion.setOnClickListener {
                seleccionarOpcionUnica(opcionesAlimentacion, opcion)
            }
        }

        // 3. Protocolo Caídas
        view.findViewById<TextView>(R.id.btnProtocoloCaidas)?.setOnClickListener {
            Toast.makeText(requireContext(), "Protocolo consultado", Toast.LENGTH_SHORT).show()
        }

        // 4. Terapias
        val btnTerapiasFisicas = view.findViewById<TextView>(R.id.btnTerapiasFisicas)
        val btnTerapiaRespiratoria = view.findViewById<TextView>(R.id.btnTerapiaRespiratoria)
        
        btnTerapiasFisicas?.setOnClickListener { toggleSeleccionMultiple(it as TextView) }
        btnTerapiaRespiratoria?.setOnClickListener { toggleSeleccionMultiple(it as TextView) }

        // 5. Higiene Oral
        val btnHigieneManana = view.findViewById<TextView>(R.id.btnHigieneManana)
        val btnHigieneTarde = view.findViewById<TextView>(R.id.btnHigieneTarde)
        val btnHigieneNoche = view.findViewById<TextView>(R.id.btnHigieneNoche)
        
        val opcionesHigiene = listOfNotNull(btnHigieneManana, btnHigieneTarde, btnHigieneNoche)
        opcionesHigiene.forEach { opcion ->
            opcion.setOnClickListener {
                toggleSeleccionMultiple(opcion)
            }
        }
    }

    private fun seleccionarOpcionUnica(grupo: List<TextView>, seleccionada: TextView) {
        grupo.forEach { tv ->
            if (tv == seleccionada) {
                tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_selected)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_unselected)
                tv.setTextColor(Color.parseColor("#666666"))
            }
        }
    }

    private fun toggleSeleccionMultiple(tv: TextView) {
        val estaSeleccionado = tv.tag as? Boolean ?: (tv.currentTextColor == Color.WHITE)

        if (estaSeleccionado) {
            tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_unselected)
            tv.setTextColor(Color.parseColor("#666666"))
            tv.tag = false
        } else {
            tv.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_selected)
            tv.setTextColor(Color.WHITE)
            tv.tag = true
        }
    }
}