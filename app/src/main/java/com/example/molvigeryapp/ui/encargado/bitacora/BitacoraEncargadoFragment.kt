package com.example.molvigeryapp.ui.encargado.bitacora

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.repository.BitacoraRepository
import com.example.molvigeryapp.databinding.FragmentBitacoraEncargadoBinding
import com.example.molvigeryapp.ui.encargado.cuidadores.DetalleBitacoraEncargadoFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BitacoraEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentBitacoraEncargadoBinding? = null

    private val binding
        get() = requireNotNull(_binding)


    // =====================================================
    // REPOSITORIO
    // =====================================================

    private val repository =
        BitacoraRepository()


    // =====================================================
    // DATOS DEL CUIDADOR
    // =====================================================

    private var idUsuarioCuidador: Int =
        -1

    private var nombreCuidador: String =
        ""

    private var cargoCuidador: String =
        ""


    // =====================================================
    // DATOS
    // =====================================================

    private var listaBitacoras:
            List<Bitacora> =
        emptyList()

    private var listaPacientes:
            List<Paciente> =
        emptyList()


    // =====================================================
    // ADAPTER
    // =====================================================

    private lateinit var adapter:
            BitacoraAdapter


    // =====================================================
    // CONTROL DE CARGA
    // =====================================================

    /**
     * Evita que se ejecuten varias solicitudes
     * al mismo tiempo.
     */
    private var cargandoDatos =
        false


    /**
     * Indica si la primera carga ya terminó.
     *
     * El ProgressBar solamente se muestra
     * durante la primera carga.
     */
    private var primeraCargaCompletada =
        false


    // =====================================================
    // CONSTANTES
    // =====================================================

    companion object {

        private const val INTERVALO_ACTUALIZACION =
            2_000L


        fun newInstance(
            idUsuario: Int,
            nombre: String,
            cargo: String
        ): BitacoraEncargadoFragment {

            return BitacoraEncargadoFragment().apply {

                arguments =
                    Bundle().apply {

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


    // =====================================================
    // CREAR FRAGMENT
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        idUsuarioCuidador =
            arguments?.getInt(
                "id_usuario",
                -1
            ) ?: -1


        nombreCuidador =
            arguments
                ?.getString(
                    "nombre"
                )
                .orEmpty()


        cargoCuidador =
            arguments
                ?.getString(
                    "cargo"
                )
                .orEmpty()
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
            FragmentBitacoraEncargadoBinding.inflate(
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


        configurarDatosCuidador()

        configurarRecyclerView()

        configurarBotonVolver()

        configurarBuscador()

        iniciarActualizacionAutomatica()
    }


    // =====================================================
    // DATOS DEL CUIDADOR
    // =====================================================

    private fun configurarDatosCuidador() {

        binding
            .txtNombreCuidadorBitacora
            .text =
            if (
                nombreCuidador.isNotBlank()
            ) {

                nombreCuidador

            } else {

                "Cuidador"
            }


        binding
            .txtCargoCuidadorBitacora
            .text =
            if (
                cargoCuidador.isNotBlank()
            ) {

                cargoCuidador

            } else {

                "Cargo no disponible"
            }
    }


    // =====================================================
    // RECYCLER VIEW
    // =====================================================

    private fun configurarRecyclerView() {

        adapter =
            BitacoraAdapter(
                listaBitacoras =
                    emptyList(),

                listaPacientes =
                    emptyList()
            ) { bitacora ->

                abrirRegistroBitacora(
                    bitacora
                )
            }


        binding
            .recyclerEventosBitacora
            .layoutManager =
            LinearLayoutManager(
                requireContext()
            )


        binding
            .recyclerEventosBitacora
            .adapter =
            adapter
    }


    // =====================================================
    // BOTÓN VOLVER
    // =====================================================

    private fun configurarBotonVolver() {

        binding
            .btnVolverBitacora
            .setOnClickListener {

                parentFragmentManager
                    .popBackStack()
            }
    }


    // =====================================================
    // BUSCADOR
    // =====================================================

    private fun configurarBuscador() {

        binding
            .edtBuscarBitacora
            .addTextChangedListener(

                object : TextWatcher {

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                        // No se necesita lógica aquí.
                    }


                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                        filtrarBitacora(
                            s?.toString()
                                .orEmpty()
                        )
                    }


                    override fun afterTextChanged(
                        s: Editable?
                    ) {
                        // No se necesita lógica aquí.
                    }
                }
            )
    }


    // =====================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =====================================================

    private fun iniciarActualizacionAutomatica() {

        viewLifecycleOwner
            .lifecycleScope
            .launch {

                viewLifecycleOwner
                    .repeatOnLifecycle(
                        Lifecycle.State.STARTED
                    ) {

                        // =========================================
                        // PRIMERA CARGA INMEDIATA
                        // =========================================

                        cargarBitacora(
                            mostrarCargaInicial = true
                        )


                        // =========================================
                        // ACTUALIZACIÓN CADA 2 SEGUNDOS
                        // =========================================

                        while (isActive) {

                            delay(
                                INTERVALO_ACTUALIZACION
                            )


                            if (!isActive) {
                                break
                            }


                            cargarBitacora(
                                mostrarCargaInicial = false
                            )
                        }
                    }
            }
    }


    // =====================================================
    // CARGAR BITÁCORA
    // =====================================================

    private suspend fun cargarBitacora(
        mostrarCargaInicial: Boolean
    ) {

        // =================================================
        // VALIDAR VISTA
        // =================================================

        if (_binding == null) {
            return
        }


        // =================================================
        // VALIDAR CUIDADOR
        // =================================================

        if (
            idUsuarioCuidador == -1
        ) {

            if (
                !primeraCargaCompletada
            ) {

                primeraCargaCompletada =
                    true


                mostrarSinRegistros()


                mostrarMensaje(
                    "No se encontró el cuidador.",
                    Toast.LENGTH_SHORT
                )
            }


            return
        }


        // =================================================
        // EVITAR SOLICITUDES SIMULTÁNEAS
        // =================================================

        if (cargandoDatos) {
            return
        }


        cargandoDatos =
            true


        // =================================================
        // MOSTRAR LOADING SOLO EN LA PRIMERA CARGA
        // =================================================

        if (
            mostrarCargaInicial &&
            !primeraCargaCompletada
        ) {

            mostrarCargandoInicial()
        }


        try {

            // =================================================
            // OBTENER BITÁCORAS
            // =================================================

            val bitacoras =
                repository
                    .obtenerBitacoras()


            // =================================================
            // OBTENER PACIENTES
            // =================================================

            val pacientes =
                RetrofitClient
                    .apiService
                    .getPacientes()


            // =================================================
            // COMPROBAR CORRUTINA Y VISTA
            // =================================================

            if (
                !currentCoroutineContext().isActive ||
                _binding == null
            ) {

                return
            }


            // =================================================
            // GUARDAR PACIENTES
            // =================================================

            listaPacientes =
                pacientes


            // =================================================
            // FILTRAR BITÁCORAS DEL CUIDADOR
            // =================================================

            listaBitacoras =
                bitacoras.filter { bitacora ->

                    bitacora.idUsuario ==
                            idUsuarioCuidador
                }


            // =================================================
            // ACTUALIZAR ADAPTER
            // =================================================

            actualizarAdapterSegunBusqueda()


            // =================================================
            // TERMINAR PRIMERA CARGA
            // =================================================

            if (
                !primeraCargaCompletada
            ) {

                primeraCargaCompletada =
                    true


                ocultarCargandoInicial()
            }


        } catch (
            e: CancellationException
        ) {

            /*
             * Cancelación normal cuando el Fragment
             * deja de estar visible.
             */

            throw e


        } catch (
            e: Exception
        ) {

            e.printStackTrace()


            if (
                !currentCoroutineContext().isActive ||
                _binding == null
            ) {

                return
            }


            // =================================================
            // ERROR EN LA PRIMERA CARGA
            // =================================================

            if (
                !primeraCargaCompletada
            ) {

                primeraCargaCompletada =
                    true


                ocultarCargandoInicial()


                mostrarSinRegistros()


                mostrarMensaje(
                    "No se pudo cargar la bitácora.",
                    Toast.LENGTH_SHORT
                )
            }


            /*
             * Si es una actualización posterior,
             * NO hacemos ningún cambio visual.
             *
             * Los datos que ya estaban en pantalla
             * permanecen visibles.
             */
        }


        // =================================================
        // LIBERAR CONTROL DE CARGA
        // =================================================

        finally {

            cargandoDatos =
                false
        }
    }


    // =====================================================
    // ACTUALIZAR ADAPTER SEGÚN BÚSQUEDA
    // =====================================================

    private fun actualizarAdapterSegunBusqueda() {

        if (_binding == null) {
            return
        }


        val textoBusqueda =
            binding
                .edtBuscarBitacora
                .text
                .toString()
                .trim()


        if (
            textoBusqueda.isEmpty()
        ) {

            adapter.actualizarDatos(
                listaBitacoras,
                listaPacientes
            )


            actualizarEstadoVista()

            return
        }


        filtrarBitacora(
            textoBusqueda
        )
    }


    // =====================================================
    // MOSTRAR CARGA INICIAL
    // =====================================================

    private fun mostrarCargandoInicial() {

        if (_binding == null) {
            return
        }


        binding
            .recyclerEventosBitacora
            .visibility =
            View.GONE


        binding
            .txtSinRegistrosBitacora
            .visibility =
            View.GONE


        binding
            .progressBarBitacora
            .visibility =
            View.VISIBLE
    }


    // =====================================================
    // OCULTAR CARGA INICIAL
    // =====================================================

    private fun ocultarCargandoInicial() {

        if (_binding == null) {
            return
        }


        binding
            .progressBarBitacora
            .visibility =
            View.GONE
    }


    // =====================================================
    // MOSTRAR MENSAJE
    // =====================================================

    private fun mostrarMensaje(
        mensaje: String,
        duracion: Int
    ) {

        val contexto =
            context
                ?: return


        Toast.makeText(
            contexto,
            mensaje,
            duracion
        ).show()
    }


    // =====================================================
    // FILTRAR BITÁCORA
    // =====================================================

    private fun filtrarBitacora(
        texto: String
    ) {

        if (_binding == null) {
            return
        }


        val busqueda =
            texto
                .trim()
                .lowercase()


        // =================================================
        // SIN BÚSQUEDA
        // =================================================

        if (
            busqueda.isEmpty()
        ) {

            adapter.actualizarDatos(
                listaBitacoras,
                listaPacientes
            )


            actualizarEstadoVista()

            return
        }


        // =================================================
        // FILTRAR
        // =================================================

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
                    bitacora
                        .tipoRegistro
                        .lowercase()
                        .contains(
                            busqueda
                        )


                val coincideDescripcion =
                    bitacora
                        .descripcion
                        .lowercase()
                        .contains(
                            busqueda
                        )


                val coincidePaciente =
                    nombrePaciente
                        .lowercase()
                        .contains(
                            busqueda
                        )


                coincideTipo ||
                        coincideDescripcion ||
                        coincidePaciente
            }


        // =================================================
        // ACTUALIZAR ADAPTER
        // =================================================

        adapter.actualizarDatos(
            registrosFiltrados,
            listaPacientes
        )


        // =================================================
        // MOSTRAR RESULTADO
        // =================================================

        if (
            registrosFiltrados.isEmpty()
        ) {

            binding
                .txtSinRegistrosBitacora
                .visibility =
                View.VISIBLE


            binding
                .txtSinRegistrosBitacora
                .text =
                "No se encontraron registros"


            binding
                .recyclerEventosBitacora
                .visibility =
                View.GONE

        } else {

            binding
                .txtSinRegistrosBitacora
                .visibility =
                View.GONE


            binding
                .recyclerEventosBitacora
                .visibility =
                View.VISIBLE
        }
    }


    // =====================================================
    // OBTENER PACIENTE
    // =====================================================

    private fun obtenerPaciente(
        idPaciente: Int?
    ): Paciente? {

        if (
            idPaciente == null
        ) {

            return null
        }


        return listaPacientes.find { paciente ->

            paciente.idPaciente ==
                    idPaciente
        }
    }


    // =====================================================
    // ACTUALIZAR ESTADO DE LA VISTA
    // =====================================================

    private fun actualizarEstadoVista() {

        if (_binding == null) {
            return
        }


        if (
            listaBitacoras.isEmpty()
        ) {

            mostrarSinRegistros()

        } else {

            binding
                .recyclerEventosBitacora
                .visibility =
                View.VISIBLE


            binding
                .txtSinRegistrosBitacora
                .visibility =
                View.GONE
        }
    }


    // =====================================================
    // MOSTRAR SIN REGISTROS
    // =====================================================

    private fun mostrarSinRegistros() {

        if (_binding == null) {
            return
        }


        binding
            .recyclerEventosBitacora
            .visibility =
            View.GONE


        binding
            .txtSinRegistrosBitacora
            .visibility =
            View.VISIBLE


        binding
            .txtSinRegistrosBitacora
            .text =
            "No hay registros en la bitácora"
    }


    // =====================================================
    // ABRIR REGISTRO
    // =====================================================

    private fun abrirRegistroBitacora(
        bitacora: Bitacora
    ) {

        abrirDetalleBitacora(
            bitacora
        )
    }


    // =====================================================
    // DETALLE DE BITÁCORA
    // =====================================================

    private fun abrirDetalleBitacora(
        bitacora: Bitacora
    ) {

        val paciente =
            obtenerPaciente(
                bitacora.idPaciente
            )


        val nombrePaciente =
            paciente?.let {

                "${it.nombre} ${it.apellido}"

            } ?: "Paciente no disponible"


        val datos =
            Bundle().apply {

                putString(
                    "tipo_registro",
                    bitacora.tipoRegistro
                )


                putBoolean(
                    "estado",
                    bitacora.estado
                )


                putString(
                    "nombre_paciente",
                    nombrePaciente
                )


                putString(
                    "fecha_hora",
                    bitacora.fechaHora
                )


                putString(
                    "nombre_cuidador",
                    nombreCuidador
                )


                putString(
                    "descripcion",
                    bitacora.descripcion
                )


                putInt(
                    "id_bitacora",
                    bitacora.idBitacora ?: 0
                )


                putInt(
                    "id_paciente",
                    bitacora.idPaciente ?: 0
                )
            }


        val fragment =
            DetalleBitacoraEncargadoFragment()


        fragment.arguments =
            datos


        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }


    // =====================================================
    // DESTRUIR VISTA
    // =====================================================

    override fun onDestroyView() {

        _binding = null

        super.onDestroyView()
    }
}