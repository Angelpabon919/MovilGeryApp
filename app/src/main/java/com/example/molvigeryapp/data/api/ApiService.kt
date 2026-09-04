package com.example.molvigeryapp.data.api
import com.example.molvigeryapp.data.model.Paciente
import retrofit2.http.GET
interface ApiService {
    @GET("api/pacientes/")
    suspend fun getPacientes(): List<Paciente>
}