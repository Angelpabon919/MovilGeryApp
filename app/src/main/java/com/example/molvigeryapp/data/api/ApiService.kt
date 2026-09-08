package com.example.molvigeryapp.data.api

import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.data.model.LoginResponse
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Usuario
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // =========================================================
    // PACIENTES
    // =========================================================

    @GET("pacientes/")
    suspend fun getPacientes(): List<Paciente>

    @GET("pacientes/{id}/")
    suspend fun getPacienteById(
        @Path("id") id: Int
    ): Paciente

    // =========================================================
    // USUARIOS
    // =========================================================

    @POST("usuarios/registro/")
    suspend fun registrarUsuario(
        @Body usuario: Usuario
    ): Usuario

    @POST("usuarios/login/")
    suspend fun loginUsuario(
        @Body datos: LoginRequest
    ): LoginResponse
}