package com.example.molvigeryapp.ui.encargado.cuidadores

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentDetalleBitacoraEncargadoBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class DetalleBitacoraEncargadoFragment : Fragment() {

    private var _binding:
            FragmentDetalleBitacoraEncargadoBinding? = null

    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDetalleBitacoraEncargadoBinding.inflate(
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

        super.onViewCreated(
            view,
            savedInstanceState
        )

        cargarDatosBitacora()
        configurarBotonVolver()
    }

    private fun cargarDatosBitacora() {

        val tipoRegistro =
            arguments?.getString(
                "tipo_registro"
            ) ?: "Registro de bitácora"

        val estado =
            arguments?.getBoolean(
                "estado",
                true
            ) ?: true

        val nombrePaciente =
            arguments?.getString(
                "nombre_paciente"
            ) ?: "Paciente no disponible"

        val fechaHora =
            arguments?.getString(
                "fecha_hora"
            ) ?: ""

        val nombreCuidador =
            arguments?.getString(
                "nombre_cuidador"
            ) ?: "Cuidador no disponible"

        val descripcion =
            arguments?.getString(
                "descripcion"
            ) ?: "Sin descripción registrada."

        val idBitacora =
            arguments?.getInt(
                "id_bitacora",
                0
            ) ?: 0


        // TIPO DE REGISTRO

        binding.txtTipoActividad.text =
            obtenerNombreTipo(
                tipoRegistro
            )


        // ESTADO

        binding.txtEstadoActividad.text =
            if (estado) {
                "Registro activo"
            } else {
                "Registro inactivo"
            }


        // PACIENTE

        binding.txtPacienteActividad.text =
            nombrePaciente.ifBlank {
                "Paciente no disponible"
            }


        // CUIDADOR

        binding.txtCuidadorActividad.text =
            nombreCuidador.ifBlank {
                "Cuidador no disponible"
            }


        // DESCRIPCIÓN

        binding.txtDescripcionActividad.text =
            descripcion.ifBlank {
                "Sin descripción registrada."
            }


        // FECHA

        binding.txtFechaActividad.text =
            if (fechaHora.isNotBlank()) {

                formatearFechaHora(
                    fechaHora
                )

            } else {

                "Fecha no disponible"
            }


        // ID DE BITÁCORA

        binding.txtIdBitacora.text =
            if (idBitacora > 0) {

                "Registro #$idBitacora"

            } else {

                "Registro de bitácora"
            }
    }

    private fun obtenerNombreTipo(
        tipoRegistro: String
    ): String {

        return when {

            tipoRegistro.equals(
                "normal",
                ignoreCase = true
            ) -> "Registro normal"

            tipoRegistro.equals(
                "actividad",
                ignoreCase = true
            ) -> "Actividad"

            tipoRegistro.equals(
                "evento adverso",
                ignoreCase = true
            ) -> "Evento adverso"

            tipoRegistro.equals(
                "caída",
                ignoreCase = true
            ) -> "Caída"

            tipoRegistro.equals(
                "descomposición",
                ignoreCase = true
            ) -> "Descomposición"

            tipoRegistro.equals(
                "paciente enfermo",
                ignoreCase = true
            ) -> "Paciente enfermo"

            else -> {

                tipoRegistro.ifBlank {
                    "Registro de bitácora"
                }
            }
        }
    }

    private fun formatearFechaHora(
        fechaHora: String
    ): String {

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss",
                    Locale.US
                )

            formatoEntrada.timeZone =
                TimeZone.getTimeZone(
                    "UTC"
                )

            val fecha =
                formatoEntrada.parse(
                    fechaHora
                )

            val formatoSalida =
                SimpleDateFormat(
                    "dd 'de' MMMM 'de' yyyy · HH:mm",
                    Locale("es", "CO")
                )

            formatoSalida.timeZone =
                TimeZone.getTimeZone(
                    "America/Bogota"
                )

            if (fecha != null) {

                formatoSalida.format(
                    fecha
                )

            } else {

                fechaHora
            }

        } catch (e: Exception) {

            fechaHora
                .replace(
                    "T",
                    " "
                )
                .replace(
                    "Z",
                    ""
                )
                .substringBefore(".")
        }
    }

    private fun configurarBotonVolver() {

        binding.btnVolverActividad.setOnClickListener {

            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}