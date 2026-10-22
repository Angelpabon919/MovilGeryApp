package com.example.molvigeryapp.ui.cuidador.bitacora

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.SignosVitales
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentEventosAdversosBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModelFactory
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventosAdversosFragment : Fragment() {

    private var _binding: FragmentEventosAdversosBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var idPaciente: Int? = null
    private var idBitacora: Int? = null

    // Foto seleccionada para mostrar en el formulario.
    private var fotoBitmap: Bitmap? = null
    private var fotoUri: Uri? = null

    // Abrir cámara y obtener una vista previa.
    private val tomarFotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->

            if (bitmap != null) {
                fotoBitmap = bitmap
                fotoUri = null

                binding.ivFoto.setImageBitmap(bitmap)
                binding.ivFoto.visibility = View.VISIBLE

                Toast.makeText(
                    requireContext(),
                    "Foto tomada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // Seleccionar una imagen de la galería.
    private val seleccionarFotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {
                fotoUri = uri
                fotoBitmap = null

                binding.ivFoto.setImageURI(uri)
                binding.ivFoto.visibility = View.VISIBLE

                Toast.makeText(
                    requireContext(),
                    "Foto seleccionada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventosAdversosBinding.inflate(
            inflater,
            container,
            false
        )

        idBitacora = arguments?.getInt("ID_BITACORA")
            ?.takeIf { it != 0 }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observarPaciente()
        observarBitacora()
        configurarBotones()

        binding.ivFoto.visibility = View.GONE
    }

    private fun observarPaciente() {
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            paciente?.let {
                idPaciente = it.idPaciente

                Log.d(
                    "EVENTO_ADVERSO",
                    "Paciente seleccionado: ${it.idPaciente}"
                )
            }
        }
    }

    private fun observarBitacora() {
        pacienteViewModel.idBitacora.observe(viewLifecycleOwner) { id ->
            id?.let {
                idBitacora = it

                Log.d(
                    "EVENTO_ADVERSO",
                    "ID Bitácora recibido: $it"
                )
            }
        }
    }

    private fun configurarBotones() {
        binding.btnAdjuntarFoto.setOnClickListener {
            mostrarOpcionesFoto()
        }

        binding.btnGuardar.setOnClickListener {
            guardarEvento()
        }

        binding.btnCancelar.setOnClickListener {
            limpiarFormulario()
        }
    }

    private fun mostrarOpcionesFoto() {
        val opciones = arrayOf(
            "Tomar foto con la cámara",
            "Seleccionar de la galería"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Adjuntar fotografía")
            .setItems(opciones) { _, seleccion ->

                when (seleccion) {
                    0 -> tomarFotoLauncher.launch(null)
                    1 -> seleccionarFotoLauncher.launch("image/*")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun obtenerFechaHoraActual(): String {
        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        return formato.format(Date())
    }

    private fun guardarEvento() {
        val bitacoraId = idBitacora

        if (bitacoraId == null) {
            Toast.makeText(
                requireContext(),
                "Aún no se ha creado la bitácora",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val pacienteId = idPaciente

        if (pacienteId == null) {
            Toast.makeText(
                requireContext(),
                "No se encontró el paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val descripcion = binding.etDescripcion.text.toString().trim()
        val acciones = binding.etAccionesRealizadas.text.toString().trim()

        if (descripcion.isEmpty()) {
            binding.etDescripcion.error = "Ingrese una descripción"
            return
        }

        if (acciones.isEmpty()) {
            binding.etAccionesRealizadas.error =
                "Ingrese las acciones realizadas"
            return
        }

        val idTipoEmergencia = when {
            binding.rbLeve.isChecked -> 1
            binding.rbModerado.isChecked -> 2
            binding.rbGrave.isChecked -> 3

            else -> {
                Toast.makeText(
                    requireContext(),
                    "Seleccione una evaluación",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val temperatura = binding.etTemperatura.text.toString().trim()
        val frecuenciaCardiaca = binding.etFrecuencia.text.toString().trim()
        val presionSistolica =
            binding.etPresionSistolica.text.toString().trim()
        val presionDiastolica =
            binding.etPresionDiastolica.text.toString().trim()
        val frecuenciaRespiratoria =
            binding.etFrecuenciaRespiratoria.text.toString().trim()
        val saturacion = binding.etSaturacion.text.toString().trim()
        val peso = binding.etPeso.text.toString().trim()
        val observaciones = binding.etObservaciones.text.toString().trim()
        val fechaHora = obtenerFechaHoraActual()

        binding.btnGuardar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Registrar el evento adverso.
                val evento = EventoAdverso(
                    idBitacora = bitacoraId,
                    idEvento = null,
                    idTipoEmergencia = idTipoEmergencia,
                    fechaHora = fechaHora,
                    descripcion = descripcion,
                    accionesRealizadas = acciones,
                    estado = "pendiente"
                )

                val respuestaEvento =
                    RetrofitClient.apiService.crearEventoAdverso(evento)

                Log.d(
                    "EVENTO_ADVERSO",
                    "Evento creado: ${respuestaEvento.idEventoAdverso}"
                )

                // 2. Registrar los signos vitales.
                val signos = SignosVitales(
                    idBitacora = bitacoraId,
                    temperatura = temperatura,
                    presionSistolica = presionSistolica,
                    presionDiastolica = presionDiastolica,
                    frecuenciaCardiaca = frecuenciaCardiaca,
                    frecuenciaRespiratoria = frecuenciaRespiratoria,
                    saturacionOxigeno = saturacion,
                    peso = peso,
                    fechaHora = fechaHora,
                    observaciones = observaciones
                )

                RetrofitClient.apiService.crearSignosVitales(signos)

                // La foto todavía no se envía al backend.
                val mensaje = if (fotoBitmap != null || fotoUri != null) {
                    "Evento y signos vitales guardados. " +
                            "La foto solo está en el formulario y no se ha subido al servidor."
                } else {
                    "Evento adverso y signos vitales guardados correctamente"
                }

                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()

                limpiarFormulario()

            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()

                Log.e("EVENTO_ADVERSO", "Código HTTP: ${e.code()}")
                Log.e("EVENTO_ADVERSO", "Respuesta de la API: $errorBody")

                if (_binding != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error ${e.code()}: $errorBody",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(
                    "EVENTO_ADVERSO",
                    "Error inesperado al guardar evento",
                    e
                )

                if (_binding != null) {
                    Toast.makeText(
                        requireContext(),
                        "Error inesperado: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {
                if (_binding != null) {
                    binding.btnGuardar.isEnabled = true
                }
            }
        }
    }

    private fun limpiarFormulario() {
        binding.etDescripcion.text.clear()
        binding.etTemperatura.text.clear()
        binding.etFrecuencia.text.clear()
        binding.etPresionSistolica.text.clear()
        binding.etPresionDiastolica.text.clear()
        binding.etFrecuenciaRespiratoria.text.clear()
        binding.etSaturacion.text.clear()
        binding.etPeso.text.clear()
        binding.etObservaciones.text.clear()
        binding.etAccionesRealizadas.text.clear()

        binding.rgEvaluacion.clearCheck()

        // Limpiar la fotografía seleccionada.
        fotoBitmap = null
        fotoUri = null

        binding.ivFoto.setImageDrawable(null)
        binding.ivFoto.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}