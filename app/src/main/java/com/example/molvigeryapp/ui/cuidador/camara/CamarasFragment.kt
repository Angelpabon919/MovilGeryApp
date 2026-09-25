package com.example.molvigeryapp.ui.cuidador.camara

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.databinding.FragmentCamaraBinding
import com.example.molvigeryapp.databinding.ItemCamaraBinding

class CamarasFragment : Fragment() {

    private var _binding: FragmentCamaraBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCamaraBinding.inflate(
            inflater,
            container,
            false
        )

        agregarCamara("Cámara - Habitación 1")

        return binding.root
    }

    private fun agregarCamara(nombre: String) {

        val camaraBinding = ItemCamaraBinding.inflate(
            layoutInflater,
            binding.contenedorCamaras,
            false
        )

        camaraBinding.tvNombreCamara.text = nombre
        camaraBinding.tvEstadoCamara.text = "● En línea"

        binding.contenedorCamaras.addView(
            camaraBinding.root
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

