package com.example.molvigeryapp.ui.cuidador.camara

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.EventoIa
import com.example.molvigeryapp.ui.cuidador.notificaciones.NotificacionesFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.HomeFragment

class EventosCamaraFragment : Fragment() {

    private val viewModel: EventoIaViewModel by viewModels()

    private lateinit var adapter: EventoIaAdapter

    private lateinit var recyclerEventos: RecyclerView
    private lateinit var tvSinEventos: TextView
    private lateinit var progressEventos: View

    private var listaCompleta = listOf<EventoIa>()
    private var primeraConsultaFinalizada = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_eventos_camara,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        inicializarVistas(view)
        configurarRecyclerView()
        configurarBotones(view)
        configurarBotonNotificaciones(view)
        observarDatos()

        viewModel.obtenerTiposEventos()
        viewModel.obtenerPacientes()
        viewModel.obtenerCamaras()
        viewModel.obtenerHabitaciones()
    }

    private fun inicializarVistas(view: View) {
        recyclerEventos = view.findViewById(R.id.recyclerEventos)
        tvSinEventos = view.findViewById(R.id.tvSinEventos)
        progressEventos = view.findViewById(R.id.progressEventos)

        tvSinEventos.visibility = View.GONE
    }

    private fun configurarRecyclerView() {
        adapter = EventoIaAdapter { evento ->

            val detalleFragment =
                DetalleEventoFragment.newInstance(evento.id_evento)

            parentFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainer, detalleFragment)
                .addToBackStack("eventos_camara")
                .commit()
        }

        recyclerEventos.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerEventos.adapter = adapter
    }

    private fun configurarBotones(view: View) {
        view.findViewById<View>(R.id.btnVolverEventos)
            ?.setOnClickListener {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeFragment()
                    )
                    .commit()
            }
    }

    // Abrir notificaciones desde la campanita
    private fun configurarBotonNotificaciones(view: View) {
        view.findViewById<View>(R.id.btnNotificationCamara)
            ?.setOnClickListener {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        NotificacionesFragment()
                    )
                    .addToBackStack("notificaciones_cuidador")
                    .commit()
            }
    }

    private fun observarDatos() {

        viewModel.eventos.observe(viewLifecycleOwner) { eventos ->
            listaCompleta = eventos
            adapter.actualizarEventos(eventos)
            actualizarEstadoVacio()
        }

        viewModel.tiposEventos.observe(viewLifecycleOwner) { tipos ->
            adapter.actualizarTiposEventos(tipos)
        }

        viewModel.pacientes.observe(viewLifecycleOwner) { pacientes ->
            adapter.actualizarPacientes(pacientes)
        }

        viewModel.camaras.observe(viewLifecycleOwner) { camaras ->
            adapter.actualizarCamaras(camaras)
        }

        viewModel.habitaciones.observe(viewLifecycleOwner) { habitaciones ->
            adapter.actualizarHabitaciones(habitaciones)
        }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->

            if (!cargando) {
                primeraConsultaFinalizada = true
            }

            progressEventos.visibility =
                if (cargando && listaCompleta.isEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            actualizarEstadoVacio()
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(
                    requireContext(),
                    error,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun actualizarEstadoVacio() {
        val sinEventos =
            primeraConsultaFinalizada && listaCompleta.isEmpty()

        tvSinEventos.visibility =
            if (sinEventos) View.VISIBLE else View.GONE
    }

    override fun onStart() {
        super.onStart()
        viewModel.iniciarActualizacionAutomatica()
    }

    override fun onStop() {
        viewModel.detenerActualizacionAutomatica()
        super.onStop()
    }

    override fun onDestroyView() {
        recyclerEventos.adapter = null
        super.onDestroyView()
    }
}