package com.example.molvigeryapp.ui.encargado.notificaciones

import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.repository.NotificacionesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object ContadorNotificaciones {

    fun iniciar(
        fragment: Fragment,
        badge: TextView
    ) {

        fragment.viewLifecycleOwner.lifecycleScope.launch {

            val preferencias =
                fragment.requireContext()
                    .getSharedPreferences(
                        "SESION",
                        0
                    )

            val idUsuario =
                preferencias.getInt(
                    "ID_USUARIO",
                    -1
                )

            while (isActive) {

                try {

                    val cantidad =
                        NotificacionesRepository
                            .obtenerCantidadNoLeidas(
                                idUsuario
                            )

                    if (!fragment.isAdded) {
                        break
                    }

                    if (cantidad > 0) {

                        badge.visibility =
                            View.VISIBLE

                        badge.text =
                            if (cantidad > 99) {
                                "99+"
                            } else {
                                cantidad.toString()
                            }

                    } else {

                        badge.visibility =
                            View.GONE
                    }

                } catch (
                    e: CancellationException
                ) {

                    throw e

                } catch (e: Exception) {

                    e.printStackTrace()
                }

                delay(2_000L)
            }
        }
    }
}