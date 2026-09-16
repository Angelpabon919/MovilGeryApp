package com.example.molvigeryapp.data.api

import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.LoginRequest
import com.example.molvigeryapp.data.model.LoginResponse
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.SignosVitales
import com.example.molvigeryapp.data.model.TipoEmergencia
import com.example.molvigeryapp.data.model.TipoInsumo
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.data.model.Usuario
import retrofit2.Response

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

    @GET("recomendaciones/")
    suspend fun getRecomendacionesPorPaciente(
        @Query("id_paciente") idPaciente: Int
    ): List<Recomendacion>

    @POST("recomendaciones/")
    suspend fun guardarRecomendacion(
        @Body recomendacion: Recomendacion
    ): Recomendacion

    @POST("cuidados_enfermeria/")
    suspend fun guardarCuidadoEnfermeria(
        @Body cuidado: CuidadoEnfermeria
    ): CuidadoEnfermeria

    @GET("cuidados_enfermeria/")
    suspend fun getCuidadosPorPaciente(
        @Query("id_paciente") idPaciente: Int
    ): List<CuidadoEnfermeria>

    @POST("asignacion_paciente_cuidador/")
    suspend fun guardarAsignacion(
        @Body asignacion: AsignacionPacienteCuidador
    ): Response<AsignacionPacienteCuidador>

    @GET("elementos_paciente/")
    suspend fun getElementosPorPaciente(
        @Query("id_paciente") idPaciente: Int
    ): Response<List<ElementoPaciente>>

    @POST("elementos_paciente/")
    suspend fun guardarElementoPaciente(
        @Body elemento: ElementoPaciente
    ): Response<ElementoPaciente>

    // 3. Catálogo maestro de medicamentos
    @GET("medicamentos/")
    suspend fun getMedicamentos(): Response<List<Medicamento>>

    // 4. Catálogos maestros para insumos
    @GET("tipo_insumo/")
    suspend fun getTiposInsumos(): Response<List<TipoInsumo>>

    @GET("insumos/")
    suspend fun getInsumosPorTipo(
        @Query("id_tipo") idTipoInsumo: Int
    ): Response<List<Insumo>>



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

    @POST("eventos_adversos/")
    suspend fun crearEventoAdverso(
        @Body evento: EventoAdverso
    ): EventoAdverso

    @POST("signos_vitales/")
    suspend fun crearSignosVitales(
        @Body signos: SignosVitales
    ): SignosVitales

    @GET("tipo_emergencia/")
    suspend fun getTiposEmergencia(): List<TipoEmergencia>
}