package com.example.molvigeryapp.ui.encargado.notificaciones

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.data.repository.NotificacionesRepository
import com.example.molvigeryapp.databinding.FragmentDetalleNotificacionEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


class DetalleNotificacionEncargadoFragment : Fragment() {

    private var _binding: FragmentDetalleNotificacionEncargadoBinding? =
        null

    private val binding: FragmentDetalleNotificacionEncargadoBinding
        get() = requireNotNull(_binding)


    private var notificacion: Notificacion? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        recibirNotificacion()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentDetalleNotificacionEncargadoBinding.inflate(
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

        configurarBotonVolver()

        mostrarNotificacion()

        marcarComoLeida()
    }


    // =========================================================
    // RECIBIR NOTIFICACIÓN
    // =========================================================

    private fun recibirNotificacion() {

        val args =
            arguments ?: return


        val recibida =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                args.getSerializable(
                    "notificacion",
                    Notificacion::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                args.getSerializable(
                    "notificacion"
                ) as? Notificacion
            }


        notificacion =
            recibida
    }


    // =========================================================
    // MOSTRAR NOTIFICACIÓN
    // =========================================================

    private fun mostrarNotificacion() {

        val actual =
            notificacion ?: return


        val bindingActual =
            _binding ?: return


        if (actual.icono != 0) {

            bindingActual
                .imgIconoDetalleNotificacion
                .setImageResource(
                    actual.icono
                )

        } else {

            bindingActual
                .imgIconoDetalleNotificacion
                .setImageDrawable(
                    null
                )
        }


        bindingActual
            .txtTipoDetalleNotificacion
            .text =
            actual.tipo


        bindingActual
            .txtTituloDetalleNotificacion
            .text =
            actual.titulo


        bindingActual
            .txtFechaDetalleNotificacion
            .text =
            actual.fecha


        bindingActual
            .txtMensajeDetalleNotificacion
            .text =
            actual.detalle


        // =====================================================
        // PACIENTE
        // =====================================================

        if (actual.idPaciente != null) {

            bindingActual
                .cardPacienteDetalleNotificacion
                .visibility =
                View.VISIBLE


            bindingActual
                .txtEtiquetaPacienteDetalle
                .visibility =
                View.VISIBLE


            bindingActual
                .txtPacienteDetalleNotificacion
                .text =
                "Paciente asociado · ID ${actual.idPaciente}"

        } else {

            bindingActual
                .cardPacienteDetalleNotificacion
                .visibility =
                View.GONE


            bindingActual
                .txtEtiquetaPacienteDetalle
                .visibility =
                View.GONE
        }


        actualizarEstadoEnPantalla(
            actual.leida
        )
    }


    // =========================================================
    // ACTUALIZAR ESTADO
    // =========================================================

    private fun actualizarEstadoEnPantalla(
        leida: Boolean
    ) {

        val bindingActual =
            _binding ?: return


        bindingActual
            .txtEstadoDetalleNotificacion
            .text =
            if (leida) {
                "Leída"
            } else {
                "No leída"
            }
    }


    // =========================================================
    // MARCAR COMO LEÍDA
    // =========================================================

    private fun marcarComoLeida() {

        val actual = notificacion ?: return

        if (actual.leida) {
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val contexto = context
                    ?: return@launch

                val preferencias =
                    contexto.getSharedPreferences(
                        "SESION",
                        0
                    )

                val idUsuario =
                    preferencias.getInt(
                        "ID_USUARIO",
                        -1
                    )

                if (idUsuario <= 0) {
                    return@launch
                }

                // =================================================
                // MARCAR CUALQUIER NOTIFICACIÓN
                // =================================================

                val marcada =
                    NotificacionesRepository
                        .marcarCualquierNotificacionLeida(
                            idNotificacion =
                                actual.idNotificacion,

                            idUsuario =
                                idUsuario
                        )

                if (!marcada) {

                    if (!isAdded) {
                        return@launch
                    }

                    Toast.makeText(
                        requireContext(),
                        "No se pudo marcar la notificación como leída.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                // =================================================
                // ACTUALIZAR ESTADO LOCAL
                // =================================================

                val actualizada =
                    actual.copy(
                        leida = true
                    )

                notificacion =
                    actualizada

                _binding?.txtEstadoDetalleNotificacion?.text =
                    "Leída"

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                if (!isAdded) {
                    return@launch
                }

                Toast.makeText(
                    requireContext(),
                    "No se pudo marcar la notificación como leída.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding
            .btnVolverDetalleNotificacion
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }
}