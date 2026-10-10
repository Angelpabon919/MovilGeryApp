package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.databinding.FragmentCardexBinding

class CardexFragment : Fragment() {

    private var _binding: FragmentCardexBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels()

    private var cuidadoActual: CuidadoEnfermeria? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentCardexBinding.inflate(
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

        // =============================================
        // CARDEX SOLO LECTURA
        // =============================================

        configurarSoloLectura()

        // =============================================
        // ESCUCHAR PACIENTE SELECCIONADO
        // =============================================

        pacienteViewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            val idPaciente = paciente?.idPaciente

            if (idPaciente != null) {

                // Realiza el GET de cuidados del paciente
                pacienteViewModel.cargarCuidados(idPaciente)

            } else {

                limpiarCampos()
            }
        }

        // =============================================
        // MOSTRAR DATOS RECIBIDOS DEL GET
        // =============================================

        pacienteViewModel.cuidados.observe(
            viewLifecycleOwner
        ) { listaCuidados ->

            cuidadoActual =
                listaCuidados?.firstOrNull()

            if (cuidadoActual == null) {

                limpiarCampos()

            } else {

                cuidadoActual?.let { cuidado ->

                    binding.etBanoPaciente.setText(
                        cuidado.banoPaciente ?: ""
                    )

                    binding.etPesoTalla.setText(
                        cuidado.pesoTalla ?: ""
                    )

                    binding.etControlGlucemia.setText(
                        cuidado.controlGlucemia ?: ""
                    )

                    binding.etCuraciones.setText(
                        cuidado.curaciones ?: ""
                    )

                    binding.etLiquidos.setText(
                        cuidado.liquidosAdministradosEliminados
                            ?: ""
                    )

                    binding.etControlDeposicion.setText(
                        cuidado.controlDeposicion ?: ""
                    )

                    binding.etAdminMedicamentos.setText(
                        cuidado.administracionMedicamentos
                            ?: ""
                    )
                }
            }
        }
    }

    // =============================================
    // DEJAR TODOS LOS CAMPOS SOLO LECTURA
    // =============================================

    private fun configurarSoloLectura() {

        binding.etBanoPaciente.isEnabled = false
        binding.etPesoTalla.isEnabled = false
        binding.etControlGlucemia.isEnabled = false
        binding.etCuraciones.isEnabled = false
        binding.etLiquidos.isEnabled = false
        binding.etControlDeposicion.isEnabled = false
        binding.etAdminMedicamentos.isEnabled = false

        // Ya no se guarda desde esta pantalla
    }

    // =============================================
    // LIMPIAR CAMPOS
    // =============================================

    private fun limpiarCampos() {

        binding.etBanoPaciente.setText("")
        binding.etPesoTalla.setText("")
        binding.etControlGlucemia.setText("")
        binding.etCuraciones.setText("")
        binding.etLiquidos.setText("")
        binding.etControlDeposicion.setText("")
        binding.etAdminMedicamentos.setText("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}