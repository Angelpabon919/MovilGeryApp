
package com.example.molvigeryapp.ui.cuidador.camara

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
import android.view.ViewGroup
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

    @Volatile
    private var conexionVideo: HttpURLConnection? = null

    @Volatile
    private var flujoVideo: InputStream? = null

    private var hiloVideo: Thread? = null
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
        try {
            flujoVideo?.close()
        } catch (_: Exception) {
        }
        flujoVideo = null

        try {
            conexionVideo?.disconnect()
        } catch (_: Exception) {
        }
        conexionVideo = null

        hiloVideo?.interrupt()
        hiloVideo = null
    }

    private fun iniciarVideo(camara: Camara) {
        detenerVideo()

        if (!camara.estado || !isAdded || view == null) return

        val generacion = generacionVideo
        val nombreCodificado = URLEncoder.encode(camara.nombre, "UTF-8")
        val contexto = context ?: return
        val servidor = ConfiguracionServidorCamaras.obtenerServidor(contexto)
        val direccion =
            "$servidor/video?camara=$nombreCodificado"

        imagenVideoCamara.setImageDrawable(null)
        imagenVideoCamara.visibility = View.VISIBLE
        contenedorSinVideo.visibility = View.GONE
        tvEstadoVideo.text = "Conectando con la cámara..."

        val vista = view ?: return

        hiloVideo = Thread({
            var conexion: HttpURLConnection? = null
            var flujo: InputStream? = null

            try {
                conexion = (URL(direccion).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 15000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "multipart/x-mixed-replace")
                    useCaches = false
                }

                conexionVideo = conexion
                conexion.connect()

                if (generacion != generacionVideo) return@Thread

                val respuesta = conexion.responseCode
                if (respuesta != HttpURLConnection.HTTP_OK) {
                    throw IllegalStateException("El servidor de video respondió HTTP $respuesta")
                }

                flujo = conexion.inputStream
                flujoVideo = flujo

                val fotograma = ByteArrayOutputStream()
                var dentroDeImagen = false
                var anterior = -1
                var primerFotograma = true

                while (
                    generacion == generacionVideo &&
                    !Thread.currentThread().isInterrupted
                ) {
                    val byte = flujo.read()
                    if (byte == -1) break

                    if (!dentroDeImagen) {
                        if (anterior == 0xFF && byte == 0xD8) {
                            fotograma.reset()
                            fotograma.write(0xFF)
                            fotograma.write(0xD8)
                            dentroDeImagen = true
                            anterior = -1
                        } else {
                            anterior = byte
                        }
                    } else {
                        fotograma.write(byte)

                        if (anterior == 0xFF && byte == 0xD9) {
                            val bytesImagen = fotograma.toByteArray()
                            val bitmap = BitmapFactory.decodeByteArray(
                                bytesImagen, 0, bytesImagen.size
                            )

                            if (bitmap != null && generacion == generacionVideo) {
                                vista.post {
                                    if (
                                        generacion == generacionVideo &&
                                        isAdded &&
                                        ::imagenVideoCamara.isInitialized
                                    ) {
                                        imagenVideoCamara.setImageBitmap(bitmap)
                                        imagenVideoCamara.visibility = View.VISIBLE
                                        contenedorSinVideo.visibility = View.GONE

                                        if (primerFotograma) {
                                            tvEstadoConexion.text = "Transmisión conectada"
                                            primerFotograma = false
                                        }
                                        tvEstadoVideo.text = "Transmisión en vivo"
                                    } else {
                                        bitmap.recycle()
                                    }
                                }
                            }
                            dentroDeImagen = false
                            anterior = -1
                        } else {
                            anterior = byte
                        }

                        // Evita acumular datos indefinidamente si llega una imagen corrupta.
                        if (fotograma.size() > 10 * 1024 * 1024) {
                            fotograma.reset()
                            dentroDeImagen = false
                            anterior = -1
                        }
                    }
                }

                if (generacion == generacionVideo) {
                    throw IllegalStateException("La transmisión de video se interrumpió")
                }
            } catch (error: Exception) {
                if (generacion == generacionVideo) {
                    vista.post {
                        if (generacion == generacionVideo && isAdded) {
                            tvEstadoConexion.text = "Video desconectado"
                            mostrarMensajeVideo(
                                "No se pudo recibir el video. Verifica que el servidor esté activo."
                            )
                        }
                    }
                }
            } finally {
                try {
                    flujo?.close()
                } catch (_: Exception) {
                }
                conexion?.disconnect()

                if (generacion == generacionVideo) {
                    flujoVideo = null
                    conexionVideo = null
                    hiloVideo = null
                }
            }
        }, "GerIApp-MJPEG-${camara.idCamara}").apply {
            isDaemon = true
            start()
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
