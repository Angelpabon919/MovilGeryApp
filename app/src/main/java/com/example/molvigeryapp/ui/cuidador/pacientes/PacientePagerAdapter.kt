package com.example.molvigeryapp.ui.cuidador.pacientes

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.molvigeryapp.ui.cuidador.bitacora.BitacoraFragment

class PacientePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AplicacionMedicamentosFragment()
            1 -> BitacoraFragment()
            2 -> RecomendacionesFragment()
            else -> AplicacionMedicamentosFragment()
        }
    }
}