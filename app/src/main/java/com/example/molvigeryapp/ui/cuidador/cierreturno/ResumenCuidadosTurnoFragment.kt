package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.BitacoraResumenTurno
import com.example.molvigeryapp.data.model.ControlesPeriodicosBitacora
import com.example.molvigeryapp.data.model.PacienteResumenTurno
import com.example.molvigeryapp.data.model.SignosVitales
import com.example.molvigeryapp.databinding.FragmentResumenCuidadosTurnoBinding
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ResumenCuidadosTurnoFragment : Fragment() {

    private var _binding: FragmentResumenCuidadosTurnoBinding? = null
    private val binding get() = _binding!!

    companion object {

        private const val ARG_ID_ASIGNACION = "id_asignacion"

        fun newInstance(idAsignacion: Int) =
            ResumenCuidadosTurnoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_ASIGNACION, idAsignacion)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentResumenCuidadosTurnoBinding.inflate(
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

        binding.btnVolverResumen.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        cargarResumen()
    }

    // =========================================================
    // CARGAR RESUMEN DESDE EL BACKEND
    // =========================================================
    private fun cargarResumen() {

        val idAsignacion =
            arguments?.getInt(ARG_ID_ASIGNACION, -1) ?: -1

        if (idAsignacion <= 0) {
            mostrarEstado(
                "No se recibió una asignación válida del turno."
            )
            return
        }

        mostrarCargando()

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val respuesta =
                    RetrofitClient.apiService.obtenerResumenCuidadosTurno(
                        idAsignacion
                    )

                if (!isAdded || _binding == null) return@launch

                binding.tvSubtituloResumen.text =
                    "${respuesta.turno?.nombre ?: "Turno"} · " +
                            "${respuesta.turno?.fecha ?: ""}"

                val pacientes = respuesta.pacientes

                if (pacientes.isEmpty()) {
                    mostrarEstado(
                        "No se encontraron pacientes asignados para este turno."
                    )
                    return@launch
                }

                binding.contenedorPacientesResumen.removeAllViews()

                pacientes.forEach { paciente ->
                    agregarTarjetaPaciente(paciente)
                }

                binding.contenedorCargandoResumen.visibility = View.GONE
                binding.contenedorEstadoResumen.visibility = View.GONE
                binding.scrollResumen.visibility = View.VISIBLE

            } catch (e: Exception) {

                Log.e(
                    "RESUMEN_CUIDADOS",
                    "Error al cargar el resumen del turno",
                    e
                )

                if (!isAdded || _binding == null) return@launch

                mostrarEstado(
                    "No se pudo cargar el resumen de cuidados. " +
                            "Verifica tu conexión e inténtalo nuevamente."
                )

                Toast.makeText(
                    requireContext(),
                    "Error al consultar los cuidados del turno.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // TARJETA DESPLEGABLE POR PACIENTE
    // =========================================================
    private fun agregarTarjetaPaciente(
        paciente: PacienteResumenTurno
    ) {

        val tarjeta = MaterialCardView(requireContext()).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(3).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = dp(1)
            strokeColor = Color.parseColor("#E2E8F0")

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val contenidoTarjeta = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val encabezado = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(16),
                dp(18),
                dp(16),
                dp(18)
            )
            setBackgroundResource(
                android.R.drawable.list_selector_background
            )
        }

        val informacionPaciente = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nombre = listOfNotNull(
            paciente.nombre,
            paciente.apellido
        ).joinToString(" ").ifBlank {
            "Paciente"
        }

        informacionPaciente.addView(
            crearTexto(
                nombre,
                17f,
                "#1E293B",
                true
            )
        )

        val ubicacion = buildString {

            if (!paciente.habitacion.isNullOrBlank()) {
                append("Habitación ${paciente.habitacion}")
            }

            if (!paciente.cama.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append("Cama ${paciente.cama}")
            }
        }

        if (ubicacion.isNotBlank()) {
            informacionPaciente.addView(
                crearTexto(
                    ubicacion,
                    12f,
                    "#64748B"
                ).apply {
                    setPadding(0, dp(4), 0, 0)
                }
            )
        }

        encabezado.addView(informacionPaciente)

        val flecha = crearTexto(
            "⌄",
            26f,
            "#2459C4",
            true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, 0, 0)
        }

        encabezado.addView(flecha)

        val contenido = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(16),
                dp(4),
                dp(16),
                dp(18)
            )
            visibility = View.VISIBLE
        }

        // Bitácoras del paciente.
        agregarTituloSeccion(
            contenido,
            "Bitácoras diarias"
        )

        if (paciente.bitacoras.isEmpty()) {

            agregarMensaje(
                contenido,
                "No hay bitácoras registradas durante este turno."
            )

        } else {

            paciente.bitacoras.forEachIndexed { index, bitacora ->

                agregarBitacora(
                    contenido,
                    bitacora,
                    index + 1
                )
            }
        }

        // Controles periódicos.
        agregarTituloSeccion(
            contenido,
            "Controles periódicos"
        )

        if (paciente.controlesPeriodicos.isEmpty()) {

            agregarMensaje(
                contenido,
                "No hay controles periódicos registrados durante este turno."
            )

        } else {

            paciente.controlesPeriodicos.forEachIndexed { index, control ->

                agregarControlPeriodico(
                    contenido,
                    control,
                    index + 1
                )
            }
        }

        encabezado.setOnClickListener {

            if (contenido.visibility == View.VISIBLE) {
                contenido.visibility = View.GONE
                flecha.text = "⌄"
            } else {
                contenido.visibility = View.VISIBLE
                flecha.text = "⌃"
            }
        }

        contenidoTarjeta.addView(encabezado)
        contenidoTarjeta.addView(contenido)

        tarjeta.addView(contenidoTarjeta)

        binding.contenedorPacientesResumen.addView(tarjeta)
    }

    // =========================================================
    // BITÁCORA
    // =========================================================
    private fun agregarBitacora(
        contenedor: LinearLayout,
        bitacora: BitacoraResumenTurno,
        numero: Int
    ) {

        val tarjeta = crearTarjetaInterna()

        tarjeta.addView(
            crearTexto(
                "Registro $numero",
                15f,
                "#2459C4",
                true
            )
        )

        agregarDato(
            tarjeta,
            "Fecha y hora",
            formatearFechaHora(bitacora.fechaHora)
        )

        agregarDato(
            tarjeta,
            "Tipo de registro",
            bitacora.tipoRegistro
        )

        agregarDato(
            tarjeta,
            "Descripción",
            bitacora.descripcion
        )

        agregarDato(
            tarjeta,
            "Estado",
            if (bitacora.estado) "Registrada" else "Inactiva"
        )

        // Signos vitales asociados a esta bitácora.
        tarjeta.addView(
            crearTexto(
                "Signos vitales",
                14f,
                "#2459C4",
                true
            ).apply {
                setPadding(0, dp(14), 0, dp(6))
            }
        )

        if (bitacora.signosVitales.isEmpty()) {

            agregarMensaje(
                tarjeta,
                "No se registraron signos vitales."
            )

        } else {

            bitacora.signosVitales.forEachIndexed { index, signo ->

                agregarSignosVitales(
                    tarjeta,
                    signo,
                    index + 1
                )
            }
        }

        contenedor.addView(tarjeta)
    }

    // =========================================================
    // SIGNOS VITALES
    // =========================================================
    private fun agregarSignosVitales(
        contenedor: LinearLayout,
        signo: SignosVitales,
        numero: Int
    ) {

        contenedor.addView(
            crearTexto(
                "Medición $numero · ${formatearFechaHora(signo.fechaHora)}",
                12f,
                "#64748B",
                true
            ).apply {
                setPadding(0, dp(6), 0, dp(4))
            }
        )

        agregarDato(contenedor, "Temperatura", signo.temperatura, "°C")
        agregarDato(contenedor, "Presión sistólica", signo.presionSistolica, "mmHg")
        agregarDato(contenedor, "Presión diastólica", signo.presionDiastolica, "mmHg")
        agregarDato(contenedor, "Frecuencia cardíaca", signo.frecuenciaCardiaca, "lpm")
        agregarDato(contenedor, "Frecuencia respiratoria", signo.frecuenciaRespiratoria, "rpm")
        agregarDato(contenedor, "Saturación de oxígeno", signo.saturacionOxigeno, "%")
        agregarDato(contenedor, "Peso", signo.peso, "kg")
        agregarDato(contenedor, "Observaciones", signo.observaciones)
    }

    // =========================================================
    // CONTROLES PERIÓDICOS
    // =========================================================
    private fun agregarControlPeriodico(
        contenedor: LinearLayout,
        control: ControlesPeriodicosBitacora,
        numero: Int
    ) {

        val tarjeta = crearTarjetaInterna()

        tarjeta.addView(
            crearTexto(
                "Control periódico $numero",
                15f,
                "#2459C4",
                true
            )
        )

        agregarDato(
            tarjeta,
            "Fecha del control",
            formatearFechaHora(control.fechaControl)
        )

        agregarDato(
            tarjeta,
            "Fecha de registro",
            formatearFechaHora(control.fechaRegistro)
        )

        // Peso y talla.
        agregarTituloSeccion(
            tarjeta,
            "Peso y talla"
        )

        agregarDato(
            tarjeta,
            "Estado",
            control.pesoTallaEstado
        )

        agregarDato(
            tarjeta,
            "Peso",
            control.pesoKg?.toString(),
            "kg"
        )

        agregarDato(
            tarjeta,
            "Talla",
            control.tallaCm?.toString(),
            "cm"
        )

        // Glicemia.
        agregarTituloSeccion(
            tarjeta,
            "Glicemia"
        )

        agregarDato(
            tarjeta,
            "Estado",
            control.glicemiaEstado
        )

        agregarDato(
            tarjeta,
            "Valor",
            control.valorGlicemia?.toString(),
            "mg/dL"
        )

        // Terapia física.
        agregarTituloSeccion(
            tarjeta,
            "Terapia física"
        )

        agregarDato(
            tarjeta,
            "Estado",
            control.terapiaFisicaEstado
        )

        agregarDato(
            tarjeta,
            "Observaciones",
            control.terapiaFisicaObservaciones
        )

        // Terapia respiratoria.
        agregarTituloSeccion(
            tarjeta,
            "Terapia respiratoria"
        )

        agregarDato(
            tarjeta,
            "Estado",
            control.terapiaRespiratoriaEstado
        )

        agregarDato(
            tarjeta,
            "Observaciones",
            control.terapiaRespiratoriaObservaciones
        )

        // Corte de uñas y cabello.
        agregarTituloSeccion(
            tarjeta,
            "Cuidado personal"
        )

        agregarDato(
            tarjeta,
            "Corte de uñas",
            control.corteUnasEstado
        )

        agregarDato(
            tarjeta,
            "Corte de cabello",
            control.corteCabelloEstado
        )

        // Observaciones generales.
        agregarTituloSeccion(
            tarjeta,
            "Observaciones generales"
        )

        agregarDato(
            tarjeta,
            "Detalle",
            control.observacionesGenerales
        )

        agregarDato(
            tarjeta,
            "Próximo control",
            formatearFechaHora(control.fechaProximoControl)
        )

        contenedor.addView(tarjeta)
    }

    // =========================================================
    // CREAR TARJETA INTERNA
    // =========================================================
    private fun crearTarjetaInterna(): LinearLayout {

        return LinearLayout(requireContext()).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
            )

            setBackgroundColor(
                Color.parseColor("#F4F7FC")
            )

            val parametros = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            parametros.topMargin = dp(10)

            layoutParams = parametros
        }
    }

    // =========================================================
    // TÍTULOS DE SECCIÓN
    // =========================================================
    private fun agregarTituloSeccion(
        contenedor: LinearLayout,
        titulo: String
    ) {

        contenedor.addView(
            crearTexto(
                titulo,
                15f,
                "#2459C4",
                true
            ).apply {
                setPadding(
                    0,
                    dp(16),
                    0,
                    dp(5)
                )
            }
        )
    }

    // =========================================================
    // AGREGAR DATO
    // =========================================================
    private fun agregarDato(
        contenedor: LinearLayout,
        etiqueta: String,
        valor: String?,
        unidad: String = ""
    ) {

        if (valor.isNullOrBlank()) return

        val textoValor = if (unidad.isNotBlank()) {
            "$valor $unidad"
        } else {
            valor
        }

        val fila = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(5), 0, dp(5))
        }

        fila.addView(
            crearTexto(
                etiqueta,
                12f,
                "#64748B"
            )
        )

        fila.addView(
            crearTexto(
                textoValor,
                14f,
                "#1E293B"
            ).apply {
                setPadding(0, dp(2), 0, 0)
            }
        )

        contenedor.addView(fila)
    }

    // =========================================================
    // MENSAJES SIN INFORMACIÓN
    // =========================================================
    private fun agregarMensaje(
        contenedor: LinearLayout,
        mensaje: String
    ) {

        contenedor.addView(
            crearTexto(
                mensaje,
                13f,
                "#64748B"
            ).apply {
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(8)
                )
            }
        )
    }

    // =========================================================
    // CREAR TEXTO
    // =========================================================
    private fun crearTexto(
        texto: String,
        tamano: Float,
        color: String,
        negrita: Boolean = false
    ): TextView {

        return TextView(requireContext()).apply {

            text = texto
            textSize = tamano
            setTextColor(Color.parseColor(color))

            if (negrita) {
                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            }
        }
    }

    // =========================================================
    // FORMATEAR FECHA Y HORA A HORARIO COLOMBIANO
    // =========================================================
    private fun formatearFechaHora(
        fecha: String?
    ): String {

        if (fecha.isNullOrBlank()) {
            return "No disponible"
        }

        return try {

            if (fecha.length < 19) {
                return fecha
            }

            val fechaSinZona = fecha.substring(0, 19)

            val entrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
            ).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val salida = SimpleDateFormat(
                "dd/MM/yyyy · hh:mm a",
                Locale("es", "CO")
            ).apply {
                timeZone = TimeZone.getTimeZone("America/Bogota")
            }

            salida.format(
                entrada.parse(fechaSinZona)!!
            )

        } catch (e: Exception) {

            Log.e(
                "RESUMEN_CUIDADOS",
                "Error formateando fecha: $fecha",
                e
            )

            fecha
        }
    }

    // =========================================================
    // ESTADOS DE LA PANTALLA
    // =========================================================
    private fun mostrarCargando() {

        binding.contenedorCargandoResumen.visibility = View.VISIBLE
        binding.contenedorEstadoResumen.visibility = View.GONE
        binding.scrollResumen.visibility = View.GONE
    }

    private fun mostrarEstado(mensaje: String) {

        binding.contenedorCargandoResumen.visibility = View.GONE
        binding.scrollResumen.visibility = View.GONE
        binding.contenedorEstadoResumen.visibility = View.VISIBLE

        binding.tvEstadoResumen.text = mensaje
    }

    // =========================================================
    // CONVERSIÓN DP
    // =========================================================
    private fun dp(valor: Int): Int {

        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}