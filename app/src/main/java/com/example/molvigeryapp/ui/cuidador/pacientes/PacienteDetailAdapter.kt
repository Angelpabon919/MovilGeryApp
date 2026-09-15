package com.example.molvigeryapp.ui.cuidador.pacientes

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.molvigeryapp.ui.cuidador.bitacora.BitacoraFragment
import com.example.molvigeryapp.ui.cuidador.bitacora.EventosAdversosFragment

class PacienteDetailAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DatosBasicosFragment()
            1 -> ElementosFragment()
            2 -> CardexFragment()
            3 -> RecomendacionesFragment()
            4 -> BitacoraFragment()
            else -> DatosBasicosFragment()
        }
    }
}