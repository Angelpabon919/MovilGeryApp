package com.example.molvigeryapp.data.model

data class LoginResponse(
    val mensaje: String,
    val token: String,
    val usuario: UsuarioLogin
)
