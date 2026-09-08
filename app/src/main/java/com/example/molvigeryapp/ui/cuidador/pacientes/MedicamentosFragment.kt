package com.example.molvigeryapp.ui.cuidador.pacientes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.molvigeryapp.data.model.TratamientoMedicamento

class MedicamentosFragment : Fragment() {

    private val adapter = MedicamentoAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(com.example.molvigeryapp.R.layout.fragment_medicamentos, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(com.example.molvigeryapp.R.id.rvMedicamentos)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        rv?.adapter = adapter

        cargarDatosLocales()
    }

    private fun cargarDatosLocales() {
        val listaPrueba = listOf(
            TratamientoMedicamento(
                id_tratamiento_medicamento = 1,
                dosis = "1",
                frecuencia = "Cada 12h (6:00 AM / 6:00 PM)",
                via_administracion = "Vía Oral",
                duracion = "30 días",
                cantidad_prescrita = "60 pastillas",
                observaciones = "Losartán 50mg - Tomar con agua",
                estado = "activo"
            ),
            TratamientoMedicamento(
                id_tratamiento_medicamento = 2,
                dosis = "1",
                frecuencia = "Cada 24h (Aynas)",
                via_administracion = "Vía Oral",
                duracion = "15 días",
                cantidad_prescrita = "15 cápsulas",
                observaciones = "Omeprazol 20mg - Antes del desayuno",
                estado = "activo"
            ),
            TratamientoMedicamento(
                id_tratamiento_medicamento = 3,
                dosis = "1",
                frecuencia = "Cada 8h",
                via_administracion = "Vía Oral",
                duracion = "60 días",
                cantidad_prescrita = "180 tabletas",
                observaciones = "Metformina 850mg - Después de las comidas",
                estado = "activo"
            )
        )

        adapter.actualizarLista(listaPrueba)
    }
}