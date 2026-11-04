package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class ResumenCuidadosTurno(

    @SerializedName("id_asignacion")
    val idAsignacion: Int? = null,

    @SerializedName("id_cuidador")
    val idCuidador: Int? = null,

    @SerializedName("turno")
    val turno: TurnoResumen? = null,

    @SerializedName("fecha_hora_inicio")
    val fechaHoraInicio: String? = null,

    @SerializedName("fecha_hora_fin")
    val fechaHoraFin: String? = null,

    @SerializedName("pacientes")
    val pacientes: List<PacienteResumenTurno> = emptyList()
)

data class TurnoResumen(

    @SerializedName("id_turno")
    val idTurno: Int? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("fecha")
    val fecha: String? = null,

    @SerializedName("hora_inicio")
    val horaInicio: String? = null,

    @SerializedName("hora_fin")
    val horaFin: String? = null
)

data class PacienteResumenTurno(

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("apellido")
    val apellido: String? = null,

    @SerializedName("foto")
    val foto: String? = null,

    @SerializedName("habitacion")
    val habitacion: String? = null,

    @SerializedName("cama")
    val cama: String? = null,

    @SerializedName("bitacoras")
    val bitacoras: List<BitacoraResumenTurno> = emptyList(),

    @SerializedName("controles_periodicos")
    val controlesPeriodicos: List<ControlesPeriodicosBitacora> = emptyList()
)

data class BitacoraResumenTurno(

    @SerializedName("id_bitacora")
    val idBitacora: Int? = null,

    @SerializedName("estado")
    val estado: Boolean = true,

    @SerializedName("tipo_registro")
    val tipoRegistro: String? = null,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("fecha_hora")
    val fechaHora: String? = null,

    @SerializedName("id_usuario")
    val idUsuario: Int? = null,

    @SerializedName("id_paciente")
    val idPaciente: Int? = null,

    @SerializedName("signos_vitales")
    val signosVitales: List<SignosVitales> = emptyList()
)