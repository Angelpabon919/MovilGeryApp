package com.example.molvigeryapp.ui.encargado.citas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Usuario
import com.example.molvigeryapp.data.repository.CitasRepository
import com.example.molvigeryapp.databinding.DialogEliminarCitaEncargadoBinding
import com.example.molvigeryapp.databinding.FragmentDetalleCitaEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DetalleCitaEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentDetalleCitaEncargadoBinding? = null

    private val binding
        get() = requireNotNull(_binding)

    // =========================================================
    // DATOS
    // =========================================================

    private var idCita: String? = null
    private var idPaciente: Int = -1

    // =========================================================
    // CONSTANTES
    // =========================================================

    private companion object {
        const val TAG = "DETALLE_CITA"
        const val INTERVALO_ACTUALIZACION = 30_000L
    }

    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetalleCitaEncargadoBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    // =========================================================
    // VISTA CREADA
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        idCita = arguments?.getString("idCita")
        idPaciente = arguments?.getInt("idPaciente", -1) ?: -1

        configurarBotonVolver()
        configurarBotonEditar()
        configurarBotonEliminar()

        cargarDetalle()
        iniciarActualizacionAutomatica()
    }

    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {
        binding.btnVolverDetalleCita.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // =========================================================
    // BOTÓN EDITAR
    // =========================================================

    private fun configurarBotonEditar() {
        binding.btnEditarCita.setOnClickListener {

            val identificador = idCita?.takeIf {
                it.isNotBlank()
            }

            if (identificador == null) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el identificador de la cita.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val datos = Bundle().apply {
                putBoolean("modoEdicion", true)
                putString("idCita", identificador)
                putInt("idPaciente", idPaciente)
            }

            val formulario = NuevaCitaEncargadoFragment().apply {
                arguments = datos
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, formulario)
                .addToBackStack(null)
                .commit()
        }
    }

    // =========================================================
    // BOTÓN ELIMINAR
    // =========================================================

    private fun configurarBotonEliminar() {
        binding.btnEliminarCita.setOnClickListener {

            val identificador = idCita?.takeIf {
                it.isNotBlank()
            }

            if (identificador == null) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el identificador de la cita.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val dialogBinding =
                DialogEliminarCitaEncargadoBinding.inflate(
                    LayoutInflater.from(requireContext())
                )

            val dialogo = AlertDialog.Builder(requireContext())
                .setView(dialogBinding.root)
                .create()

            dialogo.setOnShowListener {
                dialogo.window?.setBackgroundDrawableResource(
                    android.R.color.transparent
                )
            }

            dialogBinding.btnCancelarEliminarCita.setOnClickListener {
                dialogo.dismiss()
            }

            dialogBinding.btnConfirmarEliminarCita.setOnClickListener {

                dialogBinding.btnConfirmarEliminarCita.isEnabled = false
                dialogBinding.btnCancelarEliminarCita.isEnabled = false

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val resultado =
                            CitasRepository.eliminarCita(identificador)

                        if (!isAdded || _binding == null) {
                            dialogo.dismiss()
                            return@launch
                        }

                        resultado
                            .onSuccess {
                                dialogo.dismiss()

                                Toast.makeText(
                                    requireContext(),
                                    "Cita eliminada correctamente.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                parentFragmentManager.popBackStack()
                            }
                            .onFailure { error ->

                                Log.e(
                                    TAG,
                                    "Error eliminando la cita",
                                    error
                                )

                                dialogBinding
                                    .btnConfirmarEliminarCita
                                    .isEnabled = true

                                dialogBinding
                                    .btnCancelarEliminarCita
                                    .isEnabled = true

                                Toast.makeText(
                                    requireContext(),
                                    "No se pudo eliminar la cita: " +
                                            (error.message
                                                ?: "Error desconocido"),
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                    } catch (e: CancellationException) {
                        dialogo.dismiss()
                        throw e

                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Error inesperado eliminando la cita",
                            e
                        )

                        if (!isAdded || _binding == null) {
                            dialogo.dismiss()
                            return@launch
                        }

                        dialogBinding
                            .btnConfirmarEliminarCita
                            .isEnabled = true

                        dialogBinding
                            .btnCancelarEliminarCita
                            .isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            "Ocurrió un error al eliminar la cita.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            dialogo.show()

            dialogo.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }
    }

    // =========================================================
    // CARGAR DETALLE
    // =========================================================

    private fun cargarDetalle(
        mostrarLoading: Boolean = true
    ) {
        viewLifecycleOwner.lifecycleScope.launch {

            try {
                if (mostrarLoading) {
                    mostrarLoading()
                }

                val identificadorCita = idCita

                if (identificadorCita.isNullOrBlank()) {
                    mostrarError(
                        "No se recibió el identificador de la cita."
                    )
                    return@launch
                }

                // Obtener las citas.
                val citas = CitasRepository.obtenerCitasDesdeApi()

                if (_binding == null) return@launch

                val cita = citas.firstOrNull {
                    it.id == identificadorCita ||
                            it.idCita == identificadorCita
                }

                if (cita == null) {
                    mostrarError(
                        "No se encontró la cita en el servidor."
                    )
                    return@launch
                }

                // Obtener el paciente asociado a la cita.
                val pacientes = RetrofitClient.apiService.getPacientes()

                if (_binding == null) return@launch

                val paciente = pacientes.firstOrNull {
                    it.idPaciente == cita.idPaciente
                }

                // Obtener el cuidador asignado al paciente.
                var cuidadorAsignado: Usuario? = null

                try {
                    val respuestaAsignaciones =
                        RetrofitClient.apiService
                            .getAsignacionesPacienteCuidador()

                    val asignaciones =
                        if (respuestaAsignaciones.isSuccessful) {
                            respuestaAsignaciones.body().orEmpty()
                        } else {
                            emptyList()
                        }

                    // Priorizar la asignación activa.
                    val asignacion = asignaciones.firstOrNull {
                        it.idPaciente == cita.idPaciente &&
                                it.estado.equals(
                                    "Activo",
                                    ignoreCase = true
                                )
                    } ?: asignaciones.firstOrNull {
                        it.idPaciente == cita.idPaciente
                    }

                    if (asignacion != null) {
                        val usuarios =
                            RetrofitClient.apiService.getUsuarios()

                        cuidadorAsignado = usuarios.firstOrNull { usuario ->
                            usuario.idUsuario == asignacion.idUsuario &&
                                    usuario.idRol == 3
                        }
                    }

                } catch (e: CancellationException) {
                    throw e

                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error consultando el cuidador asignado",
                        e
                    )
                }

                if (_binding == null) return@launch

                // Mostrar toda la información una sola vez.
                mostrarInformacion(
                    cita = cita,
                    paciente = paciente,
                    cuidador = cuidadorAsignado
                )

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error cargando detalle de cita",
                    e
                )

                if (_binding != null) {
                    mostrarError(
                        "No se pudo cargar la información de la cita."
                    )
                }

            } finally {
                if (_binding != null) {
                    ocultarLoading()
                }
            }
        }
    }

    // =========================================================
    // CARGAR FOTO DEL PACIENTE
    // =========================================================

    private fun cargarFotoPaciente(url: String?) {
        val bindingActual = _binding ?: return

        Glide.with(this)
            .load(url?.takeIf { it.isNotBlank() })
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(R.drawable.persona_encargado)
            .error(R.drawable.persona_encargado)
            .fallback(R.drawable.persona_encargado)
            .centerCrop()
            .into(bindingActual.imgPacienteDetalleCita)
    }

    // =========================================================
    // CARGAR FOTO DEL CUIDADOR
    // =========================================================

    private fun cargarFotoCuidador(url: String?) {
        val bindingActual = _binding ?: return

        Glide.with(this)
            .load(url?.takeIf { it.isNotBlank() })
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .placeholder(R.drawable.persona_encargado)
            .error(R.drawable.persona_encargado)
            .fallback(R.drawable.persona_encargado)
            .centerCrop()
            .into(bindingActual.imgCuidadorDetalleCita)
    }

    // =========================================================
    // MOSTRAR INFORMACIÓN
    // =========================================================

    private fun mostrarInformacion(
        cita: Cita,
        paciente: Paciente?,
        cuidador: Usuario?
    ) {
        val bindingActual = _binding ?: return

        // -----------------------------------------------------
        // FOTO Y NOMBRE DEL PACIENTE
        // -----------------------------------------------------

        cargarFotoPaciente(paciente?.foto)

        val nombrePaciente = paciente
            ?.let {
                "${it.nombre} ${it.apellido}".trim()
            }
            ?.takeIf { it.isNotBlank() }
            ?: cita.nombrePaciente
                ?.takeIf { it.isNotBlank() }
            ?: "Paciente no encontrado"

        bindingActual.txtPacienteDetalleCita.text = nombrePaciente

        // -----------------------------------------------------
        // HABITACIÓN Y CAMA
        // -----------------------------------------------------

        bindingActual.txtHabitacionDetalleCita.text = when {
            paciente?.habitacion != null &&
                    paciente.cama != null -> {
                "Habitación ${paciente.habitacion} • Cama ${paciente.cama}"
            }

            paciente?.habitacion != null -> {
                "Habitación ${paciente.habitacion}"
            }

            paciente?.cama != null -> {
                "Cama ${paciente.cama}"
            }

            else -> "Sin habitación asignada"
        }

        // -----------------------------------------------------
        // TIPO Y ESPECIALIDAD
        // -----------------------------------------------------

        val (tipo, especialidad) = separarMotivo(
            cita.motivo.orEmpty()
        )

        bindingActual.txtTipoDetalleCita.text =
            cita.tipoCita
                ?.takeIf { it.isNotBlank() }
                ?: tipo

        bindingActual.txtEspecialidadDetalleCita.text =
            cita.especialidad
                ?.takeIf { it.isNotBlank() }
                ?: especialidad

        bindingActual.txtMotivoDetalleCita.text =
            obtenerMotivoLimpio(cita.motivo.orEmpty())

        // -----------------------------------------------------
        // LUGAR DE LA CITA
        // -----------------------------------------------------

        bindingActual.txtLugarDetalleCita.text =
            cita.lugar
                ?.takeIf { it.isNotBlank() }
                ?: "Lugar no disponible"

        // -----------------------------------------------------
        // FECHA Y HORA
        // -----------------------------------------------------

        bindingActual.txtFechaDetalleCita.text =
            formatearFecha(cita.fecha)

        bindingActual.txtHoraDetalleCita.text =
            cita.hora.takeIf { it.isNotBlank() }
                ?: "Sin hora"

        // -----------------------------------------------------
        // ESTADO
        // -----------------------------------------------------

        bindingActual.txtEstadoDetalleCita.text =
            cita.estado.takeIf { it.isNotBlank() }
                ?: "Sin estado"

        // -----------------------------------------------------
        // OBSERVACIONES
        // -----------------------------------------------------

        bindingActual.txtObservacionesDetalleCita.text =
            cita.observaciones
                ?.takeIf { it.isNotBlank() }
                ?: "Sin observaciones registradas"

        // -----------------------------------------------------
        // FOTO Y DATOS DEL CUIDADOR
        // -----------------------------------------------------

        cargarFotoCuidador(cuidador?.foto)

        val nombreCuidador = cuidador
            ?.let {
                "${it.nombres} ${it.apellidos}".trim()
            }
            ?.takeIf { it.isNotBlank() }

        bindingActual.txtCuidadorDetalleCita.text =
            nombreCuidador ?: "No asignado"

        bindingActual.txtTelefonoCuidadorDetalleCita.text =
            cuidador?.telefono
                ?.takeIf { it.isNotBlank() }
                ?: "No disponible"

        // -----------------------------------------------------
        // ASISTENCIA Y RESULTADO
        // -----------------------------------------------------

        bindingActual.txtAsistenciaDetalleCita.text =
            "Sin registrar"

        bindingActual.txtResultadoDetalleCita.text =
            "Sin registrar"

        // -----------------------------------------------------
        // ESTADO VISUAL
        // -----------------------------------------------------

        bindingActual.contenedorErrorDetalleCita.visibility =
            View.GONE

        bindingActual.scrollDetalleCita.visibility =
            View.VISIBLE
    }

    // =========================================================
    // SEPARAR MOTIVO
    // =========================================================

    private fun separarMotivo(
        motivo: String
    ): Pair<String, String> {

        if (motivo.isBlank()) {
            return Pair(
                "Sin tipo",
                "Sin especialidad"
            )
        }

        return try {
            val partesGuion = motivo.split(
                " - ",
                limit = 2
            )

            if (partesGuion.size < 2) {
                return Pair(
                    motivo.trim(),
                    "Sin especialidad"
                )
            }

            val tipo = partesGuion[0].trim()
            val resto = partesGuion[1].trim()

            val especialidad = resto
                .substringBefore(":")
                .trim()
                .ifEmpty { "Sin especialidad" }

            Pair(tipo, especialidad)

        } catch (e: Exception) {
            Pair(
                motivo.trim(),
                "Sin especialidad"
            )
        }
    }

    // =========================================================
    // OBTENER MOTIVO LIMPIO
    // =========================================================

    private fun obtenerMotivoLimpio(
        motivo: String
    ): String {
        if (motivo.isBlank()) {
            return "Sin motivo registrado"
        }

        return try {
            val partes = motivo.split(
                ":",
                limit = 2
            )

            if (partes.size == 2) {
                partes[1].trim()
            } else {
                motivo.trim()
            }

        } catch (e: Exception) {
            motivo.trim()
        }
    }

    // =========================================================
    // FORMATEAR FECHA
    // =========================================================

    private fun formatearFecha(
        fecha: String
    ): String {
        if (fecha.isBlank()) {
            return "Sin fecha"
        }

        return try {
            val partes = fecha.split("-")

            if (partes.size == 3) {
                "${partes[2]}/${partes[1]}/${partes[0]}"
            } else {
                fecha
            }

        } catch (e: Exception) {
            fecha
        }
    }

    // =========================================================
    // MOSTRAR LOADING
    // =========================================================

    private fun mostrarLoading() {
        val bindingActual = _binding ?: return

        bindingActual.loadingDetalleCita.visibility =
            View.VISIBLE

        bindingActual.scrollDetalleCita.visibility =
            View.GONE

        bindingActual.contenedorErrorDetalleCita.visibility =
            View.GONE

        bindingActual.txtLoadingDetalleCita.text =
            "Cargando información..."
    }

    // =========================================================
    // OCULTAR LOADING
    // =========================================================

    private fun ocultarLoading() {
        val bindingActual = _binding ?: return

        bindingActual.loadingDetalleCita.visibility =
            View.GONE
    }

    // =========================================================
    // MOSTRAR ERROR
    // =========================================================

    private fun mostrarError(
        mensaje: String
    ) {
        val bindingActual = _binding ?: return

        bindingActual.loadingDetalleCita.visibility =
            View.GONE

        bindingActual.scrollDetalleCita.visibility =
            View.GONE

        bindingActual.contenedorErrorDetalleCita.visibility =
            View.VISIBLE

        bindingActual.txtErrorDetalleCita.text =
            mensaje
    }

    // =========================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =========================================================

    private fun iniciarActualizacionAutomatica() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                while (isActive) {
                    delay(INTERVALO_ACTUALIZACION)

                    if (!isActive) break

                    cargarDetalle(mostrarLoading = false)
                }
            }
        }
    }

    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
