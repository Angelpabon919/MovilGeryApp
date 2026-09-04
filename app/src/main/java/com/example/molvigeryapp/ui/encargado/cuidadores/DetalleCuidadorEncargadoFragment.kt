package com.example.molvigeryapp.ui.encargado.cuidadores

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.ContenidoEventosEncargadoBinding
import com.example.molvigeryapp.databinding.ContenidoMedicamentosEncargadoBinding
import com.example.molvigeryapp.databinding.ContenidoPacientesEncargadoBinding
import com.example.molvigeryapp.databinding.ContenidoResponsabilidadesEncargadoBinding
import com.example.molvigeryapp.databinding.ContenidoTurnosEncargadoBinding
import com.example.molvigeryapp.databinding.FragmentDetalleCuidadorEncBinding
import com.example.molvigeryapp.ui.encargado.bitacora.DetalleEventoAdversoEncargadoFragment

class DetalleCuidadorEncargadoFragment : Fragment() {

    private var _binding: FragmentDetalleCuidadorEncBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val AZUL = "#3B5BDB"
        private const val GRIS = "#98A2B3"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentDetalleCuidadorEncBinding.inflate(
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

        cargarDatosCuidador()

        configurarBotonVolver()

        configurarModulos()

        // Al entrar se muestra Eventos Adversos
        seleccionarModulo(Modulo.EVENTOS)
    }

    // =====================================================
    // DATOS DEL CUIDADOR
    // =====================================================

    private fun cargarDatosCuidador() {

        val nombre = arguments?.getString("nombre") ?: ""
        val cargo = arguments?.getString("cargo") ?: ""
        val estado = arguments?.getString("estado") ?: ""
        val pacientes = arguments?.getInt("pacientes") ?: 0

        binding.txtNombreCuidador.text = nombre
        binding.txtCargoCuidador.text = cargo
        binding.txtEstadoCuidador.text = estado
        binding.txtCantidadPacientes.text = pacientes.toString()
    }

    // =====================================================
    // BOTÓN VOLVER
    // =====================================================

    private fun configurarBotonVolver() {

        binding.btnVolver.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    // =====================================================
    // CONFIGURAR MÓDULOS
    // =====================================================

    private fun configurarModulos() {

        binding.moduloEventos.setOnClickListener {
            seleccionarModulo(Modulo.EVENTOS)
        }

        binding.moduloTurnos.setOnClickListener {
            seleccionarModulo(Modulo.TURNOS)
        }

        binding.moduloPacientes.setOnClickListener {
            seleccionarModulo(Modulo.PACIENTES)
        }

        binding.moduloMedicamentos.setOnClickListener {
            seleccionarModulo(Modulo.MEDICAMENTOS)
        }

        binding.moduloResponsabilidades.setOnClickListener {
            seleccionarModulo(Modulo.RESPONSABILIDADES)
        }
    }

    // =====================================================
    // SELECCIONAR MÓDULO
    // =====================================================

    private fun seleccionarModulo(modulo: Modulo) {

        ponerTodosLosModulosEnGris()

        when (modulo) {

            Modulo.EVENTOS -> {

                seleccionar(
                    binding.textEventos,
                    binding.lineaEventos
                )

                mostrarEventos()
            }

            Modulo.TURNOS -> {

                seleccionar(
                    binding.textTurnos,
                    binding.lineaTurnos
                )

                mostrarTurnos()
            }

            Modulo.PACIENTES -> {

                seleccionar(
                    binding.textPacientes,
                    binding.lineaPacientes
                )

                mostrarPacientes()
            }

            Modulo.MEDICAMENTOS -> {

                seleccionar(
                    binding.textMedicamentos,
                    binding.lineaMedicamentos
                )

                mostrarMedicamentos()
            }

            Modulo.RESPONSABILIDADES -> {

                seleccionar(
                    binding.textResponsabilidades,
                    binding.lineaResponsabilidades
                )

                mostrarResponsabilidades()
            }
        }
    }

    // =====================================================
    // PONER TODOS LOS MÓDULOS EN GRIS
    // =====================================================

    private fun ponerTodosLosModulosEnGris() {

        val gris = Color.parseColor(GRIS)

        binding.textEventos.setTextColor(gris)
        binding.textTurnos.setTextColor(gris)
        binding.textPacientes.setTextColor(gris)
        binding.textMedicamentos.setTextColor(gris)
        binding.textResponsabilidades.setTextColor(gris)

        binding.textEventos.setTypeface(
            null,
            Typeface.NORMAL
        )

        binding.textTurnos.setTypeface(
            null,
            Typeface.NORMAL
        )

        binding.textPacientes.setTypeface(
            null,
            Typeface.NORMAL
        )

        binding.textMedicamentos.setTypeface(
            null,
            Typeface.NORMAL
        )

        binding.textResponsabilidades.setTypeface(
            null,
            Typeface.NORMAL
        )

        binding.lineaEventos.setBackgroundColor(
            Color.TRANSPARENT
        )

        binding.lineaTurnos.setBackgroundColor(
            Color.TRANSPARENT
        )

        binding.lineaPacientes.setBackgroundColor(
            Color.TRANSPARENT
        )

        binding.lineaMedicamentos.setBackgroundColor(
            Color.TRANSPARENT
        )

        binding.lineaResponsabilidades.setBackgroundColor(
            Color.TRANSPARENT
        )
    }

    // =====================================================
    // MARCAR MÓDULO SELECCIONADO
    // =====================================================

    private fun seleccionar(
        texto: android.widget.TextView,
        linea: View
    ) {

        val azul = Color.parseColor(AZUL)

        texto.setTextColor(azul)

        texto.setTypeface(
            null,
            Typeface.BOLD
        )

        linea.setBackgroundColor(azul)
    }

    // =====================================================
    // EVENTOS ADVERSOS
    // =====================================================

    private fun mostrarEventos() {

        binding.contenedorContenido.removeAllViews()

        val contenidoBinding =
            ContenidoEventosEncargadoBinding.inflate(
                layoutInflater,
                binding.contenedorContenido,
                false
            )

        binding.contenedorContenido.addView(
            contenidoBinding.root
        )

        // ==============================================
        // EVENTO ROSA
        // ==============================================

        contenidoBinding.eventoRosa.setOnClickListener {

            abrirDetalleEvento(
                paciente = "Rosa Martínez",
                tipo = "Caída",
                fecha = "03 Sep 2026 · 14:30",
                estado = "Pendiente",
                descripcion = "El paciente presentó una caída mientras se encontraba en la habitación. El cuidador informó inmediatamente del evento para realizar el seguimiento correspondiente.",
                observaciones = "Se recomienda continuar observando al paciente y registrar cualquier cambio en su estado.",
                fechaFotografia = "Registrada el 03 Sep 2026 · 14:35"
            )
        }

        // ==============================================
        // EVENTO CARLOS
        // ==============================================

        contenidoBinding.eventoCarlos.setOnClickListener {

            abrirDetalleEvento(
                paciente = "Carlos Gómez",
                tipo = "Reacción a medicamento",
                fecha = "02 Sep 2026 · 09:15",
                estado = "Revisado",
                descripcion = "El paciente presentó una reacción posterior a la administración de su medicamento habitual.",
                observaciones = "El evento fue revisado por el encargado y se dejó registro para seguimiento.",
                fechaFotografia = "Registrada el 02 Sep 2026 · 09:20"
            )
        }

        // ==============================================
        // EVENTO ELENA
        // ==============================================

        contenidoBinding.eventoElena.setOnClickListener {

            abrirDetalleEvento(
                paciente = "Elena Pérez",
                tipo = "Alteración de conducta",
                fecha = "01 Sep 2026 · 18:20",
                estado = "Pendiente",
                descripcion = "Se observó un cambio en el comportamiento habitual de la paciente durante el turno.",
                observaciones = "Se recomienda realizar seguimiento de la conducta y registrar cualquier cambio adicional.",
                fechaFotografia = "Registrada el 01 Sep 2026 · 18:25"
            )
        }
    }

    // =====================================================
    // ABRIR DETALLE DEL EVENTO
    // =====================================================

    private fun abrirDetalleEvento(
        paciente: String,
        tipo: String,
        fecha: String,
        estado: String,
        descripcion: String,
        observaciones: String,
        fechaFotografia: String
    ) {

        val detalle =
            DetalleEventoAdversoEncargadoFragment()

        val datos = Bundle()

        datos.putString(
            "paciente",
            paciente
        )

        datos.putString(
            "tipo",
            tipo
        )

        datos.putString(
            "fecha",
            fecha
        )

        datos.putString(
            "estado",
            estado
        )

        datos.putString(
            "cuidador",
            arguments?.getString("nombre") ?: ""
        )

        datos.putString(
            "descripcion",
            descripcion
        )

        datos.putString(
            "observaciones",
            observaciones
        )

        datos.putString(
            "fechaFotografia",
            fechaFotografia
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
    // TURNOS
    // =====================================================

    private fun mostrarTurnos() {

        binding.contenedorContenido.removeAllViews()

        val contenidoBinding =
            ContenidoTurnosEncargadoBinding.inflate(
                layoutInflater,
                binding.contenedorContenido,
                false
            )

        binding.contenedorContenido.addView(
            contenidoBinding.root
        )
    }

    // =====================================================
    // PACIENTES
    // =====================================================

    private fun mostrarPacientes() {

        binding.contenedorContenido.removeAllViews()

        val contenidoBinding =
            ContenidoPacientesEncargadoBinding.inflate(
                layoutInflater,
                binding.contenedorContenido,
                false
            )

        binding.contenedorContenido.addView(
            contenidoBinding.root
        )
    }

    // =====================================================
    // MEDICAMENTOS
    // =====================================================

    private fun mostrarMedicamentos() {

        binding.contenedorContenido.removeAllViews()

        val contenidoBinding =
            ContenidoMedicamentosEncargadoBinding.inflate(
                layoutInflater,
                binding.contenedorContenido,
                false
            )

        binding.contenedorContenido.addView(
            contenidoBinding.root
        )
    }

    // =====================================================
    // RESPONSABILIDADES
    // =====================================================

    private fun mostrarResponsabilidades() {

        binding.contenedorContenido.removeAllViews()

        val contenidoBinding =
            ContenidoResponsabilidadesEncargadoBinding.inflate(
                layoutInflater,
                binding.contenedorContenido,
                false
            )

        binding.contenedorContenido.addView(
            contenidoBinding.root
        )
    }

    // =====================================================
    // MÓDULOS
    // =====================================================

    private enum class Modulo {
        EVENTOS,
        TURNOS,
        PACIENTES,
        MEDICAMENTOS,
        RESPONSABILIDADES
    }

    // =====================================================
    // DESTRUIR BINDING
    // =====================================================

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}