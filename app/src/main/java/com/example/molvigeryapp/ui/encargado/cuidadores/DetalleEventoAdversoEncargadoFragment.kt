package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.databinding.FragmentDetalleEventoAdversoEncargadoBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetalleEventoAdversoEncargadoFragment : Fragment() {

    private var _binding: FragmentDetalleEventoAdversoEncargadoBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDetalleEventoAdversoEncargadoBinding.inflate(
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

        mostrarCargando()

        cargarDatosEvento()
    }

    private fun cargarDatosEvento() {

        val idEventoAdverso =
            arguments?.getInt("id_evento_adverso", 0) ?: 0

        val idBitacora =
            arguments?.getInt("id_bitacora", 0) ?: 0

        val idTipoEmergencia =
            arguments?.getInt("id_tipo_emergencia", 0) ?: 0

        // ==========================================
        // DATOS DEL EVENTO
        // ==========================================

        val estado =
            arguments?.getString("estado")
                ?: "Sin estado"

        val fechaHora =
            arguments?.getString("fecha_hora")
                ?: ""

        val descripcion =
            arguments?.getString("descripcion")
                ?: "Sin descripción registrada."

        val acciones =
            arguments?.getString("acciones_realizadas")
                ?: "No se registraron acciones."

        // ==========================================
        // INFORMACIÓN PRINCIPAL
        // ==========================================

        binding.txtIdEvento.text =
            if (idEventoAdverso > 0) {
                "#$idEventoAdverso"
            } else {
                "No disponible"
            }

        binding.txtEstadoEvento.text =
            estado.ifBlank {
                "Sin estado"
            }

        // ==========================================
        // FECHA Y HORA
        // ==========================================

        val fechaFormateada =
            if (fechaHora.isNotBlank()) {
                formatearFechaHora(fechaHora)
            } else {
                "Fecha no disponible"
            }

        binding.txtFechaEvento.text =
            fechaFormateada

        // ==========================================
        // DESCRIPCIÓN
        // ==========================================

        binding.txtDescripcionEvento.text =
            descripcion.ifBlank {
                "Sin descripción registrada."
            }

        // ==========================================
        // ACCIONES REALIZADAS
        // ==========================================

        binding.txtAccionesRealizadasEvento.text =
            acciones.ifBlank {
                "No se registraron acciones."
            }

        // ==========================================
        // BITÁCORA
        // ==========================================

        binding.txtIdBitacoraEvento.text =
            if (idBitacora > 0) {
                "Bitácora #$idBitacora"
            } else {
                "Bitácora no disponible"
            }

        // ==========================================
        // DATOS INICIALES
        // ==========================================

        binding.txtPacienteEvento.text =
            arguments?.getString("nombre_paciente")
                ?: "Paciente no disponible"

        binding.txtCuidadorEvento.text =
            arguments?.getString("nombre_cuidador")
                ?: "Cuidador no disponible"

        binding.txtTipoEvento.text =
            arguments?.getString("tipo_evento")
                ?: "Evento adverso"

        // ==========================================
        // TIPO DE EMERGENCIA
        // ==========================================

        binding.txtTipoEmergenciaEvento.text =
            if (idTipoEmergencia > 0) {
                "Cargando..."
            } else {
                "No especificado"
            }

        binding.txtNivelEmergenciaEvento.text =
            if (idTipoEmergencia > 0) {
                "Cargando..."
            } else {
                "No especificado"
            }

        // ==========================================
        // REGISTRO FOTOGRÁFICO
        // ==========================================

        binding.imgRegistroFotografico.visibility =
            View.VISIBLE

        binding.txtSinRegistroFotografico.visibility =
            View.GONE

        binding.txtFechaFotografia.text =
            "Evidencia fotográfica registrada"

        // ==========================================
        // CONSULTAR API
        // ==========================================

        if (idBitacora > 0 || idTipoEmergencia > 0) {

            viewLifecycleOwner.lifecycleScope.launch {

                try {

                    cargarInformacionRelacionada(
                        idBitacora = idBitacora,
                        idTipoEmergencia = idTipoEmergencia
                    )

                } catch (e: Exception) {

                    e.printStackTrace()

                    Toast.makeText(
                        requireContext(),
                        "No se pudo completar la información del evento.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        } else {

            ocultarCargando()
        }
    }

    // ==========================================
    // FORMATEAR FECHA Y HORA
    // ==========================================

    private fun formatearFechaHora(
        fechaHora: String
    ): String {

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'",
                    Locale.US
                )

            // La API entrega la fecha en UTC
            formatoEntrada.timeZone =
                TimeZone.getTimeZone("UTC")

            val fecha =
                formatoEntrada.parse(fechaHora)

            // Hora de Colombia
            val formatoSalida =
                SimpleDateFormat(
                    "dd 'de' MMMM 'de' yyyy · HH:mm",
                    Locale("es", "CO")
                )

            formatoSalida.timeZone =
                TimeZone.getTimeZone("America/Bogota")

            if (fecha != null) {
                formatoSalida.format(fecha)
            } else {
                "Fecha no disponible"
            }

        } catch (e: Exception) {

            e.printStackTrace()

            // Si la fecha viene en otro formato,
            // intentamos quitar solamente la T y la Z.
            fechaHora
                .replace("T", " ")
                .replace("Z", "")
                .substringBefore(".")
        }
    }

    private suspend fun cargarInformacionRelacionada(
        idBitacora: Int,
        idTipoEmergencia: Int
    ) {

        // ==========================================
        // BITÁCORA
        // ==========================================

        var bitacora =
            null as com.example.molvigeryapp.data.model.Bitacora?

        if (idBitacora > 0) {

            val bitacoras =
                RetrofitClient.apiService.getBitacoras()

            bitacora =
                bitacoras.firstOrNull {
                    it.idBitacora == idBitacora
                }
        }

        // ==========================================
        // PACIENTE
        // ==========================================

        if (bitacora?.idPaciente != null) {

            val pacientes =
                RetrofitClient.apiService.getPacientes()

            val paciente =
                pacientes.firstOrNull {
                    it.idPaciente == bitacora?.idPaciente
                }

            if (paciente != null) {

                binding.txtPacienteEvento.text =
                    "${paciente.nombre} ${paciente.apellido}".trim()
            }
        }

        // ==========================================
        // CUIDADOR
        // ==========================================

        if (bitacora?.idUsuario != null) {

            try {

                val usuario =
                    RetrofitClient.apiService.getUsuarioById(
                        bitacora!!.idUsuario!!
                    )

                binding.txtCuidadorEvento.text =
                    "${usuario.nombres} ${usuario.apellidos}".trim()

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }

        // ==========================================
        // TIPO DE EMERGENCIA
        // ==========================================

        if (idTipoEmergencia > 0) {

            try {

                val tiposEmergencia =
                    RetrofitClient.apiService.getTiposEmergencia()

                val tipoEmergencia =
                    tiposEmergencia.firstOrNull {
                        it.idTipoEmergencia == idTipoEmergencia
                    }

                if (tipoEmergencia != null) {

                    // Nombre
                    binding.txtTipoEmergenciaEvento.text =
                        tipoEmergencia.nombre.ifBlank {
                            "Nombre no disponible"
                        }

                    // Nivel
                    binding.txtNivelEmergenciaEvento.text =
                        tipoEmergencia.nivel.ifBlank {
                            "Nivel no disponible"
                        }

                } else {

                    binding.txtTipoEmergenciaEvento.text =
                        "Tipo no encontrado"

                    binding.txtNivelEmergenciaEvento.text =
                        "Nivel no disponible"
                }

            } catch (e: Exception) {

                e.printStackTrace()

                binding.txtTipoEmergenciaEvento.text =
                    "No disponible"

                binding.txtNivelEmergenciaEvento.text =
                    "No disponible"
            }

        } else {

            binding.txtTipoEmergenciaEvento.text =
                "No especificado"

            binding.txtNivelEmergenciaEvento.text =
                "No especificado"
        }

        // ==========================================
        // FOTOGRAFÍA
        // ==========================================

        binding.txtFechaFotografia.text =
            "Evidencia fotográfica registrada"

        ocultarCargando()
    }

    private fun mostrarCargando() {

        binding.txtPacienteEvento.text =
            "Cargando..."

        binding.txtCuidadorEvento.text =
            "Cargando..."

        binding.txtTipoEmergenciaEvento.text =
            "Cargando..."

        binding.txtNivelEmergenciaEvento.text =
            "Cargando..."
    }

    private fun ocultarCargando() {

        binding.imgRegistroFotografico.visibility =
            View.VISIBLE

        binding.txtSinRegistroFotografico.visibility =
            View.GONE
    }

    private fun configurarBotonVolver() {

        binding.btnVolverEvento.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}