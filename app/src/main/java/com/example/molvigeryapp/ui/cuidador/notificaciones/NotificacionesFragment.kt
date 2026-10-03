package com.example.molvigeryapp.ui.cuidador.notificaciones

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
        private const val INTERVALO_ACTUALIZACION = 8000L // 5 segundos para no saturar el servidor
    }

    override fun onResume() {
        super.onResume()
        // Como es una función suspend, la lanzamos en el lifecycleScope del Fragment
        viewLifecycleOwner.lifecycleScope.launch {
            cargarNotificacionesCuidador()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notificaciones_cuidador, container, false)

        // Enlazamos tus vistas con findViewById
        recyclerNotificaciones = view.findViewById(R.id.recyclerNotificaciones)
        txtCantidadNoLeidas = view.findViewById(R.id.txtCantidadNoLeidas)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón de retroceso (flecha)
        view.findViewById<View>(R.id.btnVolverNotificaciones).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        configurarRecyclerView()
        iniciarActualizacionAutomatica()
    }

    private fun configurarRecyclerView() {
        notificacionAdapter = NotificacionAdapter(emptyList()) { notificacionSeleccionada ->

            // 1. Si no está leída, llamamos a la API para marcarla como leída en Django
            if (!notificacionSeleccionada.leida) {
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        NotificacionesRepository.marcarNotificacionLeida(notificacionSeleccionada.idNotificacionDestinatario)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error al marcar la notificación como leída", e)
                    }
                }
            }

            // 2. Preparamos el Bundle con el objeto para enviarlo al detalle
            val bundle = Bundle().apply {
                putSerializable("notificacion", notificacionSeleccionada)
            }

            val detalleFragment = DetalleNotificacionCuidadorFragment().apply {
                arguments = bundle
            }

            // 3. Navegamos al fragmento de detalle usando el contenedor del fragmento actual
            val containerId = (view?.parent as? View)?.id ?: R.id.fragmentContainer

            parentFragmentManager.beginTransaction()
                .replace(containerId, detalleFragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerNotificaciones.layoutManager = LinearLayoutManager(requireContext())
        recyclerNotificaciones.adapter = notificacionAdapter
    }

    private fun iniciarActualizacionAutomatica() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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
            val preferencias = contexto.getSharedPreferences("SESION", 0)
            val idUsuario = preferencias.getInt("ID_USUARIO", -1)

            if (idUsuario <= 0) return

            // Consumimos el repositorio que ya usa tu compañero
            val listaFinal = NotificacionesRepository.obtenerNotificacionesUsuario(idUsuario)

            if (!isAdded || view == null) return

            // Actualizamos la lista en el adaptador
            notificacionAdapter.actualizarLista(listaFinal)

            // Actualizamos el número rojo de no leídas dinámicamente
            actualizarContador(listaFinal)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando notificaciones", e)
        }
    }

    private fun actualizarContador(lista: List<Notificacion>) {
        val cantidadNoLeidas = lista.count { !it.leida }

        if (cantidadNoLeidas > 0) {
            txtCantidadNoLeidas.visibility = View.VISIBLE
            txtCantidadNoLeidas.text = if (cantidadNoLeidas > 99) "99+" else cantidadNoLeidas.toString()
        } else {
            txtCantidadNoLeidas.visibility = View.GONE
        }
    }
}