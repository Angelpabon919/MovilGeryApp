package com.example.molvigeryapp.ui.encargado.notificaciones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.databinding.FragmentNotificacionesEncargadoBinding

class NotificacionesEncargadoFragment : Fragment() {

    private var _binding: FragmentNotificacionesEncargadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: NotificacionAdapter

    private val listaNotificaciones = listOf(

        Notificacion(
            tipo = "Evento adverso",
            titulo = "Caída de paciente",
            detalle = "Rosa Martínez",
            fecha = "Hace 15 minutos",
            icono = R.drawable.img,
            leida = false
        ),

        Notificacion(
            tipo = "Stock bajo",
            titulo = "Medicamento próximo a agotarse",
            detalle = "Donepezilo · 4 unidades",
            fecha = "Hace 1 hora",
            icono = R.drawable.img,
            leida = false
        ),

        Notificacion(
            tipo = "Nueva cita médica",
            titulo = "Cita programada",
            detalle = "Carlos Rodríguez · Hoy 16:00",
            fecha = "Hace 2 horas",
            icono = R.drawable.img,
            leida = false
        ),

        Notificacion(
            tipo = "Nueva bitácora",
            titulo = "Se registró una nueva bitácora",
            detalle = "Cuidador: Javier Ríos",
            fecha = "Ayer · 18:30",
            icono = R.drawable.img,
            leida = true
        ),

        Notificacion(
            tipo = "Turno",
            titulo = "Turno pendiente de revisión",
            detalle = "Ana Suárez · Mañana 06:00",
            fecha = "Ayer · 16:20",
            icono = R.drawable.img,
            leida = true
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentNotificacionesEncargadoBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()
        configurarBotonVolver()
        actualizarCantidadNoLeidas()
    }

    private fun configurarRecyclerView() {

        adapter = NotificacionAdapter(
            listaNotificaciones
        ) { notificacion ->

            abrirNotificacion(notificacion)
        }

        binding.recyclerNotificaciones.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerNotificaciones.adapter =
            adapter
    }

    private fun configurarBotonVolver() {

        binding.btnVolverNotificaciones.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    private fun actualizarCantidadNoLeidas() {

        val cantidad = listaNotificaciones.count {
            !it.leida
        }

        binding.txtCantidadNoLeidas.text =
            cantidad.toString()
    }

    private fun abrirNotificacion(
        notificacion: Notificacion
    ) {

        when (notificacion.tipo) {

            "Evento adverso" -> {

                // Posteriormente abriremos
                // DetalleEventoAdversoEncargadoFragment
            }

            "Stock bajo" -> {

                // Posteriormente abriremos
                // detalle de medicamentos
            }

            "Nueva cita médica" -> {

                // Posteriormente abriremos
                // detalle de la cita
            }

            "Nueva bitácora" -> {

                // Posteriormente abriremos
                // detalle de bitácora
            }

            "Turno" -> {

                // Posteriormente abriremos
                // detalle del turno
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}