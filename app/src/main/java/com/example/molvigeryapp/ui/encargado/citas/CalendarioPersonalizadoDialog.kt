package com.example.molvigeryapp.ui.encargado.citas

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.molvigeryapp.data.model.DiaCalendario
import com.example.molvigeryapp.databinding.DialogCalendarioPersonalizadoEncargadoBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.view.WindowManager

class CalendarioPersonalizadoDialog : DialogFragment() {

    // =========================================================
    // VIEW BINDING
    // =========================================================

    private var _binding:
            DialogCalendarioPersonalizadoEncargadoBinding? = null

    private val binding
        get() = _binding!!


    // =========================================================
    // ADAPTER
    // =========================================================

    private lateinit var adapter:
            DiaCalendarioAdapter


    // =========================================================
    // FECHAS
    // =========================================================

    private var calendarioActual =
        Calendar.getInstance()

    private var fechaSeleccionada =
        Calendar.getInstance()

    private var fechaInicial:
            String? = null


    // =========================================================
    // CALLBACK
    // =========================================================

    private var onFechaSeleccionada:
            ((String) -> Unit)? = null


    // =========================================================
    // CONSTANTES
    // =========================================================

    companion object {

        private const val TAG_CALENDARIO =
            "CalendarioPersonalizado"

        fun newInstance(
            fechaInicial: String? = null,
            onFechaSeleccionada:
                (String) -> Unit
        ): CalendarioPersonalizadoDialog {

            val dialog =
                CalendarioPersonalizadoDialog()

            dialog.fechaInicial =
                fechaInicial

            dialog.onFechaSeleccionada =
                onFechaSeleccionada

            return dialog
        }
    }


    // =========================================================
    // CREAR
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        if (!fechaInicial.isNullOrBlank()) {

            try {

                val formato =
                    SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    )

                formato.isLenient =
                    false

                val fecha =
                    formato.parse(
                        fechaInicial!!
                    )

                if (fecha != null) {

                    fechaSeleccionada.time =
                        fecha

                    calendarioActual.time =
                        fecha
                }

            } catch (_: Exception) {

                establecerFechaActual()
            }

        } else {

            establecerFechaActual()
        }
    }


    // =========================================================
    // FECHA ACTUAL
    // =========================================================

    private fun establecerFechaActual() {

        val hoy =
            Calendar.getInstance()

        limpiarHora(hoy)

        fechaSeleccionada =
            hoy.clone() as Calendar

        calendarioActual =
            hoy.clone() as Calendar
    }


    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            DialogCalendarioPersonalizadoEncargadoBinding
                .inflate(
                    inflater,
                    container,
                    false
                )

        return binding.root
    }


    // =========================================================
    // VISTA CREADA
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(
            view,
            savedInstanceState
        )

        configurarRecycler()

        configurarBotones()

        actualizarCalendario()
    }


    // =========================================================
    // RECYCLER
    // =========================================================

    private fun configurarRecycler() {

        adapter =
            DiaCalendarioAdapter(
                emptyList()
            ) { dia ->

                seleccionarDia(
                    dia
                )
            }


        binding.recyclerDiasCalendario.apply {

            layoutManager =
                GridLayoutManager(
                    requireContext(),
                    7
                )

            adapter =
                this@CalendarioPersonalizadoDialog
                    .adapter

            setHasFixedSize(true)

            isNestedScrollingEnabled =
                false

            overScrollMode =
                View.OVER_SCROLL_NEVER
        }
    }


    // =========================================================
    // BOTONES
    // =========================================================

    private fun configurarBotones() {

        binding.btnMesAnterior.setOnClickListener {

            if (!puedeIrAlMesAnterior()) {
                return@setOnClickListener
            }

            calendarioActual.add(
                Calendar.MONTH,
                -1
            )

            animarCambioMes(
                haciaDerecha = false
            )
        }


        binding.btnMesSiguiente.setOnClickListener {

            calendarioActual.add(
                Calendar.MONTH,
                1
            )

            animarCambioMes(
                haciaDerecha = true
            )
        }


        binding.btnCancelarCalendario.setOnClickListener {

            dismiss()
        }


        binding.btnSeleccionarCalendario.setOnClickListener {

            /*
             * Verificamos que exista una fecha válida
             * antes de devolverla.
             */

            if (!fechaSeleccionadaEsValida()) {
                return@setOnClickListener
            }


            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )


            val fecha =
                formato.format(
                    fechaSeleccionada.time
                )


            onFechaSeleccionada?.invoke(
                fecha
            )


            dismiss()
        }
    }


    // =========================================================
    // SELECCIONAR DÍA
    // =========================================================

    private fun seleccionarDia(
        dia: DiaCalendario
    ) {

        // No hacemos nada si:
        // - es espacio vacío
        // - es fecha anterior

        if (
            dia.dia == null ||
            dia.esAnterior
        ) {
            return
        }


        val fechaTexto =
            dia.fecha

        if (
            fechaTexto.isNullOrBlank()
        ) {
            return
        }


        try {

            val formato =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            formato.isLenient =
                false

            val fecha =
                formato.parse(
                    fechaTexto
                )


            if (fecha != null) {

                fechaSeleccionada.time =
                    fecha

                calendarioActual.time =
                    fecha

                actualizarCalendario()
            }

        } catch (_: Exception) {
        }
    }


    // =========================================================
    // ACTUALIZAR CALENDARIO
    // =========================================================

    private fun actualizarCalendario() {

        if (_binding == null) {
            return
        }


        val formatoMes =
            SimpleDateFormat(
                "MMMM yyyy",
                Locale(
                    "es",
                    "ES"
                )
            )


        val textoMes =
            formatoMes
                .format(
                    calendarioActual.time
                )
                .replaceFirstChar {
                    it.uppercase()
                }


        binding.txtMesActual.text =
            textoMes


        val dias =
            generarDiasDelMes()


        adapter.actualizar(
            dias
        )


        actualizarEstadoBotonAnterior()
    }


    // =========================================================
    // GENERAR DÍAS
    // =========================================================

    private fun generarDiasDelMes():
            List<DiaCalendario> {

        val resultado =
            mutableListOf<DiaCalendario>()


        val calendario =
            calendarioActual.clone()
                    as Calendar


        calendario.set(
            Calendar.DAY_OF_MONTH,
            1
        )


        val primerDia =
            calendario.get(
                Calendar.DAY_OF_WEEK
            )


        /*
         * Nuestro calendario empieza en lunes.
         */

        val espaciosIniciales =
            if (
                primerDia ==
                Calendar.SUNDAY
            ) {

                6

            } else {

                primerDia -
                        Calendar.MONDAY
            }


        repeat(
            espaciosIniciales
        ) {

            resultado.add(
                DiaCalendario(
                    dia = null,
                    fecha = null
                )
            )
        }


        val cantidadDias =
            calendarioActual
                .getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )


        val formatoFecha =
            SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )


        formatoFecha.isLenient =
            false


        val hoy =
            Calendar.getInstance()

        limpiarHora(
            hoy
        )


        for (
        numeroDia in
        1..cantidadDias
        ) {

            calendario.set(
                Calendar.DAY_OF_MONTH,
                numeroDia
            )

            limpiarHora(
                calendario
            )


            // =============================================
            // ES HOY
            // =============================================

            val esHoy =
                calendario.get(
                    Calendar.YEAR
                ) ==
                        hoy.get(
                            Calendar.YEAR
                        ) &&
                        calendario.get(
                            Calendar.DAY_OF_YEAR
                        ) ==
                        hoy.get(
                            Calendar.DAY_OF_YEAR
                        )


            // =============================================
            // ES ANTERIOR
            // =============================================

            val esAnterior =
                calendario.before(
                    hoy
                )


            // =============================================
            // ES SELECCIONADO
            // =============================================

            val esSeleccionado =
                calendario.get(
                    Calendar.YEAR
                ) ==
                        fechaSeleccionada.get(
                            Calendar.YEAR
                        ) &&
                        calendario.get(
                            Calendar.DAY_OF_YEAR
                        ) ==
                        fechaSeleccionada.get(
                            Calendar.DAY_OF_YEAR
                        )


            resultado.add(

                DiaCalendario(

                    dia =
                        numeroDia,

                    fecha =
                        formatoFecha.format(
                            calendario.time
                        ),

                    esHoy =
                        esHoy,

                    esSeleccionado =
                        esSeleccionado,

                    esAnterior =
                        esAnterior
                )
            )
        }


        // =============================================
        // COMPLETAR ÚLTIMA SEMANA
        // =============================================

        val espaciosFinales =
            (
                    7 -
                            (
                                    resultado.size %
                                            7
                                    )
                    ) % 7


        repeat(
            espaciosFinales
        ) {

            resultado.add(
                DiaCalendario(
                    dia = null,
                    fecha = null
                )
            )
        }


        return resultado
    }


    // =========================================================
    // ¿PUEDE IR AL MES ANTERIOR?
    // =========================================================

    private fun puedeIrAlMesAnterior():
            Boolean {

        val hoy =
            Calendar.getInstance()

        return calendarioActual.get(
            Calendar.YEAR
        ) >
                hoy.get(
                    Calendar.YEAR
                ) ||

                (
                        calendarioActual.get(
                            Calendar.YEAR
                        ) ==
                                hoy.get(
                                    Calendar.YEAR
                                ) &&

                                calendarioActual.get(
                                    Calendar.MONTH
                                ) >
                                hoy.get(
                                    Calendar.MONTH
                                )
                        )
    }


    // =========================================================
    // ACTUALIZAR BOTÓN ANTERIOR
    // =========================================================

    private fun actualizarEstadoBotonAnterior() {

        if (_binding == null) {
            return
        }


        val habilitado =
            puedeIrAlMesAnterior()


        binding.btnMesAnterior.isEnabled =
            habilitado


        binding.btnMesAnterior.alpha =
            if (habilitado) {
                1f
            } else {
                0.35f
            }
    }


    // =========================================================
    // LIMPIAR HORA
    // =========================================================

    private fun limpiarHora(
        calendario: Calendar
    ) {

        calendario.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        calendario.set(
            Calendar.MINUTE,
            0
        )

        calendario.set(
            Calendar.SECOND,
            0
        )

        calendario.set(
            Calendar.MILLISECOND,
            0
        )
    }

    private fun fechaSeleccionadaEsValida(): Boolean {

        val hoy =
            Calendar.getInstance()

        limpiarHora(hoy)

        val seleccionada =
            fechaSeleccionada.clone()
                    as Calendar

        limpiarHora(seleccionada)

        /*
         * No permitimos fechas anteriores
         * al día actual.
         */

        return !seleccionada.before(hoy)
    }

    private fun animarCambioMes(
        haciaDerecha: Boolean
    ) {

        if (_binding == null) {
            return
        }

        val recycler =
            binding.recyclerDiasCalendario

        recycler.animate().cancel()

        val desplazamiento =
            if (haciaDerecha) {
                recycler.width * 0.08f
            } else {
                -recycler.width * 0.08f
            }

        recycler.apply {

            alpha = 0.35f
            translationX = desplazamiento
        }

        actualizarCalendario()

        recycler.animate()
            .translationX(0f)
            .alpha(1f)
            .setDuration(180L)
            .start()


        binding.txtMesActual.apply {

            alpha = 0.4f

            animate()
                .alpha(1f)
                .setDuration(180L)
                .start()
        }
    }


    // =========================================================
    // TAMAÑO DEL DIÁLOGO
    // =========================================================

    override fun onStart() {
        super.onStart()

        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)

            setLayout(
                (resources.displayMetrics.widthPixels * 0.92f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            attributes = attributes.apply {
                dimAmount = 0.55f
            }

            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }


    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}