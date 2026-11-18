package com.example.molvigeryapp.ui.cuidador.pacientes
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.molvigeryapp.ui.cuidador.bitacora.BitacoraFragment
import com.example.molvigeryapp.ui.cuidador.medicamentos.AplicacionMedicamentosFragment

class PacientePagerAdapter(
    fragment: Fragment,
    private val idFormulacionAlarma: Int? = null
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AplicacionMedicamentosFragment().apply {
                if (idFormulacionAlarma != null) {
                    arguments = Bundle().apply {
                        putInt(
                            "ID_FORMULACION_ALARMA",
                            idFormulacionAlarma
                        )
                    }
                    }
            }
            1 -> BitacoraFragment()
            2 -> RecomendacionesFragment()
            3 -> CardexFragment()
            else -> AplicacionMedicamentosFragment()
        }
    }
}
