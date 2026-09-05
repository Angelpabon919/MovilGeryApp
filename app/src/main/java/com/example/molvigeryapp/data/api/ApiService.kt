package com.example.molvigeryapp.data.api
import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.data.model.LoginResponse
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Usuario
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {
    @GET("api/pacientes/")
    suspend fun getPacientes(): List<Paciente>

    @POST("api/usuarios/")
    suspend fun registrarUsuario(
        @Body usuario: Usuario
    ): Usuario

    @POST("api/usuarios/login/")
    suspend fun loginUsuario(
        @Body datos: LoginRequest
    ): LoginResponse
}