package com.example.molvigeryapp.ui.encargado.perfil

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.UsuarioRepository
import com.example.molvigeryapp.databinding.FragmentPerfilEncargadoBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.WindowInsetsEncargado
import com.example.molvigeryapp.ui.encargado.asignarturno.AsignarTurnoEncargadoFragment
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.ContadorNotificaciones
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


class PerfilEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentPerfilEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =====================================================
    // REPOSITORIO
    // =====================================================

    private val repository by lazy {
        UsuarioRepository()
    }


    // =====================================================
    // ESTADO DE CARGA
    // =====================================================

    private var cargandoPerfil = false


    // =====================================================
    // SESIÓN
    // =====================================================

    private fun obtenerPreferenciasSesion():
            android.content.SharedPreferences? {

        val contexto =
            context ?: return null

        return contexto.getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )
    }


    // =====================================================
    // CREAR VISTA
    // =====================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentPerfilEncargadoBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }


    // =====================================================
    // VISTA CREADA
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )


        // =================================================
        // WINDOW INSETS
        // =================================================

        WindowInsetsEncargado.aplicar(
            root = binding.root,
            contenido = binding.scrollPerfil,
            menuInferior = binding.bottomNavigationPerfil
        )


        // =================================================
        // CONFIGURACIONES
        // =================================================

        configurarNotificaciones()

        configurarEditarPerfil()

        configurarCambiarContrasena()

        configurarCerrarSesion()

        configurarMenuInferior()


        // =================================================
        // CONTADOR DE NOTIFICACIONES
        // =================================================

        ContadorNotificaciones.iniciar(
            fragment = this,
            badge = binding.txtNotificacionesPerfil
        )
    }


    // =====================================================
    // RECARGAR AL VOLVER A LA PANTALLA
    // =====================================================

    override fun onResume() {

        super.onResume()

        if (_binding != null) {
            cargarDatosPerfil()
        }
    }


    // =====================================================
    // CARGAR DATOS DEL PERFIL
    // =====================================================

    private fun cargarDatosPerfil() {

        // Evitar solicitudes simultáneas
        if (cargandoPerfil) {
            return
        }


        val preferencias =
            obtenerPreferenciasSesion()
                ?: return


        // =================================================
        // ID DEL USUARIO
        // =================================================

        val idUsuario =
            preferencias.getInt(
                "ID_USUARIO",
                -1
            )


        if (idUsuario <= 0) {

            mostrarMensaje(
                "No se encontró el usuario de la sesión.",
                Toast.LENGTH_LONG
            )

            return
        }


        // =================================================
        // MOSTRAR CARGA
        // =================================================

        cargandoPerfil = true

        mostrarCargando(true)


        // =================================================
        // CONSULTAR API
        // =================================================

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                /*
                 * UsuarioRepository consulta:
                 *
                 * GET /usuarios/{id}/
                 */

                val usuario =
                    repository.obtenerUsuarioPorId(
                        idUsuario
                    )


                // =================================================
                // COMPROBAR VISTA
                // =================================================

                val bindingActual =
                    _binding
                        ?: return@launch


                // =================================================
                // USUARIO NO ENCONTRADO
                // =================================================

                if (usuario == null) {

                    mostrarCargando(false)

                    mostrarMensaje(
                        "No se pudo cargar la información del perfil.",
                        Toast.LENGTH_LONG
                    )

                    return@launch
                }


                // =================================================
                // NOMBRE COMPLETO
                // =================================================

                val nombreCompleto =
                    "${usuario.nombres} ${usuario.apellidos}"
                        .trim()


                bindingActual
                    .txtNombrePerfil
                    .text =
                    nombreCompleto


                bindingActual
                    .txtNombreCompletoPerfil
                    .text =
                    nombreCompleto


                // =================================================
                // DOCUMENTO
                // =================================================

                val tipoDocumento =
                    usuario.tipoDocumento
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "CC"


                val numeroDocumento =
                    usuario.numeroDocumento
                        ?.trim()
                        .orEmpty()


                bindingActual
                    .txtDocumentoPerfil
                    .text =
                    if (numeroDocumento.isNotEmpty()) {
                        "$tipoDocumento $numeroDocumento"
                    } else {
                        tipoDocumento
                    }


                // =================================================
                // CORREO
                // =================================================

                bindingActual
                    .txtCorreoPerfil
                    .text =
                    usuario.correo


                // =================================================
                // TELÉFONO
                // =================================================

                bindingActual
                    .txtTelefonoPerfil
                    .text =
                    usuario.telefono


                // =================================================
                // FINALIZAR CARGA
                // =================================================

                mostrarCargando(false)


            } catch (
                e: CancellationException
            ) {

                // Cancelación normal del ciclo de vida.
                throw e


            } catch (
                e: Exception
            ) {

                Log.e(
                    "PERFIL_ENCARGADO",
                    "Error inesperado al cargar el perfil",
                    e
                )


                if (_binding == null) {
                    return@launch
                }


                mostrarCargando(false)


                mostrarMensaje(
                    "Error al cargar los datos del perfil.",
                    Toast.LENGTH_LONG
                )


            } finally {

                cargandoPerfil = false
            }
        }
    }


    // =====================================================
    // ESTADO DE CARGA
    // =====================================================

    private fun mostrarCargando(
        cargando: Boolean
    ) {

        val bindingActual =
            _binding ?: return


        if (cargando) {

            bindingActual
                .scrollPerfil
                .visibility =
                View.INVISIBLE

        } else {

            bindingActual
                .scrollPerfil
                .visibility =
                View.VISIBLE
        }
    }


    // =====================================================
    // MOSTRAR MENSAJE
    // =====================================================

    private fun mostrarMensaje(
        mensaje: String,
        duracion: Int
    ) {

        val contexto =
            context ?: return


        Toast.makeText(
            contexto,
            mensaje,
            duracion
        ).show()
    }


    // =====================================================
    // NOTIFICACIONES
    // =====================================================

    private fun configurarNotificaciones() {

        binding
            .btnNotificacionesPerfil
            .setOnClickListener {

                if (!isAdded) {
                    return@setOnClickListener
                }


                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        NotificacionesEncargadoFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }
    }


    // =====================================================
    // EDITAR PERFIL
    // =====================================================

    private fun configurarEditarPerfil() {

        binding
            .btnEditarPerfil
            .setOnClickListener {

                if (!isAdded) {
                    return@setOnClickListener
                }


                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        EditarPerfilEncargadoFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }
    }


    // =====================================================
    // CAMBIAR CONTRASEÑA
    // =====================================================

    private fun configurarCambiarContrasena() {

        binding
            .cardCambiarContrasena
            .setOnClickListener {

                if (!isAdded) {
                    return@setOnClickListener
                }


                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CambiarContrasenaEncargadoFragment()
                    )
                    .addToBackStack(null)
                    .commit()
            }
    }


    // =====================================================
    // CERRAR SESIÓN
    // =====================================================

    private fun configurarCerrarSesion() {

        binding
            .btnCerrarSesion
            .setOnClickListener {

                // =============================================
                // SESIÓN
                // =============================================

                val preferencias =
                    obtenerPreferenciasSesion()
                        ?: return@setOnClickListener


                // =============================================
                // LIMPIAR SESIÓN
                // =============================================

                preferencias
                    .edit()
                    .clear()
                    .apply()


                // =============================================
                // CONTEXTO
                // =============================================

                val contexto =
                    context
                        ?: return@setOnClickListener


                // =============================================
                // IR AL LOGIN
                // =============================================

                val intent =
                    Intent(
                        contexto,
                        LoginActivity::class.java
                    )


                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK


                startActivity(
                    intent
                )
            }
    }


    // =====================================================
    // MENÚ INFERIOR
    // =====================================================

    private fun configurarMenuInferior() {

        NavegacionEncargado.configurar(

            // =================================================
            // INICIO
            // =================================================

            navInicio =
                binding.navInicioPerfil,

            iconInicio =
                binding.iconInicioPerfil,

            textInicio =
                binding.textInicioPerfil,


            // =================================================
            // ASIGNAR TURNO
            // =================================================

            navAsignarTurno =
                binding.navAsignarTurnoPerfil,

            iconAsignarTurno =
                binding.iconAsignarTurnoPerfil,

            textAsignarTurno =
                binding.textAsignarTurnoPerfil,


            // =================================================
            // CITAS
            // =================================================

            navCitas =
                binding.navCitasPerfil,

            iconCitas =
                binding.iconCitasPerfil,

            textCitas =
                binding.textCitasPerfil,


            // =================================================
            // PERFIL
            // =================================================

            navPerfil =
                binding.navPerfilPerfil,

            iconPerfil =
                binding.iconPerfilPerfil,

            textPerfil =
                binding.textPerfilPerfil,


            // =================================================
            // PANTALLA ACTUAL
            // =================================================

            pantallaActual =
                NavegacionEncargado.Pantalla.PERFIL,


            // =================================================
            // INICIO
            // =================================================

            onInicio = {

                if (!isAdded) {
                    return@configurar
                }


                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        HomeEncargadoFragment()
                    )
                    .commit()
            },


            // =================================================
            // ASIGNAR TURNO
            // =================================================

            onAsignarTurno = {

                if (!isAdded) {
                    return@configurar
                }


                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        AsignarTurnoEncargadoFragment()
                    )
                    .commit()
            },


            // =================================================
            // CITAS
            // =================================================

            onCitas = {

                if (!isAdded) {
                    return@configurar
                }


                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CitasEncargadoFragment()
                    )
                    .commit()
            },


            // =================================================
            // PERFIL ACTUAL
            // =================================================

            onPerfil = {
                // Ya estamos en Perfil.
            }
        )
    }


    // =====================================================
    // DESTRUIR VISTA
    // =====================================================

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }
}