package com.example.molvigeryapp.ui.cuidador.bitacora

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.SignosVitales
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentBitacoraBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModelFactory
import kotlinx.coroutines.launch
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

        observarPaciente()
        configurarBotones()
        configurarSignosVitales()
        configurarCuidadosDiarios()
    }

    // ==========================================
    // SWITCH SIGNOS VITALES
    // ==========================================

    private fun configurarSignosVitales() {

        binding.switchSignosVitales.setOnCheckedChangeListener { _, activado ->

            if (activado) {

                binding.layoutSignosVitales.visibility = View.VISIBLE

            } else {

                binding.layoutSignosVitales.visibility = View.GONE

            }
        }
    }

    // ==========================================
    // SWITCH CUIDADOS DIARIOS
    // ==========================================

    private fun configurarCuidadosDiarios() {

        binding.switchCuidadosDiarios.setOnCheckedChangeListener { _, activado ->

            if (activado) {

                binding.layoutCuidadosDiarios.visibility = View.VISIBLE

            } else {

                binding.layoutCuidadosDiarios.visibility = View.GONE

            }
        }
    }

    // ==========================================
    // OBSERVAR PACIENTE
    // ==========================================

    private fun observarPaciente() {

        pacienteViewModel.pacienteSeleccionado.observe(
            viewLifecycleOwner
        ) { paciente ->

            paciente?.let {

                idPaciente = it.idPaciente

                android.util.Log.d(
                    "BITACORA",
                    "Paciente seleccionado: ${it.nombre} ${it.apellido}"
                )

                android.util.Log.d(
                    "BITACORA",
                    "ID Paciente: $idPaciente"
                )
            }
        }
    }

    // ==========================================
    // BOTONES
    // ==========================================

    private fun configurarBotones() {

        binding.btnGuardar.setOnClickListener {
            guardarBitacora()
        }

        binding.btnEventoAdverso.setOnClickListener {
            abrirEventoAdverso()
        }
    }

    // ==========================================
    // ABRIR EVENTO ADVERSO
    // ==========================================

    private fun abrirEventoAdverso() {

        val idBitacoraActual = pacienteViewModel.idBitacora.value

        if (idBitacoraActual == null) {

            Toast.makeText(
                requireContext(),
                "Primero debes guardar la bitácora",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(
                com.example.molvigeryapp.R.id.fragmentContainer,
                EventosAdversosFragment()
            )
            .addToBackStack(null)
            .commit()
    }

    // ==========================================
    // OBTENER ID USUARIO
    // ==========================================

    private fun obtenerIdUsuario(): Int {

        val preferences = requireActivity().getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )

        return preferences.getInt(
            "ID_USUARIO",
            -1
        )
    }

    // ==========================================
    // FECHA Y HORA
    // ==========================================

    private fun obtenerFechaHoraActual(): String {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            Locale.getDefault()
        )

        return formato.format(Date())
    }

    // ==========================================
    // GUARDAR BITÁCORA
    // ==========================================

    private fun guardarBitacora() {

        // 1. Obtener usuario de la sesión

        val idUsuario = obtenerIdUsuario()

        if (idUsuario == -1) {

            Toast.makeText(
                requireContext(),
                "No se encontró el usuario de la sesión",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // 2. Obtener paciente seleccionado

        val pacienteId = idPaciente

        if (pacienteId == null) {

            Toast.makeText(
                requireContext(),
                "No hay paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // 3. Obtener información del formulario

        val tipoRegistro =
            binding.etTipoRegistro.text.toString().trim()

        val descripcion =
            binding.etDescripcion.text.toString().trim()

        // 4. Validar tipo de registro

        if (tipoRegistro.isEmpty()) {

            binding.etTipoRegistro.error =
                "Ingrese el tipo de registro"

            return
        }

        // 5. Validar descripción

        if (descripcion.isEmpty()) {

            binding.etDescripcion.error =
                "Ingrese una descripción"

            return
        }

        // 6. Obtener fecha y hora

        val fechaHora = obtenerFechaHoraActual()

        // 7. Crear objeto Bitácora

        val bitacora = Bitacora(

            estado = true,

            tipoRegistro = tipoRegistro,

            descripcion = descripcion,

            fechaHora = fechaHora,

            idUsuario = idUsuario,

            idPaciente = pacienteId
        )

        // 8. Enviar Bitácora a la API

        lifecycleScope.launch {

            try {

                val respuesta =
                    RetrofitClient.apiService.crearBitacora(bitacora)

                // 9. Guardamos el ID de la bitácora

                idBitacora = respuesta.idBitacora

                idBitacora?.let { idBitacoraActual ->

                    pacienteViewModel.guardarIdBitacora(
                        idBitacoraActual
                    )


                    // ==========================================
                    // SIGNOS VITALES
                    // ==========================================

                    if (binding.switchSignosVitales.isChecked) {

                        val signosVitales = SignosVitales(

                            idBitacora = idBitacoraActual,

                            temperatura =
                                binding.etTemperatura.text
                                    .toString()
                                    .trim(),

                            presionSistolica =
                                binding.etPresionSistolica.text
                                    .toString()
                                    .trim(),

                            presionDiastolica =
                                binding.etPresionDiastolica.text
                                    .toString()
                                    .trim(),

                            frecuenciaCardiaca =
                                binding.etFrecuenciaCardiaca.text
                                    .toString()
                                    .trim(),

                            frecuenciaRespiratoria =
                                binding.etFrecuenciaRespiratoria.text
                                    .toString()
                                    .trim(),

                            saturacionOxigeno =
                                binding.etSaturacion.text
                                    .toString()
                                    .trim(),

                            peso =
                                binding.etPeso.text
                                    .toString()
                                    .trim(),

                            fechaHora = fechaHora,

                            observaciones =
                                binding.etObservacionesVitales.text
                                    .toString()
                                    .trim()
                        )

                        val respuestaSignos =
                            RetrofitClient.apiService
                                .crearSignosVitales(signosVitales)

                        android.util.Log.d(
                            "SIGNOS_VITALES",
                            "Signos vitales creados: ${respuestaSignos.idSignosVitales}"
                        )
                    }


                    // ==========================================
                    // CUIDADOS DE ENFERMERÍA
                    // ==========================================

                    if (binding.switchCuidadosDiarios.isChecked) {

                        // Baño del paciente

                        val banoPaciente = when {

                            binding.rbBanoCama.isChecked ->
                                "En cama"

                            binding.rbBanoSilla.isChecked ->
                                "En silla"

                            binding.rbBanoDucha.isChecked ->
                                "En ducha"

                            else ->
                                ""
                        }


                        // Peso y talla

                        val pesoDiario =
                            binding.etPesoDiario.text
                                .toString()
                                .trim()

                        val talla =
                            binding.etTalla.text
                                .toString()
                                .trim()

                        val pesoTalla =
                            "$pesoDiario kg - $talla cm"


                        // Control de glucemia

                        val glucemia =
                            binding.etGlicemia.text
                                .toString()
                                .trim()


                        // Curaciones

                        val curaciones =
                            if (binding.switchCuraciones.isChecked) {
                                "Sí"
                            } else {
                                "No"
                            }


                        // Líquidos

                        val liquidosAdministrados =
                            binding.etLiquidosAdministrados.text
                                .toString()
                                .trim()

                        val liquidosEliminados =
                            binding.etLiquidosEliminados.text
                                .toString()
                                .trim()

                        val liquidos =
                            "$liquidosAdministrados ml administrados - " +
                                    "$liquidosEliminados ml eliminados"


                        // Control de deposición

                        val controlDeposicion = when {

                            binding.rbDeposicionManana.isChecked ->
                                "Mañana"

                            binding.rbDeposicionTarde.isChecked ->
                                "Tarde"

                            binding.rbDeposicionNoche.isChecked ->
                                "Noche"

                            else ->
                                ""
                        }


                        // Crear objeto de cuidados

                        val cuidados = CuidadoEnfermeria(

                            banoPaciente = banoPaciente,

                            pesoTalla = pesoTalla,

                            controlGlucemia = glucemia,

                            curaciones = curaciones,

                            liquidosAdministradosEliminados =
                                liquidos,

                            controlDeposicion =
                                controlDeposicion,

                            idPaciente = pacienteId
                        )


                        // Enviar cuidados a la API

                        val respuestaCuidados =
                            RetrofitClient.apiService
                                .guardarCuidadoEnfermeria(cuidados)

                        android.util.Log.d(
                            "CUIDADOS_ENFERMERIA",
                            "Cuidados creados: ${respuestaCuidados.idCuidado}"
                        )
                    }
                }


                // ==========================================
                // LOGS
                // ==========================================

                android.util.Log.d(
                    "BITACORA",
                    "Bitácora creada correctamente"
                )

                android.util.Log.d(
                    "BITACORA",
                    "ID Bitácora: $idBitacora"
                )

                android.util.Log.d(
                    "BITACORA",
                    "ID Usuario: $idUsuario"
                )

                android.util.Log.d(
                    "BITACORA",
                    "ID Paciente: $pacienteId"
                )


                // ==========================================
                // MENSAJE
                // ==========================================

                Toast.makeText(
                    requireContext(),
                    "Bitácora creada correctamente\nID: $idBitacora",
                    Toast.LENGTH_LONG
                ).show()


                limpiarFormulario()

            } catch (e: Exception) {

                android.util.Log.e(
                    "BITACORA",
                    "Error al crear la bitácora",
                    e
                )

                Toast.makeText(
                    requireContext(),
                    "Error al crear la bitácora",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // ==========================================
    // LIMPIAR FORMULARIO
    // ==========================================

    private fun limpiarFormulario() {

        binding.etTipoRegistro.setText("")
        binding.etDescripcion.setText("")

    }

    // ==========================================
    // DESTRUIR BINDING
    // ==========================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}