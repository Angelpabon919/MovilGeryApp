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
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.DialogAdjuntarFotografiaBinding
import com.example.molvigeryapp.databinding.FragmentEventosAdversosBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModelFactory
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class EventosAdversosFragment : Fragment() {

    private var _binding: FragmentEventosAdversosBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var idPaciente: Int? = null
    private var idBitacora: Int? = null

    // Fotografía seleccionada.
    private var fotoBitmap: Bitmap? = null
    private var fotoUri: Uri? = null

    // Tomar fotografía con la cámara.
    private val tomarFotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->

            if (bitmap != null) {
                fotoBitmap = bitmap
                fotoUri = null

                _binding?.let {
                    it.ivFoto.setImageBitmap(bitmap)
                    it.ivFoto.visibility = View.VISIBLE
                }

                Toast.makeText(
                    requireContext(),
                    "Foto tomada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val solicitarPermisoCamaraLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->

            if (concedido) {
                tomarFotoLauncher.launch(null)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Debes permitir el acceso a la cámara para tomar fotografías.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // Seleccionar fotografía de la galería.
    private val seleccionarFotoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            if (uri != null) {
                fotoUri = uri
                fotoBitmap = null

                _binding?.let {
                    it.ivFoto.setImageURI(uri)
                    it.ivFoto.visibility = View.VISIBLE
                }

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

    // Obtener paciente seleccionado.
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

    // Obtener ID de la bitácora.
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

    // Configurar botones.
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

    // Opciones para adjuntar fotografía.
    private fun mostrarOpcionesFoto() {

        val dialogBinding = DialogAdjuntarFotografiaBinding.inflate(
            layoutInflater
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialogBinding.opcionCamara.setOnClickListener {
            dialog.dismiss()
            abrirCamara()
        }

        dialogBinding.opcionGaleria.setOnClickListener {
            dialog.dismiss()
            seleccionarFotoLauncher.launch("image/*")
        }

        dialogBinding.btnCerrarDialogo.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    //permiso para abrir la camara

    private fun abrirCamara() {

        val permisoConcedido = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (permisoConcedido) {
            tomarFotoLauncher.launch(null)
        } else {
            solicitarPermisoCamaraLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }

    // Fecha y hora actual.
    private fun obtenerFechaHoraActual(): String {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        return formato.format(Date())
    }

    // Registrar evento adverso.
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

        val observaciones = binding.etObservaciones.text.toString().trim()

        val acciones = binding.etAccionesRealizadas.text.toString().trim()

        // Validar descripción.
        if (descripcion.isEmpty()) {
            binding.etDescripcion.error = "Ingrese una descripción"
            return
        }

        // Validar acciones realizadas.
        if (acciones.isEmpty()) {
            binding.etAccionesRealizadas.error =
                "Ingrese las acciones realizadas"
            return
        }

        // Obtener gravedad seleccionada.
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

        // Incorporar observaciones a la descripción,
        // ya que el modelo actual no tiene un campo independiente.
        val descripcionCompleta = if (observaciones.isNotEmpty()) {
            "$descripcion\n\nObservaciones generales:\n$observaciones"
        } else {
            descripcion
        }

        val fechaHora = obtenerFechaHoraActual()

        binding.btnGuardar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                // 1. Crear evento adverso.
                val evento = EventoAdverso(
                    idBitacora = bitacoraId,
                    idEvento = null,
                    idTipoEmergencia = idTipoEmergencia,
                    fechaHora = fechaHora,
                    descripcion = descripcionCompleta,
                    accionesRealizadas = acciones,
                    estado = "pendiente"
                )

                // 2. Enviar evento al backend.
                val respuestaEvento =
                    RetrofitClient.apiService.crearEventoAdverso(evento)

                Log.d(
                    "EVENTO_ADVERSO",
                    "Evento creado: ${respuestaEvento.idEventoAdverso}"
                )

                // 3. Confirmar registro.
                if (isAdded && _binding != null) {

                    val mensaje = if (
                        fotoBitmap != null || fotoUri != null
                    ) {
                        "Evento adverso guardado correctamente. " +
                                "La fotografía todavía no se ha subido al servidor."
                    } else {
                        "Evento adverso guardado correctamente"
                    }

                    Toast.makeText(
                        requireContext(),
                        mensaje,
                        Toast.LENGTH_LONG
                    ).show()

                    limpiarFormulario()
                }

            } catch (e: HttpException) {

                val errorBody = e.response()?.errorBody()?.string()

                Log.e("EVENTO_ADVERSO", "Código HTTP: ${e.code()}")
                Log.e("EVENTO_ADVERSO", "Respuesta API: $errorBody")

                _binding?.let {

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

                _binding?.let {

                    Toast.makeText(
                        requireContext(),
                        "Error inesperado: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {

                _binding?.btnGuardar?.isEnabled = true
            }
        }
    }

    // Limpiar formulario.
    private fun limpiarFormulario() {

        _binding?.let { b ->

            b.etDescripcion.text?.clear()
            b.etObservaciones.text?.clear()
            b.etAccionesRealizadas.text?.clear()

            b.rgEvaluacion.clearCheck()

            // Limpiar fotografía.
            fotoBitmap = null
            fotoUri = null

            b.ivFoto.setImageDrawable(null)
            b.ivFoto.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}