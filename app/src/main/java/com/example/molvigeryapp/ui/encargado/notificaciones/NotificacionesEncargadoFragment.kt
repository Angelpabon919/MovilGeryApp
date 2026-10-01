package com.example.molvigeryapp.ui.encargado.notificaciones

import android.os.Bundle
import android.util.Log
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
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.data.repository.NotificacionesRepository
import com.example.molvigeryapp.databinding.FragmentNotificacionesEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificacionesEncargadoFragment : Fragment() {

    private var _binding: FragmentNotificacionesEncargadoBinding? =
        null

    private val binding
        get() = requireNotNull(_binding)

    private lateinit var adapter: NotificacionAdapter

    private var cargando = false

    private var primeraCarga = true

    companion object {

        private const val TAG =
            "NOTIFICACIONES_ENCARGADO"

        private const val INTERVALO_ACTUALIZACION =
            800L
    }


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

        super.onViewCreated(
            view,
            savedInstanceState
        )

        configurarRecyclerView()

        configurarBotonVolver()

        iniciarActualizacionAutomatica()
    }


    private fun configurarRecyclerView() {

        adapter =
            NotificacionAdapter(
                emptyList()
            ) { notificacion ->

                abrirDetalleNotificacion(
                    notificacion
                )
            }

        binding.recyclerNotificaciones.apply {

            layoutManager =
                LinearLayoutManager(
                    requireContext()
                )

            adapter =
                this@NotificacionesEncargadoFragment
                    .adapter

            setHasFixedSize(false)
        }
    }


    private fun configurarBotonVolver() {

        binding
            .btnVolverNotificaciones
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }


    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                cargarNotificaciones()

                while (isActive) {

                    delay(
                        INTERVALO_ACTUALIZACION
                    )

                    cargarNotificaciones()
                }
            }
        }
    }


    private suspend fun cargarNotificaciones() {

        if (cargando) {
            return
        }

        cargando = true

        try {

            if (primeraCarga) {
                mostrarCarga(true)
            }

            val contexto =
                context ?: return

            val preferencias =
                contexto.getSharedPreferences(
                    "SESION",
                    0
                )

            val idUsuario =
                preferencias.getInt(
                    "ID_USUARIO",
                    -1
                )

            if (idUsuario <= 0) {

                throw Exception(
                    "No se encontró el usuario de la sesión."
                )
            }

            Log.d(
                TAG,
                "Cargando notificaciones del usuario $idUsuario"
            )

            val listaFinal =
                NotificacionesRepository
                    .obtenerNotificacionesUsuario(
                        idUsuario
                    )

            if (
                !isAdded ||
                _binding == null
            ) {
                return
            }

            Log.d(
                TAG,
                "Notificaciones encontradas: ${listaFinal.size}"
            )

            adapter.actualizarLista(
                listaFinal
            )

            actualizarContador(
                listaFinal
            )

            mostrarCarga(false)

            primeraCarga = false

        } catch (e: CancellationException) {

            throw e

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error cargando notificaciones",
                e
            )

            if (
                !isAdded ||
                _binding == null
            ) {
                return
            }

            mostrarCarga(false)

            Toast.makeText(
                requireContext(),
                e.message
                    ?: "No se pudieron cargar las notificaciones.",
                Toast.LENGTH_LONG
            ).show()

        } finally {

            cargando = false
        }
    }


    private fun actualizarContador(
        lista: List<Notificacion>
    ) {

        val cantidadNoLeidas =
            lista.count { notificacion ->
                !notificacion.leida
            }

        if (cantidadNoLeidas > 0) {

            binding
                .txtCantidadNoLeidas
                .visibility =
                View.VISIBLE

            binding
                .txtCantidadNoLeidas
                .text =
                if (cantidadNoLeidas > 99) {
                    "99+"
                } else {
                    cantidadNoLeidas.toString()
                }

        } else {

            binding
                .txtCantidadNoLeidas
                .visibility =
                View.GONE
        }
    }


    private fun mostrarCarga(
        mostrar: Boolean
    ) {

        val bindingActual =
            _binding ?: return

        bindingActual
            .progressBarNotificaciones
            .visibility =
            if (mostrar) {
                View.VISIBLE
            } else {
                View.GONE
            }

        bindingActual
            .recyclerNotificaciones
            .visibility =
            if (mostrar) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
    }


    private fun abrirDetalleNotificacion(
        notificacion: Notificacion
    ) {

        val datos =
            Bundle().apply {

                putSerializable(
                    "notificacion",
                    notificacion
                )
            }

        val fragment =
            DetalleNotificacionEncargadoFragment()

        fragment.arguments =
            datos

        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }


    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }
}