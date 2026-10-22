package com.example.molvigeryapp.ui.cuidador.bitacora

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.SignosVitales
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentBitacoraBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModelFactory
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BitacoraFragment : Fragment() {

    private var _binding: FragmentBitacoraBinding? = null
    private val binding get() = _binding!!

    private val pacienteViewModel: PacienteViewModel by activityViewModels {
        PacienteViewModelFactory(PacienteRepository())
    }

    private var idPaciente: Int? = null
    private var idBitacora: Int? = null
    private var callbackVolverEvento: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBitacoraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observarPaciente()
        configurarBotones()
        configurarSignosVitales()
        configurarControlesPeriodicos()
        configurarBotonAtrasEvento()
    }

    private fun observarPaciente() {
        pacienteViewModel.pacienteSeleccionado.observe(viewLifecycleOwner) { paciente ->
            idPaciente = paciente?.idPaciente
            Log.d("BITACORA", "Paciente seleccionado. ID: $idPaciente")
        }
    }

    private fun configurarBotones() {
        binding.btnGuardar.setOnClickListener {
            guardarBitacora()
        }

        binding.btnEventoAdverso.setOnClickListener {
            abrirEventoAdverso()
        }
    }

    private fun configurarBotonAtrasEvento() {
        val callback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (childFragmentManager.backStackEntryCount > 0) {
                    childFragmentManager.popBackStack()
                }

                binding.contenedorEventoAdverso.visibility = View.GONE
                binding.scrollBitacora.visibility = View.VISIBLE

                isEnabled = false
            }
        }

        callbackVolverEvento = callback

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callback
        )
    }

    private fun abrirEventoAdverso() {
        val idActual = pacienteViewModel.idBitacora.value

        if (idActual == null) {
            Toast.makeText(
                requireContext(),
                "Primero debes guardar la bitácora",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (childFragmentManager.backStackEntryCount > 0) {
            return
        }

        val evento = EventosAdversosFragment().apply {
            arguments = Bundle().apply {
                putInt("ID_BITACORA", idActual)
            }
        }

        binding.scrollBitacora.visibility = View.GONE
        binding.contenedorEventoAdverso.visibility = View.VISIBLE

        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.contenedorEventoAdverso, evento)
            addToBackStack("evento_adverso")
        }

        callbackVolverEvento?.isEnabled = true
    }

    private fun configurarSignosVitales() {
        binding.switchSignosVitales.setOnCheckedChangeListener { _, activado ->
            binding.layoutSignosVitales.visibility =
                if (activado) View.VISIBLE else View.GONE
        }
    }

    private fun configurarControlesPeriodicos() {
        binding.switchControlesPeriodicos.setOnCheckedChangeListener { _, activado ->
            binding.layoutControlesPeriodicos.visibility =
                if (activado) View.VISIBLE else View.GONE
        }

        configurarGrupo(
            binding.rgEstadoPesoTalla,
            binding.layoutDatosPesoTalla,
            binding.rbPesoTallaNoAplica.id
        )

        configurarGrupo(
            binding.rgEstadoGlicemia,
            binding.layoutDatoGlicemia,
            binding.rbGlicemiaNoAplica.id
        )
    }

    private fun configurarGrupo(
        grupo: RadioGroup,
        contenido: View,
        idNoAplica: Int
    ) {
        grupo.setOnCheckedChangeListener { _, idSeleccionado ->
            contenido.visibility =
                if (idSeleccionado != -1 && idSeleccionado != idNoAplica) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    private fun obtenerIdUsuario(): Int {
        val preferencias = requireActivity()
            .getSharedPreferences("SESION", Context.MODE_PRIVATE)

        return preferencias.getInt("ID_USUARIO", -1)
    }

    private fun obtenerFechaHoraActual(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        ).format(Date())
    }

    private fun leerNumero(texto: String): Double? {
        return texto.trim().replace(',', '.').toDoubleOrNull()
    }

    private fun validarControlesPeriodicos(): Boolean {
        if (!binding.switchControlesPeriodicos.isChecked) {
            return true
        }

        val grupos = listOf(
            binding.rgEstadoPesoTalla to "peso y talla",
            binding.rgEstadoGlicemia to "glicemia",
            binding.rgTerapiasFisicas to "terapias físicas",
            binding.rgTerapiaRespiratoria to "terapia respiratoria",
            binding.rgCorteUnas to "corte de uñas",
            binding.rgCorteCabello to "corte de cabello"
        )

        val incompleto = grupos.firstOrNull {
            it.first.checkedRadioButtonId == -1
        }

        if (incompleto != null) {
            Toast.makeText(
                requireContext(),
                "Selecciona Realizado o No aplica para ${incompleto.second}",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        if (binding.rbPesoTallaRealizado.isChecked) {
            val peso = leerNumero(
                binding.etPesoPeriodico.text.toString()
            )

            val talla = leerNumero(
                binding.etTallaPeriodica.text.toString()
            )

            if (peso == null || peso <= 0) {
                binding.etPesoPeriodico.error =
                    "Ingresa un peso numérico mayor que cero"
                binding.etPesoPeriodico.requestFocus()
                return false
            }

            if (talla == null || talla <= 0) {
                binding.etTallaPeriodica.error =
                    "Ingresa una talla numérica mayor que cero"
                binding.etTallaPeriodica.requestFocus()
                return false
            }
        }

        if (binding.rbGlicemiaRealizado.isChecked) {
            val glicemia = leerNumero(
                binding.etGlicemiaPeriodica.text.toString()
            )

            if (glicemia == null || glicemia <= 0) {
                binding.etGlicemiaPeriodica.error =
                    "Ingresa una glicemia numérica mayor que cero"
                binding.etGlicemiaPeriodica.requestFocus()
                return false
            }
        }

        return true
    }

    private fun crearJsonControles(): String {
        val json = JSONObject()

        val pesoRealizado = binding.rbPesoTallaRealizado.isChecked

        json.put(
            "peso_talla_estado",
            if (pesoRealizado) "Realizado" else "No aplica"
        )

        json.put(
            "peso_kg",
            if (pesoRealizado) {
                leerNumero(binding.etPesoPeriodico.text.toString())
            } else {
                ""
            }
        )

        json.put(
            "talla_cm",
            if (pesoRealizado) {
                leerNumero(binding.etTallaPeriodica.text.toString())
            } else {
                ""
            }
        )

        val glicemiaRealizada = binding.rbGlicemiaRealizado.isChecked

        json.put(
            "glicemia_estado",
            if (glicemiaRealizada) "Realizado" else "No aplica"
        )

        json.put(
            "glicemia_mg_dl",
            if (glicemiaRealizada) {
                leerNumero(binding.etGlicemiaPeriodica.text.toString())
            } else {
                ""
            }
        )

        json.put(
            "terapias_fisicas",
            if (binding.rbTerapiasFisicasRealizado.isChecked) {
                "Realizado"
            } else {
                "No aplica"
            }
        )

        json.put(
            "terapia_respiratoria",
            if (binding.rbTerapiaRespiratoriaRealizado.isChecked) {
                "Realizado"
            } else {
                "No aplica"
            }
        )

        json.put(
            "corte_unas",
            if (binding.rbCorteUnasRealizado.isChecked) {
                "Realizado"
            } else {
                "No aplica"
            }
        )

        json.put(
            "corte_cabello",
            if (binding.rbCorteCabelloRealizado.isChecked) {
                "Realizado"
            } else {
                "No aplica"
            }
        )

        return json.toString()
    }

    private fun guardarBitacora() {
        val idUsuario = obtenerIdUsuario()
        val pacienteId = idPaciente

        if (idUsuario == -1) {
            Toast.makeText(
                requireContext(),
                "No se encontró el usuario de la sesión",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (pacienteId == null) {
            Toast.makeText(
                requireContext(),
                "No hay paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val tipoRegistro = binding.etTipoRegistro.text.toString().trim()
        val descripcion = binding.etDescripcion.text.toString().trim()

        if (tipoRegistro.isEmpty()) {
            binding.etTipoRegistro.error = "Ingresa el tipo de registro"
            binding.etTipoRegistro.requestFocus()
            return
        }

        if (descripcion.isEmpty()) {
            binding.etDescripcion.error = "Ingresa una descripción"
            binding.etDescripcion.requestFocus()
            return
        }

        if (!validarControlesPeriodicos()) {
            return
        }

        val fechaHora = obtenerFechaHoraActual()

        val bitacora = Bitacora(
            estado = true,
            tipoRegistro = tipoRegistro,
            descripcion = descripcion,
            fechaHora = fechaHora,
            idUsuario = idUsuario,
            idPaciente = pacienteId!!
        )

        binding.btnGuardar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val respuesta = RetrofitClient.apiService.crearBitacora(bitacora)

                idBitacora = respuesta.idBitacora

                val idActual = idBitacora
                    ?: throw IllegalStateException(
                        "El servidor no devolvió el ID de la bitácora"
                    )

                pacienteViewModel.guardarIdBitacora(idActual)

                if (binding.switchSignosVitales.isChecked) {
                    val signos = SignosVitales(
                        idBitacora = idActual,
                        temperatura = binding.etTemperatura.text.toString().trim(),
                        presionSistolica = binding.etPresionSistolica.text.toString().trim(),
                        presionDiastolica = binding.etPresionDiastolica.text.toString().trim(),
                        frecuenciaCardiaca = binding.etFrecuenciaCardiaca.text.toString().trim(),
                        frecuenciaRespiratoria = binding.etFrecuenciaRespiratoria.text.toString().trim(),
                        saturacionOxigeno = binding.etSaturacion.text.toString().trim(),
                        peso = binding.etPeso.text.toString().trim(),
                        fechaHora = fechaHora,
                        observaciones = binding.etObservacionesVitales.text.toString().trim()
                    )

                    RetrofitClient.apiService.crearSignosVitales(signos)
                }

                if (binding.switchControlesPeriodicos.isChecked) {
                    val controles = CuidadoEnfermeria(
                        administracionMedicamentos = crearJsonControles(),
                        idPaciente = pacienteId!!
                    )

                    RetrofitClient.apiService.guardarCuidadoEnfermeria(controles)
                }

                Toast.makeText(
                    requireContext(),
                    "Bitácora guardada correctamente",
                    Toast.LENGTH_LONG
                ).show()

                limpiarFormulario()

            } catch (e: Exception) {
                Log.e("BITACORA", "Error guardando la bitácora", e)

                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        "Ocurrió un error al guardar. Revisa Logcat.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                _binding?.btnGuardar?.isEnabled = true
            }
        }
    }

    private fun limpiarFormulario() {
        binding.etTipoRegistro.setText("")
        binding.etDescripcion.setText("")

        binding.switchSignosVitales.isChecked = false
        binding.switchControlesPeriodicos.isChecked = false

        binding.rgEstadoPesoTalla.clearCheck()
        binding.rgEstadoGlicemia.clearCheck()
        binding.rgTerapiasFisicas.clearCheck()
        binding.rgTerapiaRespiratoria.clearCheck()
        binding.rgCorteUnas.clearCheck()
        binding.rgCorteCabello.clearCheck()

        binding.etPesoPeriodico.setText("")
        binding.etTallaPeriodica.setText("")
        binding.etGlicemiaPeriodica.setText("")

        binding.etTemperatura.setText("")
        binding.etPresionSistolica.setText("")
        binding.etPresionDiastolica.setText("")
        binding.etFrecuenciaCardiaca.setText("")
        binding.etFrecuenciaRespiratoria.setText("")
        binding.etSaturacion.setText("")
        binding.etPeso.setText("")
        binding.etObservacionesVitales.setText("")
    }

    override fun onDestroyView() {
        callbackVolverEvento = null
        super.onDestroyView()
        _binding = null
    }
}