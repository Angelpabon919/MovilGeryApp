package com.example.molvigeryapp.ui.cuidador.home

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.FragmentBienvenidaSedeCuidadorBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment

class BienvenidaSedeCuidadorFragment : Fragment() {

    private var _binding: FragmentBienvenidaSedeCuidadorBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())

    private var navegacionRealizada = false

    private val continuarRunnable = Runnable {
        navegarAPacientes()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentBienvenidaSedeCuidadorBinding.inflate(
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

        configurarAnimacion()

        // Esperar unos segundo antes de continuar
        handler.postDelayed(
            continuarRunnable,
            2500L
        )
    }

    private fun configurarAnimacion() {

        binding.iconoVerificado.apply {
            alpha = 0f
            scaleX = 0.6f
            scaleY = 0.6f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(450L)
                .start()
        }

        binding.tvBienvenida.apply {
            alpha = 0f

            animate()
                .alpha(1f)
                .setDuration(500L)
                .start()
        }

        binding.tvMensajeLlegada.apply {
            alpha = 0f

            animate()
                .alpha(1f)
                .setDuration(650L)
                .start()
        }
    }

    private fun navegarAPacientes() {

        if (navegacionRealizada) return

        if (!isAdded) return

        navegacionRealizada = true

        parentFragmentManager
            .beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(
                R.id.fragmentContainer,
                PacientesListFragment()
            )
            .commit()
    }

    override fun onDestroyView() {

        handler.removeCallbacks(continuarRunnable)

        binding.root.clearAnimation()
        binding.iconoVerificado.clearAnimation()
        binding.tvBienvenida.clearAnimation()
        binding.tvMensajeLlegada.clearAnimation()

        _binding = null

        super.onDestroyView()
    }
}
