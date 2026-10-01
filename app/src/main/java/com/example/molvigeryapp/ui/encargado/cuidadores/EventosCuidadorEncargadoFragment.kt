package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.FragmentEventosCuidadorEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EventosCuidadorEncargadoFragment : Fragment() {

    companion object {
        private const val INTERVALO_ACTUALIZACION = 2_000L
    }

    private var _binding: FragmentEventosCuidadorEncargadoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: EventoAdversoAdapter

    private var listaEventos: List<EventoAdversoUI> = emptyList()

    /**
     * Permite diferenciar la primera carga de las actualizaciones
     * automáticas posteriores.
     */
    private var primeraCarga = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentEventosCuidadorEncargadoBinding.inflate(
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

        cargarDatosCuidador()
        configurarRecyclerView()
        configurarBotonVolver()
        iniciarActualizacionAutomatica()
    }

    /**
     * Los datos del nombre y cargo vienen del Fragment anterior.
     * No se inventan datos reales aquí.
     */
    private fun cargarDatosCuidador() {

        val nombre = arguments?.getString("nombre")
            ?.takeIf { it.isNotBlank() }
            ?: "Cuidador"

        val cargo = arguments?.getString("cargo")
            ?.takeIf { it.isNotBlank() }
            ?: "Cuidador"

        _binding?.let { bindingActual ->

            bindingActual.txtNombreCuidadorEventos.text = nombre
            bindingActual.txtCargoCuidadorEventos.text = cargo
        }
    }

    private fun configurarRecyclerView() {

        val bindingActual = _binding ?: return

        bindingActual.recyclerEventosCuidador.layoutManager =
            LinearLayoutManager(requireContext())

        adapter = EventoAdversoAdapter(
            emptyList()
        ) { evento ->

            abrirDetalleEvento(evento)
        }

        bindingActual.recyclerEventosCuidador.adapter = adapter
    }

    /**
     * Hace una carga inmediatamente al entrar o volver a la pantalla
     * y posteriormente actualiza cada 2 segundos mientras el Fragment
     * esté visible.
     *
     * repeatOnLifecycle cancela automáticamente la consulta cuando
     * la vista deja de estar STARTED.
     */
    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                while (true) {

                    cargarEventosAdversos(
                        mostrarCargaInicial = primeraCarga
                    )

                    primeraCarga = false

                    delay(INTERVALO_ACTUALIZACION)
                }
            }
        }
    }

    private suspend fun cargarEventosAdversos(
        mostrarCargaInicial: Boolean
    ) {

        val idUsuario = arguments?.getInt(
            "id_usuario",
            0
        ) ?: 0

        if (idUsuario == 0) {

            if (_binding == null) return

            mostrarSinEventos()

            if (mostrarCargaInicial && isAdded) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el cuidador seleccionado.",
                    Toast.LENGTH_LONG
                ).show()
            }

            return
        }

        if (mostrarCargaInicial) {
            mostrarCargando()
        }

        try {

            val asignaciones =
                obtenerAsignacionesDelCuidador(idUsuario)

            val pacientesAsignados =
                asignaciones
                    .mapNotNull { it.idPaciente }
                    .distinct()

            if (pacientesAsignados.isEmpty()) {

                if (_binding == null) return

                mostrarSinEventos()
                return
            }

            val bitacoras =
                RetrofitClient.apiService.getBitacoras()

            val bitacorasDelCuidador =
                bitacoras.filter { bitacora ->

                    bitacora.idUsuario == idUsuario &&
                            bitacora.idPaciente != null &&
                            pacientesAsignados.contains(
                                bitacora.idPaciente
                            )
                }

            if (bitacorasDelCuidador.isEmpty()) {

                if (_binding == null) return

                mostrarSinEventos()
                return
            }

            val idsBitacoras =
                bitacorasDelCuidador
                    .mapNotNull { it.idBitacora }
                    .toSet()

            val eventos =
                RetrofitClient.apiService.getEventosAdversos()

            val eventosDelCuidador =
                eventos.filter { evento ->

                    evento.idBitacora != null &&
                            idsBitacoras.contains(
                                evento.idBitacora
                            )
                }

            val pacientes =
                RetrofitClient.apiService.getPacientes()

            val pacientesPorId =
                pacientes.associateBy {
                    it.idPaciente
                }

            listaEventos =
                convertirEventosUI(
                    eventos = eventosDelCuidador,
                    bitacoras = bitacorasDelCuidador,
                    pacientesPorId = pacientesPorId
                )

            if (_binding == null) return

            if (listaEventos.isEmpty()) {

                mostrarSinEventos()

            } else {

                mostrarEventos(listaEventos)
            }

        } catch (e: CancellationException) {

            // La consulta fue cancelada porque el Fragment
            // dejó de estar visible. No es un error.

            throw e

        } catch (e: Exception) {

            e.printStackTrace()

            if (_binding == null) return

            /**
             * En una actualización automática no borramos
             * inmediatamente los datos que ya estaban visibles.
             *
             * Solo mostramos el error durante la primera carga.
             */
            if (mostrarCargaInicial) {

                mostrarSinEventos()

                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudieron cargar los eventos adversos.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } finally {

            if (_binding != null && mostrarCargaInicial) {
                ocultarCargando()
            }
        }
    }

    private suspend fun obtenerAsignacionesDelCuidador(
        idUsuario: Int
    ): List<AsignacionPacienteCuidador> {

        val respuesta =
            RetrofitClient.apiService
                .getAsignacionesPacienteCuidador()

        if (!respuesta.isSuccessful) {
            return emptyList()
        }

        return respuesta
            .body()
            .orEmpty()
            .filter { asignacion ->

                asignacion.idUsuario == idUsuario
            }
    }

    private fun convertirEventosUI(
        eventos: List<EventoAdverso>,
        bitacoras: List<Bitacora>,
        pacientesPorId: Map<Int?, Paciente>
    ): List<EventoAdversoUI> {

        val bitacorasPorId =
            bitacoras.associateBy {
                it.idBitacora
            }

        return eventos.mapNotNull { evento ->

            val bitacora =
                bitacorasPorId[evento.idBitacora]

            if (bitacora == null) {
                return@mapNotNull null
            }

            val paciente =
                pacientesPorId[bitacora.idPaciente]

            val nombrePaciente =
                if (paciente != null) {

                    "${paciente.nombre} ${paciente.apellido}"
                        .trim()

                } else {

                    "Paciente no disponible"
                }

            EventoAdversoUI(
                evento = evento,
                nombrePaciente = nombrePaciente
            )
        }
    }

    private fun mostrarCargando() {

        val bindingActual = _binding ?: return

        bindingActual.progressBarEventosCuidador.visibility =
            View.VISIBLE

        bindingActual.recyclerEventosCuidador.visibility =
            View.GONE

        bindingActual.txtSinEventosCuidador.visibility =
            View.GONE
    }

    private fun ocultarCargando() {

        val bindingActual = _binding ?: return

        bindingActual.progressBarEventosCuidador.visibility =
            View.GONE
    }

    private fun mostrarEventos(
        eventos: List<EventoAdversoUI>
    ) {

        val bindingActual = _binding ?: return

        bindingActual.progressBarEventosCuidador.visibility =
            View.GONE

        bindingActual.recyclerEventosCuidador.visibility =
            View.VISIBLE

        bindingActual.txtSinEventosCuidador.visibility =
            View.GONE

        bindingActual.txtCantidadEventos.text =
            eventos.size.toString()

        adapter.actualizarLista(eventos)
    }

    private fun mostrarSinEventos() {

        val bindingActual = _binding ?: return

        bindingActual.progressBarEventosCuidador.visibility =
            View.GONE

        bindingActual.recyclerEventosCuidador.visibility =
            View.GONE

        bindingActual.txtSinEventosCuidador.visibility =
            View.VISIBLE

        bindingActual.txtCantidadEventos.text =
            "0"

        if (::adapter.isInitialized) {

            adapter.actualizarLista(emptyList())
        }
    }

    private fun abrirDetalleEvento(
        item: EventoAdversoUI
    ) {

        val bindingActual = _binding ?: return

        val evento = item.evento

        val datos = Bundle().apply {

            putString(
                "tipo_evento",
                "Evento adverso"
            )

            putString(
                "estado",
                evento.estado
            )

            putString(
                "nombre_paciente",
                item.nombrePaciente
            )

            putString(
                "fecha_hora",
                evento.fechaHora
            )

            putString(
                "nombre_cuidador",
                bindingActual.txtNombreCuidadorEventos.text.toString()
            )

            putString(
                "descripcion",
                evento.descripcion
            )

            putString(
                "acciones_realizadas",
                evento.accionesRealizadas
            )

            putInt(
                "id_evento_adverso",
                evento.idEventoAdverso ?: 0
            )

            putInt(
                "id_bitacora",
                evento.idBitacora ?: 0
            )

            putInt(
                "id_tipo_emergencia",
                evento.idTipoEmergencia ?: 0
            )
        }

        val fragment =
            DetalleEventoAdversoEncargadoFragment()

        fragment.arguments = datos

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }

    private fun configurarBotonVolver() {

        _binding?.btnVolverEventos?.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}