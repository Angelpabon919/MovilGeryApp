package com.example.molvigeryapp.ui.cuidador.perfil

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment
import kotlinx.coroutines.launch

class PerfilCuidadorFragment : Fragment() {

    // =====================================================
    // REPOSITORIO Y SESIÓN
    // =====================================================
    private val repository by lazy { PacienteRepository() }

    private val preferenciasSesion by lazy {
        requireContext().getSharedPreferences("SESION", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_perfil_cuidador, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cargarDatosPerfil(view)
        configurarNotificaciones(view)
        configurarEditarPerfil(view)
        configurarCerrarSesion(view)
        configurarMenuInferior(view)
    }

    // =====================================================
    // CARGAR DATOS DEL PERFIL
    // =====================================================
    private fun cargarDatosPerfil(view: View) {
        val idUsuario = preferenciasSesion.getInt("ID_USUARIO", -1)

        if (idUsuario == -1) {
            Toast.makeText(requireContext(), "No se encontró el usuario de la sesión.", Toast.LENGTH_LONG).show()
            return
        }

        val txtNombrePerfil = view.findViewById<TextView>(R.id.txtNombrePerfil)
        val txtNombreCompletoPerfil = view.findViewById<TextView>(R.id.txtNombreCompletoPerfil)
        val txtDocumentoPerfil = view.findViewById<TextView>(R.id.txtDocumentoPerfil)
        val txtCorreoPerfil = view.findViewById<TextView>(R.id.txtCorreoPerfil)
        val txtTelefonoPerfil = view.findViewById<TextView>(R.id.txtTelefonoPerfil)

        lifecycleScope.launch {
            try {
                val usuario = repository.obtenerUsuarioPorId(idUsuario)

                if (usuario == null) {
                    Toast.makeText(requireContext(), "No se pudo cargar el usuario $idUsuario", Toast.LENGTH_LONG).show()
                    return@launch
                }

                val nombreCompleto = "${usuario.nombres} ${usuario.apellidos}".trim()

                txtNombrePerfil?.text = nombreCompleto
                txtNombreCompletoPerfil?.text = nombreCompleto
                txtDocumentoPerfil?.text = "${usuario.tipoDocumento ?: "CC"} ${usuario.numeroDocumento}"
                txtCorreoPerfil?.text = usuario.correo
                txtTelefonoPerfil?.text = usuario.telefono

            } catch (e: Exception) {
                android.util.Log.e("PERFIL_CUIDADOR", "Error inesperado al cargar el perfil", e)
                Toast.makeText(requireContext(), "Error al cargar los datos del perfil.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // =====================================================
    // ACCIONES (NOTIFICACIONES, EDITAR, CERRAR SESIÓN)
    // =====================================================
    private fun configurarNotificaciones(view: View) {
        val btnNotificaciones = view.findViewById<View>(R.id.btnNotificacionesPerfil)
        btnNotificaciones?.setOnClickListener {
            Toast.makeText(requireContext(), "Sin notificaciones pendientes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun configurarEditarPerfil(view: View) {
        val btnEditar = view.findViewById<View>(R.id.btnEditarPerfil)
        btnEditar?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EditarPerfilCuidadorFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun configurarCerrarSesion(view: View) {
        val btnCerrarSesion = view.findViewById<View>(R.id.btnCerrarSesion)
        btnCerrarSesion?.setOnClickListener {
            preferenciasSesion.edit().clear().apply()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    // =====================================================
    // MENÚ INFERIOR
    // =====================================================
    private fun configurarMenuInferior(view: View) {
        val navInicio = view.findViewById<LinearLayout>(R.id.navInicioPerfil)
        navInicio?.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PacientesListFragment())
                .commit()
        }
    }
}
