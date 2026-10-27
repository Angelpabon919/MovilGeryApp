package com.example.molvigeryapp.ui.cuidador.cierreturno

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.CierreTurnoRequest
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.databinding.FragmentCierreTurnoBinding
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale

class CierreTurnoFragment : Fragment() {

    private var _binding: FragmentCierreTurnoBinding? = null
    private val binding get() = _binding!!

    private var asignacionActual: AsignacionTurnoUsuario? = null
    private var turnoActual: Turno? = null
    private var enviandoCierre = false

    private val usuarioRepository by lazy {
        PacienteRepository()
    }

    private val preferenciasSesion by lazy {
        requireContext().getSharedPreferences(
            "SESION",
            Context.MODE_PRIVATE
        )
    }

    companion object {
        const val ARG_ID_ASIGNACION = "id_asignacion_turno_usuario"

        fun newInstance(idAsignacion: Int) =
            CierreTurnoFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ID_ASIGNACION, idAsignacion)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCierreTurnoBinding.inflate(
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

        binding.btnVolverCierre.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Recibir las observaciones de la pantalla final.
        parentFragmentManager.setFragmentResultListener(
            ObservacionesFinalesCierreTurnoFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->

            val idAsignacionResultado = bundle.getInt(
                ObservacionesFinalesCierreTurnoFragment.KEY_ID_ASIGNACION,
                -1
            )

            val observaciones = bundle.getString(
                ObservacionesFinalesCierreTurnoFragment.KEY_OBSERVACIONES
            ).orEmpty()

            val asignacion = asignacionActual

            if (
                asignacion == null ||
                asignacion.id_asignacion_turno_usuario != idAsignacionResultado
            ) {
                Toast.makeText(
                    requireContext(),
                    "No se pudo identificar el turno que deseas cerrar.",
                    Toast.LENGTH_LONG
                ).show()
                return@setFragmentResultListener
            }

            enviarCierre(asignacion, observaciones)
        }

        // Eventos adversos.
        binding.cardEventosCierre.setOnClickListener {

            val idAsignacion =
                asignacionActual?.id_asignacion_turno_usuario

            if (idAsignacion == null || idAsignacion <= 0) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró la asignación del turno.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val contenedor = requireView().parent as? ViewGroup

            if (contenedor == null || contenedor.id == View.NO_ID) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el contenedor de navegación.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    contenedor.id,
                    EventosAdversosCierreTurnoFragment.newInstance(
                        idAsignacion
                    )
                )
                .addToBackStack("eventos_adversos_cierre")
                .commit()
        }

        // Continuar a observaciones finales.
        binding.btnContinuarCierre.setOnClickListener {

            val idAsignacion =
                asignacionActual?.id_asignacion_turno_usuario

            if (idAsignacion == null || idAsignacion <= 0) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró la asignación del turno.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val contenedor = requireView().parent as? ViewGroup

            if (contenedor == null || contenedor.id == View.NO_ID) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el contenedor de navegación.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    contenedor.id,
                    ObservacionesFinalesCierreTurnoFragment.newInstance(
                        idAsignacion
                    )
                )
                .addToBackStack("observaciones_finales_cierre")
                .commit()
        }

        // Lista de pacientes.
        binding.cardPacientesCierre.setOnClickListener {

            val contenedor = requireView().parent as? ViewGroup

            if (contenedor == null || contenedor.id == View.NO_ID) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el contenedor de navegación.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .replace(
                    contenedor.id,
                    PacientesCierreTurnoFragment()
                )
                .addToBackStack("pacientes_cierre_turno")
                .commit()
        }

        // Lista de citas.
        binding.cardCitasCierre.setOnClickListener {

            val idAsignacion =
                asignacionActual?.id_asignacion_turno_usuario

            if (idAsignacion == null || idAsignacion <= 0) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró la asignación del turno.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val contenedor = requireView().parent as? ViewGroup

            if (contenedor == null || contenedor.id == View.NO_ID) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el contenedor de navegación.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            parentFragmentManager.beginTransaction()
                .replace(
                    contenedor.id,
                    CitasCierreTurnoFragment.newInstance(idAsignacion)
                )
                .addToBackStack("citas_cierre_turno")
                .commit()
        }

        configurarResumenInicial()
        configurarFotoPredeterminada()
        cargarDatosTurno()
    }

    // =========================================================
    // FOTO PREDETERMINADA
    // =========================================================
    private fun configurarFotoPredeterminada() {
        binding.tvNombreCuidadorCierre.text = "Cargando cuidador..."

        binding.ivFotoCuidadorCierre.setImageResource(
            R.drawable.person_icon
        )
    }

    // =========================================================
    // CARGAR NOMBRE Y FOTO DEL CUIDADOR
    // Reutiliza el repositorio que usa PerfilCuidadorFragment.
    // =========================================================
    private fun cargarPerfilCuidador(idUsuario: Int) {

        if (idUsuario <= 0) {
            binding.tvNombreCuidadorCierre.text = "Cuidador"
            binding.ivFotoCuidadorCierre.setImageResource(
                R.drawable.person_icon
            )
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val usuario =
                    usuarioRepository.obtenerUsuarioPorId(idUsuario)

                if (!isAdded || _binding == null) return@launch

                if (usuario == null) {
                    binding.tvNombreCuidadorCierre.text = "Cuidador"
                    binding.ivFotoCuidadorCierre.setImageResource(
                        R.drawable.person_icon
                    )
                    return@launch
                }

                val nombreCompleto =
                    "${usuario.nombres} ${usuario.apellidos}".trim()

                binding.tvNombreCuidadorCierre.text =
                    nombreCompleto.ifBlank { "Cuidador" }

                if (!usuario.foto.isNullOrBlank()) {

                    Glide.with(this@CierreTurnoFragment)
                        .load(usuario.foto)
                        .placeholder(R.drawable.person_icon)
                        .error(R.drawable.person_icon)
                        .centerCrop()
                        .into(binding.ivFotoCuidadorCierre)

                } else {

                    Glide.with(this@CierreTurnoFragment)
                        .clear(binding.ivFotoCuidadorCierre)

                    binding.ivFotoCuidadorCierre.setImageResource(
                        R.drawable.person_icon
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    "CIERRE_TURNO",
                    "Error al cargar el perfil del cuidador $idUsuario",
                    e
                )

                if (!isAdded || _binding == null) return@launch

                binding.tvNombreCuidadorCierre.text = "Cuidador"

                Glide.with(this@CierreTurnoFragment)
                    .clear(binding.ivFotoCuidadorCierre)

                binding.ivFotoCuidadorCierre.setImageResource(
                    R.drawable.person_icon
                )
            }
        }
    }

    // =========================================================
    // RESUMEN INICIAL
    // =========================================================
    private fun configurarResumenInicial() {
        binding.tvCantidadPacientesCierre.text =
            "Resumen de pacientes pendiente de conectar"

        binding.tvResumenCuidadosCierre.text =
            "Resumen de cuidados pendiente de conectar"

        binding.tvResumenCitasCierre.text =
            "Cargando resumen de citas..."

        binding.tvResumenEventosCierre.text =
            "Resumen de eventos pendiente de conectar"
    }

    // =========================================================
    // CARGAR DATOS DEL TURNO
    // =========================================================
    private fun cargarDatosTurno() {
        val idAsignacion =
            arguments?.getInt(ARG_ID_ASIGNACION, -1) ?: -1

        if (idAsignacion <= 0) {
            Toast.makeText(
                requireContext(),
                "No se recibió la asignación del turno.",
                Toast.LENGTH_LONG
            ).show()

            binding.btnContinuarCierre.isEnabled = false
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClient.apiService

                val asignaciones = api.getAsignacionesTurno()

                val asignacion = asignaciones.firstOrNull {
                    it.id_asignacion_turno_usuario == idAsignacion
                }

                if (asignacion == null) {
                    Toast.makeText(
                        requireContext(),
                        "No se encontró la asignación del turno.",
                        Toast.LENGTH_LONG
                    ).show()

                    binding.btnContinuarCierre.isEnabled = false
                    return@launch
                }

                asignacionActual = asignacion

                // Obtener el usuario real asignado al turno.
                cargarPerfilCuidador(asignacion.id_usuario)

                val turnos = api.getTurnos()

                val turno = turnos.firstOrNull {
                    it.id_turno == asignacion.id_turno
                }

                turnoActual = turno

                binding.tvEstadoTurnoCierre.text =
                    asignacion.estado

                binding.tvFechaTurnoCierre.text =
                    formatearFecha(asignacion.fecha)

                binding.tvHorarioTurnoCierre.text =
                    "${turno?.hora_inicio ?: "Sin hora de inicio"} - " +
                            "${turno?.hora_fin ?: "Sin hora de fin"}"

                binding.tvDuracionTurnoCierre.text =
                    calcularDuracion(
                        turno?.hora_inicio,
                        turno?.hora_fin
                    )

                cargarResumenCitas()

                // Evitar intentar cerrar un turno ya finalizado.
                if (asignacion.estado.equals("Finalizado", true)) {
                    binding.btnContinuarCierre.isEnabled = false
                }

            } catch (e: Exception) {
                Log.e(
                    "CIERRE_TURNO",
                    "Error cargando datos del turno",
                    e
                )

                if (isAdded && _binding != null) {
                    Toast.makeText(
                        requireContext(),
                        "No se pudieron cargar los datos del turno.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // =========================================================
    // RESUMEN DE CITAS
    // =========================================================
    private suspend fun cargarResumenCitas() {
        try {
            val api = RetrofitClient.apiService
            val asignacionTurno = asignacionActual

            if (asignacionTurno == null) {
                binding.tvResumenCitasCierre.text =
                    "No se encontró el turno actual"
                return
            }

            val idCuidador = asignacionTurno.id_usuario
            val fechaTurno = asignacionTurno.fecha?.take(10)

            if (idCuidador <= 0 || fechaTurno.isNullOrBlank()) {
                binding.tvResumenCitasCierre.text =
                    "No se pudo identificar la fecha del turno"
                return
            }

            // Consultar las asignaciones de pacientes.
            val respuestaAsignaciones =
                api.getAsignacionesPacienteCuidador()

            if (!respuestaAsignaciones.isSuccessful) {
                binding.tvResumenCitasCierre.text =
                    "No se pudieron consultar los pacientes"
                return
            }

            // Pacientes activos del cuidador durante la fecha del turno.
            val pacientesAsignados =
                respuestaAsignaciones.body()
                    .orEmpty()
                    .filter { asignacion ->

                        val inicio = asignacion.fechaInicio.take(10)
                        val fin = asignacion.fechaFin?.take(10)

                        asignacion.idUsuario == idCuidador &&
                                asignacion.estado.equals(
                                    "Activo",
                                    ignoreCase = true
                                ) &&
                                inicio <= fechaTurno &&
                                (fin.isNullOrBlank() || fin >= fechaTurno)
                    }
                    .map { it.idPaciente }
                    .toSet()

            if (pacientesAsignados.isEmpty()) {
                binding.tvResumenCitasCierre.text =
                    "No hay pacientes asignados para este turno"
                return
            }

            // Consultar citas.
            val respuestaCitas = api.getCitas()

            if (!respuestaCitas.isSuccessful) {
                binding.tvResumenCitasCierre.text =
                    "No se pudieron consultar las citas"
                return
            }

            // Filtrar por pacientes y fecha.
            val citasDelTurno =
                respuestaCitas.body()
                    .orEmpty()
                    .filter { cita ->
                        cita.idPaciente in pacientesAsignados &&
                                cita.fecha.take(10) == fechaTurno
                    }

            val finalizadas = citasDelTurno.count {
                it.estado.equals(
                    "Finalizada",
                    ignoreCase = true
                )
            }

            val programadas = citasDelTurno.count {
                it.estado.equals(
                    "Programada",
                    ignoreCase = true
                )
            }

            val totalContabilizado = finalizadas + programadas

            binding.tvResumenCitasCierre.text =
                "$totalContabilizado citas · " +
                        "$finalizadas finalizadas · " +
                        "$programadas programadas"

        } catch (e: Exception) {
            Log.e(
                "CIERRE_TURNO",
                "Error cargando resumen de citas",
                e
            )

            if (isAdded && _binding != null) {
                binding.tvResumenCitasCierre.text =
                    "No se pudo cargar el resumen de citas"
            }
        }
    }

    // =========================================================
    // CONFIRMAR FINALIZACIÓN DESDE EL BOTÓN EXISTENTE
    // =========================================================
    private fun confirmarFinalizacion() {
        val asignacion = asignacionActual

        if (asignacion == null) {
            Toast.makeText(
                requireContext(),
                "No hay una asignación cargada para cerrar.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (asignacion.estado.equals("Finalizado", true)) {
            Toast.makeText(
                requireContext(),
                "Este turno ya está finalizado.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Finalizar turno")
            .setMessage(
                "¿Confirmas que revisaste la información y deseas " +
                        "finalizar este turno?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Confirmar") { _, _ ->
                enviarCierre(asignacion)
            }
            .show()
    }

    // =========================================================
    // ENVIAR CIERRE AL BACKEND
    // =========================================================
    private fun enviarCierre(
        asignacion: AsignacionTurnoUsuario,
        observaciones: String = ""
    ) {
        if (enviandoCierre) return

        if (asignacion.estado.equals("Finalizado", true)) {
            Toast.makeText(
                requireContext(),
                "Este turno ya está finalizado.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        enviandoCierre = true
        binding.btnContinuarCierre.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val idAsignacion =
                    asignacion.id_asignacion_turno_usuario
                        ?: throw IllegalStateException(
                            "La asignación no tiene un ID válido."
                        )

                val solicitud = CierreTurnoRequest(
                    idAsignacionTurnoUsuario = idAsignacion,
                    observaciones = observaciones
                )

                val respuesta =
                    RetrofitClient.apiService.crearCierreTurno(solicitud)

                if (respuesta.estado.equals("Finalizado", true)) {
                    Toast.makeText(
                        requireContext(),
                        "Turno finalizado correctamente.",
                        Toast.LENGTH_LONG
                    ).show()

                    parentFragmentManager.popBackStack()

                } else {
                    binding.btnContinuarCierre.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "El servidor no confirmó el cierre.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: HttpException) {
                Log.e(
                    "CIERRE_TURNO",
                    "Error HTTP al finalizar turno",
                    e
                )

                binding.btnContinuarCierre.isEnabled = true

                val mensaje = when (e.code()) {
                    409 -> "Este turno ya tiene un cierre registrado."
                    400 -> "El servidor rechazó los datos del cierre."
                    else -> "Error del servidor (${e.code()})."
                }

                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Log.e(
                    "CIERRE_TURNO",
                    "Error al finalizar turno",
                    e
                )

                if (isAdded && _binding != null) {
                    binding.btnContinuarCierre.isEnabled = true

                    Toast.makeText(
                        requireContext(),
                        "No se pudo confirmar el cierre. Revisa la conexión.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } finally {
                enviandoCierre = false
            }
        }
    }

    // =========================================================
    // FORMATO DE FECHA
    // =========================================================
    private fun formatearFecha(fecha: String?): String {
        if (fecha.isNullOrBlank()) {
            return "Fecha no disponible"
        }

        return try {
            val entrada = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

            val salida = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            salida.format(entrada.parse(fecha.take(10))!!)
        } catch (_: Exception) {
            fecha
        }
    }

    // =========================================================
    // CALCULAR DURACIÓN DEL TURNO
    // =========================================================
    private fun calcularDuracion(
        inicio: String?,
        fin: String?
    ): String {
        if (inicio.isNullOrBlank() || fin.isNullOrBlank()) {
            return "Duración no disponible"
        }

        return try {
            val formato = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            )

            val horaInicio =
                formato.parse(normalizarHora(inicio))!!

            val horaFin =
                formato.parse(normalizarHora(fin))!!

            var minutos =
                (horaFin.time - horaInicio.time) / 60000

            if (minutos < 0) {
                minutos += 24 * 60
            }

            "${minutos / 60} h ${minutos % 60} min"

        } catch (_: Exception) {
            "Duración no disponible"
        }
    }

    private fun normalizarHora(hora: String): String {
        return if (hora.length == 5) {
            "$hora:00"
        } else {
            hora
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding?.let {
            Glide.with(this).clear(it.ivFotoCuidadorCierre)
        }

        _binding = null
    }
}