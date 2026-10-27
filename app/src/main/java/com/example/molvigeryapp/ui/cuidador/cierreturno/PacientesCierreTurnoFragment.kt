package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.databinding.FragmentPacientesCierreTurnoBinding
import kotlinx.coroutines.launch
import java.util.Locale

class PacientesCierreTurnoFragment : Fragment() {

    private var _binding: FragmentPacientesCierreTurnoBinding? = null
    private val binding get() = _binding!!

    private var idUsuarioCuidador = -1

    private lateinit var pacientesAdapter: PacienteCierreTurnoAdapter
    private var pacientesOriginales: List<Paciente> = emptyList()

    companion object {
        private const val ARG_ID_USUARIO = "id_usuario_cuidador"

        fun newInstance(idUsuario: Int) =
            PacientesCierreTurnoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_USUARIO, idUsuario)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPacientesCierreTurnoBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Primero intenta obtener el ID enviado por la navegación.
        val idRecibido = arguments?.getInt(ARG_ID_USUARIO, -1) ?: -1

        // Si no llegó por navegación, lo obtiene de la sesión.
        val preferencias = requireContext()
            .getSharedPreferences("SESION", Context.MODE_PRIVATE)

        idUsuarioCuidador = if (idRecibido > 0) {
            idRecibido
        } else {
            preferencias.getInt("ID_USUARIO", -1)
        }

        Log.d(
            "PACIENTES_CIERRE",
            "ID recibido=$idRecibido, ID de sesión=" +
                    preferencias.getInt("ID_USUARIO", -1) +
                    ", ID final=$idUsuarioCuidador"
        )

        binding.btnVolverPacientesCierre.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        pacientesAdapter = PacienteCierreTurnoAdapter()

        binding.rvPacientesCierre.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvPacientesCierre.adapter = pacientesAdapter

        binding.etBuscarPacienteCierre.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filtrarPacientes(s?.toString().orEmpty())
                }

                override fun afterTextChanged(s: Editable?) = Unit
            }
        )

        if (idUsuarioCuidador <= 0) {
            mostrarMensaje(
                "No se pudo identificar al cuidador. " +
                        "Cierra sesión e inicia sesión nuevamente."
            )
            return
        }

        cargarPacientesAsignados()
    }

    private fun cargarPacientesAsignados() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.tvSinPacientesCierre.visibility = View.GONE
            binding.rvPacientesCierre.visibility = View.GONE

            try {
                val api = RetrofitClient.apiService

                val respuesta = api.getAsignacionesPacienteCuidador()

                if (!respuesta.isSuccessful) {
                    throw Exception("Error HTTP ${respuesta.code()}")
                }

                val asignaciones = respuesta.body().orEmpty()

                val idsActivos = asignaciones
                    .filter { asignacion ->
                        asignacion.idUsuario == idUsuarioCuidador &&
                                asignacion.estado.equals(
                                    "Activo",
                                    ignoreCase = true
                                )
                    }
                    .map { it.idPaciente }
                    .toSet()

                val todosLosPacientes = api.getPacientes()

                pacientesOriginales = todosLosPacientes.filter { paciente ->
                    paciente.idPaciente != null &&
                            paciente.idPaciente in idsActivos
                }

                pacientesAdapter.actualizarLista(pacientesOriginales)
                actualizarContador(pacientesOriginales.size)

                if (pacientesOriginales.isEmpty()) {
                    mostrarMensaje(
                        "No hay pacientes activos asignados a este cuidador."
                    )
                } else {
                    binding.rvPacientesCierre.visibility = View.VISIBLE
                    binding.tvSinPacientesCierre.visibility = View.GONE
                }

                Log.d(
                    "PACIENTES_CIERRE",
                    "Cuidador=$idUsuarioCuidador, " +
                            "IDs activos=$idsActivos, " +
                            "pacientes=${pacientesOriginales.size}"
                )
            } catch (e: Exception) {
                Log.e(
                    "PACIENTES_CIERRE",
                    "Error cargando pacientes",
                    e
                )

                mostrarMensaje(
                    "No se pudieron cargar los pacientes. Intenta nuevamente."
                )

                Toast.makeText(
                    requireContext(),
                    "Error al consultar los pacientes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun filtrarPacientes(texto: String) {
        val consulta = texto.trim().lowercase(Locale.ROOT)

        val filtrados = if (consulta.isBlank()) {
            pacientesOriginales
        } else {
            pacientesOriginales.filter { paciente ->
                "${paciente.nombre} ${paciente.apellido}"
                    .lowercase(Locale.ROOT)
                    .contains(consulta) ||
                        paciente.numeroDocumento.orEmpty()
                            .lowercase(Locale.ROOT)
                            .contains(consulta) ||
                        paciente.habitacion?.toString().orEmpty()
                            .contains(consulta) ||
                        paciente.cama?.toString().orEmpty()
                            .contains(consulta)
            }
        }

        pacientesAdapter.actualizarLista(filtrados)
        actualizarContador(filtrados.size)

        binding.rvPacientesCierre.visibility =
            if (filtrados.isEmpty()) View.GONE else View.VISIBLE

        binding.tvSinPacientesCierre.visibility =
            if (filtrados.isEmpty()) View.VISIBLE else View.GONE

        binding.tvSinPacientesCierre.text =
            if (consulta.isBlank()) {
                "No hay pacientes activos asignados."
            } else {
                "No se encontraron pacientes con esa búsqueda."
            }
    }

    private fun actualizarContador(cantidad: Int) {
        binding.tvTotalPacientesCierre.text =
            "Pacientes asignados ($cantidad)"
    }

    private fun mostrarMensaje(mensaje: String) {
        _binding?.rvPacientesCierre?.visibility = View.GONE
        _binding?.tvSinPacientesCierre?.visibility = View.VISIBLE
        _binding?.tvSinPacientesCierre?.text = mensaje
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}