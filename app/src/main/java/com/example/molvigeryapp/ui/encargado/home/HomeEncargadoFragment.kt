package com.example.molvigeryapp.ui.encargado.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.Cuidador
import com.example.molvigeryapp.databinding.FragmentHomeEncargadoBinding
import com.example.molvigeryapp.ui.encargado.cuidadores.CuidadorAdapter
import com.example.molvigeryapp.ui.encargado.cuidadores.DetalleCuidadorEncargadoFragment

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

    private val listaCuidadores = listOf(

        Cuidador(
            nombre = "María Fernández",
            cargo = "Auxiliar de Enfermería",
            estado = "Activo",
            pacientes = 3
        ),

        Cuidador(
            nombre = "Javier Ríos",
            cargo = "Enfermero Profesional",
            estado = "Activo",
            pacientes = 2
        ),

        Cuidador(
            nombre = "Ana Suárez",
            cargo = "Auxiliar de Enfermería",
            estado = "Descanso",
            pacientes = 3
        ),

        Cuidador(
            nombre = "Carlos Rodríguez",
            cargo = "Auxiliar de Enfermería",
            estado = "Activo",
            pacientes = 2
        ),

        Cuidador(
            nombre = "Lucía Gómez",
            cargo = "Enfermera Profesional",
            estado = "Activo",
            pacientes = 4
        )
    )


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
    }


    // =====================================================
    // CONFIGURAR RECYCLERVIEW
    // =====================================================

    private fun configurarRecyclerView() {

        binding.recyclerCuidadores.layoutManager =
            LinearLayoutManager(requireContext())


        adapter = CuidadorAdapter(
            listaCuidadores
        ) { cuidador ->

            abrirDetalleCuidador(cuidador)
        }


        binding.recyclerCuidadores.adapter = adapter
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
                    // No necesitamos hacer nada aquí
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

                    filtrarCuidadores(textoBuscado)
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                    // No necesitamos hacer nada aquí
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

                // Si el buscador está vacío,
                // mostramos todos los cuidadores.

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


        adapter.actualizarLista(listaFiltrada)
    }


    // =====================================================
    // ABRIR DETALLE DEL CUIDADOR
    // =====================================================

    private fun abrirDetalleCuidador(
        cuidador: Cuidador
    ) {

        val detalle =
            DetalleCuidadorEncargadoFragment()


        // =================================================
        // ENVIAR INFORMACIÓN DEL CUIDADOR
        // =================================================

        val datos = Bundle()

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


        // =================================================
        // ABRIR FRAGMENT DEL DETALLE
        // =================================================

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
    // DESTRUIR BINDING
    // =====================================================

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}