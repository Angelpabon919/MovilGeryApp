package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class RecomendacionesFragment : Fragment() {

    private lateinit var adapter: RecomendacionesAdapter
    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    // Última revisión procesada por paciente.
    // Se conserva mientras exista el Fragment, incluso si su vista se recrea.
    private val revisionesProcesadas = mutableMapOf<Int, Long>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_recomendaciones,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicializar adaptador.
        adapter = RecomendacionesAdapter()

        // 2. Configurar RecyclerView.
        val rv = view.findViewById<RecyclerView>(R.id.rvRecomendaciones)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        // 3. Escuchar el paciente seleccionado.
        pacienteViewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            val idPaciente = paciente?.idPaciente

            Log.d(
                "DEBUG_RECOMENDACIONES",
                "Paciente activo: $idPaciente"
            )

            if (idPaciente != null) {
                pacienteViewModel.cargarRecomendaciones(idPaciente)
            } else {
                adapter.actualizarLista(emptyList())
            }
        }

        // 4. Mostrar únicamente la recomendación más reciente.
        pacienteViewModel.recomendaciones.observe(
            viewLifecycleOwner
        ) { listaRecomendaciones ->

            if (!listaRecomendaciones.isNullOrEmpty()) {
                val ultimaRecomendacion = listaRecomendaciones.last()
                adapter.actualizarLista(listOf(ultimaRecomendacion))
            } else {
                adapter.actualizarLista(emptyList())
            }
        }

        // 5. Escuchar cambios detectados por el actualizador centralizado.
        observarNovedadesRecomendaciones()
    }

    private fun observarNovedadesRecomendaciones() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                ContenidoPacienteNovedadManager
                    .revisionRecomendaciones
                    .collect { revisiones ->

                        val idPaciente =
                            pacienteViewModel.pacienteSeleccionado.value?.idPaciente
                                ?: return@collect

                        val revisionActual =
                            revisiones[idPaciente] ?: 0L

                        val revisionAnterior =
                            revisionesProcesadas[idPaciente]

                        if (revisionAnterior == null) {

                            // Primera observación: establecer referencia.
                            revisionesProcesadas[idPaciente] =
                                revisionActual

                        } else if (revisionActual > revisionAnterior) {

                            // Se detectó una modificación real.
                            revisionesProcesadas[idPaciente] =
                                revisionActual

                            Log.d(
                                "ACTUALIZADOR_RECOMENDACIONES",
                                "Novedad detectada para paciente $idPaciente. Recargando."
                            )

                            pacienteViewModel.cargarRecomendaciones(
                                idPaciente
                            )
                        }
                    }
            }
        }
    }
}