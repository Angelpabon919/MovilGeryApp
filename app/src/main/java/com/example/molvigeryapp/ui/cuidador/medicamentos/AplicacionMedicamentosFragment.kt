package com.example.molvigeryapp.ui.cuidador.medicamentos

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.Inventario
import com.example.molvigeryapp.data.model.SeguimientoAdministracionMedicamento
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class AplicacionMedicamentosFragment :
    Fragment(R.layout.fragment_aplicacion_medicamentos) {

    private val viewModel: PacienteViewModel by activityViewModels()

    private lateinit var spinnerMedicamento: AutoCompleteTextView
    private lateinit var tvPacienteSeleccionado: TextView
    private lateinit var etCantidadAplicada: TextInputEditText
    private lateinit var etObservaciones: TextInputEditText
    private lateinit var cbConfirmarAdministracion: MaterialCheckBox
    private lateinit var btnVerInformacion: MaterialButton
    private lateinit var btnRegistrar: MaterialButton
    private lateinit var btnNoAdministrado: MaterialButton
    private lateinit var layoutEstadoSeguimiento: LinearLayout
    private lateinit var tvEstadoSeguimiento: TextView
    private lateinit var tvMotivoNoAdministracion: TextView

    private var formulacionSeleccionada: FormulacionMedicamento? = null
    private var elementoSeleccionado: ElementoPaciente? = null

    private var medicamentosPendientes:
            List<FormulacionMedicamento> = emptyList()

    private var elementosPaciente:
            List<ElementoPaciente> = emptyList()

    private var inventariosPaciente:
            List<Inventario> = emptyList()

    private var inventarioSeleccionado:
            Inventario? = null
    private var seguimientosPaciente:
            List<SeguimientoAdministracionMedicamento> = emptyList()

    private var seguimientoSeleccionado:
            SeguimientoAdministracionMedicamento? = null

    private var mapaNombresMedicamentos:
            Map<Int, String> = emptyMap()

    private var seleccionInicialAlarmaAplicada = false


    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        inicializarVistas(view)

        val paciente = viewModel.pacienteSeleccionado.value

        if (paciente == null || paciente.idPaciente == null) {

            Toast.makeText(
                requireContext(),
                "No hay paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val idPaciente = paciente.idPaciente!!


        // =====================================================
        // MOSTRAR PACIENTE
        // =====================================================

        tvPacienteSeleccionado.text =
            "${paciente.nombre} ${paciente.apellido}".trim()


        // =====================================================
        // CARGAR INFORMACIÓN NECESARIA
        // =====================================================

        viewModel.cargarCatalogoMedicamentos()
        viewModel.cargarElementosDelPaciente(idPaciente)
        viewModel.cargarFormulacionesMedicamentos()
        viewModel.cargarGruposMedicacion()
        viewModel.cargarAplicacionesMedicamento()
        viewModel.cargarInventariosPaciente(idPaciente)
        viewModel.cargarSeguimientosAdministracionMedicamentos(idPaciente)
        viewModel.seguimientosAdministracionMedicamentos.observe(
            viewLifecycleOwner
        ) { seguimientos ->

            seguimientosPaciente = seguimientos ?: emptyList()

            actualizarSeguimientoSeleccionado()
        }


        // =====================================================
        // CATÁLOGO DE MEDICAMENTOS
        // =====================================================

        viewModel.medicamentosCatalogo.observe(
            viewLifecycleOwner
        ) { catalogo ->

            mapaNombresMedicamentos =
                catalogo?.associateBy(
                    { it.idMedicamento },
                    { it.nombreMedicamento }
                ) ?: emptyMap()

            actualizarSelectorMedicamentos(idPaciente)
        }
        // =====================================================
        // INVENTARIO / ELEMENTOS DEL PACIENTE
        // =====================================================

        viewModel.elementosPaciente.observe(
            viewLifecycleOwner
        ) { elementos ->

            elementosPaciente =
                elementos?.filter {
                    it.idPaciente == idPaciente &&
                            it.idMedicamentos != null
                } ?: emptyList()

            actualizarElementoSeleccionado()
        }
        // =====================================================
// INVENTARIOS DEL PACIENTE
// =====================================================

        viewModel.inventariosPaciente.observe(
            viewLifecycleOwner
        ) { inventarios ->

            inventariosPaciente = inventarios ?: emptyList()

            actualizarElementoSeleccionado()
        }


        // =====================================================
        // MEDICAMENTOS QUE CORRESPONDEN AHORA
        // =====================================================

        viewModel.medicamentosManana.observe(
            viewLifecycleOwner
        ) {
            actualizarSelectorMedicamentos(idPaciente)
        }

        viewModel.medicamentosTarde.observe(
            viewLifecycleOwner
        ) {
            actualizarSelectorMedicamentos(idPaciente)
        }

        viewModel.medicamentosNoche.observe(
            viewLifecycleOwner
        ) {
            actualizarSelectorMedicamentos(idPaciente)
        }
        // =====================================================
        // SELECCIONAR MEDICAMENTO
        // =====================================================

        spinnerMedicamento.setOnItemClickListener { _, _, position, _ ->

            if (position < medicamentosPendientes.size) {

                formulacionSeleccionada =
                    medicamentosPendientes[position]

                actualizarSeguimientoSeleccionado()

                actualizarElementoSeleccionado()

            }
        }


        // =====================================================
        // VER INFORMACIÓN
        // =====================================================

        btnVerInformacion.setOnClickListener {

            mostrarInformacionMedicamento()
        }
        viewModel.registroAplicacionState.observe(
            viewLifecycleOwner
        ) { result ->

            result?.onSuccess { mensaje ->

                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()

                etCantidadAplicada.text?.clear()
                etObservaciones.text?.clear()
                cbConfirmarAdministracion.isChecked = false
                viewModel.cargarSeguimientosAdministracionMedicamentos(
                    idPaciente
                )

            }?.onFailure { error ->

                Toast.makeText(
                    requireContext(),
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        viewModel.noAdministradoState.observe(
            viewLifecycleOwner
        ) { result ->

            result?.onSuccess { mensaje ->

                Log.i(
                    "NO_ADMIN_MED",
                    "REGISTRO EXITOSO: $mensaje"
                )

                Toast.makeText(
                    requireContext(),
                    mensaje,
                    Toast.LENGTH_LONG
                ).show()

            }?.onFailure { error ->
                Log.e(
                    "NO_ADMIN_MED",
                    "ERROR AL REGISTRAR",
                    error
                )

                Toast.makeText(
                    requireContext(),
                    "Error: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        // =====================================================
        // REGISTRAR
        // =====================================================

        btnRegistrar.setOnClickListener {

            validarRegistro()
        }
        // =====================================================
// REGISTRAR MEDICAMENTO NO ADMINISTRADO
// =====================================================

        btnNoAdministrado.setOnClickListener {

            val formulacion = formulacionSeleccionada

            if (formulacion == null) {
                Toast.makeText(
                    requireContext(),
                    "Seleccione un medicamento",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val idFormulacion = formulacion.idFormulacion
            val horaAdministrada = formulacion.horaAdministrada

            if (idFormulacion == null || idFormulacion <= 0) {
                Toast.makeText(
                    requireContext(),
                    "No se identificó la formulación",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (horaAdministrada.isNullOrBlank()) {
                Toast.makeText(
                    requireContext(),
                    "No se encontró el horario del medicamento",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val seguimiento = seguimientoSeleccionado

            if (
                seguimiento != null &&
                !seguimiento.estado.equals("PENDIENTE", ignoreCase = true)
            ) {
                Toast.makeText(
                    requireContext(),
                    "Solo puedes registrar como no administrada una dosis pendiente",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            val campoMotivo = TextInputEditText(requireContext()).apply {
                hint = "Escribe el motivo de la no administración"
                minLines = 2
                maxLines = 4
            }

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Medicamento no administrado")
                .setMessage("Indica por qué no se administró esta dosis.")
                .setView(campoMotivo)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar") { _, _ ->

                    val motivo = campoMotivo.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                    if (motivo.isBlank()) {
                        Toast.makeText(
                            requireContext(),
                            "Debes indicar el motivo",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.d(
                            "NO_ADMIN_MED",
                            "Enviando solicitud. ID seguimiento=${seguimiento?.idSeguimiento} | Formulación=$idFormulacion"
                        )
                        viewModel.registrarMedicamentoNoAdministrado(
                            idSeguimiento = seguimiento?.idSeguimiento,
                            motivo = motivo,
                            idFormulacion = idFormulacion,
                            horaAdministrada = horaAdministrada
                        )
                    }
                }
                .show()
        }
    }




    // =========================================================
    // INICIALIZAR VISTAS
    // =========================================================

    private fun inicializarVistas(view: View) {

        spinnerMedicamento =
            view.findViewById(R.id.spinnerMedicamento)

        tvPacienteSeleccionado =
            view.findViewById(R.id.tvPacienteSeleccionado)

        etCantidadAplicada =
            view.findViewById(R.id.etCantidadAplicada)

        etObservaciones =
            view.findViewById(R.id.etObservaciones)

        cbConfirmarAdministracion =
            view.findViewById(R.id.cbConfirmarAdministracion)

        btnVerInformacion =
            view.findViewById(R.id.btnVerInformacionMedicamento)

        btnRegistrar =
            view.findViewById(R.id.btnRegistrarAplicacion)

        btnNoAdministrado =
            view.findViewById(R.id.btnNoAdministrado)

        layoutEstadoSeguimiento =
            view.findViewById(R.id.layoutEstadoSeguimiento)

        tvEstadoSeguimiento =
            view.findViewById(R.id.tvEstadoSeguimiento)

        tvMotivoNoAdministracion =
            view.findViewById(R.id.tvMotivoNoAdministracion)
    }


    // =========================================================
    // ACTUALIZAR SELECTOR
    // =========================================================

    private fun actualizarSelectorMedicamentos(
        idPaciente: Int
    ) {

        val manana =
            viewModel.medicamentosManana.value
                ?: emptyList()

        val tarde =
            viewModel.medicamentosTarde.value
                ?: emptyList()

        val noche =
            viewModel.medicamentosNoche.value
                ?: emptyList()


        medicamentosPendientes =
            (manana + tarde + noche)
                .filter {
                    it.idPaciente == idPaciente
                }
                .distinctBy {
                    it.idFormulacion
                }


        val nombres =
            medicamentosPendientes.map { formulacion ->

                mapaNombresMedicamentos[
                    formulacion.idMedicamentos
                ] ?: "Medicamento #${formulacion.idMedicamentos}"
            }


        val adapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                nombres
            )


        spinnerMedicamento.setAdapter(adapter)

        // Seleccionar automáticamente el medicamento de la alarma,
        // solamente una vez.
        val idFormulacionAlarma =
            arguments?.getInt("ID_FORMULACION_ALARMA", -1) ?: -1

        if (idFormulacionAlarma > 0 && !seleccionInicialAlarmaAplicada) {

            val indiceAlarma = medicamentosPendientes.indexOfFirst {
                it.idFormulacion == idFormulacionAlarma
            }

            if (indiceAlarma >= 0) {

                formulacionSeleccionada = medicamentosPendientes[indiceAlarma]

                spinnerMedicamento.setText(
                    nombres[indiceAlarma],
                    false
                )

                seleccionInicialAlarmaAplicada = true

                actualizarSeguimientoSeleccionado()
                actualizarElementoSeleccionado()

                android.util.Log.d(
                    "ALARMA_MEDICAMENTO",
                    "Medicamento seleccionado: formulación=$idFormulacionAlarma"
                )
                return

            }
        }
        // Si solo hay un medicamento,
        // lo seleccionamos automáticamente.
        if (
            idFormulacionAlarma <= 0 &&
            formulacionSeleccionada == null &&
            medicamentosPendientes.size == 1
        ) {
            formulacionSeleccionada = medicamentosPendientes.first()

            spinnerMedicamento.setText(
                nombres.first(),
                false
            )
            actualizarSeguimientoSeleccionado()
            actualizarElementoSeleccionado()
        }
    }
    private fun actualizarSeguimientoSeleccionado() {

        val formulacion = formulacionSeleccionada

        val idFormulacion = formulacion?.idFormulacion

        if (idFormulacion == null || idFormulacion <= 0) {
            seguimientoSeleccionado = null
            actualizarEstadoVisualSeguimiento()
            return
        }

        val horaProgramada = obtenerHoraYMinuto(
            formulacion.horaAdministrada
        )

        if (horaProgramada == null) {
            seguimientoSeleccionado = null
            actualizarEstadoVisualSeguimiento()
            return
        }

        val (hora, minuto) = horaProgramada

        val hoy = Calendar.getInstance()

        val coincidencias = seguimientosPaciente.filter { seguimiento ->

            if (seguimiento.idFormulacion != idFormulacion) {
                return@filter false
            }

            val fechaSeguimiento =
                seguimiento.fechaHoraProgramada
                    ?.let { parsearFechaHoraProgramada(it) }
                    ?: return@filter false

            fechaSeguimiento.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                    fechaSeguimiento.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR) &&
                    fechaSeguimiento.get(Calendar.HOUR_OF_DAY) == hora &&
                    fechaSeguimiento.get(Calendar.MINUTE) == minuto
        }

        seguimientoSeleccionado =
            coincidencias.firstOrNull {
                it.estado.equals("PENDIENTE", ignoreCase = true)
            } ?: coincidencias.firstOrNull()

        actualizarEstadoVisualSeguimiento()

        android.util.Log.d(
            "SEGUIMIENTO_SELECCION",
            "FORMULACION=$idFormulacion | " +
                    "SEGUIMIENTO=${seguimientoSeleccionado?.idSeguimiento} | " +
                    "ESTADO=${seguimientoSeleccionado?.estado}"
        )
    }

    private fun actualizarEstadoVisualSeguimiento() {
        val formulacion = formulacionSeleccionada

        if (formulacion == null) {
            layoutEstadoSeguimiento.visibility = View.GONE
            tvMotivoNoAdministracion.visibility = View.GONE
            btnRegistrar.isEnabled = true
            btnRegistrar.alpha = 1f
            btnNoAdministrado.isEnabled = true
            btnNoAdministrado.alpha = 1f
            return
        }

        layoutEstadoSeguimiento.visibility = View.VISIBLE

        val estado = seguimientoSeleccionado?.estado
            ?.trim()
            ?.uppercase()
            ?: "PENDIENTE"

        when (estado) {
            "ADMINISTRADO" -> {
                tvEstadoSeguimiento.text = "Estado: Administrado"
                tvEstadoSeguimiento.setTextColor(Color.parseColor("#15803D"))
                tvMotivoNoAdministracion.visibility = View.GONE
                btnRegistrar.isEnabled = false
                btnRegistrar.alpha = 0.5f
                btnNoAdministrado.isEnabled = false
                btnNoAdministrado.alpha = 0.5f
            }

            "NO_ADMINISTRADO" -> {
                tvEstadoSeguimiento.text = "Estado: No administrado"
                tvEstadoSeguimiento.setTextColor(Color.parseColor("#B91C1C"))

                val motivo = seguimientoSeleccionado?.motivoNoAdministracion?.trim().orEmpty()
                if (motivo.isNotEmpty()) {
                    tvMotivoNoAdministracion.text = "Motivo: $motivo"
                    tvMotivoNoAdministracion.visibility = View.VISIBLE
                } else {
                    tvMotivoNoAdministracion.visibility = View.GONE
                }

                btnRegistrar.isEnabled = false
                btnRegistrar.alpha = 0.5f
                btnNoAdministrado.isEnabled = false
                btnNoAdministrado.alpha = 0.5f
            }

            else -> {
                tvEstadoSeguimiento.text = "Estado: Pendiente"
                tvEstadoSeguimiento.setTextColor(Color.parseColor("#B45309"))
                tvMotivoNoAdministracion.visibility = View.GONE
                btnRegistrar.isEnabled = true
                btnRegistrar.alpha = 1f
                btnNoAdministrado.isEnabled = true
                btnNoAdministrado.alpha = 1f
            }
        }
    }


    private fun obtenerHoraYMinuto(
        valor: String?
    ): Pair<Int, Int>? {

        val texto = valor
            ?.trim()
            ?.substringAfterLast("T")
            ?.substringBefore("Z")
            ?.substringBefore("+")
            ?.substringBefore("-")
            ?: return null

        val partes = texto.split(":")

        val hora = partes.getOrNull(0)?.toIntOrNull()
            ?: return null

        val minuto = partes.getOrNull(1)
            ?.take(2)
            ?.toIntOrNull()
            ?: return null

        if (hora !in 0..23 || minuto !in 0..59) {
            return null
        }

        return hora to minuto
    }


    private fun parsearFechaHoraProgramada(
        valor: String
    ): Calendar? {

        val normalizada = valor
            .trim()
            .replace(Regex("\\.\\d+"), "")

        val patrones = listOf(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (patron in patrones) {

            try {

                val formato = java.text.SimpleDateFormat(
                    patron,
                    java.util.Locale.US
                ).apply {

                    isLenient = false

                    if (patron.endsWith("'Z'")) {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                }

                val fecha = formato.parse(normalizada)
                    ?: continue

                return Calendar.getInstance().apply {
                    time = fecha
                }

            } catch (_: Exception) {
                continue
            }
        }

        return null
    }




    // =========================================================
    // BUSCAR INVENTARIO DEL MEDICAMENTO
    // =========================================================

    private fun actualizarElementoSeleccionado() {

        val formulacion =
            formulacionSeleccionada
        if (formulacion == null){
            inventarioSeleccionado = null
            elementoSeleccionado = null
            return
        }
        val idPaciente=
            viewModel.pacienteSeleccionado.value?.idPaciente
                ?:return
        val formatoFecha = java.text.SimpleDateFormat(
            "yyyy-MM-dd",
            java.util.Locale.US
        )
        val hoy = formatoFecha.parse(
            formatoFecha.format(java.util.Date())
        )
        // Buscar inventarios del medicamento seleccionado.
        val inventariosDisponibles =
            inventariosPaciente.filter { inventario ->

                val vencimiento = try {
                    inventario.fechaVencimiento
                        ?.take(10)
                        ?.let { formatoFecha.parse(it) }
                } catch (e: Exception) {
                    null
                }

                inventario.idPaciente == idPaciente &&
                        inventario.idMedicamentos ==
                        formulacion.idMedicamentos &&
                        inventario.estado == true &&
                        (inventario.cantidadActual ?: 0) > 0 &&
                        vencimiento != null &&
                        hoy != null &&
                        !vencimiento.before(hoy)
            }

        // Priorizar el lote que vence primero.
        val inventario = inventariosDisponibles.minByOrNull {
            formatoFecha.parse(
                it.fechaVencimiento!!.take(10)
            )?.time ?: Long.MAX_VALUE
        }

        inventarioSeleccionado = inventario

        // Encontrar el elemento exacto asociado al inventario.
        elementoSeleccionado =
            inventario?.idElemento?.let { idElemento ->

                elementosPaciente.find { elemento ->

                    elemento.idElemento == idElemento &&
                            elemento.idPaciente == idPaciente &&
                            elemento.idMedicamentos ==
                            formulacion.idMedicamentos
                }
            }

        android.util.Log.d(
            "INVENTARIO_SELECCION",
            "FORMULACION=${formulacion.idFormulacion} | " +
                    "INVENTARIO=${inventario?.idInventario} | " +
                    "ELEMENTO=${inventario?.idElemento} | " +
                    "STOCK=${inventario?.cantidadActual}"
        )
    }
    // =========================================================
    // INFORMACIÓN DEL MEDICAMENTO
    // =========================================================

    private fun mostrarInformacionMedicamento() {

        val formulacion = formulacionSeleccionada

        if (formulacion == null) {

            Toast.makeText(
                requireContext(),
                "Seleccione un medicamento",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        val paciente =
            viewModel.pacienteSeleccionado.value


        val nombrePaciente =
            "${paciente?.nombre ?: ""} " +
                    "${paciente?.apellido ?: ""}"


        val nombreMedicamento =
            mapaNombresMedicamentos[
                formulacion.idMedicamentos
            ] ?: "Medicamento"


        val stock =
            elementoSeleccionado
                ?.cantidadActual
                ?: elementoSeleccionado
                    ?.cantidad
                ?: 0


        val hora =
            obtenerHoraProgramada(formulacion)


        val mensaje = """
    Paciente: ${nombrePaciente.trim()}

    Medicamento: $nombreMedicamento

    Dosis: ${formulacion.dosis ?: "No registrada"}

    Vía: ${formulacion.via ?: "No registrada"}

    Horario: $hora

    Presentación: ${formulacion.presentacion ?: "No registrada"}

    Stock disponible: $stock unidades
        """.trimIndent()


        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Información del medicamento")
            .setMessage(mensaje)
            .setPositiveButton("Cerrar", null)
            .show()
    }


    // =========================================================
    // OBTENER HORA PROGRAMADA
    // =========================================================

    private fun obtenerHoraProgramada(
        formulacion: FormulacionMedicamento
    ): String {
        val horaOriginal = formulacion.horaAdministrada
            ?.trim()
            ?.takeIf{it.isNotEmpty()}
            ?:return "sin horario"

        val horaTexto = horaOriginal
            .substringAfterLast("T")
            .substringBefore("Z")
            .substringBefore("+")
            .substringBefore("-")

        val partes = horaTexto.split(":")

        val hora = partes.getOrNull(0)?.toIntOrNull()
            ?: return "sin horario"

        val minuto = partes.getOrNull(1)
            ?.take(2)
            ?.toIntOrNull() ?: 0

        if (hora !in 0..23 || minuto !in 0..59) {
            return "sin horario"
        }
        return convertirHora12Horas(hora, minuto)
    }
    private fun convertirHora12Horas(
            hora : Int,
            minuto: Int
        ):String{

        val hora12 = when (hora % 12){
            0 -> 12
            else -> hora % 12
        }
        val periodo = if (hora<12)"a. m." else "p. m."

        return String.format(
            java.util.Locale.getDefault(),
            "%d:%02d %s",
            hora12,
            minuto,
            periodo
        )
    }


    // =========================================================
    // VALIDACIÓN ANTES DE REGISTRAR
    // =========================================================

    private fun validarRegistro() {

        val formulacion = formulacionSeleccionada

        if (formulacion == null) {
            Toast.makeText(
                requireContext(),
                "Seleccione un medicamento",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        // =============================================
        // VALIDAR INVENTARIO SELECCIONADO
        // =============================================

        val inventario = inventarioSeleccionado

        if (inventario == null || inventario.estado != true) {
            Toast.makeText(
                requireContext(),
                "No se encontró un inventario disponible",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val elemento = elementoSeleccionado

        if (elemento == null ||
                elemento.idElemento != inventario.idElemento
        ){
            Toast.makeText(
                requireContext(),
                "No se encontró el elemento correspondiente al inventario",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val cantidadAplicada =
            etCantidadAplicada.text
                ?.toString()
                ?.trim()
                ?.toIntOrNull()

        if (cantidadAplicada == null || cantidadAplicada <= 0) {
            Toast.makeText(
                requireContext(),
                "Ingrese una cantidad válida",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val stockActual = inventarioSeleccionado?.cantidadActual?:0

        android.util.Log.d(
            "INVENTARIO_DIAGNOSTICO",
            "ELEMENTO=${elemento.idElemento} | " +
                    "INVENTARIO=${elemento.idInventario} | " +
                    "CANTIDAD_ACTUAL=${elemento.cantidadActual} | " +
                    "CANTIDAD=${elemento.cantidad} | " +
                    "STOCK_MOSTRADO=$stockActual"
        )
        if (cantidadAplicada > stockActual) {
            Toast.makeText(
                requireContext(),
                "La cantidad supera el stock disponible",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!cbConfirmarAdministracion.isChecked) {
            Toast.makeText(
                requireContext(),
                "Debe confirmar la administración",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val idMedicamento = formulacion.idMedicamentos

        if (idMedicamento == null) {
            Toast.makeText(
                requireContext(),
                "Medicamento inválido",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val idFormulacion = formulacion.idFormulacion
        val horaAdministrada = formulacion.horaAdministrada

        if (idFormulacion == null || idFormulacion <= 0) {
            Toast.makeText(
                requireContext(),
                "No se identificó la formulación",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (horaAdministrada.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                "No se encontró el horario del medicamento",
                Toast.LENGTH_SHORT
            ).show()
            return
        }



        val idElemento = elemento.idElemento

        if (idElemento == null) {
            Toast.makeText(
                requireContext(),
                "No se encontró el elemento del paciente",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val paciente = viewModel.pacienteSeleccionado.value

        if (paciente == null || paciente.idPaciente == null) {
            Toast.makeText(
                requireContext(),
                "No hay paciente seleccionado",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val nombrePaciente =
            "${paciente?.nombre ?: ""} ${paciente?.apellido ?: ""}".trim()

        val nombreMedicamento =
            mapaNombresMedicamentos[idMedicamento]
                ?: "Medicamento"

        val observacion =
            etObservaciones.text
                ?.toString()
                ?.trim()
                ?: ""
        android.util.Log.d(
            "INVENTARIO_REGISTRO",
            "PACIENTE=${paciente.idPaciente} | " +
                    "MEDICAMENTO=$idMedicamento | " +
                    "INVENTARIO=${inventario.idInventario} | " +
                    "ELEMENTO=$idElemento | " +
                    "STOCK=$stockActual | " +
                    "CANTIDAD=$cantidadAplicada"
        )
        val preferenciasSesion = requireContext()
            .getSharedPreferences(
                "SESION",
                android.content.Context.MODE_PRIVATE
            )

        val idCuidador = preferenciasSesion.getInt(
            "ID_USUARIO",
            -1
        )

        if (idCuidador <= 0) {
            Toast.makeText(
                requireContext(),
                "No se pudo identificar al cuidador",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        // Obtener el usuario que inició sesión
        val idUsuarioSesion = requireContext()
            .getSharedPreferences(
                "SESION",
                android.content.Context.MODE_PRIVATE
            )
            .getInt("ID_USUARIO", -1)

// Validar que exista una sesión válida
        if (idUsuarioSesion <= 0) {
            Toast.makeText(
                requireContext(),
                "No se pudo identificar al usuario que realiza la administración",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        val seguimientoActual = seguimientoSeleccionado

        if (
            seguimientoActual != null &&
            !seguimientoActual.estado.equals(
                "PENDIENTE",
                ignoreCase = true
            )
        ) {

            val mensaje = when {
                seguimientoActual.estado.equals(
                    "ADMINISTRADO",
                    ignoreCase = true
                ) -> "Esta dosis ya fue administrada."

                seguimientoActual.estado.equals(
                    "NO_ADMINISTRADO",
                    ignoreCase = true
                ) -> "Esta dosis ya fue registrada como no administrada."

                else -> "Esta dosis ya tiene un seguimiento cerrado."
            }

            Toast.makeText(
                requireContext(),
                mensaje,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        viewModel.registrarAplicacionMedicamento(
            idTratamientoMedicamento = idMedicamento,
            idUsuario = idCuidador,

            dosis = formulacion.dosis ?: "",
            via = formulacion.via ?: "",

            observacion = observacion,

            idInventario = inventarioSeleccionado!!.idInventario,
            idElementoPaciente = idElemento,

            cantidadActual = stockActual,
            cantidadAplicada = cantidadAplicada,

            idEncargado = 1,
            nombreMedicamento = nombreMedicamento,
            nombrePaciente = nombrePaciente,

            context = requireContext(),
            idFormulacion = idFormulacion,
            horaAdministrada = horaAdministrada,
            idSeguimiento = seguimientoActual?.idSeguimiento
        )
    }
}