package com.example.molvigeryapp.ui.cuidador.medicamentos

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.ui.cuidador.pacientes.PacienteViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

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

    private var formulacionSeleccionada: FormulacionMedicamento? = null
    private var elementoSeleccionado: ElementoPaciente? = null

    private var medicamentosPendientes:
            List<FormulacionMedicamento> = emptyList()

    private var elementosPaciente:
            List<ElementoPaciente> = emptyList()

    private var mapaNombresMedicamentos:
            Map<Int, String> = emptyMap()


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

        spinnerMedicamento.setOnItemClickListener {
                _, _, position, _ ->

            if (position < medicamentosPendientes.size) {

                formulacionSeleccionada =
                    medicamentosPendientes[position]

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

            }?.onFailure { error ->

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


        // Si solo hay un medicamento,
        // lo seleccionamos automáticamente.
        if (medicamentosPendientes.size == 1) {

            formulacionSeleccionada =
                medicamentosPendientes.first()

            spinnerMedicamento.setText(
                nombres.first(),
                false
            )

            actualizarElementoSeleccionado()
        }
    }


    // =========================================================
    // BUSCAR INVENTARIO DEL MEDICAMENTO
    // =========================================================

    private fun actualizarElementoSeleccionado() {

        val formulacion =
            formulacionSeleccionada ?: return

        elementoSeleccionado =
            elementosPaciente.find {
                it.idMedicamentos ==
                        formulacion.idMedicamentos
            }
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

        val grupo =
            viewModel.gruposMedicacion.value
                ?.find {
                    it.idGrupo == formulacion.idGrupo
                }
                ?: return "Sin horario"


        val horaBase =
            grupo.horaAdministracion
                ?.substringBefore(":")
                ?.toIntOrNull()
                ?: return "Sin horario"


        val horaActual = 8


        val horaProgramada =

            if (
                formulacion.idGrupo == 2 &&
                horaActual == ((horaBase + 12) % 24)
            ) {

                (horaBase + 12) % 24

            } else {

                horaBase
            }


        return convertirHora12Horas(horaProgramada)
    }


    // =========================================================
    // CONVERTIR 20 -> 8:00 p. m.
    // =========================================================

    private fun convertirHora12Horas(
        hora: Int
    ): String {

        return when {

            hora == 0 ->
                "12:00 a. m."

            hora < 12 ->
                "$hora:00 a. m."

            hora == 12 ->
                "12:00 p. m."

            else ->
                "${hora - 12}:00 p. m."
        }
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

        val elemento = elementoSeleccionado

        if (elemento == null) {
            Toast.makeText(
                requireContext(),
                "El medicamento no está disponible en el inventario del paciente",
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

        val stockActual =
            elemento.cantidadActual
                ?: elemento.cantidad
                ?: 0

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

        viewModel.registrarAplicacionMedicamento(
            idTratamientoMedicamento = idMedicamento,
            idUsuario = 1,

            dosis = formulacion.dosis ?: "",
            via = formulacion.via ?: "",

            observacion = observacion,

            idInventario = elemento.idInventario,
            idElementoPaciente = idElemento,

            cantidadActual = stockActual,
            cantidadAplicada = cantidadAplicada,

            idEncargado = 1,
            nombreMedicamento = nombreMedicamento,
            nombrePaciente = nombrePaciente,

            context = requireContext()
        )
    }
}