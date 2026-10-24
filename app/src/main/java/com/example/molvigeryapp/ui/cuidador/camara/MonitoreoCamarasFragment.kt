
package com.example.molvigeryapp.ui.cuidador.camara

import android.os.Bundle
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

        tvEstadoVideo.text = when {
            !camara.estado ->
                "Esta cámara está registrada como inactiva"

            camara.direccionStream.isNullOrBlank() ->
                "La cámara no tiene una dirección de transmisión configurada"

            else ->
                "Cámara seleccionada. La transmisión aún no está conectada"
        }
    }

    private fun mostrarSinCamara() {
        tvNombreCamara.text = "Cámara no disponible"
        tvHabitacionCamara.text = "Habitación: sin cámaras registradas"
        tvEstadoConexion.text = "Estado: sin cámara disponible"
        tvEstadoVideo.text = "No hay cámaras registradas para esta selección"
        idCamaraSeleccionada = null
        actualizarOtrasCamaras()
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
