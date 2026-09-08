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

class CardexFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cardex, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarInteracciones(view)
    }

    private fun configurarInteracciones(view: View) {
        // Opciones de Baño
        val btnBanoCama = view.findViewById<TextView>(R.id.btnBanoCama)
        val btnBanoDucha = view.findViewById<TextView>(R.id.btnBanoDucha)
        val btnBanoSilla = view.findViewById<TextView>(R.id.btnBanoSilla)

        val opcionesBano = listOfNotNull(btnBanoCama, btnBanoDucha, btnBanoSilla)
        opcionesBano.forEach { opcion ->
            opcion.setOnClickListener {
                seleccionarOpcionUnica(opcionesBano, opcion)
                Toast.makeText(
                    requireContext(),
                    "Baño actualizado: ${opcion.text}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Opciones de Deposición
        val btnDepManana = view.findViewById<TextView>(R.id.btnDepManana)
        val btnDepTarde = view.findViewById<TextView>(R.id.btnDepTarde)
        val btnDepNoche = view.findViewById<TextView>(R.id.btnDepNoche)

        val opcionesDeposicion = listOfNotNull(btnDepManana, btnDepTarde, btnDepNoche)
        opcionesDeposicion.forEach { opcion ->
            opcion.setOnClickListener {
                seleccionarOpcionUnica(opcionesDeposicion, opcion)
            }
        }
    }



    private fun seleccionarOpcionUnica(grupo: List<TextView>, seleccionada: TextView) {
        grupo.forEach { tv ->
            if (tv == seleccionada) {
                tv.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_selected)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_unselected)
                tv.setTextColor(Color.parseColor("#666666"))
            }
        }

    }
}