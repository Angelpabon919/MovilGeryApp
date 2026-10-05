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

    private val binding: FragmentNotificacionesEncargadoBinding
        get() = requireNotNull(_binding)


    // =========================================================
    // ADAPTER
    // =========================================================

    private lateinit var adapter: NotificacionAdapter


    // =========================================================
    // CONTROL DE CARGA
    // =========================================================

    private var cargando = false

    private var primeraCarga = true


    companion object {

        private const val TAG =
            "NOTIFICACIONES_ENCARGADO"

        // =====================================================
        // ACTUALIZACIÓN AUTOMÁTICA CADA 2 SEGUNDOS
        // =====================================================

        private const val INTERVALO_ACTUALIZACION =
            2_000L
    }


    // =========================================================
    // CREAR VISTA
    // =========================================================

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


    // =========================================================
    // VISTA CREADA
    // =========================================================

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


    // =========================================================
    // CONFIGURAR RECYCLER VIEW
    // =========================================================

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
                this@NotificacionesEncargadoFragment.adapter

            setHasFixedSize(false)
        }
    }


    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding.btnVolverNotificaciones
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }


    // =========================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =========================================================

    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // =================================================
                // CARGA INMEDIATA
                // =================================================

                cargarNotificaciones()


                // =================================================
                // ACTUALIZACIÓN CADA 2 SEGUNDOS
                // =================================================

                while (isActive) {

                    delay(
                        INTERVALO_ACTUALIZACION
                    )

                    cargarNotificaciones()
                }
            }
        }
    }


    // =========================================================
    // CARGAR NOTIFICACIONES
    // =========================================================

    private suspend fun cargarNotificaciones() {

        // =====================================================
        // EVITAR PETICIONES SIMULTÁNEAS
        // =====================================================

        if (cargando) {
            return
        }

        cargando = true

        try {

            // =================================================
            // MOSTRAR CARGA SOLO EN LA PRIMERA CARGA
            // =================================================

            if (primeraCarga) {
                mostrarCarga(true)
            }


            // =================================================
            // OBTENER CONTEXTO
            // =================================================

            val contexto =
                context ?: return


            // =================================================
            // OBTENER USUARIO DE SESIÓN
            // =================================================

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


            // =================================================
            // CONSULTAR API
            // =================================================

            val listaFinal =
                NotificacionesRepository
                    .obtenerNotificacionesUsuario(
                        idUsuario
                    )


            // =================================================
            // COMPROBAR VISTA
            // =================================================

            if (!isAdded || _binding == null) {
                return
            }


            Log.d(
                TAG,
                "Notificaciones encontradas: ${listaFinal.size}"
            )


            // =================================================
            // ACTUALIZAR ADAPTER
            //
            // La lista ya viene ordenada desde Repository:
            //
            // MÁS NUEVA
            // MÁS ANTIGUA
            // =================================================

            adapter.actualizarLista(
                listaFinal
            )


            // =================================================
            // ACTUALIZAR CONTADOR
            // =================================================

            actualizarContador(
                listaFinal
            )


            // =================================================
            // QUITAR LOADING
            // =================================================

            mostrarCarga(false)

            primeraCarga = false


        } catch (e: CancellationException) {

            // =================================================
            // CANCELACIÓN NORMAL
            // =================================================

            throw e


        } catch (e: Exception) {

            Log.e(
                TAG,
                "Error cargando notificaciones",
                e
            )


            if (!isAdded || _binding == null) {
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


    // =========================================================
    // CONTADOR DE NO LEÍDAS
    // =========================================================

    private fun actualizarContador(
        lista: List<Notificacion>
    ) {

        val cantidadNoLeidas =
            lista.count {
                !it.leida
            }


        if (cantidadNoLeidas > 0) {

            binding.txtCantidadNoLeidas.visibility =
                View.VISIBLE

            binding.txtCantidadNoLeidas.text =
                if (cantidadNoLeidas > 99) {

                    "99+"

                } else {

                    cantidadNoLeidas.toString()
                }

        } else {

            binding.txtCantidadNoLeidas.visibility =
                View.GONE
        }
    }


    // =========================================================
    // LOADING
    // =========================================================

    private fun mostrarCarga(
        mostrar: Boolean
    ) {

        val bindingActual =
            _binding ?: return


        if (mostrar) {

            bindingActual.progressBarNotificaciones
                .visibility = View.VISIBLE

        } else {

            bindingActual.progressBarNotificaciones
                .visibility = View.GONE
        }
    }


    // =========================================================
    // ABRIR DETALLE
    // =========================================================

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