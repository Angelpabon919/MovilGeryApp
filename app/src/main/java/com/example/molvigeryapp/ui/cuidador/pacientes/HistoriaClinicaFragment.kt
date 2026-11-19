package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.DocumentoHistoriaClinica
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentHistoriaClinicaBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HistoriaClinicaFragment : Fragment() {

    private var _binding: FragmentHistoriaClinicaBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var idPaciente: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoriaClinicaBinding.inflate(
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

        configurarBotonVolver()
        observarPaciente()
    }

    // ==========================================
    // BOTÓN VOLVER
    // ==========================================

    private fun configurarBotonVolver() {
        binding.btnVolver.setOnClickListener {
            requireActivity()
                .onBackPressedDispatcher
                .onBackPressed()
        }
    }

    // ==========================================
    // OBSERVAR PACIENTE
    // ==========================================

    private fun observarPaciente() {
        pacienteViewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            paciente?.let {

                if (idPaciente != it.idPaciente) {
                    binding.contenedorDocumentosPDF.removeAllViews()
                }

                idPaciente = it.idPaciente

                val nombreCompleto =
                    "${it.nombre ?: ""} ${it.apellido ?: ""}".trim()

                binding.tvNombrePaciente.text =
                    nombreCompleto.ifBlank { "Paciente sin nombre" }

                cargarHistoriaClinica()
                cargarDocumentosPDF()
            }
        }
    }

    // ==========================================
    // CARGAR HISTORIA CLÍNICA
    // ==========================================

    private fun cargarHistoriaClinica() {

        val pacienteId = idPaciente ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val historias =
                    RetrofitClient.apiService.getHistoriasClinicas()

                val historia = historias.find {
                    it.idPaciente == pacienteId
                }

                if (!isAdded) return@launch

                if (historia != null) {
                    mostrarHistoriaClinica(historia)
                } else {
                    binding.tvNumeroHistoria.text =
                        "Sin historia clínica registrada"

                    binding.tvFechaApertura.text = "Sin información"
                    binding.tvAntecedentes.text = "Ninguna"
                    binding.tvAlergias.text = "Ninguna"
                    binding.tvObservaciones.text = "Ninguna"

                    Toast.makeText(
                        requireContext(),
                        "Este paciente no tiene historia clínica registrada",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "HISTORIA_CLINICA",
                    "Error al cargar historia clínica",
                    e
                )

                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Error al cargar la historia clínica",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun mostrarHistoriaClinica(
        historia: com.example.molvigeryapp.data.model.HistoriaClinica
    ) {
        binding.tvNumeroHistoria.text =
            "Historia clínica #${historia.idHistoriaClinica ?: "-"}"

        binding.tvFechaApertura.text =
            formatearFecha(historia.fechaApertura)

        binding.tvAntecedentes.text =
            historia.antecedentes.ifBlank { "Ninguna" }

        binding.tvAlergias.text =
            historia.alergias.ifBlank { "Ninguna" }

        binding.tvObservaciones.text =
            historia.observaciones.ifBlank { "Ninguna" }
    }

    // ==========================================
    // CONSULTAR DOCUMENTOS DEL PACIENTE
    // ==========================================

    private fun cargarDocumentosPDF() {

        val pacienteId = idPaciente ?: return

        binding.tvEstadoDocumentosPDF.text =
            "Cargando documentos..."

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val respuesta =
                    RetrofitClient.apiService.obtenerDocumentosHistoriaClinica(
                        pacienteId
                    )

                if (!isAdded) return@launch

                if (respuesta.isSuccessful) {

                    val documentos = respuesta.body().orEmpty()
                        .filter {
                            it.estado &&
                                (it.idPaciente == null || it.idPaciente == pacienteId)
                        }

                    mostrarDocumentosPDF(documentos)

                } else {

                    binding.tvEstadoDocumentosPDF.text =
                        "No se pudieron consultar los documentos"

                    android.util.Log.e(
                        "HISTORIA_CLINICA",
                        "Error consultando documentos: ${respuesta.code()} " +
                                respuesta.errorBody()?.string()
                    )
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "HISTORIA_CLINICA",
                    "Error cargando documentos PDF",
                    e
                )

                if (isAdded) {
                    binding.tvEstadoDocumentosPDF.text =
                        "Error de conexión al consultar documentos"
                }
            }
        }
    }

    // ==========================================
    // MOSTRAR DOCUMENTOS EN LA TARJETA
    // ==========================================

    private fun mostrarDocumentosPDF(
        documentos: List<DocumentoHistoriaClinica>
    ) {

        binding.contenedorDocumentosPDF.removeAllViews()

        if (documentos.isEmpty()) {
            binding.tvEstadoDocumentosPDF.text =
                "Este paciente todavía no tiene documentos PDF."

            return
        }

        binding.tvEstadoDocumentosPDF.text =
            "${documentos.size} documento(s) registrado(s)"

        documentos.forEach { documento ->

            val fila = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12)
                setBackgroundResource(R.drawable.bg_pdf_selector)

                val parametros = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                parametros.bottomMargin = 10
                layoutParams = parametros
            }

            val nombre = TextView(requireContext()).apply {
                text = "📄 ${documento.nombreArchivo ?: "Documento PDF"}"
                textSize = 14f
                setTextColor(
                    android.graphics.Color.rgb(30, 41, 59)
                )
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val fecha = TextView(requireContext()).apply {
                text = "Fecha: ${documento.fechaSubida ?: "No disponible"}"
                textSize = 12f
                setTextColor(
                    android.graphics.Color.rgb(100, 116, 139)
                )
            }

            val botonAbrir = Button(requireContext()).apply {
                text = "Abrir documento"
                isAllCaps = false

                setOnClickListener {
                    abrirDocumentoPDF(documento)
                }
            }

            fila.addView(nombre)
            fila.addView(fecha)
            fila.addView(botonAbrir)

            binding.contenedorDocumentosPDF.addView(fila)
        }
    }

    // ==========================================
    // ABRIR DOCUMENTO PDF
    // ==========================================

    private fun abrirDocumentoPDF(
        documento: DocumentoHistoriaClinica
    ) {

        val ruta = documento.archivo

        if (ruta.isNullOrBlank()) {
            mostrarMensaje("El documento no tiene una dirección disponible")
            return
        }

        try {

            val url = if (
                ruta.startsWith("http://", true) ||
                ruta.startsWith("https://", true)
            ) {
                ruta
            } else {
                val dominio = RetrofitClient.BASE_URL
                    .substringBefore("/api/")
                    .trimEnd('/')

                dominio + "/" + ruta.trimStart('/')
            }

            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            startActivity(
                Intent.createChooser(
                    intent,
                    "Abrir documento clínico"
                )
            )

        } catch (e: Exception) {

            android.util.Log.e(
                "HISTORIA_CLINICA",
                "Error abriendo PDF",
                e
            )

            mostrarMensaje("No se pudo abrir el documento")
        }
    }

    // ==========================================
    // FORMATEAR FECHA
    // ==========================================

    private fun formatearFecha(fecha: String): String {

        return try {

            val formatoEntrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                Locale.US
            )

            val formatoSalida = SimpleDateFormat(
                "d 'de' MMMM 'de' yyyy",
                Locale("es", "ES")
            )

            val fechaConvertida = formatoEntrada.parse(fecha)

            fechaConvertida?.let {
                formatoSalida.format(it)
            } ?: fecha

        } catch (e: Exception) {
            fecha
        }
    }

    // ==========================================
    // MENSAJES
    // ==========================================

    private fun mostrarMensaje(mensaje: String) {
        if (isAdded) {
            Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // DESTRUIR VISTA
    // ==========================================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
