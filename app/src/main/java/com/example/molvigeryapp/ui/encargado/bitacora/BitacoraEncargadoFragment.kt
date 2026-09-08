package com.example.molvigeryapp.ui.encargado.bitacora

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.data.model.EstadoEvento
import com.example.molvigeryapp.data.model.EventoBitacora
import com.example.molvigeryapp.data.model.TipoEvento
import com.example.molvigeryapp.databinding.FragmentBitacoraEncargadoBinding
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment

class BitacoraEncargadoFragment : Fragment() {

    private var _binding: FragmentBitacoraEncargadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EventoBitacoraAdapter

    private val listaEventos = listOf(

        EventoBitacora(
            id = 1,
            tipo = TipoEvento.EVENTO_ADVERSO,
            paciente = "Marta Galindo",
            descripcion = "Caída durante su actividad de rutina",
            cuidador = "Marlon Ocoro",
            fecha = "07/09/2026",
            hora = "08:35 AM",
            estado = EstadoEvento.PENDIENTE,
            observaciones = "Se recomienda continuar observando al paciente y registrar cualquier cambio en su estado."
        ),

        EventoBitacora(
            id = 2,
            tipo = TipoEvento.ACTIVIDAD,
            paciente = "Cuarguil Martínez",
            descripcion = "Se realizó caminata asistida durante 20 minutos",
            cuidador = "Laura Pérez",
            fecha = "07/09/2026",
            hora = "09:20 AM",
            estado = EstadoEvento.REVISADO,
            observaciones = "El paciente realizó la actividad sin presentar dificultades."
        ),

        EventoBitacora(
            id = 3,
            tipo = TipoEvento.OBSERVACION,
            paciente = "Ana Rodríguez",
            descripcion = "Presentó poco apetito durante el desayuno",
            cuidador = "Daniel López",
            fecha = "07/09/2026",
            hora = "10:15 AM",
            estado = EstadoEvento.PENDIENTE,
            observaciones = "Se recomienda realizar seguimiento a la alimentación durante el resto del día."
        ),

        EventoBitacora(
            id = 2,
            tipo = TipoEvento.ACTIVIDAD,
            paciente = "José Martínez",
            descripcion = "Se realizó caminata asistida durante 20 minutos",
            cuidador = "Laura Pérez",
            fecha = "07/09/2026",
            hora = "09:20 AM",
            estado = EstadoEvento.REVISADO,
            observaciones = "El paciente realizó la actividad sin presentar dificultades."
        ),

        EventoBitacora(
            id = 1,
            tipo = TipoEvento.EVENTO_ADVERSO,
            paciente = "María González",
            descripcion = "Caída durante traslado",
            cuidador = "Carlos Rodríguez",
            fecha = "07/09/2026",
            hora = "08:35 AM",
            estado = EstadoEvento.PENDIENTE,
            observaciones = "Se recomienda continuar observando al paciente y registrar cualquier cambio en su estado."
        ),
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBitacoraEncargadoBinding.inflate(
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

        configurarResumen()

        configurarFiltros()

        configurarBusqueda()

        configurarNotificaciones()

        configurarNavegacion()
    }

    private fun configurarRecyclerView() {

        adapter = EventoBitacoraAdapter(
            listaEventos = listaEventos,
            onEventoClick = { evento ->
                abrirDetalleEvento(evento)
            }
        )

        binding.recyclerEventosBitacora.apply {

            layoutManager =
                LinearLayoutManager(requireContext())

            adapter = this@BitacoraEncargadoFragment.adapter

            isNestedScrollingEnabled = false

            setHasFixedSize(false)
        }

        mostrarEventos(listaEventos)
    }

    private fun configurarResumen() {

        val total = listaEventos.size

        val pendientes =
            listaEventos.count {
                it.estado == EstadoEvento.PENDIENTE
            }

        binding.txtTotalRegistros.text =
            total.toString()

        binding.txtPendientes.text =
            pendientes.toString()
    }

    private fun configurarFiltros() {

        binding.filtroTodos.setOnClickListener {

            seleccionarFiltro("todos")

            mostrarEventos(listaEventos)
        }

        binding.filtroEventos.setOnClickListener {

            seleccionarFiltro("eventos")

            val filtrados =
                listaEventos.filter {
                    it.tipo == TipoEvento.EVENTO_ADVERSO
                }

            mostrarEventos(filtrados)
        }

        binding.filtroActividades.setOnClickListener {

            seleccionarFiltro("actividades")

            val filtrados =
                listaEventos.filter {
                    it.tipo == TipoEvento.ACTIVIDAD
                }

            mostrarEventos(filtrados)
        }

        binding.filtroObservaciones.setOnClickListener {

            seleccionarFiltro("observaciones")

            val filtrados =
                listaEventos.filter {
                    it.tipo == TipoEvento.OBSERVACION
                }

            mostrarEventos(filtrados)
        }
    }

    private fun seleccionarFiltro(
        filtro: String
    ) {

        restaurarFiltros()

        when (filtro) {

            "todos" -> {
                seleccionarFiltroVisual(
                    binding.filtroTodos,
                    binding.textFiltroTodos
                )
            }

            "eventos" -> {
                seleccionarFiltroVisual(
                    binding.filtroEventos,
                    binding.textFiltroEventos
                )
            }

            "actividades" -> {
                seleccionarFiltroVisual(
                    binding.filtroActividades,
                    binding.textFiltroActividades
                )
            }

            "observaciones" -> {
                seleccionarFiltroVisual(
                    binding.filtroObservaciones,
                    binding.textFiltroObservaciones
                )
            }
        }
    }

    private fun restaurarFiltros() {

        val colorGris =
            android.graphics.Color.parseColor("#687078")

        binding.filtroTodos.setBackgroundResource(
            R.drawable.bg_search
        )

        binding.filtroEventos.setBackgroundResource(
            R.drawable.bg_search
        )

        binding.filtroActividades.setBackgroundResource(
            R.drawable.bg_search
        )

        binding.filtroObservaciones.setBackgroundResource(
            R.drawable.bg_search
        )

        binding.textFiltroTodos.setTextColor(colorGris)
        binding.textFiltroEventos.setTextColor(colorGris)
        binding.textFiltroActividades.setTextColor(colorGris)
        binding.textFiltroObservaciones.setTextColor(colorGris)
    }

    private fun seleccionarFiltroVisual(
        contenedor: View,
        texto: View
    ) {

        contenedor.setBackgroundResource(
            R.drawable.bg_tab_seleccionada
        )

        if (texto is android.widget.TextView) {

            texto.setTextColor(
                android.graphics.Color.WHITE
            )
        }
    }

    private fun configurarBusqueda() {

        binding.edtBuscarBitacora.doAfterTextChanged {

            filtrarPorBusqueda()
        }
    }

    private fun filtrarPorBusqueda() {

        val texto =
            binding.edtBuscarBitacora
                .text
                .toString()
                .trim()
                .lowercase()

        if (texto.isEmpty()) {

            mostrarEventos(listaEventos)

            return
        }

        val filtrados =
            listaEventos.filter { evento ->

                evento.paciente.lowercase()
                    .contains(texto) ||

                        evento.cuidador.lowercase()
                            .contains(texto) ||

                        evento.descripcion.lowercase()
                            .contains(texto) ||

                        evento.tipo.name.lowercase()
                            .contains(texto)
            }

        mostrarEventos(filtrados)
    }

    private fun mostrarEventos(
        eventos: List<EventoBitacora>
    ) {

        adapter.actualizarLista(eventos)

        if (eventos.isEmpty()) {

            binding.recyclerEventosBitacora.visibility =
                View.GONE

            binding.txtSinRegistrosBitacora.visibility =
                View.VISIBLE

        } else {

            binding.recyclerEventosBitacora.visibility =
                View.VISIBLE

            binding.txtSinRegistrosBitacora.visibility =
                View.GONE
        }
    }

    private fun configurarNotificaciones() {

        binding.btnNotificacionesBitacora.setOnClickListener {

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    NotificacionesEncargadoFragment()
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun abrirDetalleEvento(
        evento: EventoBitacora
    ) {

        val datos = Bundle().apply {

            putString(
                "tipoEvento",
                when (evento.tipo) {

                    TipoEvento.EVENTO_ADVERSO ->
                        "EVENTO ADVERSO"

                    TipoEvento.ACTIVIDAD ->
                        "ACTIVIDAD"

                    TipoEvento.OBSERVACION ->
                        "OBSERVACIÓN"
                }
            )

            putString(
                "estado",
                when (evento.estado) {

                    EstadoEvento.PENDIENTE ->
                        "PENDIENTE"

                    EstadoEvento.REVISADO ->
                        "REVISADO"
                }
            )

            putString(
                "paciente",
                evento.paciente
            )

            putString(
                "fecha",
                evento.fecha
            )

            putString(
                "hora",
                evento.hora
            )

            putString(
                "cuidador",
                evento.cuidador
            )

            putString(
                "descripcion",
                evento.descripcion
            )

            putString(
                "observaciones",
                evento.observaciones
            )
        }

        val detalle =
            DetalleEventoAdversoEncargadoFragment()

        detalle.arguments = datos

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                detalle
            )
            .addToBackStack(null)
            .commit()
    }

    private fun configurarNavegacion() {

        NavegacionEncargado.configurar(

            navInicio = binding.navInicioBitacora,
            iconInicio = binding.iconInicioBitacora,
            textInicio = binding.textInicioBitacora,

            navCitas = binding.navCitasBitacora,
            iconCitas = binding.iconCitasBitacora,
            textCitas = binding.textCitasBitacora,

            navBitacora = binding.navBitacoraBitacora,
            iconBitacora = binding.iconBitacoraBitacora,
            textBitacora = binding.textBitacoraBitacora,

            navPerfil = binding.navPerfilBitacora,
            iconPerfil = binding.iconPerfilBitacora,
            textPerfil = binding.textPerfilBitacora,

            pantallaActual =
                NavegacionEncargado.Pantalla.BITACORA,

            onInicio = {

                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeEncargadoFragment()
                    )
                    .commit()
            },

            onCitas = {

                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CitasEncargadoFragment()
                    )
                    .commit()
            },

            onBitacora = {
                // Ya estamos en Bitácora
            },

            onPerfil = {

                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        PerfilEncargadoFragment()
                    )
                    .commit()
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}