package com.example.molvigeryapp.data.model

import com.google.gson.annotations.SerializedName

data class Inventario(
    @SerializedName("id_inventario")
    val idInventario: Int,

    @SerializedName("id_paciente")
    val idPaciente: Int?,

    @SerializedName("id_medicamentos")
    val idMedicamentos: Int?,

    @SerializedName("id_elemento")
    val idElemento: Int?


)