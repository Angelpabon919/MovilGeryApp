
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
import com.example.molvigeryapp.ui.cuidador.pacientes.HomeFragment

class EventosCamaraFragment : Fragment() {

    // =====================================================
    // VIEWMODEL Y ADAPTER
    // =====================================================

    private val viewModel: EventoIaViewModel by viewModels()

    private lateinit var adapter: EventoIaAdapter

    // =====================================================
    // COMPONENTES DE LA PANTALLA
    // =====================================================

    private lateinit var recyclerEventos: RecyclerView
    private lateinit var tvSinEventos: TextView
    private lateinit var progressEventos: View

    private var listaCompleta = listOf<EventoIa>()

    // Permite distinguir entre una lista todavía no
    // consultada y una respuesta exitosa sin eventos.
    private var primeraConsultaFinalizada = false

    // =====================================================
    // CREAR VISTA
    // =====================================================

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

    // =====================================================
    // CONFIGURAR PANTALLA
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        inicializarVistas(view)
        configurarRecyclerView()
        configurarBotones(view)
        observarDatos()

        // Obtener nombres, descripciones y riesgos
        // desde tipos_evento_ia/
        viewModel.obtenerTiposEventos()
        viewModel.obtenerPacientes()
        viewModel.obtenerCamaras()
        viewModel.obtenerHabitaciones()

    }

    // =====================================================
    // INICIALIZAR COMPONENTES
    // =====================================================

    private fun inicializarVistas(view: View) {

        recyclerEventos =
            view.findViewById(R.id.recyclerEventos)

        tvSinEventos =
            view.findViewById(R.id.tvSinEventos)

        progressEventos =
            view.findViewById(R.id.progressEventos)

        tvSinEventos.visibility = View.GONE
    }

    // =====================================================
    // CONFIGURAR RECYCLERVIEW
    // =====================================================

    private fun configurarRecyclerView() {

        adapter = EventoIaAdapter { evento ->

            // Abrir el detalle del evento seleccionado
            val detalleFragment =
                DetalleEventoFragment.newInstance(
                    evento.id_evento
                )

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    detalleFragment
                )
                .addToBackStack("eventos_camara")
                .commit()
        }

        recyclerEventos.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerEventos.adapter = adapter
    }

    // =====================================================
    // CONFIGURAR BOTÓN VOLVER
    // =====================================================

    private fun configurarBotones(view: View) {

        view.findViewById<View>(
            R.id.btnVolverEventos
        )?.setOnClickListener {

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    HomeFragment()
                )
                .commit()
        }
    }

    // =====================================================
    // OBSERVAR DATOS DEL BACKEND
    // =====================================================

    private fun observarDatos() {

        // -------------------------------------------------
        // EVENTOS DE INTELIGENCIA ARTIFICIAL
        // -------------------------------------------------

        viewModel.eventos.observe(viewLifecycleOwner) { eventos ->

            listaCompleta = eventos

            // Actualizar las tarjetas con los eventos reales
            adapter.actualizarEventos(eventos)

            // No mostrar "Sin eventos" antes de completar
            // la primera consulta.
            actualizarEstadoVacio()
        }

        // -------------------------------------------------
        // TIPOS DE EVENTOS
        // -------------------------------------------------

        viewModel.tiposEventos.observe(viewLifecycleOwner) { tipos ->

            // Relacionar cada evento con su nombre
            // y nivel de riesgo obtenidos desde la API
            adapter.actualizarTiposEventos(tipos)
        }
        // -------------------------------------------------
        // NUEVO: PACIENTES
        // -------------------------------------------------

        viewModel.pacientes.observe(viewLifecycleOwner) { pacientes ->

            // Relacionaremos id_paciente con nombre y apellido
            adapter.actualizarPacientes(pacientes)
        }
        viewModel.camaras.observe(viewLifecycleOwner) { camaras ->

            adapter.actualizarCamaras(camaras)
        }
        viewModel.habitaciones.observe(viewLifecycleOwner) { habitaciones ->

            adapter.actualizarHabitaciones(habitaciones)
        }
        // -------------------------------------------------
        // INDICADOR DE CARGA
        // -------------------------------------------------

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

        // -------------------------------------------------
        // ERRORES DE CONEXIÓN
        // -------------------------------------------------

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

    // =====================================================
    // MOSTRAR MENSAJE CUANDO NO HAY EVENTOS
    // =====================================================

    private fun actualizarEstadoVacio() {

        val sinEventos =
            primeraConsultaFinalizada && listaCompleta.isEmpty()

        tvSinEventos.visibility =
            if (sinEventos) {
                View.VISIBLE
            } else {
                View.GONE
            }
    }
    // =====================================================
    // INICIAR ACTUALIZACIÓN AUTOMÁTICA
    // =====================================================

    override fun onStart() {
        super.onStart()

        // Consultar eventos periódicamente mientras
        // la pantalla esté activa
        viewModel.iniciarActualizacionAutomatica()
    }

    // =====================================================
    // DETENER ACTUALIZACIÓN AUTOMÁTICA
    // =====================================================

    override fun onStop() {

        // Evitar consultas innecesarias cuando el
        // cuidador abandone la pantalla
        viewModel.detenerActualizacionAutomatica()

        super.onStop()
    }

    // =====================================================
    // LIMPIAR REFERENCIA DEL RECYCLERVIEW
    // =====================================================

    override fun onDestroyView() {

        recyclerEventos.adapter = null

        super.onDestroyView()
    }
}
