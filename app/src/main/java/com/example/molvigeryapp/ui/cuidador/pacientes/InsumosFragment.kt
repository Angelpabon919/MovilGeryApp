package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R

class InsumosFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_insumos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configurarBotones(view)
    }

    private fun configurarBotones(view: View) {
        view.findViewById<TextView>(R.id.btnPedirPanales)?.setOnClickListener {
            mostrarDialogoSolicitud("Pañales Talla L")
        }

        view.findViewById<TextView>(R.id.btnPedirGuantes)?.setOnClickListener {
            mostrarDialogoSolicitud("Jabon de baño")
        }

        view.findViewById<TextView>(R.id.btnPedirCrema)?.setOnClickListener {
            mostrarDialogoSolicitud("Crema Anti-Escaras")
        }

        view.findViewById<TextView>(R.id.btnPedirPanitos)?.setOnClickListener {
            mostrarDialogoSolicitud("Pañitos Húmedos (Paquete)")
        }
    }

    private fun mostrarDialogoSolicitud(nombreInsumo: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Solicitar Insumo")
        builder.setMessage("¿Deseas enviar un reporte para solicitar reabastecimiento de $nombreInsumo?")

        builder.setPositiveButton("Enviar Solicitud") { dialog, _ ->
            Toast.makeText(requireContext(), "Solicitud enviada para $nombreInsumo", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

}