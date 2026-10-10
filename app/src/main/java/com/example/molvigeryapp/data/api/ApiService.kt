package com.example.molvigeryapp.data.api

import com.example.molvigeryapp.data.model.AplicacionMedicamento
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Actividad
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.Bitacora
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.EventoAdverso
import com.example.molvigeryapp.data.model.HistoriaClinica
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
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.Camara
import com.example.molvigeryapp.data.model.Inventario
import com.example.molvigeryapp.data.model.NotificacionApi
import com.example.molvigeryapp.data.model.NotificacionDestinatarioRequest
import com.example.molvigeryapp.data.model.NotificacionRequest
import com.example.molvigeryapp.data.model.NotificacionResponse
import com.example.molvigeryapp.data.model.Cita
import com.example.molvigeryapp.data.model.Usuario
import okhttp3.ResponseBody
import com.example.molvigeryapp.data.model.CambiarContrasenaRequest
import com.example.molvigeryapp.data.model.RespuestaMensaje
import com.example.molvigeryapp.data.model.CitaApiResponse
import com.example.molvigeryapp.data.model.CrearCitaRequest
import com.example.molvigeryapp.data.model.EventoIa
import com.example.molvigeryapp.data.model.EvidenciaIa
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.GrupoMedicacion
import com.example.molvigeryapp.data.model.Habitacion
import com.example.molvigeryapp.data.model.NotificacionDestinatario
import com.example.molvigeryapp.data.model.MarcarNotificacionLeidaRequest
import com.example.molvigeryapp.data.model.MarcarNotificacionLeidaCompletaRequest
import com.example.molvigeryapp.data.model.TipoEventoIa

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
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


    // =========================================================
    // APLICACIÓN DE MEDICAMENTOS
    // =========================================================
    @POST("aplicacion_medicamento/")
    suspend fun registrarAplicacion(
        @Body request: AplicacionRequest
    ): Response<ResponseBody>

    @GET("inventario/")
    suspend fun obtenerInventario(): Response<List<Inventario>>



    @PATCH("inventario/{id}/")
    suspend fun actualizarInventarioStock(
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): Response<ResponseBody>

    @PATCH("elementos_paciente/{id}/")
    suspend fun actualizarCantidadElemento(
        @Path("id") idElemento: Int,
        @Body body: Map<String, Int>
    ): Response<ResponseBody>

    @POST("notificaciones/")
    suspend fun crearNotificacion(
        @Body request: NotificacionRequest
    ): Response<NotificacionResponse>

    // 3. Endpoint para asociar la notificación al encargado (Tabla NotificacionDestinatario)
    @POST("notificacion_destinatario/")
    suspend fun asociarNotificacionDestinatario(
        @Body request: NotificacionDestinatarioRequest
    ): Response<ResponseBody>

    @POST("notificaciones/")
    suspend fun crearNotificacionStock(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<NotificacionResponse>
    @POST("notificacion_destinatario/")
    suspend fun asociarNotificacionDestinatarioStock(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>


    // =========================================================
    // RECOMENDACIONES
    // =========================================================

    @GET("recomendaciones/")
    suspend fun getRecomendacionesPorPaciente(
        @Query("id_paciente") idPaciente: Int
    ): List<Recomendacion>

    @POST("recomendaciones/")
    suspend fun guardarRecomendacion(
        @Body recomendacion: Recomendacion
    ): Response<Recomendacion>


    // =========================================================
    // CUIDADOS DE ENFERMERÍA
    // =========================================================

    @POST("cuidados_enfermeria/")
    suspend fun guardarCuidadoEnfermeria(
        @Body cuidado: CuidadoEnfermeria
    ): CuidadoEnfermeria

    @GET("cuidados_enfermeria/")
    suspend fun getCuidadosPorPaciente(
        @Query("id_paciente") idPaciente: Int
    ): List<CuidadoEnfermeria>


    // =========================================================
    // ASIGNACIÓN DE PACIENTES A CUIDADORES
    // =========================================================

    @POST("asignacion_paciente_cuidador/")
    suspend fun guardarAsignacion(
        @Body asignacion: AsignacionPacienteCuidador
    ): Response<AsignacionPacienteCuidador>

    @GET("asignacion_paciente_cuidador/")
    suspend fun getAsignacionesPacienteCuidador():
            Response<List<AsignacionPacienteCuidador>>

    @PATCH("asignacion_paciente_cuidador/{id}/")
    suspend fun actualizarEstadoAsignacion(
        @Path("id") id: Int,
        @Body datos: Map<String, String>
    ): Response<AsignacionPacienteCuidador>


    // =========================================================
    // ELEMENTOS DEL PACIENTE
    // =========================================================

    @GET("elementos_paciente/")
    suspend fun getElementosPorPaciente(
        @Query("id_paciente") idPaciente: Int
    ): Response<List<ElementoPaciente>>

    @POST("elementos_paciente/")
    suspend fun guardarElementoPaciente(
        @Body elemento: ElementoPaciente
    ): Response<ElementoPaciente>


    // =========================================================
    // MEDICAMENTOS
    // =========================================================

    @GET("medicamentos/")
    suspend fun getMedicamentos(): Response<List<Medicamento>>

    @GET("formulacion_medicamentos/")
    suspend fun getFormulacionesMedicamentos():
            Response<List<FormulacionMedicamento>>

    @GET("grupo_medicacion/")
    suspend fun getGrupoMedicacion():
            Response<List<GrupoMedicacion>>


    // =========================================================
    // TIPOS DE INSUMOS
    // =========================================================

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

    @GET("usuarios/{id}/")
    suspend fun getUsuarioById(
        @Path("id") id: Int
    ): Usuario

    @POST("usuarios/cambiar-contrasena/")
    suspend fun cambiarContrasena(
        @Body datos: CambiarContrasenaRequest
    ): Response<RespuestaMensaje>

    @PATCH("usuarios/{id}/")
    suspend fun actualizarUsuario(
        @Path("id") id: Int,
        @Body datos: Map<String, @JvmSuppressWildcards Any>
    ): Response<Usuario>


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
// NOTIFICACIONES
// =========================================================

    @GET("notificaciones/")
    suspend fun getNotificaciones(): List<NotificacionApi>

    @GET("notificacion_destinatario/")
    suspend fun getNotificacionesDestinatarios(): List<NotificacionDestinatario>

    @PATCH("notificacion_destinatario/{id}/")
    suspend fun marcarNotificacionLeida(
        @Path("id") id: Int,
        @Body datos: MarcarNotificacionLeidaRequest
    ): Response<RespuestaMensaje>

    @POST("notificacion_destinatario/marcar-leida/")
    suspend fun marcarNotificacionLeidaCompleta(
        @Body datos: MarcarNotificacionLeidaCompletaRequest
    ): Response<Map<String, Any>>

    @POST("fcm_tokens/")
    suspend fun registrarTokenFCM(
        @Body datos: Map<String, @JvmSuppressWildcards Any>
    ): Response<Any>


    // =========================================================
    // ASIGNACIONES DE TURNOS
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


    // =========================================================
    // BITÁCORA
    // =========================================================

    @GET("bitacora/")
    suspend fun getBitacoras(): List<Bitacora>

    @POST("bitacora/")
    suspend fun crearBitacora(
        @Body bitacora: Bitacora
    ): Bitacora


    // =========================================================
    // ACTIVIDADES
    // =========================================================

    @GET("actividades/")
    suspend fun getActividades(): List<Actividad>

    // =========================================================
    // CITAS
    // =========================================================

    @POST("citas/")
    suspend fun crearCita(
        @Body cita: CrearCitaRequest
    ): Response<CitaApiResponse>

    @GET("citas/")
    suspend fun getCitas():
    Response<List<Cita>>



    // =========================================================
    // EVENTOS ADVERSOS
    // =========================================================

    @GET("eventos_adversos/")
    suspend fun getEventosAdversos(): List<EventoAdverso>

    @POST("eventos_adversos/")
    suspend fun crearEventoAdverso(
        @Body evento: EventoAdverso
    ): EventoAdverso


    // =========================================================
    // SIGNOS VITALES
    // =========================================================

    @POST("signos_vitales/")
    suspend fun crearSignosVitales(
        @Body signos: SignosVitales
    ): SignosVitales


    // =========================================================
    // TIPOS DE EMERGENCIA
    // =========================================================

    @GET("tipo_emergencia/")
    suspend fun getTiposEmergencia(): List<TipoEmergencia>


    // =========================================================
    // HISTORIAS CLÍNICAS
    // =========================================================

    @GET("historia_clinicas/")
    suspend fun getHistoriasClinicas(): List<HistoriaClinica>

    @POST("historia_clinicas/")
    suspend fun crearHistoriaClinica(
        @Body historia: HistoriaClinica
    ): HistoriaClinica
    // =========================================================
// EVENTOS DE INTELIGENCIA ARTIFICIAL
// =========================================================

    @GET("eventos_ia/")
    suspend fun getEventosIa(): Response<List<EventoIa>>

    @GET("tipos_evento_ia/")
    suspend fun getTiposEventoIa(): Response<List<TipoEventoIa>>


    // =====================================================
    // EVIDENCIAS DE INTELIGENCIA ARTIFICIAL
    // =====================================================

    @GET("evidencias_ia/")
    suspend fun getEvidenciasIa(): Response<List<EvidenciaIa>>

    // ==========================================
// OBTENER CÁMARAS DESDE EL BACKEND
// ==========================================

    @GET("camaras/")
    suspend fun getCamaras(): List<Camara>

    // ==========================================
// OBTENER HABITACIONES DESDE EL BACKEND
// ==========================================

    @GET("habitaciones/")
    suspend fun getHabitaciones(): List<Habitacion>


}