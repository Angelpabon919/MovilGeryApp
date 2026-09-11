package com.example.molvigeryapp.utils

import android.content.Context

class SessionManager (context : Context) {

    private val preferences = context.getSharedPreferences("GerIAppPrefs", Context.MODE_PRIVATE)

    fun guardarSesion(idRol: Int){
        preferences.edit().putBoolean("SESION_INICIADA", true)
            .putInt("ID_ROL", idRol).apply()
    }

    fun sesionIniciada(): Boolean {
        return preferences.getBoolean("SESION_INICIADA", false)
    }

    fun obtenerIdRol(): Int {
        return preferences.getInt("ID_ROL", -1)
    }

    fun cerrarSesion(){
        preferences.edit().clear().apply()
    }
}