package com.example.molvigeryapp.ui.encargado.bitacora

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.BitacoraRepository
import com.example.molvigeryapp.databinding.FragmentBitacoraEncargadoBinding
import kotlinx.coroutines.launch

class BitacoraEncargadoFragment : Fragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding: FragmentBitacoraEncargadoBinding? = null
    private val binding get() = _binding!!

    // =========================================================
    // REPOSITORY
    // =========================================================

    private val repository = BitacoraRepository()

    // =========================================================
    // DATOS DEL CUIDADOR
    // =========================================================

    private var idUsuarioCuidador: Int = -1

    private var nombreCuidador: String = ""
    private var cargoCuidador: String = ""

    // =========================================================
    // DATOS DE BITÁCORA
    // =========================================================

    private var listaBitacoras: List<Bitacora> = emptyList()

    private var listaPacientes: List<Paciente> = emptyList()

    private lateinit var adapter: BitacoraAdapter

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        idUsuarioCuidador =
            arguments?.getInt("id_usuario", -1) ?: -1

        nombreCuidador =
            arguments?.getString("nombre").orEmpty()

        cargoCuidador =
            arguments?.getString("cargo").orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBitacoraEncargadoBinding.inflate(
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

        configurarDatosCuidador()
        configurarRecyclerView()
        configurarBotonVolver()
        configurarBuscador()

        cargarBitacora()
    }

    // =========================================================
    // DATOS DEL CUIDADOR
    // =========================================================

    private fun configurarDatosCuidador() {

        binding.txtNombreCuidadorBitacora.text =
            if (nombreCuidador.isNotBlank()) {
                nombreCuidador
            } else {
                "Cuidador"
            }

        binding.txtCargoCuidadorBitacora.text =
            if (cargoCuidador.isNotBlank()) {
                cargoCuidador
            } else {
                "Cargo no disponible"
            }
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private fun configurarRecyclerView() {

        adapter =
            BitacoraAdapter(
                listaBitacoras = emptyList(),
                listaPacientes = emptyList()
            )

        binding.recyclerEventosBitacora.layoutManager =
            LinearLayoutManager(requireContext())

        binding.recyclerEventosBitacora.adapter =
            adapter
    }

    // =========================================================
    // BOTÓN VOLVER
    // =========================================================

    private fun configurarBotonVolver() {

        binding.btnVolverBitacora.setOnClickListener {

            requireActivity()
                .supportFragmentManager
                .popBackStack()
        }
    }

    // =========================================================
    // BUSCADOR
    // =========================================================

    private fun configurarBuscador() {

        binding.edtBuscarBitacora.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // No necesitamos realizar ninguna acción aquí.
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    filtrarBitacora(
                        s?.toString().orEmpty()
                    )
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                    // No necesitamos realizar ninguna acción aquí.
                }
            }
        )
    }

    // =========================================================
    // CARGAR BITÁCORA
    // =========================================================

    private fun cargarBitacora() {

        if (idUsuarioCuidador == -1) {

            Toast.makeText(
                requireContext(),
                "No se encontró el cuidador",
                Toast.LENGTH_SHORT
            ).show()

            mostrarSinRegistros()

            return
        }

        lifecycleScope.launch {

            try {

                // -------------------------------------------------
                // OBTENER BITÁCORAS
                // -------------------------------------------------

                val bitacoras =
                    repository.obtenerBitacoras()

                // -------------------------------------------------
                // OBTENER PACIENTES
                // -------------------------------------------------

                val pacientes =
                    RetrofitClient.apiService.getPacientes()

                listaPacientes =
                    pacientes

                // -------------------------------------------------
                // FILTRAR POR CUIDADOR
                // -------------------------------------------------

                listaBitacoras =
                    bitacoras.filter { bitacora ->

                        bitacora.idUsuario ==
                                idUsuarioCuidador
                    }

                // -------------------------------------------------
                // ACTUALIZAR ADAPTER
                // -------------------------------------------------

                adapter.actualizarDatos(
                    listaBitacoras,
                    listaPacientes
                )

                actualizarEstadoVista()

            } catch (e: Exception) {

                e.printStackTrace()

                Toast.makeText(
                    requireContext(),
                    "No se pudo cargar la bitácora",
                    Toast.LENGTH_SHORT
                ).show()

                mostrarSinRegistros()
            }
        }
    }

    // =========================================================
    // FILTRAR BITÁCORA
    // =========================================================

    private fun filtrarBitacora(
        texto: String
    ) {

        val busqueda =
            texto.trim().lowercase()

        if (busqueda.isEmpty()) {

            adapter.actualizarDatos(
                listaBitacoras,
                listaPacientes
            )

            actualizarEstadoVista()

            return
        }

        val registrosFiltrados =
            listaBitacoras.filter { bitacora ->

                val paciente =
                    obtenerPaciente(
                        bitacora.idPaciente
                    )

                val nombrePaciente =
                    paciente?.let {

                        "${it.nombre} ${it.apellido}"

                    }.orEmpty()

                val coincideTipo =
                    bitacora.tipoRegistro
                        .lowercase()
                        .contains(busqueda)

                val coincideDescripcion =
                    bitacora.descripcion
                        .lowercase()
                        .contains(busqueda)

                val coincidePaciente =
                    nombrePaciente
                        .lowercase()
                        .contains(busqueda)

                coincideTipo ||
                        coincideDescripcion ||
                        coincidePaciente
            }

        adapter.actualizarDatos(
            registrosFiltrados,
            listaPacientes
        )

        if (registrosFiltrados.isEmpty()) {

            binding.txtSinRegistrosBitacora.visibility =
                View.VISIBLE

            binding.txtSinRegistrosBitacora.text =
                "No se encontraron registros"

        } else {

            binding.txtSinRegistrosBitacora.visibility =
                View.GONE

            binding.recyclerEventosBitacora.visibility =
                View.VISIBLE
        }
    }

    // =========================================================
    // BUSCAR PACIENTE
    // =========================================================

    private fun obtenerPaciente(
        idPaciente: Int?
    ): Paciente? {

        if (idPaciente == null) {
            return null
        }

        return listaPacientes.find { paciente ->

            paciente.idPaciente == idPaciente
        }
    }

    // =========================================================
    // ACTUALIZAR ESTADO DE LA VISTA
    // =========================================================

    private fun actualizarEstadoVista() {

        if (listaBitacoras.isEmpty()) {

            mostrarSinRegistros()

        } else {

            binding.recyclerEventosBitacora.visibility =
                View.VISIBLE

            binding.txtSinRegistrosBitacora.visibility =
                View.GONE
        }
    }

    // =========================================================
    // SIN REGISTROS
    // =========================================================

    private fun mostrarSinRegistros() {

        binding.recyclerEventosBitacora.visibility =
            View.GONE

        binding.txtSinRegistrosBitacora.visibility =
            View.VISIBLE

        binding.txtSinRegistrosBitacora.text =
            "No hay registros en la bitácora"
    }

    // =========================================================
    // DESTRUIR BINDING
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }

    // =========================================================
    // CREAR INSTANCIA
    // =========================================================

    companion object {

        fun newInstance(
            idUsuario: Int,
            nombre: String,
            cargo: String
        ): BitacoraEncargadoFragment {

            return BitacoraEncargadoFragment().apply {

                arguments = Bundle().apply {

                    putInt(
                        "id_usuario",
                        idUsuario
                    )

                    putString(
                        "nombre",
                        nombre
                    )

                    putString(
                        "cargo",
                        cargo
                    )
                }
            }
        }
    }
}