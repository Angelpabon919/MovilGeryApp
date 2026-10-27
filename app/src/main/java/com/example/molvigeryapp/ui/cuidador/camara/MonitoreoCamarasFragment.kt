
package com.example.molvigeryapp.ui.cuidador.camara

import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject


import android.os.Bundle
import android.graphics.BitmapFactory
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Camara
import com.example.molvigeryapp.data.model.Habitacion

class MonitoreoCamarasFragment : Fragment() {

    private val viewModel: MonitoreoCamarasViewModel by viewModels()

    private lateinit var spinnerHabitaciones: Spinner
    private lateinit var spinnerCamaras: Spinner

    private lateinit var tvTotalCamaras: TextView
    private lateinit var tvCamarasActivas: TextView
    private lateinit var tvCamarasDesconectadas: TextView
    private lateinit var tvNombreCamara: TextView
    private lateinit var tvHabitacionCamara: TextView
    private lateinit var tvEstadoConexion: TextView
    private lateinit var tvEstadoVideo: TextView
    private lateinit var imagenVideoCamara: ImageView
    private lateinit var contenedorSinVideo: View

    @Volatile
    private var generacionVideo = 0

    private var salaLiveKit: Room? = null
    private var tareaLiveKit: Job? = null
    private var renderizadorLiveKit: SurfaceViewRenderer? = null
    private var pistaLiveKit: VideoTrack? = null

    private var camaraActual: Camara? = null
    private lateinit var tvEstadoGeneral: TextView

    private lateinit var recyclerOtrasCamaras: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvSinOtrasCamaras: TextView
    private lateinit var adaptadorOtrasCamaras: MonitoreoCamaraAdapter

    private var idCamaraSeleccionada: Int? = null

    private var listaHabitaciones: List<Habitacion> = emptyList()
    private var listaCamaras: List<Camara> = emptyList()
    private var camarasFiltradas: List<Camara> = emptyList()

    private var cargandoHabitaciones = false
    private var cargandoCamaras = false

    override fun onStart() {
        super.onStart()
        camaraActual?.let { iniciarVideo(it) }
    }

    override fun onStop() {
        detenerVideo()
        super.onStop()
    }

    override fun onDestroyView() {
        detenerVideo()
        if (::imagenVideoCamara.isInitialized) {
            imagenVideoCamara.setImageDrawable(null)
        }
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_monitoreo_camaras,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        inicializarVistas(view)
        configurarBotonVolver(view)
        configurarSelectores()
        configurarOtrasCamaras()
        observarDatos()

        viewModel.cargarDatos()
    }

    private fun inicializarVistas(view: View) {
        spinnerHabitaciones = view.findViewById(R.id.spinnerHabitaciones)
        spinnerCamaras = view.findViewById(R.id.spinnerCamaras)

        tvTotalCamaras = view.findViewById(R.id.tvTotalCamaras)
        tvCamarasActivas = view.findViewById(R.id.tvCamarasActivas)
        tvCamarasDesconectadas =
            view.findViewById(R.id.tvCamarasDesconectadas)

        tvNombreCamara = view.findViewById(R.id.tvNombreCamara)
        tvHabitacionCamara = view.findViewById(R.id.tvHabitacionCamara)
        tvEstadoConexion = view.findViewById(R.id.tvEstadoConexion)
        tvEstadoVideo = view.findViewById(R.id.tvEstadoVideo)
        imagenVideoCamara = view.findViewById(R.id.imagenVideoCamara)
        contenedorSinVideo = view.findViewById(R.id.contenedorSinVideo)
        tvEstadoGeneral = view.findViewById(R.id.tvEstadoGeneralMonitoreo)

        recyclerOtrasCamaras =
            view.findViewById(R.id.recyclerOtrasCamaras)

        tvSinOtrasCamaras =
            view.findViewById(R.id.tvSinOtrasCamaras)


    }

    private fun configurarBotonVolver(view: View) {
        view.findViewById<View>(R.id.btnVolverMonitoreo)
            .setOnClickListener {
                parentFragmentManager.popBackStack()
            }
    }

    private fun configurarSelectores() {
        spinnerHabitaciones.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (cargandoHabitaciones) return

                    val habitacion = listaHabitaciones.getOrNull(position - 1)

                    if (position == 0) {
                        filtrarCamaras(null)
                    } else {
                        filtrarCamaras(habitacion?.idHabitacion)
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit
            }

        spinnerCamaras.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (cargandoCamaras) return

                    camarasFiltradas.getOrNull(position)?.let {
                        mostrarCamaraSeleccionada(it)
                    }
                }

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) = Unit
            }
    }

    private fun observarDatos() {
        viewModel.habitaciones.observe(viewLifecycleOwner) { habitaciones ->
            listaHabitaciones = habitaciones

            val opciones = listOf("Selecciona una habitación") +
                    habitaciones.map { "${it.nombre} · ${it.numero}" }

            cargandoHabitaciones = true

            spinnerHabitaciones.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                opciones
            ).also {
                it.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )
            }

            spinnerHabitaciones.setSelection(0)
            cargandoHabitaciones = false

            filtrarCamaras(null)
        }

        viewModel.camaras.observe(viewLifecycleOwner) { camaras ->
            listaCamaras = camaras

            tvTotalCamaras.text = camaras.size.toString()
            tvCamarasActivas.text = camaras.count { it.estado }.toString()
            tvCamarasDesconectadas.text =
                camaras.count { !it.estado }.toString()

            actualizarEstadoGeneral()
            filtrarCamaras(
                listaHabitaciones.getOrNull(
                    spinnerHabitaciones.selectedItemPosition - 1
                )?.idHabitacion
            )
        }

        viewModel.cargando.observe(viewLifecycleOwner) { cargando ->
            if (cargando) {
                tvEstadoGeneral.text = "● Cargando cámaras..."
            } else {
                actualizarEstadoGeneral()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    error,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun filtrarCamaras(idHabitacion: Int?) {
        camarasFiltradas = if (idHabitacion == null) {
            listaCamaras
        } else {
            listaCamaras.filter {
                it.idHabitacion == idHabitacion
            }
        }

        val opciones = if (camarasFiltradas.isEmpty()) {
            listOf("No hay cámaras registradas en esta habitación")
        } else {
            camarasFiltradas.map { it.nombre }
        }

        cargandoCamaras = true

        spinnerCamaras.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            opciones
        ).also {
            it.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
        }

        spinnerCamaras.setSelection(0)
        cargandoCamaras = false

        val primeraCamara = camarasFiltradas.firstOrNull()

        if (primeraCamara != null) {
            mostrarCamaraSeleccionada(primeraCamara)
        } else {
            mostrarSinCamara()
        }
    }

    private fun mostrarCamaraSeleccionada(camara: Camara) {
        camaraActual = camara
        tvNombreCamara.text = camara.nombre
        idCamaraSeleccionada = camara.idCamara
        actualizarOtrasCamaras()

        val habitacion = listaHabitaciones.firstOrNull {
            it.idHabitacion == camara.idHabitacion
        }

        tvHabitacionCamara.text = habitacion?.let {
            "Habitación: ${it.nombre} · ${it.numero}"
        } ?: "Habitación: sin asignar"

        tvEstadoConexion.text = if (camara.estado) {
            "Estado registrado: activa · Transmisión pendiente de comprobar"
        } else {
            "Estado registrado: inactiva"
        }

        if (camara.estado) {
            iniciarVideo(camara)
        } else {
            detenerVideo()
            mostrarMensajeVideo("Esta cámara está registrada como inactiva")
        }
    }

    private fun mostrarSinCamara() {
        camaraActual = null
        detenerVideo()
        tvNombreCamara.text = "Cámara no disponible"
        tvHabitacionCamara.text = "Habitación: sin cámaras registradas"
        tvEstadoConexion.text = "Estado: sin cámara disponible"
        mostrarMensajeVideo("No hay cámaras registradas para esta selección")
        idCamaraSeleccionada = null
        actualizarOtrasCamaras()
    }

    private fun mostrarMensajeVideo(mensaje: String) {
        if (!isAdded || !::imagenVideoCamara.isInitialized) return
        imagenVideoCamara.setImageDrawable(null)
        imagenVideoCamara.visibility = View.GONE
        contenedorSinVideo.visibility = View.VISIBLE
        tvEstadoVideo.text = mensaje
    }

    private fun detenerVideo() {
        generacionVideo++

        tareaLiveKit?.cancel()
        tareaLiveKit = null

        val pistaAnterior = pistaLiveKit
        val renderizadorAnterior = renderizadorLiveKit
        val salaAnterior = salaLiveKit

        pistaLiveKit = null
        renderizadorLiveKit = null
        salaLiveKit = null

        if (pistaAnterior != null && renderizadorAnterior != null) {
            try {
                pistaAnterior.removeRenderer(renderizadorAnterior)
            } catch (_: Exception) {
            }
        }

        if (renderizadorAnterior != null) {
            try {
                (renderizadorAnterior.parent as? ViewGroup)
                    ?.removeView(renderizadorAnterior)
                renderizadorAnterior.release()
            } catch (_: Exception) {
            }
        }

        if (salaAnterior != null && isAdded) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    salaAnterior.disconnect()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun iniciarVideo(camara: Camara) {
        Log.d("GerIAppLiveKit", "iniciarVideo: camara=${camara.idCamara}, habitacion=${camara.idHabitacion}, activa=${camara.estado}")
        detenerVideo()

        val idHabitacion = camara.idHabitacion
        if (!camara.estado || idHabitacion == null || !isAdded || view == null) {
            mostrarMensajeVideo("La cámara no tiene una habitación válida")
            return
        }

        val generacion = generacionVideo
        val contexto = context ?: return

        val contenedor = imagenVideoCamara.parent as? FrameLayout
        if (contenedor == null) {
            mostrarMensajeVideo("No se encontró el contenedor del video")
            return
        }

        imagenVideoCamara.setImageDrawable(null)
        imagenVideoCamara.visibility = View.GONE
        contenedorSinVideo.visibility = View.VISIBLE
        tvEstadoConexion.text = "Conectando con LiveKit..."
        tvEstadoVideo.text = "Esperando transmisión en vivo"

        val renderizador = SurfaceViewRenderer(contexto)
        renderizador.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        renderizador.visibility = View.VISIBLE
        contenedor.addView(renderizador)
        renderizadorLiveKit = renderizador

        tareaLiveKit = viewLifecycleOwner.lifecycleScope.launch {
            var sala: Room? = null

            try {
                val preferencias = contexto.getSharedPreferences(
                    "SESION",
                    Context.MODE_PRIVATE
                )
                val tokenSesion = preferencias.getString("TOKEN", null)
                    ?: throw IllegalStateException(
                        "No se encontró el token de sesión de GerIApp"
                    )

                Log.d("GerIAppLiveKit", "Solicitando token para habitacion=$idHabitacion")
                // Solicitar al backend el token temporal de esta habitación.
                val respuesta = withContext(Dispatchers.IO) {
                    val cuerpo = JSONObject()
                        .put("id_habitacion", idHabitacion)
                        .toString()
                        .toRequestBody(
                            "application/json; charset=utf-8".toMediaType()
                        )

                    val peticion = Request.Builder()
                        .url(
                            "https://geriapp-backend.onrender.com/api/livekit/token/"
                        )
                        .header("Authorization", "Bearer $tokenSesion")
                        .post(cuerpo)
                        .build()

                    OkHttpClient().newCall(peticion).execute().use { resultado ->
                        val contenido = resultado.body?.string().orEmpty()
                        Log.d("GerIAppLiveKit", "Respuesta token HTTP ${resultado.code}")

                        if (!resultado.isSuccessful) {
                            val detalle = try {
                                JSONObject(contenido).optString(
                                    "error",
                                    "HTTP ${resultado.code}"
                                )
                            } catch (_: Exception) {
                                "HTTP ${resultado.code}"
                            }
                            throw IllegalStateException(detalle)
                        }

                        JSONObject(contenido)
                    }
                }

                if (generacion != generacionVideo) return@launch

                val tokenLiveKit = respuesta.getString("token")
                val urlLiveKit = respuesta.getString("url")
                Log.d("GerIAppLiveKit", "Token recibido; URL presente=${urlLiveKit.isNotBlank()}")
                val identidadPublicador =
                    "geriapp-publicador-camara-${camara.idCamara}"

                sala = LiveKit.create(contexto.applicationContext)
                salaLiveKit = sala

                sala.initVideoRenderer(renderizador)

                // Escuchar las pistas antes de conectar a la sala.
                val salaActiva = sala
                launch {
                    salaActiva.events.collect { evento ->
                        if (generacion != generacionVideo) return@collect

                        when (evento) {
                            is RoomEvent.TrackSubscribed -> {
                                Log.d("GerIAppLiveKit", "TrackSubscribed: participante=${evento.participant.identity}, tipo=${evento.track.kind}")
                                val pista = evento.track
                                val esCamaraCorrecta =
                                    evento.participant.identity?.value == identidadPublicador

                                if (pista is VideoTrack && esCamaraCorrecta) {
                                    pistaLiveKit?.let { anterior ->
                                        renderizadorLiveKit?.let {
                                            anterior.removeRenderer(it)
                                        }
                                    }

                                    pistaLiveKit = pista
                                    pista.addRenderer(renderizador)

                                    imagenVideoCamara.visibility = View.GONE
                                    contenedorSinVideo.visibility = View.GONE
                                    renderizador.visibility = View.VISIBLE
                                    tvEstadoConexion.text = "Transmisión conectada"
                                    tvEstadoVideo.text = "Transmisión en vivo"
                                }
                            }

                            else -> Unit
                        }
                    }
                }

                Log.d("GerIAppLiveKit", "Conectando a la sala LiveKit")
                sala.connect(urlLiveKit, tokenLiveKit)
                Log.d("GerIAppLiveKit", "Conexión a sala completada")

                if (generacion == generacionVideo) {
                    tvEstadoConexion.text = "Conectado a LiveKit"
                    tvEstadoVideo.text = "Esperando video de la cámara..."
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("GerIAppLiveKit", "Error en el proceso de video", error)
                if (generacion == generacionVideo && isAdded) {
                    tvEstadoConexion.text = "Video desconectado"
                    mostrarMensajeVideo(
                        "No se pudo conectar a LiveKit: " +
                            (error.message ?: "error desconocido")
                    )
                }
            }
        }
    }

    private fun actualizarEstadoGeneral() {
        tvEstadoGeneral.text = when {
            listaCamaras.isEmpty() -> "● No hay cámaras registradas"
            else -> "● Cámaras consultadas · Video pendiente de conexión"


        }

    }

    private fun configurarOtrasCamaras() {
        adaptadorOtrasCamaras = MonitoreoCamaraAdapter { camara ->
            seleccionarCamaraDesdeLista(camara)
        }

        recyclerOtrasCamaras.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        recyclerOtrasCamaras.adapter = adaptadorOtrasCamaras
    }

    private fun seleccionarCamaraDesdeLista(camara: Camara) {
        val posicionHabitacion = listaHabitaciones.indexOfFirst {
            it.idHabitacion == camara.idHabitacion
        }

        if (posicionHabitacion >= 0) {
            spinnerHabitaciones.setSelection(posicionHabitacion + 1)

            // Esperar a que se actualice el filtro de cámaras.
            spinnerHabitaciones.post {
                val posicionCamara = camarasFiltradas.indexOfFirst {
                    it.idCamara == camara.idCamara
                }

                if (posicionCamara >= 0) {
                    spinnerCamaras.setSelection(posicionCamara)
                } else {
                    mostrarCamaraSeleccionada(camara)
                }
            }
        } else {
            spinnerHabitaciones.setSelection(0)
            mostrarCamaraSeleccionada(camara)
        }
    }





    private fun actualizarOtrasCamaras() {
        val otrasCamaras = listaCamaras.filter {
            it.idCamara != idCamaraSeleccionada
        }

        adaptadorOtrasCamaras.actualizarDatos(
            otrasCamaras,
            listaHabitaciones
        )

        tvSinOtrasCamaras.visibility =
            if (otrasCamaras.isEmpty()) View.VISIBLE else View.GONE

        recyclerOtrasCamaras.visibility =
            if (otrasCamaras.isEmpty()) View.GONE else View.VISIBLE
    }

}
