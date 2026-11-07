package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.os.Bundle
import android.util.Log
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
import com.example.molvigeryapp.databinding.ItemPacienteResumenBinding
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
    // TARJETA PRINCIPAL DEL PACIENTE - XML
    // =========================================================

    private fun agregarTarjetaPaciente(
        paciente: PacienteResumenTurno
    ) {

        val itemBinding = ItemPacienteResumenBinding.inflate(
            layoutInflater,
            binding.contenedorPacientesResumen,
            false
        )

        // Nombre completo.
        val nombre = listOfNotNull(
            paciente.nombre,
            paciente.apellido
        ).joinToString(" ").ifBlank {
            "Paciente"
        }

        itemBinding.tvNombrePaciente.text = nombre

        // Habitación y cama.
        val ubicacion = buildString {

            if (!paciente.habitacion.isNullOrBlank()) {
                append("Habitación ${paciente.habitacion}")
            }

            if (!paciente.cama.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append("Cama ${paciente.cama}")
            }
        }

        itemBinding.tvUbicacionPaciente.text =
            ubicacion.ifBlank {
                "Ubicación no disponible"
            }

        // Cantidad de bitácoras y controles.
        val cantidadBitacoras = paciente.bitacoras.size
        val cantidadControles = paciente.controlesPeriodicos.size

        itemBinding.tvCantidadBitacoras.text =
            "$cantidadBitacoras " +
                    if (cantidadBitacoras == 1) "registro" else "registros"

        itemBinding.tvCantidadControles.text =
            "$cantidadControles " +
                    if (cantidadControles == 1) "registro" else "registros"

        // =====================================================
        // BITÁCORAS
        // =====================================================

        val contenedorBitacoras =
            itemBinding.contenedorBitacorasResumen

        contenedorBitacoras.removeAllViews()

        if (paciente.bitacoras.isEmpty()) {

            agregarMensaje(
                contenedorBitacoras,
                "No hay bitácoras registradas durante este turno."
            )

        } else {

            paciente.bitacoras.forEachIndexed { index, bitacora ->

                agregarBitacora(
                    contenedorBitacoras,
                    bitacora,
                    index + 1
                )
            }
        }

        // =====================================================
        // CONTROLES PERIÓDICOS
        // =====================================================

        val contenedorControles =
            itemBinding.contenedorControlesPeriodicosResumen

        contenedorControles.removeAllViews()

        if (paciente.controlesPeriodicos.isEmpty()) {

            agregarMensaje(
                contenedorControles,
                "No hay controles periódicos registrados durante este turno."
            )

        } else {

            paciente.controlesPeriodicos.forEachIndexed { index, control ->

                agregarControlPeriodico(
                    contenedorControles,
                    control,
                    index + 1
                )
            }
        }

        // =====================================================
        // EXPANDIR Y CONTRAER
        // =====================================================

        itemBinding.contenedorContenidoPaciente.visibility =
            View.GONE

        itemBinding.ivFlechaPaciente.rotation = 0f

        itemBinding.encabezadoPaciente.setOnClickListener {

            val expandido =
                itemBinding.contenedorContenidoPaciente.visibility ==
                        View.VISIBLE

            itemBinding.contenedorContenidoPaciente.visibility =
                if (expandido) View.GONE else View.VISIBLE

            itemBinding.ivFlechaPaciente.rotation =
                if (expandido) 0f else 180f
        }

        // Agregar tarjeta al listado.
        binding.contenedorPacientesResumen.addView(
            itemBinding.root
        )
    }

    // =========================================================
    // BITÁCORAS DIARIAS
    // =========================================================

    private fun agregarBitacora(
        contenedor: LinearLayout,
        bitacora: BitacoraResumenTurno,
        numero: Int
    ) {

        agregarTituloSeccion(
            contenedor,
            "Registro $numero"
        )

        agregarDato(
            contenedor,
            "Fecha y hora",
            formatearFechaHora(bitacora.fechaHora)
        )

        agregarDato(
            contenedor,
            "Tipo de registro",
            bitacora.tipoRegistro
        )

        agregarDato(
            contenedor,
            "Descripción",
            bitacora.descripcion
        )

        agregarDato(
            contenedor,
            "Estado",
            if (bitacora.estado) "Registrada" else "Inactiva"
        )

        // Signos vitales asociados a la bitácora.
        agregarTituloSeccion(
            contenedor,
            "Signos vitales"
        )

        if (bitacora.signosVitales.isEmpty()) {

            agregarMensaje(
                contenedor,
                "No se registraron signos vitales."
            )

        } else {

            bitacora.signosVitales.forEachIndexed { index, signo ->

                agregarSignosVitales(
                    contenedor,
                    signo,
                    index + 1
                )
            }
        }
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
                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(4)
                )
            }
        )

        agregarDato(
            contenedor,
            "Temperatura",
            signo.temperatura,
            "°C"
        )

        agregarDato(
            contenedor,
            "Presión sistólica",
            signo.presionSistolica,
            "mmHg"
        )

        agregarDato(
            contenedor,
            "Presión diastólica",
            signo.presionDiastolica,
            "mmHg"
        )

        agregarDato(
            contenedor,
            "Frecuencia cardíaca",
            signo.frecuenciaCardiaca,
            "lpm"
        )

        agregarDato(
            contenedor,
            "Frecuencia respiratoria",
            signo.frecuenciaRespiratoria,
            "rpm"
        )

        agregarDato(
            contenedor,
            "Saturación de oxígeno",
            signo.saturacionOxigeno,
            "%"
        )

        agregarDato(
            contenedor,
            "Peso",
            signo.peso,
            "kg"
        )

        agregarDato(
            contenedor,
            "Observaciones",
            signo.observaciones
        )
    }

    // =========================================================
    // CONTROLES PERIÓDICOS
    // =========================================================

    private fun agregarControlPeriodico(
        contenedor: LinearLayout,
        control: ControlesPeriodicosBitacora,
        numero: Int
    ) {

        agregarTituloSeccion(
            contenedor,
            "Control periódico $numero"
        )

        agregarDato(
            contenedor,
            "Fecha del control",
            formatearFechaHora(control.fechaControl)
        )

        agregarDato(
            contenedor,
            "Fecha de registro",
            formatearFechaHora(control.fechaRegistro)
        )

        // Peso y talla.
        agregarTituloSeccion(
            contenedor,
            "Peso y talla"
        )

        agregarDato(
            contenedor,
            "Estado",
            control.pesoTallaEstado
        )

        agregarDato(
            contenedor,
            "Peso",
            control.pesoKg?.toString(),
            "kg"
        )

        agregarDato(
            contenedor,
            "Talla",
            control.tallaCm?.toString(),
            "cm"
        )

        // Glicemia.
        agregarTituloSeccion(
            contenedor,
            "Glicemia"
        )

        agregarDato(
            contenedor,
            "Estado",
            control.glicemiaEstado
        )

        agregarDato(
            contenedor,
            "Valor",
            control.valorGlicemia?.toString(),
            "mg/dL"
        )

        // Terapia física.
        agregarTituloSeccion(
            contenedor,
            "Terapia física"
        )

        agregarDato(
            contenedor,
            "Estado",
            control.terapiaFisicaEstado
        )

        agregarDato(
            contenedor,
            "Observaciones",
            control.terapiaFisicaObservaciones
        )

        // Terapia respiratoria.
        agregarTituloSeccion(
            contenedor,
            "Terapia respiratoria"
        )

        agregarDato(
            contenedor,
            "Estado",
            control.terapiaRespiratoriaEstado
        )

        agregarDato(
            contenedor,
            "Observaciones",
            control.terapiaRespiratoriaObservaciones
        )

        // Cuidado personal.
        agregarTituloSeccion(
            contenedor,
            "Cuidado personal"
        )

        agregarDato(
            contenedor,
            "Corte de uñas",
            control.corteUnasEstado
        )

        agregarDato(
            contenedor,
            "Corte de cabello",
            control.corteCabelloEstado
        )

        // Observaciones generales.
        agregarTituloSeccion(
            contenedor,
            "Observaciones generales"
        )

        agregarDato(
            contenedor,
            "Detalle",
            control.observacionesGenerales
        )

        agregarDato(
            contenedor,
            "Próximo control",
            formatearFechaHora(control.fechaProximoControl)
        )
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
                "#1E293B",
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

            setPadding(
                0,
                dp(5),
                0,
                dp(5)
            )
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
                setPadding(
                    0,
                    dp(2),
                    0,
                    0
                )
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

            setTextColor(
                android.graphics.Color.parseColor(color)
            )

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

        binding.contenedorCargandoResumen.visibility =
            View.VISIBLE

        binding.contenedorEstadoResumen.visibility =
            View.GONE

        binding.scrollResumen.visibility =
            View.GONE
    }

    private fun mostrarEstado(mensaje: String) {

        binding.contenedorCargandoResumen.visibility =
            View.GONE

        binding.scrollResumen.visibility =
            View.GONE

        binding.contenedorEstadoResumen.visibility =
            View.VISIBLE

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

    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}