package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.DocumentoHistoriaClinica
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentHistoriaClinicaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

class HistoriaClinicaFragment : Fragment() {

    private var _binding: FragmentHistoriaClinicaBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var idPaciente: Int? = null
    private var archivoPdfSeleccionado: Uri? = null
    private var nombreArchivoPDF: String? = null
    private var subiendoPDF = false

    companion object {
        private const val MAX_TAMANO_PDF = 10L * 1024L * 1024L
    }

    private val seleccionarPDF =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                validarYSeleccionarPDF(uri)
            }
        }

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
        configurarSelectorPDF()
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
                    limpiarSeleccionPDF()
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
    // SELECTOR PDF
    // ==========================================

    private fun configurarSelectorPDF() {

        binding.btnSeleccionarPDF.setOnClickListener {
            seleccionarPDF.launch(arrayOf("application/pdf"))
        }

        binding.btnSubirPDF.setOnClickListener {
            subirDocumentoPDF()
        }

        binding.btnSubirPDF.isEnabled = false
    }

    // ==========================================
    // VALIDAR PDF
    // ==========================================

    private fun validarYSeleccionarPDF(uri: Uri) {

        try {
            val resolver = requireContext().contentResolver
            val nombre = obtenerNombreArchivo(uri)

            if (!nombre.lowercase(Locale.ROOT).endsWith(".pdf")) {
                mostrarMensaje("Selecciona un archivo con formato PDF")
                return
            }

            val tipoMime = resolver.getType(uri)

            if (tipoMime != null && tipoMime != "application/pdf") {
                mostrarMensaje("El archivo seleccionado no es un PDF válido")
                return
            }

            val tamano = obtenerTamanoArchivo(uri)

            if (tamano <= 0L) {
                mostrarMensaje("No se pudo verificar el tamaño del archivo")
                return
            }

            if (tamano > MAX_TAMANO_PDF) {
                mostrarMensaje("El PDF no puede superar los 10 MB")
                return
            }

            archivoPdfSeleccionado = uri
            nombreArchivoPDF = nombre

            binding.tvNombreArchivoPDF.text =
                "📄 $nombre\nTamaño: ${formatearTamano(tamano)}"

            binding.btnSubirPDF.isEnabled = true
            binding.btnSubirPDF.text = "Subir documento"

            mostrarMensaje("PDF seleccionado correctamente")

        } catch (e: Exception) {
            android.util.Log.e(
                "HISTORIA_CLINICA",
                "Error al seleccionar PDF",
                e
            )
            mostrarMensaje("No se pudo leer el documento seleccionado")
        }
    }

    // ==========================================
    // SUBIR DOCUMENTO PDF
    // ==========================================

    private fun subirDocumentoPDF() {

        val pacienteId = idPaciente
        val uri = archivoPdfSeleccionado
        val nombre = nombreArchivoPDF

        if (pacienteId == null || uri == null || nombre == null) {
            mostrarMensaje("Selecciona un paciente y un PDF")
            return
        }

        if (subiendoPDF) return

        subiendoPDF = true
        binding.btnSubirPDF.isEnabled = false
        binding.btnSubirPDF.text = "Subiendo documento..."

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val archivoBytes = withContext(Dispatchers.IO) {
                    leerPDFValidado(uri)
                }

                val nombreBody = nombre
                val requestFile = archivoBytes.toRequestBody(
                    "application/pdf".toMediaTypeOrNull()
                )

                val archivoPart = MultipartBody.Part.createFormData(
                    "archivo",
                    nombreBody,
                    requestFile
                )

                val pacienteBody = pacienteId.toString().toRequestBody(
                    "text/plain".toMediaTypeOrNull()
                )

                val descripcionBody = "Documento de historia clínica".toRequestBody(
                    "text/plain".toMediaTypeOrNull()
                )

                val respuesta =
                    RetrofitClient.apiService.subirDocumentoHistoriaClinica(
                        idPaciente = pacienteBody,
                        archivo = archivoPart,
                        descripcion = descripcionBody
                    )

                if (!isAdded) return@launch

                if (respuesta.isSuccessful && respuesta.body() != null) {

                    archivoPdfSeleccionado = null
                    nombreArchivoPDF = null

                    binding.tvNombreArchivoPDF.text =
                        "Ningún archivo seleccionado"

                    binding.btnSubirPDF.isEnabled = false
                    binding.btnSubirPDF.text = "Subir documento"

                    mostrarMensaje("Documento PDF subido correctamente")

                    cargarDocumentosPDF()

                } else {

                    val error = respuesta.errorBody()?.string()

                    android.util.Log.e(
                        "HISTORIA_CLINICA",
                        "Error HTTP ${respuesta.code()}: $error"
                    )

                    mostrarMensaje(
                        "No se pudo subir el documento. Código: ${respuesta.code()}"
                    )
                }

            } catch (e: Exception) {

                android.util.Log.e(
                    "HISTORIA_CLINICA",
                    "Error al subir PDF",
                    e
                )

                if (isAdded) {
                    mostrarMensaje("Error al subir el documento PDF")
                }

            } finally {

                subiendoPDF = false

                if (isAdded && archivoPdfSeleccionado != null) {
                    binding.btnSubirPDF.isEnabled = true
                    binding.btnSubirPDF.text = "Subir documento"
                }
            }
        }
    }

    // ==========================================
    // LEER PDF Y COMPROBAR TAMAÑO
    // ==========================================

    private suspend fun leerPDFValidado(uri: Uri): ByteArray =
        withContext(Dispatchers.IO) {

            val input = requireContext()
                .contentResolver
                .openInputStream(uri)
                ?: throw Exception("No se pudo abrir el PDF")

            input.use { stream ->

                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0L

                while (true) {
                    val leidos = stream.read(buffer)

                    if (leidos == -1) break

                    total += leidos

                    if (total > MAX_TAMANO_PDF) {
                        throw Exception("El PDF supera los 10 MB")
                    }

                    output.write(buffer, 0, leidos)
                }

                val bytes = output.toByteArray()

                if (bytes.size < 5 ||
                    !String(bytes, 0, 5, Charsets.US_ASCII).startsWith("%PDF-")
                ) {
                    throw Exception("El archivo no contiene una cabecera PDF válida")
                }

                bytes
            }
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
                        .filter { it.estado }

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
    // OBTENER NOMBRE DEL ARCHIVO
    // ==========================================

    private fun obtenerNombreArchivo(uri: Uri): String {

        var nombre = "documento.pdf"

        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {
                val columna = cursor.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

                if (columna >= 0) {
                    nombre = cursor.getString(columna)
                        ?: "documento.pdf"
                }
            }
        }

        return nombre
    }

    // ==========================================
    // OBTENER TAMAÑO
    // ==========================================

    private fun obtenerTamanoArchivo(uri: Uri): Long {

        requireContext().contentResolver.query(
            uri,
            arrayOf(OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val columna = cursor.getColumnIndex(
                    OpenableColumns.SIZE
                )

                if (columna >= 0 && !cursor.isNull(columna)) {
                    val tamano = cursor.getLong(columna)

                    if (tamano >= 0L) return tamano
                }
            }
        }

        return requireContext()
            .contentResolver
            .openInputStream(uri)
            ?.use { input ->

                val buffer = ByteArray(8192)
                var total = 0L

                while (true) {
                    val leidos = input.read(buffer)

                    if (leidos == -1) break

                    total += leidos

                    if (total > MAX_TAMANO_PDF) return total
                }

                total
            } ?: -1L
    }

    // ==========================================
    // LIMPIAR SELECCIÓN
    // ==========================================

    private fun limpiarSeleccionPDF() {

        archivoPdfSeleccionado = null
        nombreArchivoPDF = null

        binding.tvNombreArchivoPDF.text =
            "Ningún archivo seleccionado"

        binding.btnSubirPDF.isEnabled = false
        binding.btnSubirPDF.text = "Subir documento"
    }

    // ==========================================
    // FORMATEAR TAMAÑO
    // ==========================================

    private fun formatearTamano(tamano: Long): String {

        return String.format(
            Locale.US,
            "%.2f MB",
            tamano / (1024.0 * 1024.0)
        )
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