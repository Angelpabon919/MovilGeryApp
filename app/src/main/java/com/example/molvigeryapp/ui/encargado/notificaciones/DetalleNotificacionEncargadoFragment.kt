package com.example.molvigeryapp.ui.encargado.notificaciones

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.MarcarNotificacionLeidaRequest
import com.example.molvigeryapp.data.model.Notificacion
import com.example.molvigeryapp.databinding.FragmentDetalleNotificacionEncargadoBinding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


class DetalleNotificacionEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentDetalleNotificacionEncargadoBinding? =
        null

    private val binding
        get() = _binding!!


    // =========================================================
    // NOTIFICACIÓN
    // =========================================================

    private var notificacion: Notificacion? = null


    // =========================================================
    // CREAR FRAGMENT
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recibirNotificacion()
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
            FragmentDetalleNotificacionEncargadoBinding.inflate(
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

        configurarBotonVolver()

        mostrarNotificacion()

        marcarComoLeida()
    }


    // =========================================================
    // RECIBIR NOTIFICACIÓN
    // =========================================================

    private fun recibirNotificacion() {

        val args = arguments ?: return

        val notificacionRecibida =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {

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
            notificacionRecibida
    }


    // =========================================================
    // MOSTRAR INFORMACIÓN
    // =========================================================

    private fun mostrarNotificacion() {

        val actual =
            notificacion ?: return


        val bindingActual =
            _binding ?: return


        // -----------------------------------------------------
        // ICONO
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // INFORMACIÓN PRINCIPAL
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // PACIENTE
        // -----------------------------------------------------

        if (
            actual.idPaciente != null
        ) {

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


        // -----------------------------------------------------
        // ESTADO
        // -----------------------------------------------------

        actualizarEstadoEnPantalla(
            actual.leida
        )
    }


    // =========================================================
    // ACTUALIZAR ESTADO EN PANTALLA
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

        val actual =
            notificacion ?: return


        // -----------------------------------------------------
        // SI YA ESTÁ LEÍDA NO HACEMOS NINGUNA PETICIÓN
        // -----------------------------------------------------

        if (actual.leida) {
            return
        }


        // -----------------------------------------------------
        // VALIDAR ID DEL DESTINATARIO
        // -----------------------------------------------------

        val idDestinatario =
            actual.idNotificacionDestinatario


        if (idDestinatario <= 0) {
            return
        }


        viewLifecycleOwner.lifecycleScope.launch {

            try {

                // =================================================
                // PETICIÓN AL API
                // =================================================

                val respuesta =
                    RetrofitClient.apiService
                        .marcarNotificacionLeida(

                            idDestinatario,

                            MarcarNotificacionLeidaRequest(
                                leida = true
                            )
                        )


                // =================================================
                // COMPROBAR RESPUESTA DEL SERVIDOR
                // =================================================

                if (!respuesta.isSuccessful) {

                    val contexto =
                        context ?: return@launch

                    if (!isAdded) {
                        return@launch
                    }

                    Toast.makeText(
                        contexto,
                        "No se pudo actualizar el estado de la notificación",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }


                // =================================================
                // LA PETICIÓN FUE EXITOSA
                // =================================================
                //
                // RespuestaMensaje NO tiene la propiedad "leida".
                //
                // Como nosotros acabamos de enviar:
                //
                //     leida = true
                //
                // y el servidor respondió correctamente,
                // actualizamos el modelo local directamente.
                // =================================================

                notificacion =
                    actual.copy(
                        leida = true
                    )


                // =================================================
                // COMPROBAR QUE LA VISTA SIGUE EXISTIENDO
                // =================================================

                val bindingActual =
                    _binding ?: return@launch


                // =================================================
                // ACTUALIZAR ESTADO EN PANTALLA
                // =================================================

                bindingActual
                    .txtEstadoDetalleNotificacion
                    .text =
                    "Leída"


            } catch (
                e: CancellationException
            ) {

                // =================================================
                // CANCELACIÓN NORMAL
                // =================================================
                //
                // Puede ocurrir si el usuario navega rápidamente
                // y la vista del Fragment se destruye.
                //
                // No debemos mostrar un Toast de error.
                // =================================================

                throw e


            } catch (
                e: Exception
            ) {

                e.printStackTrace()


                // =================================================
                // COMPROBAR QUE EL FRAGMENT SIGUE ACTIVO
                // =================================================

                val contexto =
                    context ?: return@launch


                if (!isAdded) {
                    return@launch
                }


                Toast.makeText(
                    contexto,
                    "No se pudo actualizar el estado de la notificación",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding
            .btnVolverDetalleNotificacion
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
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