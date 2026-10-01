package com.example.molvigeryapp.ui.encargado.citas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.example.molvigeryapp.databinding.DialogSelectorHoraEncargadoBinding
import java.util.Calendar
import java.util.Locale

class SelectorHoraPersonalizadoDialog : DialogFragment() {

    private var _binding: DialogSelectorHoraEncargadoBinding? = null

    private val binding: DialogSelectorHoraEncargadoBinding
        get() = requireNotNull(_binding) {
            "El binding no está disponible porque la vista del diálogo ya fue destruida."
        }

    private var horaInicial: String? = null

    private var onHoraSeleccionada: ((String) -> Unit)? = null

    companion object {

        fun newInstance(
            horaInicial: String? = null,
            onHoraSeleccionada: (String) -> Unit
        ): SelectorHoraPersonalizadoDialog {

            return SelectorHoraPersonalizadoDialog().apply {

                this.horaInicial = horaInicial

                this.onHoraSeleccionada =
                    onHoraSeleccionada
            }
        }
    }

    // ---------------------------------------------------------
    // CICLO DE VIDA
    // ---------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (horaInicial.isNullOrBlank()) {

            val calendario = Calendar.getInstance()

            val horaActual =
                calendario.get(Calendar.HOUR_OF_DAY)

            val minutosActuales =
                calendario.get(Calendar.MINUTE)

            val minutosRedondeados =
                redondearMinutos(minutosActuales)

            horaInicial = String.format(
                Locale.getDefault(),
                "%02d:%02d",
                horaActual,
                minutosRedondeados
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            DialogSelectorHoraEncargadoBinding.inflate(
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
        super.onViewCreated(
            view,
            savedInstanceState
        )

        configurarPickers()

        configurarBotones()

        actualizarHoraMostrada()
    }

    // ---------------------------------------------------------
    // CONFIGURACIÓN DE PICKERS
    // ---------------------------------------------------------

    private fun configurarPickers() {

        val horaInicialSeparada =
            obtenerHoraInicial()

        configurarPickerHora(
            horaInicialSeparada.first
        )

        configurarPickerMinutos(
            horaInicialSeparada.second
        )
    }

    // ---------------------------------------------------------
    // PICKER DE HORAS
    // ---------------------------------------------------------

    private fun configurarPickerHora(
        horaInicial: Int
    ) {

        val horas = Array(24) { hora ->

            String.format(
                Locale.getDefault(),
                "%02d",
                hora
            )
        }

        binding.pickerHora.apply {

            minValue = 0

            maxValue = 23

            displayedValues = horas

            value = horaInicial.coerceIn(
                0,
                23
            )

            wrapSelectorWheel = false

            setOnValueChangedListener { _, _, _ ->

                actualizarHoraMostrada()
            }
        }
    }

    // ---------------------------------------------------------
    // PICKER DE MINUTOS
    // ---------------------------------------------------------

    private fun configurarPickerMinutos(
        minutoInicial: Int
    ) {

        val minutos = Array(60) { minuto ->

            String.format(
                Locale.getDefault(),
                "%02d",
                minuto
            )
        }

        binding.pickerMinutos.apply {

            minValue = 0

            maxValue = 59

            displayedValues = minutos

            value = minutoInicial.coerceIn(
                0,
                59
            )

            wrapSelectorWheel = true

            setOnValueChangedListener { _, _, _ ->

                actualizarHoraMostrada()
            }
        }
    }

    // ---------------------------------------------------------
    // BOTONES
    // ---------------------------------------------------------

    private fun configurarBotones() {

        binding.btnCancelarHora.setOnClickListener {

            dismiss()
        }

        binding.btnSeleccionarHora.setOnClickListener {

            seleccionarHora()
        }
    }

    // ---------------------------------------------------------
    // SELECCIONAR HORA
    // ---------------------------------------------------------

    private fun seleccionarHora() {

        if (_binding == null) {
            return
        }

        val hora =
            binding.pickerHora.value

        val minutos =
            binding.pickerMinutos.value

        val horaFormateada =
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                hora,
                minutos
            )

        onHoraSeleccionada?.invoke(
            horaFormateada
        )

        dismiss()
    }

    // ---------------------------------------------------------
    // ACTUALIZAR TEXTO DE HORA
    // ---------------------------------------------------------

    private fun actualizarHoraMostrada() {

        if (_binding == null) {
            return
        }

        val hora =
            binding.pickerHora.value

        val minutos =
            binding.pickerMinutos.value

        binding.txtHoraSeleccionada.text =
            String.format(
                Locale.getDefault(),
                "%02d:%02d",
                hora,
                minutos
            )
    }

    // ---------------------------------------------------------
    // OBTENER HORA INICIAL
    // ---------------------------------------------------------

    private fun obtenerHoraInicial(): Pair<Int, Int> {

        val texto =
            horaInicial
                ?.trim()
                .orEmpty()

        if (texto.isBlank()) {

            return Pair(
                10,
                0
            )
        }

        return try {

            val partes =
                texto.split(":")

            if (partes.size != 2) {

                return Pair(
                    10,
                    0
                )
            }

            val hora =
                partes[0]
                    .toIntOrNull()
                    ?.coerceIn(
                        0,
                        23
                    )
                    ?: 10

            val minutos =
                partes[1]
                    .toIntOrNull()
                    ?.coerceIn(
                        0,
                        59
                    )
                    ?: 0

            Pair(
                hora,
                minutos
            )

        } catch (_: Exception) {

            Pair(
                10,
                0
            )
        }
    }

    // ---------------------------------------------------------
    // REDONDEAR MINUTOS
    // ---------------------------------------------------------

    private fun redondearMinutos(
        minutos: Int
    ): Int {

        return when {

            minutos < 8 -> {
                0
            }

            minutos < 23 -> {
                15
            }

            minutos < 38 -> {
                30
            }

            minutos < 53 -> {
                45
            }

            else -> {
                0
            }
        }
    }

    // ---------------------------------------------------------
    // TAMAÑO DEL DIÁLOGO
    // ---------------------------------------------------------

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {

            setBackgroundDrawableResource(
                android.R.color.transparent
            )

            setLayout(
                (resources.displayMetrics.widthPixels * 0.92f)
                    .toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            attributes =
                attributes.apply {

                    dimAmount = 0.55f
                }

            addFlags(
                WindowManager.LayoutParams.FLAG_DIM_BEHIND
            )
        }
    }

    // ---------------------------------------------------------
    // DESTRUIR BINDING
    // ---------------------------------------------------------

    override fun onDestroyView() {

        onHoraSeleccionada = null

        _binding = null

        super.onDestroyView()
    }
}