package com.example.molvigeryapp.data.api

import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.data.model.LoginResponse
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.SignosVitales
import com.example.molvigeryapp.data.model.TipoEmergencia
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.model.Usuario

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("pacientes/")
    suspend fun getPacientes(): List<Paciente>

    @GET("pacientes/{id}/")
    suspend fun getPacienteById(
        @Path("id") id: Int
    ): Paciente

    @GET("api/aplicacion_medicamento/")
    suspend fun getAplicacionesPorPaciente(
        @Query("id_Paciente") idPaciente: Int
    ): List<AplicacionMedicamento>

    @GET("recomendaciones/")
    suspend fun getRecomendacionesPorPaciente(
        @Query("id_paciente") idPaciente: Int
    ): List<Recomendacion>

    @POST("usuarios/registro/")
    suspend fun registrarUsuario(
        @Body usuario: Usuario
    ): Usuario

    @POST("usuarios/login/")
    suspend fun loginUsuario(
        @Body datos: LoginRequest
    ): LoginResponse

    @GET("usuarios/")
    suspend fun getUsuarios(): List<Usuario>

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

    @POST("bitacora/")
    suspend fun crearBitacora(
        @Body bitacora: Bitacora
    ): Bitacora

    @POST("evento_adverso/")
    suspend fun crearEventoAdverso(
        @Body evento : EventoAdverso
    ): EventoAdverso

    @POST("signos_vitales/")
    suspend fun crearSignosVitales(
        @Body signos: SignosVitales
    ): SignosVitales

    @GET("tipo_emergencia")
    suspend fun getTiposEmergencia():
            List<TipoEmergencia>
}