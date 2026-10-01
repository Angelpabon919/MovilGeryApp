package com.example.molvigeryapp.ui.encargado.home

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.Cuidador
import com.example.molvigeryapp.data.model.Usuario
import com.example.molvigeryapp.databinding.FragmentHomeEncargadoBinding
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.WindowInsetsEncargado
import com.example.molvigeryapp.ui.encargado.asignarturno.AsignarTurnoEncargadoFragment
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.cuidadores.CuidadorAdapter
import com.example.molvigeryapp.ui.encargado.cuidadores.DetalleCuidadorEncargadoFragment
import com.example.molvigeryapp.ui.encargado.notificaciones.ContadorNotificaciones
import com.example.molvigeryapp.ui.encargado.notificaciones.NotificacionesEncargadoFragment
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
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
    // FILTRO
    // =====================================================

    // 0 = Todos
    // 1 = Activos
    // 2 = Inactivos

    private var filtroSeleccionado: Int = 0


    // =====================================================
    // CONTROL DE CARGA
    // =====================================================

    private var cargandoCuidadores = false

    private var primeraCarga = true


    // =====================================================
    // CONSTANTES
    // =====================================================

    companion object {

        private const val TAG = "HOME_ENCARGADO"

        private const val INTERVALO_ACTUALIZACION = 2_000L
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
            FragmentHomeEncargadoBinding.inflate(
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
            contenido = binding.recyclerCuidadores,
            menuInferior = binding.bottomNavigation
        )


        // =================================================
        // CONFIGURACIONES
        // =================================================

        configurarRecyclerView()

        configurarBuscador()

        configurarFiltro()

        configurarNotificaciones()

        configurarMenuInferior()


        // =================================================
        // ACTUALIZACIÓN AUTOMÁTICA
        // =================================================

        iniciarActualizacionCuidadores()


        // =================================================
        // CONTADOR DE NOTIFICACIONES
        // =================================================

        ContadorNotificaciones.iniciar(
            fragment = this,
            badge = binding.txtNotificaciones
        )
    }


    // =====================================================
    // RECYCLER VIEW
    // =====================================================

    private fun configurarRecyclerView() {

        val contexto = context ?: return

        binding.recyclerCuidadores.layoutManager =
            LinearLayoutManager(contexto)


        adapter =
            CuidadorAdapter(
                emptyList()
            ) { cuidador ->

                abrirDetalleCuidador(
                    cuidador
                )
            }


        binding.recyclerCuidadores.adapter =
            adapter
    }


    // =====================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // =====================================================

    private fun iniciarActualizacionCuidadores() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                while (true) {

                    cargarCuidadores(
                        mostrarCargaInicial = primeraCarga
                    )

                    primeraCarga = false

                    delay(
                        INTERVALO_ACTUALIZACION
                    )
                }
            }
        }
    }


    // =====================================================
    // CARGAR CUIDADORES
    // =====================================================

    private fun cargarCuidadores(
        mostrarCargaInicial: Boolean = false
    ) {

        if (cargandoCuidadores) {
            return
        }


        if (!isAdded || _binding == null) {
            return
        }


        cargandoCuidadores = true


        if (mostrarCargaInicial) {

            mostrarCargando()
        }


        viewLifecycleOwner.lifecycleScope.launch {

            try {

                // =================================================
                // OBTENER USUARIOS
                // =================================================

                val usuarios =
                    RetrofitClient.apiService
                        .getUsuarios()


                if (_binding == null) {
                    return@launch
                }


                // =================================================
                // MOSTRAR NOMBRE DEL ENCARGADO
                // =================================================

                mostrarNombreEncargado(
                    usuarios
                )


                // =================================================
                // OBTENER ASIGNACIONES
                // =================================================

                val respuestaAsignaciones =
                    RetrofitClient.apiService
                        .getAsignacionesPacienteCuidador()


                if (_binding == null) {
                    return@launch
                }


                val asignacionesPacientes =
                    if (respuestaAsignaciones.isSuccessful) {

                        respuestaAsignaciones
                            .body()
                            .orEmpty()

                    } else {

                        emptyList()
                    }


                // =================================================
                // CREAR LISTA DE CUIDADORES
                // =================================================

                listaCuidadores =
                    usuarios
                        .filter { usuario ->

                            usuario.idRol == 5
                        }
                        .map { usuario ->

                            val idUsuario =
                                usuario.idUsuario ?: 0


                            val cantidadPacientes =
                                obtenerCantidadPacientes(
                                    idUsuario,
                                    asignacionesPacientes
                                )


                            convertirACuidador(
                                usuario,
                                cantidadPacientes
                            )
                        }


                if (_binding == null) {
                    return@launch
                }


                // =================================================
                // APLICAR FILTROS
                // =================================================

                aplicarFiltros()


            } catch (e: CancellationException) {

                // La navegación o destrucción de la vista
                // canceló la corrutina. Es un comportamiento normal.

                throw e

            } catch (e: Exception) {

                android.util.Log.e(
                    TAG,
                    "Error al actualizar cuidadores",
                    e
                )


                if (!isAdded || _binding == null) {
                    return@launch
                }


                // Solo mostramos estado de error durante
                // la carga inicial.
                //
                // Durante las actualizaciones automáticas
                // no mostramos Toast repetidamente.

                if (mostrarCargaInicial) {

                    mostrarErrorCarga()
                }

            } finally {

                cargandoCuidadores = false


                if (
                    mostrarCargaInicial &&
                    _binding != null
                ) {

                    ocultarCargando()
                }
            }
        }
    }


    // =====================================================
    // MOSTRAR CARGANDO
    // =====================================================

    private fun mostrarCargando() {

        val bindingActual =
            _binding ?: return


        bindingActual.progressBarCuidadores.visibility =
            View.VISIBLE


        bindingActual.recyclerCuidadores.visibility =
            View.GONE
    }


    // =====================================================
    // OCULTAR CARGANDO
    // =====================================================

    private fun ocultarCargando() {

        val bindingActual =
            _binding ?: return


        bindingActual.progressBarCuidadores.visibility =
            View.GONE


        bindingActual.recyclerCuidadores.visibility =
            View.VISIBLE
    }


    // =====================================================
    // ERROR DE CARGA
    // =====================================================

    private fun mostrarErrorCarga() {

        val bindingActual =
            _binding ?: return


        bindingActual.progressBarCuidadores.visibility =
            View.GONE


        bindingActual.recyclerCuidadores.visibility =
            View.VISIBLE
    }


    // =====================================================
    // CONTAR PACIENTES
    // =====================================================

    private fun obtenerCantidadPacientes(
        idUsuario: Int,
        asignaciones: List<AsignacionPacienteCuidador>
    ): Int {

        return asignaciones
            .filter { asignacion ->

                asignacion.idUsuario == idUsuario
            }
            .map { asignacion ->

                asignacion.idPaciente
            }
            .distinct()
            .size
    }


    // =====================================================
    // MOSTRAR NOMBRE DEL ENCARGADO
    // =====================================================

    private fun mostrarNombreEncargado(
        usuarios: List<Usuario>
    ) {

        val bindingActual =
            _binding ?: return


        val contexto =
            context ?: return


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


        val usuarioActual =
            usuarios.firstOrNull { usuario ->

                usuario.idUsuario == idUsuario
            }


        val nombre =
            usuarioActual
                ?.nombres
                ?.trim()
                ?.split(" ")
                ?.firstOrNull()
                ?.replaceFirstChar { caracter ->

                    caracter.uppercase()
                }


        bindingActual.txtBienvenida.text =

            if (!nombre.isNullOrBlank()) {

                "Hola encargado $nombre"

            } else {

                "Hola encargado"
            }
    }


    // =====================================================
    // CONVERTIR USUARIO A CUIDADOR
    // =====================================================

    private fun convertirACuidador(
        usuario: Usuario,
        cantidadPacientes: Int
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

            idUsuario =
                usuario.idUsuario ?: 0,

            nombre =
                nombreCompleto,

            cargo =
                "Cuidador",

            estado =
                estado,

            pacientes =
                cantidadPacientes
        )
    }


    // =====================================================
    // CONFIGURAR FILTRO
    // =====================================================

    private fun configurarFiltro() {

        filtroSeleccionado = 0

        actualizarTextoFiltro()


        binding.btnFiltroCuidadores.setOnClickListener {

            mostrarMenuFiltro()
        }
    }


    // =====================================================
    // MOSTRAR MENU DEL FILTRO
    // =====================================================

    private fun mostrarMenuFiltro() {

        if (!isAdded || _binding == null) {
            return
        }


        val contexto =
            context ?: return


        val popup =
            PopupMenu(
                contexto,
                binding.btnFiltroCuidadores
            )


        // =================================================
        // TODOS
        // =================================================

        popup.menu.add(
            0,
            0,
            0,
            if (filtroSeleccionado == 0) {

                "✓  Todos"

            } else {

                "    Todos"
            }
        )


        // =================================================
        // ACTIVOS
        // =================================================

        popup.menu.add(
            0,
            1,
            1,
            if (filtroSeleccionado == 1) {

                "✓  Activos"

            } else {

                "    Activos"
            }
        )


        // =================================================
        // INACTIVOS
        // =================================================

        popup.menu.add(
            0,
            2,
            2,
            if (filtroSeleccionado == 2) {

                "✓  Inactivos"

            } else {

                "    Inactivos"
            }
        )


        // =================================================
        // SELECCIONAR
        // =================================================

        popup.setOnMenuItemClickListener { item ->

            filtroSeleccionado =

                when (item.itemId) {

                    0 -> 0

                    1 -> 1

                    2 -> 2

                    else -> 0
                }


            actualizarTextoFiltro()

            aplicarFiltros()

            true
        }


        popup.show()
    }


    // =====================================================
    // ACTUALIZAR TEXTO E ICONO DEL FILTRO
    // =====================================================

    private fun actualizarTextoFiltro() {

        if (_binding == null) {
            return
        }


        // =================================================
        // TEXTO
        // =================================================

        binding.txtFiltroCuidadores.text =

            when (filtroSeleccionado) {

                0 -> "Todos"

                1 -> "Activos"

                2 -> "Inactivos"

                else -> "Todos"
            }


        // =================================================
        // COLOR DEL TEXTO E ICONO
        // =================================================

        when (filtroSeleccionado) {

            // TODOS

            0 -> {

                binding.txtFiltroCuidadores.setTextColor(
                    Color.parseColor("#344054")
                )

                binding.iconFiltroCuidadores.setColorFilter(
                    Color.parseColor("#3B5BDB")
                )
            }


            // ACTIVOS

            1 -> {

                binding.txtFiltroCuidadores.setTextColor(
                    Color.parseColor("#198754")
                )

                binding.iconFiltroCuidadores.setColorFilter(
                    Color.parseColor("#198754")
                )
            }


            // INACTIVOS

            2 -> {

                binding.txtFiltroCuidadores.setTextColor(
                    Color.parseColor("#667085")
                )

                binding.iconFiltroCuidadores.setColorFilter(
                    Color.parseColor("#667085")
                )
            }
        }
    }


    // =====================================================
    // BUSCADOR
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
                }


                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    aplicarFiltros()
                }


                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }


    // =====================================================
    // APLICAR FILTRO + BUSCADOR
    // =====================================================

    private fun aplicarFiltros() {

        val bindingActual =
            _binding ?: return


        val textoBuscado =
            bindingActual.edtBuscar
                .text
                .toString()
                .trim()
                .lowercase()


        var listaFiltrada: List<Cuidador> =

            when (filtroSeleccionado) {

                // TODOS

                0 -> {

                    listaCuidadores
                }


                // ACTIVOS

                1 -> {

                    listaCuidadores.filter { cuidador ->

                        cuidador.estado.equals(
                            "Activo",
                            ignoreCase = true
                        )
                    }
                }


                // INACTIVOS

                2 -> {

                    listaCuidadores.filter { cuidador ->

                        cuidador.estado.equals(
                            "Inactivo",
                            ignoreCase = true
                        )
                    }
                }


                else -> {

                    listaCuidadores
                }
            }


        // =================================================
        // FILTRO DE BÚSQUEDA
        // =================================================

        if (textoBuscado.isNotEmpty()) {

            listaFiltrada =
                listaFiltrada.filter { cuidador ->

                    cuidador.nombre
                        .lowercase()
                        .contains(textoBuscado)

                            ||

                            cuidador.cargo
                                .lowercase()
                                .contains(textoBuscado)

                            ||

                            cuidador.estado
                                .lowercase()
                                .contains(textoBuscado)
                }
        }


        // =================================================
        // ACTUALIZAR RECYCLER
        // =================================================

        adapter.actualizarLista(
            listaFiltrada
        )
    }


    // =====================================================
    // NOTIFICACIONES
    // =====================================================

    private fun configurarNotificaciones() {

        binding.btnNotificaciones.setOnClickListener {

            abrirNotificaciones()
        }
    }


    private fun abrirNotificaciones() {

        if (!isAdded) {
            return
        }


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
    // MENÚ INFERIOR
    // =====================================================

    private fun configurarMenuInferior() {

        NavegacionEncargado.configurar(

            navInicio =
                binding.navInicio,

            iconInicio =
                binding.iconInicio,

            textInicio =
                binding.textInicio,


            navAsignarTurno =
                binding.navAsignarTurno,

            iconAsignarTurno =
                binding.iconAsignarTurno,

            textAsignarTurno =
                binding.textAsignarTurno,


            navCitas =
                binding.navCitas,

            iconCitas =
                binding.iconCitas,

            textCitas =
                binding.textCitas,


            navPerfil =
                binding.navPerfil,

            iconPerfil =
                binding.iconPerfil,

            textPerfil =
                binding.textPerfil,


            pantallaActual =
                NavegacionEncargado.Pantalla.INICIO,


            onInicio = {

                // Ya estamos en Inicio.
            },


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


            onPerfil = {

                if (!isAdded) {
                    return@configurar
                }


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
    // ABRIR DETALLE DEL CUIDADOR
    // =====================================================

    private fun abrirDetalleCuidador(
        cuidador: Cuidador
    ) {

        if (!isAdded) {
            return
        }


        val detalle =
            DetalleCuidadorEncargadoFragment()


        val datos =
            Bundle()


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


        detalle.arguments =
            datos


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

        _binding?.recyclerCuidadores?.adapter = null

        _binding = null

        super.onDestroyView()
    }
}