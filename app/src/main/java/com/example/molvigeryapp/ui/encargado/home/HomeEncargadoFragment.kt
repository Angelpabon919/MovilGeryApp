package com.example.molvigeryapp.ui.encargado.home

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
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Cuidador
import com.example.molvigeryapp.data.model.Usuario
import com.example.molvigeryapp.databinding.FragmentHomeEncargadoBinding
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.asignarturno.AsignarTurnoEncargadoFragment
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.cuidadores.CuidadorAdapter
import com.example.molvigeryapp.ui.encargado.cuidadores.DetalleCuidadorEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import kotlinx.coroutines.launch

class HomeEncargadoFragment : Fragment() {

    // =====================================================
    // VIEW BINDING
    // =====================================================

    private var _binding: FragmentHomeEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =====================================================
    // ADAPTER
    // =====================================================

    private lateinit var adapter: CuidadorAdapter


    // =====================================================
    // LISTA DE CUIDADORES
    // =====================================================

    private var listaCuidadores: List<Cuidador> = emptyList()


    // =====================================================
    // CREAR VISTA
    // =====================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeEncargadoBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    // =====================================================
    // CUANDO LA VISTA YA ESTÁ CREADA
    // =====================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        configurarRecyclerView()

        configurarBuscador()

        configurarNotificaciones()

        configurarMenuInferior()

        cargarCuidadores()
    }


    // =====================================================
    // CONFIGURAR RECYCLERVIEW
    // =====================================================

    private fun configurarRecyclerView() {

        binding.recyclerCuidadores.layoutManager =
            LinearLayoutManager(requireContext())


        adapter = CuidadorAdapter(
            emptyList()
        ) { cuidador ->

            abrirDetalleCuidador(cuidador)
        }


        binding.recyclerCuidadores.adapter = adapter
    }


    // =====================================================
    // CARGAR CUIDADORES DESDE LA API
    // =====================================================

    private fun cargarCuidadores() {

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                val usuarios =
                    RetrofitClient.apiService.getUsuarios()


                // FILTRAR SOLAMENTE LOS CUIDADORES
                // id_rol = 5

                listaCuidadores =
                    usuarios
                        .filter { usuario ->
                            usuario.idRol == 5
                        }
                        .map { usuario ->

                            convertirACuidador(usuario)
                        }


                adapter.actualizarLista(
                    listaCuidadores
                )


            } catch (e: Exception) {

                Toast.makeText(
                    requireContext(),
                    "No se pudieron cargar los cuidadores",
                    Toast.LENGTH_LONG
                ).show()

                e.printStackTrace()
            }
        }
    }


    // =====================================================
    // CONVERTIR USUARIO → CUIDADOR
    // =====================================================

    private fun convertirACuidador(
        usuario: Usuario
    ): Cuidador {

        val nombreCompleto =
            "${usuario.nombres} ${usuario.apellidos}"
                .trim()


        val estado =
            if (usuario.estado) {
                "Activo"
            } else {
                "Inactivo"
            }


        return Cuidador(

            idUsuario = usuario.idUsuario ?: 0,

            nombre = nombreCompleto,

            cargo = "Cuidador",

            estado = estado,

            pacientes = 0
        )
    }


    // =====================================================
    // CONFIGURAR NOTIFICACIONES
    // =====================================================

    private fun configurarNotificaciones() {

        binding.btnNotificaciones.setOnClickListener {

            abrirNotificaciones()
        }
    }


    // =====================================================
    // ABRIR PANTALLA DE NOTIFICACIONES
    // =====================================================

    private fun abrirNotificaciones() {

        val notificaciones =
            NotificacionesEncargadoFragment()


        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                notificaciones
            )
            .addToBackStack(null)
            .commit()
    }


    // =====================================================
    // CONFIGURAR MENÚ INFERIOR
    // =====================================================

    private fun configurarMenuInferior() {

        NavegacionEncargado.configurar(

            // =========================
            // INICIO
            // =========================

            navInicio = binding.navInicio,
            iconInicio = binding.iconInicio,
            textInicio = binding.textInicio,


            // =========================
            // ASIGNAR TURNO
            // =========================

            navAsignarTurno = binding.navAsignarTurno,
            iconAsignarTurno = binding.iconAsignarTurno,
            textAsignarTurno = binding.textAsignarTurno,


            // =========================
            // CITAS
            // =========================

            navCitas = binding.navCitas,
            iconCitas = binding.iconCitas,
            textCitas = binding.textCitas,


            // =========================
            // PERFIL
            // =========================

            navPerfil = binding.navPerfil,
            iconPerfil = binding.iconPerfil,
            textPerfil = binding.textPerfil,


            // =========================
            // PANTALLA ACTUAL
            // =========================

            pantallaActual =
                NavegacionEncargado.Pantalla.INICIO,


            // =========================
            // IR A INICIO
            // =========================

            onInicio = {

                // Ya estamos en Inicio.

            },


            // =========================
            // IR A ASIGNAR TURNO
            // =========================

            onAsignarTurno = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        AsignarTurnoEncargadoFragment()
                    )
                    .commit()
            },


            // =========================
            // IR A CITAS
            // =========================

            onCitas = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        CitasEncargadoFragment()
                    )
                    .commit()
            },


            // =========================
            // IR A PERFIL
            // =========================

            onPerfil = {

                parentFragmentManager
                    .beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        PerfilEncargadoFragment()
                    )
                    .commit()
            }
        )
    }


    // =====================================================
    // CONFIGURAR BUSCADOR
    // =====================================================

    private fun configurarBuscador() {

        binding.edtBuscar.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // No hacemos nada.
                }


                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val textoBuscado =
                        s.toString()
                            .trim()
                            .lowercase()

                    filtrarCuidadores(
                        textoBuscado
                    )
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                    // No hacemos nada.
                }
            }
        )
    }


    // =====================================================
    // FILTRAR CUIDADORES
    // =====================================================

    private fun filtrarCuidadores(
        texto: String
    ) {

        val listaFiltrada =

            if (texto.isEmpty()) {

                listaCuidadores

            } else {

                listaCuidadores.filter { cuidador ->

                    cuidador.nombre
                        .lowercase()
                        .contains(texto)

                            ||

                            cuidador.cargo
                                .lowercase()
                                .contains(texto)

                            ||

                            cuidador.estado
                                .lowercase()
                                .contains(texto)
                }
            }


        adapter.actualizarLista(
            listaFiltrada
        )
    }


    // =====================================================
    // ABRIR DETALLE DEL CUIDADOR
    // =====================================================

    private fun abrirDetalleCuidador(
        cuidador: Cuidador
    ) {

        val detalle =
            DetalleCuidadorEncargadoFragment()


        val datos = Bundle()


        // =================================================
        // ID REAL DEL USUARIO
        // =================================================

        datos.putInt(
            "id_usuario",
            cuidador.idUsuario
        )


        datos.putString(
            "nombre",
            cuidador.nombre
        )


        datos.putString(
            "cargo",
            cuidador.cargo
        )


        datos.putString(
            "estado",
            cuidador.estado
        )


        datos.putInt(
            "pacientes",
            cuidador.pacientes
        )


        detalle.arguments = datos


        parentFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                detalle
            )
            .addToBackStack(null)
            .commit()
    }


    // =====================================================
    // DESTRUIR VISTA
    // =====================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}