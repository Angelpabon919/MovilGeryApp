package com.example.molvigeryapp.ui.cuidador.notificaciones

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.data.repository.NotificacionesRepository
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificacionesFragment : Fragment() {

    private lateinit var notificacionAdapter: NotificacionAdapter
    private lateinit var recyclerNotificaciones: RecyclerView
    private lateinit var txtCantidadNoLeidas: TextView

    companion object {
        private const val TAG = "NOTIFICACIONES_CUIDADOR"
        private const val INTERVALO_ACTUALIZACION = 8000L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(
            R.layout.fragment_notificaciones_cuidador,
            container,
            false
        )

        recyclerNotificaciones =
            view.findViewById(R.id.recyclerNotificaciones)

        txtCantidadNoLeidas =
            view.findViewById(R.id.txtCantidadNoLeidas)

        // Ocultar el contador hasta recibir los datos reales.
        txtCantidadNoLeidas.visibility = View.GONE

        return view
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.btnVolverNotificaciones)
            .setOnClickListener {
                requireActivity()
                    .onBackPressedDispatcher
                    .onBackPressed()
            }

        configurarRecyclerView()
        iniciarActualizacionAutomatica()
    }

    private fun configurarRecyclerView() {
        notificacionAdapter = NotificacionAdapter(emptyList()) {
                notificacionSeleccionada ->

            if (!notificacionSeleccionada.leida) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        NotificacionesRepository.marcarNotificacionLeida(
                            notificacionSeleccionada
                                .idNotificacionDestinatario
                        )

                        cargarNotificacionesCuidador()

                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.e(
                            TAG,
                            "Error al marcar la notificación como leída",
                            e
                        )
                    }
                }
            }

            val bundle = Bundle().apply {
                putSerializable(
                    "notificacion",
                    notificacionSeleccionada
                )
            }

            val detalleFragment =
                DetalleNotificacionCuidadorFragment().apply {
                    arguments = bundle
                }

            val containerId =
                (view?.parent as? View)?.id
                    ?: R.id.fragmentContainer

            parentFragmentManager.beginTransaction()
                .replace(containerId, detalleFragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerNotificaciones.layoutManager =
            LinearLayoutManager(requireContext())

        recyclerNotificaciones.adapter = notificacionAdapter
    }

    private fun iniciarActualizacionAutomatica() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                cargarNotificacionesCuidador()

                while (isActive) {
                    delay(INTERVALO_ACTUALIZACION)
                    cargarNotificacionesCuidador()
                }
            }
        }
    }

    private suspend fun cargarNotificacionesCuidador() {
        try {
            val contexto = context ?: return

            val preferencias =
                contexto.getSharedPreferences("SESION", 0)

            val idUsuario =
                preferencias.getInt("ID_USUARIO", -1)

            if (idUsuario <= 0) {
                if (isAdded && view != null) {
                    txtCantidadNoLeidas.visibility = View.GONE
                }
                return
            }

            val listaFinal =
                NotificacionesRepository.obtenerNotificacionesUsuario(
                    idUsuario
                )

            if (!isAdded || view == null) return

            Log.d(
                TAG,
                "TOTAL QUE LLEGA AL ADAPTADOR: ${listaFinal.size}"
            )

            listaFinal.forEachIndexed { index, notificacion ->
                Log.d(
                    TAG,
                    "POSICION=$index | " +
                            "ID=${notificacion.idNotificacion} | " +
                            "TIPO=${notificacion.tipo} | " +
                            "TITULO=${notificacion.titulo} | " +
                            "LEIDA=${notificacion.leida}"
                )
            }

            // Actualizar la lista una sola vez.
            notificacionAdapter.actualizarLista(listaFinal)

            // Actualizar el contador con los datos del servidor.
            actualizarContador(listaFinal)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Error cargando notificaciones",
                e
            )
        }
    }

    private fun actualizarContador(
        lista: List<Notificacion>
    ) {
        val cantidadNoLeidas =
            lista.count { !it.leida }

        if (cantidadNoLeidas > 0) {
            txtCantidadNoLeidas.text =
                if (cantidadNoLeidas > 99) {
                    "99+"
                } else {
                    cantidadNoLeidas.toString()
                }

            txtCantidadNoLeidas.visibility = View.VISIBLE
        } else {
            txtCantidadNoLeidas.visibility = View.GONE
        }
    }
}