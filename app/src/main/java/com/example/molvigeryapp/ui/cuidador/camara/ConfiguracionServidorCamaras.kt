package com.example.molvigeryapp.ui.cuidador.camara

import android.content.Context

object ConfiguracionServidorCamaras {

    private const val NOMBRE_PREFERENCIAS = "CONFIGURACION_CAMARAS"
    private const val CLAVE_SERVIDOR = "servidor_video"

    // Conserva la dirección local que ya funciona mientras configuramos el acceso remoto.
    private const val SERVIDOR_PREDETERMINADO = "http://192.168.20.29:8081"

    fun obtenerServidor(context: Context): String {
        return context.getSharedPreferences(
            NOMBRE_PREFERENCIAS,
            Context.MODE_PRIVATE
        ).getString(CLAVE_SERVIDOR, SERVIDOR_PREDETERMINADO)
            ?: SERVIDOR_PREDETERMINADO
    }

    fun guardarServidor(context: Context, direccion: String): Boolean {
        val limpia = direccion.trim().trimEnd('/')

        if (!limpia.startsWith("http://") && !limpia.startsWith("https://")) {
            return false
        }

        return context.getSharedPreferences(
            NOMBRE_PREFERENCIAS,
            Context.MODE_PRIVATE
        ).edit()
            .putString(CLAVE_SERVIDOR, limpia)
            .commit()
    }
}
