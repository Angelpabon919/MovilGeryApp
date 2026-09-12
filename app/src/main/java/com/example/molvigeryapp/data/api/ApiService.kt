package com.example.molvigeryapp.data.api
<<<<<<< Updated upstream
import com.example.molvigeryapp.data.model.AplicacionMedicamento
=======

>>>>>>> Stashed changes
import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.data.model.LoginResponse
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Usuario
<<<<<<< Updated upstream
import okhttp3.Response
=======
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
>>>>>>> Stashed changes
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

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

<<<<<<< Updated upstream
    @GET("api/aplicacion_medicamento/")
    suspend fun getAplicacionesPorPaciente(
        @Query("id_Paciente") idPaciente:Int
    ): List<AplicacionMedicamento>

    @POST("api/usuarios/registro/")
=======

    // =========================================================
    // USUARIOS
    // =========================================================

    @POST("usuarios/registro/")
>>>>>>> Stashed changes
    suspend fun registrarUsuario(
        @Body usuario: Usuario
    ): Usuario

    @POST("usuarios/login/")
    suspend fun loginUsuario(
        @Body datos: LoginRequest
    ): LoginResponse

    @GET("usuarios/")
    suspend fun getUsuarios(): List<Usuario>


    // =========================================================
    // TURNOS
    // =========================================================

    @GET("turnos/")
    suspend fun getTurnos(): List<Turno>

    @POST("turnos/")
    suspend fun crearTurno(
        @Body turno: Turno
    ): Turno

    @PUT("turnos/{id}/")
    suspend fun actualizarTurno(
        @Path("id") id: Int,
        @Body turno: Turno
    ): Turno

    @DELETE("turnos/{id}/")
    suspend fun eliminarTurno(
        @Path("id") id: Int
    )


    // =========================================================
    // ASIGNACIÓN DE TURNO A USUARIO
    // =========================================================

    @GET("asignacion_turno_usuario/")
    suspend fun getAsignacionesTurno(): List<AsignacionTurnoUsuario>

    @POST("asignacion_turno_usuario/")
    suspend fun crearAsignacionTurno(
        @Body asignacion: AsignacionTurnoUsuario
    ): AsignacionTurnoUsuario

    @PUT("asignacion_turno_usuario/{id}/")
    suspend fun actualizarAsignacionTurno(
        @Path("id") id: Int,
        @Body asignacion: AsignacionTurnoUsuario
    ): AsignacionTurnoUsuario

    @DELETE("asignacion_turno_usuario/{id}/")
    suspend fun eliminarAsignacionTurno(
        @Path("id") id: Int
    )
}