package com.example.molvigeryapp.ui.cuidador.bitacora

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.databinding.FragmentBitacoraBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BitacoraFragment : Fragment() {

    private var _binding: FragmentBitacoraBinding? = null
    private val binding get() = _binding!!

    // ID del paciente que se está observando
    private var idPaciente: Int? = null

    // ID de la bitácora creada por el API
    private var idBitacora: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recibimos el ID del paciente
        idPaciente = arguments?.getInt("ID_PACIENTE")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBitacoraBinding.inflate(
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

        configurarBotones()
    }

    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        binding.btnGuardar.setOnClickListener {
            guardarBitacora()
        }

        binding.btnCancelar.setOnClickListener {
            limpiarFormulario()
        }
    }

    // =========================================================
    // OBTENER ID DEL CUIDADOR
    // =========================================================

    private fun obtenerIdUsuario(): Int {

        val preferences =
            requireActivity().getSharedPreferences(
                "SESION",
                Context.MODE_PRIVATE
            )

        return preferences.getInt(
            "ID_USUARIO",
            -1
        )
    }

    // =========================================================
    // OBTENER FECHA Y HORA
    // =========================================================

    private fun obtenerFechaHoraActual(): String {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        return formato.format(Date())
    }

    // =========================================================
    // GUARDAR BITÁCORA
    // =========================================================

    private fun guardarBitacora() {

        // -----------------------------------------------------
        // OBTENER USUARIO
        // -----------------------------------------------------

        val idUsuario = obtenerIdUsuario()

        if (idUsuario == -1) {

            Toast.makeText(
                requireContext(),
                "No se encontró el usuario de la sesión",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // OBTENER PACIENTE
        // -----------------------------------------------------

        val pacienteId = idPaciente

        if (pacienteId == null) {

            Toast.makeText(
                requireContext(),
                "No se encontró el paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // OBTENER DATOS DEL FORMULARIO
        // -----------------------------------------------------

        val tipoRegistro =
            binding.etTipoRegistro.text.toString().trim()

        val descripcion =
            binding.etDescripcion.text.toString().trim()

        val estado =
            binding.switchEstado.isChecked

        // -----------------------------------------------------
        // VALIDAR TIPO DE REGISTRO
        // -----------------------------------------------------

        if (tipoRegistro.isEmpty()) {

            binding.etTipoRegistro.error =
                "Ingrese el tipo de registro"

            return
        }

        // -----------------------------------------------------
        // VALIDAR DESCRIPCIÓN
        // -----------------------------------------------------

        if (descripcion.isEmpty()) {

            binding.etDescripcion.error =
                "Ingrese una descripción"

            return
        }

        // -----------------------------------------------------
        // FECHA Y HORA
        // -----------------------------------------------------

        val fechaHora = obtenerFechaHoraActual()

        // -----------------------------------------------------
        // CREAR OBJETO BITÁCORA
        // -----------------------------------------------------

        val bitacora = Bitacora(

            estado = estado,

            tipoRegistro = tipoRegistro,

            descripcion = descripcion,

            fechaHora = fechaHora,

            idUsuario = idUsuario,

            idPaciente = pacienteId
        )

        // -----------------------------------------------------
        // CONSUMIR API
        // -----------------------------------------------------

        lifecycleScope.launch {

            try {

                val respuesta = RetrofitClient.apiService.crearBitacora(bitacora)

                Toast.makeText(
                    requireContext(),
                    "Registro de bitácora exitoso",
                    Toast.LENGTH_SHORT
                ).show()

                idBitacora = respuesta.idBitacora

                limpiarFormulario()

            } catch (e: Exception) {
                android.util.Log.e("BitacoraFragment", "Error guardando bitácora", e)
                Toast.makeText(
                    requireContext(),
                    "Error al guardar la bitácora",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun limpiarFormulario() {
        binding.etTipoRegistro.text?.clear()
        binding.etDescripcion.text?.clear()
        binding.switchEstado.isChecked = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
