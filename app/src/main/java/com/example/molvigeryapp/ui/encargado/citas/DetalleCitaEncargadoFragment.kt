package com.example.molvigeryapp.ui.encargado.citas

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.CitaApiResponse
import com.example.molvigeryapp.data.model.Paciente
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
        get() = _binding!!


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

        _binding =
            FragmentDetalleCitaEncargadoBinding.inflate(
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

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // =====================================================
        // RECUPERAR ARGUMENTOS
        // =====================================================

        idCita =
            arguments?.getString(
                "idCita"
            )


        idPaciente =
            arguments?.getInt(
                "idPaciente",
                -1
            ) ?: -1


        // =====================================================
        // CONFIGURAR INTERFAZ
        // =====================================================

        configurarBotonVolver()

        configurarBotonEditar()

        configurarBotonEliminar()


        // =====================================================
        // CARGA INICIAL
        // =====================================================

        cargarDetalle()


        // =====================================================
        // ACTUALIZACIÓN AUTOMÁTICA
        // =====================================================

        iniciarActualizacionAutomatica()
    }


    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding.btnVolverDetalleCita
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }


    // =========================================================
    // BOTÓN EDITAR
    // =========================================================

    private fun configurarBotonEditar() {

        binding.btnEditarCita
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "La edición de citas se implementará con el endpoint de actualización.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }


    // =========================================================
    // BOTÓN ELIMINAR
    // =========================================================

    private fun configurarBotonEliminar() {

        binding.btnEliminarCita
            .setOnClickListener {

                Toast.makeText(
                    requireContext(),
                    "La eliminación de citas se implementará con el endpoint correspondiente.",
                    Toast.LENGTH_SHORT
                ).show()
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


                // =================================================
                // VALIDAR ID
                // =================================================

                val identificadorCita =
                    idCita


                if (
                    identificadorCita.isNullOrBlank()
                ) {

                    mostrarError(
                        "No se recibió el identificador de la cita."
                    )

                    return@launch
                }


                // =================================================
                // OBTENER CITAS DESDE API
                // =================================================

                val citas =
                    RetrofitClient.apiService
                        .getCitas()


                if (_binding == null) {
                    return@launch
                }


                // =================================================
                // BUSCAR LA CITA
                // =================================================

                val cita =
                    citas.firstOrNull {

                        it.idCita ==
                                identificadorCita
                    }


                if (cita == null) {

                    mostrarError(
                        "No se encontró la cita en el servidor."
                    )

                    return@launch
                }


                // =================================================
                // OBTENER PACIENTES
                // =================================================

                val pacientes =
                    RetrofitClient.apiService
                        .getPacientes()


                if (_binding == null) {
                    return@launch
                }


                val paciente =
                    pacientes.firstOrNull {

                        it.idPaciente ==
                                cita.idPaciente
                    }


                // =================================================
                // MOSTRAR INFORMACIÓN
                // =================================================

                mostrarInformacion(
                    cita = cita,
                    paciente = paciente
                )


            } catch (e: CancellationException) {

                // Cancelación normal cuando el usuario
                // abandona rápidamente el Fragment.
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
    // MOSTRAR INFORMACIÓN
    // =========================================================

    private fun mostrarInformacion(
        cita: CitaApiResponse,
        paciente: Paciente?
    ) {

        if (_binding == null) {
            return
        }


        // =====================================================
        // PACIENTE
        // =====================================================

        val nombrePaciente =

            if (paciente != null) {

                "${paciente.nombre} ${paciente.apellido}"
                    .trim()

            } else {

                "Paciente no encontrado"
            }


        binding.txtPacienteDetalleCita.text =
            nombrePaciente


        // =====================================================
        // HABITACIÓN
        // =====================================================

        binding.txtHabitacionDetalleCita.text =

            when {

                paciente?.habitacion != null &&
                        paciente.cama != null ->

                    "Habitación ${paciente.habitacion} • Cama ${paciente.cama}"


                paciente?.habitacion != null ->

                    "Habitación ${paciente.habitacion}"


                paciente?.cama != null ->

                    "Cama ${paciente.cama}"


                else ->

                    "Sin habitación asignada"
            }


        // =====================================================
        // MOTIVO
        // =====================================================

        val partesMotivo =
            separarMotivo(
                cita.motivo
            )


        binding.txtTipoDetalleCita.text =
            partesMotivo.first


        binding.txtEspecialidadDetalleCita.text =
            partesMotivo.second


        binding.txtMotivoDetalleCita.text =
            obtenerMotivoLimpio(
                cita.motivo
            )


        // =====================================================
        // FECHA
        // =====================================================

        binding.txtFechaDetalleCita.text =
            formatearFecha(
                cita.fecha
            )


        // =====================================================
        // HORA
        // =====================================================

        binding.txtHoraDetalleCita.text =
            cita.hora


        // =====================================================
        // ESTADO
        // =====================================================

        binding.txtEstadoDetalleCita.text =
            cita.estado.ifBlank {
                "Sin estado"
            }


        // =====================================================
        // OBSERVACIONES
        // =====================================================

        binding.txtObservacionesDetalleCita.text =

            if (
                cita.observaciones.isBlank()
            ) {

                "Sin observaciones registradas"

            } else {

                cita.observaciones
            }


        // =====================================================
        // CUIDADOR
        // =====================================================
        //
        // Actualmente CitaApiResponse NO contiene:
        //
        // id_cuidador
        // nombre_cuidador
        // telefono_cuidador
        //
        // Por eso no inventamos información.
        //
        // =====================================================

        binding.txtCuidadorDetalleCita.text =
            "No asignado"


        binding.txtTelefonoCuidadorDetalleCita.text =
            "No disponible"


        // =====================================================
        // ASISTENCIA
        // =====================================================

        binding.txtAsistenciaDetalleCita.text =
            "Sin registrar"


        // =====================================================
        // RESULTADO
        // =====================================================

        binding.txtResultadoDetalleCita.text =
            "Sin registrar"


        // =====================================================
        // ERROR
        // =====================================================

        binding.contenedorErrorDetalleCita.visibility =
            View.GONE


        binding.scrollDetalleCita.visibility =
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

            val partesGuion =
                motivo.split(
                    " - ",
                    limit = 2
                )


            if (partesGuion.size < 2) {

                return Pair(
                    motivo.trim(),
                    "Sin especialidad"
                )
            }


            val tipo =
                partesGuion[0].trim()


            val resto =
                partesGuion[1].trim()


            val partesDosPuntos =
                resto.split(
                    ":",
                    limit = 2
                )


            val especialidad =
                partesDosPuntos[0].trim()


            Pair(
                tipo,
                especialidad
            )

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

            val partes =
                motivo.split(
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

            val partes =
                fecha.split("-")


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

        if (_binding == null) {
            return
        }


        binding.loadingDetalleCita.visibility =
            View.VISIBLE


        binding.scrollDetalleCita.visibility =
            View.GONE


        binding.contenedorErrorDetalleCita.visibility =
            View.GONE


        binding.txtLoadingDetalleCita.text =
            "Cargando información..."
    }


    // =========================================================
    // OCULTAR LOADING
    // =========================================================

    private fun ocultarLoading() {

        if (_binding == null) {
            return
        }


        binding.loadingDetalleCita.visibility =
            View.GONE
    }


    // =========================================================
    // MOSTRAR ERROR
    // =========================================================

    private fun mostrarError(
        mensaje: String
    ) {

        if (_binding == null) {
            return
        }


        binding.loadingDetalleCita.visibility =
            View.GONE


        binding.scrollDetalleCita.visibility =
            View.GONE


        binding.contenedorErrorDetalleCita.visibility =
            View.VISIBLE


        binding.txtErrorDetalleCita.text =
            mensaje
    }


    // =========================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =========================================================

    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner.lifecycleScope.launch {

            while (isActive) {

                delay(
                    INTERVALO_ACTUALIZACION
                )


                if (!isActive) {
                    break
                }


                cargarDetalle(
                    mostrarLoading = false
                )
            }
        }
    }


    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}