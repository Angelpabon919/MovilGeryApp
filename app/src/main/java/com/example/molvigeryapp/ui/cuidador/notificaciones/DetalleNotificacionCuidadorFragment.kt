package com.example.molvigeryapp.ui.cuidador.notificaciones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Notificacion

class DetalleNotificacionCuidadorFragment : Fragment() {

    private lateinit var txtTipo: TextView
    private lateinit var txtTitulo: TextView
    private lateinit var txtMensaje: TextView
    private lateinit var txtFecha: TextView
    private lateinit var btnVolver: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_detalle_notificacion_cuidador, container, false)

        // Enlazamos las vistas usando findViewById según tu nuevo XML
        txtTipo = view.findViewById(R.id.txtTipoDetalleNotificacion)
        txtTitulo = view.findViewById(R.id.txtTituloDetalleNotificacion)
        txtMensaje = view.findViewById(R.id.txtMensajeDetalleNotificacion)
        txtFecha = view.findViewById(R.id.txtFechaDetalleNotificacion)
        btnVolver = view.findViewById(R.id.btnVolverDetalleNotificacion)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Funcionalidad de la flecha para volver atrás
        btnVolver.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Recuperamos el objeto Notificacion enviado desde la lista
        val notificacion = arguments?.getSerializable("notificacion") as? Notificacion

        notificacion?.let {
            txtTipo.text = it.tipo.uppercase()
            txtTitulo.text = it.titulo
            txtMensaje.text = it.detalle
            txtFecha.text = it.fecha
        }
    }
}