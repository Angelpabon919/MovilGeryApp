package com.example.molvigeryapp.data.api
import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.data.model.LoginResponse
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Usuario
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("api/pacientes/")
    suspend fun getPacientes(): List<Paciente>

    @GET("api/pacientes/{id}/")
    suspend fun getPacienteById(@Path("id") id: Int): Paciente

    @GET("api/aplicacion_medicamento/")
    suspend fun getAplicacionesPorPaciente(
        @Query("id_Paciente") idPaciente:Int
    ): List<AplicacionMedicamento>

    @POST("api/usuarios/registro/")
    suspend fun registrarUsuario(
        @Body usuario: Usuario
    ): Usuario

    @POST("api/usuarios/login/")
    suspend fun loginUsuario(
        @Body datos: LoginRequest
    ): LoginResponse

    @GET("api/usuarios/")
    suspend fun getUsuarios(): List<Usuario>
}