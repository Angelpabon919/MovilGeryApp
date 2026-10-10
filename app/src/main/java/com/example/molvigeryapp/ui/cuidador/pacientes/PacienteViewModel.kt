package com.example.molvigeryapp.ui.cuidador.pacientes

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.molvigeryapp.data.model.AplicacionRequest
import com.example.molvigeryapp.data.model.AsignacionPacienteCuidador
import com.example.molvigeryapp.data.model.CuidadoEnfermeria
import com.example.molvigeryapp.data.model.ElementoPaciente
import com.example.molvigeryapp.data.model.FormulacionMedicamento
import com.example.molvigeryapp.data.model.GrupoMedicacion
import com.example.molvigeryapp.data.model.Insumo
import com.example.molvigeryapp.data.model.Inventario
import com.example.molvigeryapp.data.model.Medicamento
import com.example.molvigeryapp.data.model.NotificacionDestinatarioRequest
import com.example.molvigeryapp.data.model.NotificacionRequest
import com.example.molvigeryapp.data.model.Paciente
import com.example.molvigeryapp.data.model.Recomendacion
import com.example.molvigeryapp.data.model.TipoInsumo
import com.example.molvigeryapp.data.repository.PacienteRepository
import com.example.molvigeryapp.data.repository.StockNotificacionesRepository
import kotlinx.coroutines.launch

class PacienteViewModel(
    private val repository: PacienteRepository
) : ViewModel() {

    private val _pacientes = MutableLiveData<List<Paciente>>()
    val pacientes: LiveData<List<Paciente>> get() = _pacientes

    private val _pacientesSeleccionadosHome = MutableLiveData<List<Paciente>>()
    val pacientesSeleccionadosHome: LiveData<List<Paciente>> get() = _pacientesSeleccionadosHome

    private var listaPacientesCompleta: List<Paciente> = emptyList()

    private val _pacienteSeleccionado = MutableLiveData<Paciente?>()
    val pacienteSeleccionado: LiveData<Paciente?> get() = _pacienteSeleccionado

    private val _idBitacora = MutableLiveData<Int?>()
    val idBitacora: LiveData<Int?> get() = _idBitacora

    fun guardarIdBitacora(id: Int) {
        _idBitacora.value = id
    }

    private val _recomendaciones = MutableLiveData<List<Recomendacion>>()
    val recomendaciones: LiveData<List<Recomendacion>> get() = _recomendaciones

    private val _cuidados = MutableLiveData<List<CuidadoEnfermeria>?>()
    val cuidados: LiveData<List<CuidadoEnfermeria>?> = _cuidados

    private val _formulacionesMedicamentos = MutableLiveData<List<FormulacionMedicamento>>()
    val formulacionesMedicamentos: LiveData<List<FormulacionMedicamento>> get() = _formulacionesMedicamentos

    val medicamentosManana = MutableLiveData<List<FormulacionMedicamento>>()
    val medicamentosTarde = MutableLiveData<List<FormulacionMedicamento>>()
    val medicamentosNoche = MutableLiveData<List<FormulacionMedicamento>>()

    fun obtenerMedicamentosPaciente(idPaciente: Int): List<FormulacionMedicamento> {
        val lista = formulacionesMedicamentos.value ?: emptyList()
        return lista.filter { it.idPaciente == idPaciente }
    }

    private val _gruposMedicacion = MutableLiveData<List<GrupoMedicacion>>()
    val gruposMedicacion: LiveData<List<GrupoMedicacion>> get() = _gruposMedicacion

    private val _elementos = MutableLiveData<List<ElementoPaciente>>(emptyList())
    val elementos: LiveData<List<ElementoPaciente>> get() = _elementos
    val elementosPaciente: LiveData<List<ElementoPaciente>> get() = _elementos

    private val _medicamentosCatalogo = MutableLiveData<List<Medicamento>?>()
    val medicamentosCatalogo: LiveData<List<Medicamento>?> get() = _medicamentosCatalogo

    private val _tiposInsumosCatalogo = MutableLiveData<List<TipoInsumo>?>()
    val tiposInsumosCatalogo: LiveData<List<TipoInsumo>?> get() = _tiposInsumosCatalogo
    val tiposInsumos: LiveData<List<TipoInsumo>?> get() = _tiposInsumosCatalogo

    private val _insumosCatalogo = MutableLiveData<List<Insumo>?>()
    val insumosCatalogo: LiveData<List<Insumo>?> get() = _insumosCatalogo
    val insumosPorTipo: LiveData<List<Insumo>?> get() = _insumosCatalogo

    var tienePacientesSeleccionados: Boolean = false
        private set

    private val _registroAplicacionState = MutableLiveData<Result<String>>()
    val registroAplicacionState: LiveData<Result<String>> get() = _registroAplicacionState

    fun cargarPacientes() {
        viewModelScope.launch {
            try {
                if (listaPacientesCompleta.isEmpty()) {
                    listaPacientesCompleta = repository.obtenerPacientes()
                }
                _pacientes.value = listaPacientesCompleta
                if (tienePacientesSeleccionados) {
                    _pacientesSeleccionadosHome.value = listaPacientesCompleta.filter { it.isSelected }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR_REAL", "Error al cargar", e)
                _pacientes.value = emptyList()
                _pacientesSeleccionadosHome.value = emptyList()
            }
        }
    }

    fun confirmarSeleccionDelDia(idUsuarioLogueado: Int) {
        tienePacientesSeleccionados = true
        val seleccionados = listaPacientesCompleta.filter { it.isSelected }
        _pacientesSeleccionadosHome.value = seleccionados

        val fechaActual = java.text.SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        seleccionados.forEach { paciente ->
            paciente.idPaciente?.let { idPaciente ->
                val asignacion = AsignacionPacienteCuidador(
                    idUsuario = idUsuarioLogueado,
                    idPaciente = idPaciente,
                    fechaInicio = fechaActual,
                    fechaFin = fechaActual,
                    estado = "Activo",
                    observaciones = "Asignación desde app Cuidador"
                )
                viewModelScope.launch {
                    try {
                        repository.guardarAsignacion(asignacion)
                    } catch (e: Exception) {
                        Log.e("API_ERROR", "Error al guardar asignacion", e)
                    }
                }
            }
        }
    }

    fun restaurarListaCompleta() {
        tienePacientesSeleccionados = false
        listaPacientesCompleta.forEach { it.isSelected = false }
        _pacientes.value = listaPacientesCompleta
        _pacientesSeleccionadosHome.value = emptyList()
    }

    fun cargarPacientePorId(id: Int) {
        viewModelScope.launch {
            try {
                val paciente = repository.obtenerPacientePorId(id)
                _pacienteSeleccionado.value = paciente
            } catch (e: Exception) {
                Log.e("API_ERROR_REAL", "Error al obtener paciente $id", e)
                _pacienteSeleccionado.value = null
            }
        }
    }

    fun seleccionarPaciente(paciente: Paciente) {
        _pacienteSeleccionado.value = paciente
    }

    fun cargarRecomendaciones(idPaciente: Int) {
        viewModelScope.launch {
            _recomendaciones.value = emptyList()
            val lista = repository.getRecomendaciones(idPaciente)
            _recomendaciones.value = lista ?: emptyList()
        }
    }

    fun guardarRecomendacion(recomendacion: Recomendacion) {
        viewModelScope.launch {
            repository.guardarRecomendacion(recomendacion)
        }
    }

    fun cargarCuidados(idPaciente: Int) {
        viewModelScope.launch {
            val lista = repository.getCuidadosPorPaciente(idPaciente)
            _cuidados.value = lista
        }
    }

    fun guardarCuidado(cuidado: CuidadoEnfermeria) {
        viewModelScope.launch {
            val exito = repository.guardarCuidadoEnfermeria(cuidado)
            if (exito && cuidado.idPaciente != null) {
                cargarCuidados(cuidado.idPaciente)
            }
        }
    }

    fun cargarElementosPaciente(idPaciente: Int) {
        viewModelScope.launch {
            val lista = repository.getElementosPorPaciente(idPaciente)
            _elementos.value = lista
        }
    }

    fun cargarElementosDelPaciente(idPaciente: Int) {
        cargarElementosPaciente(idPaciente)
    }

    fun guardarElementoPaciente(elemento: ElementoPaciente) {
        viewModelScope.launch {
            val exito = repository.guardarElementoPaciente(elemento)
            if (exito) {
                kotlinx.coroutines.delay(300)
                elemento.idPaciente?.let { cargarElementosPaciente(it) }
            } else {
                Log.e("PACIENTE_VM", "Error al guardar el elemento en la API")
            }
        }
    }
    fun registrarAplicacionMedicamento(
        idTratamientoMedicamento: Int,
        idUsuario: Int,
        dosis: String,
        via: String,
        observacion: String,

        idInventario: Int?,
        idElementoPaciente: Int,

        cantidadActual: Int,
        cantidadAplicada: Int,

        idEncargado: Int = 1,
        nombreMedicamento: String = "Medicamento",
        nombrePaciente: String = "Paciente",
        context: Context
    ) {

        viewModelScope.launch {

            try {

                // =============================================
                // VALIDAR STOCK
                // =============================================

                if (cantidadAplicada <= 0) {
                    _registroAplicacionState.value =
                        Result.failure(
                            Exception("La cantidad aplicada debe ser mayor a 0")
                        )
                    return@launch
                }

                if (cantidadAplicada > cantidadActual) {
                    _registroAplicacionState.value =
                        Result.failure(
                            Exception("No hay suficiente stock disponible")
                        )
                    return@launch
                }


                // =============================================
                // CALCULAR NUEVO STOCK
                // =============================================

                val stockRestante =
                    (cantidadActual - cantidadAplicada)
                        .coerceAtLeast(0)


                // =============================================
                // FECHA ACTUAL
                // =============================================

                val fechaActual =
                    java.text.SimpleDateFormat(
                        "yyyy-MM-dd'T'HH:mm:ss'Z'",
                        java.util.Locale.getDefault()
                    ).format(java.util.Date())


                val idPaciente =
                    _pacienteSeleccionado.value?.idPaciente

                if (idPaciente == null) {

                    _registroAplicacionState.value =
                        Result.failure(
                            Exception("No hay paciente seleccionado")
                        )

                    return@launch
                }
                // =============================================
// BUSCAR INVENTARIO CORRECTO
// =============================================

                val listaInventario =
                    repository.obtenerInventario()

                val inventarioEncontrado =
                    if (idInventario != null) {

                        listaInventario.find {
                            it.idInventario == idInventario
                        }

                    } else {

                        // 1. Primero buscar por el elemento exacto del paciente
                        listaInventario.find {
                            it.idElemento == idElementoPaciente &&
                                    it.idPaciente == idPaciente
                        }

                        // 2. Si no aparece, buscar por paciente + medicamento
                            ?: listaInventario.find {
                                it.idPaciente == idPaciente &&
                                        it.idMedicamentos == idTratamientoMedicamento
                            }
                    }

                val idInventarioValido =
                    idInventario
                        ?: inventarioEncontrado?.idInventario

                if (idInventarioValido == null) {

                    _registroAplicacionState.value =
                        Result.failure(
                            Exception(
                                "No se encontró un inventario asociado al medicamento"
                            )
                        )

                    return@launch
                }


                // =============================================
                // REGISTRAR APLICACIÓN
                // =============================================

                val request = AplicacionRequest(

                    idPaciente = idPaciente,

                    idMedicamento =
                        idTratamientoMedicamento,

                    idInventario =
                        idInventarioValido,

                    idUsuario =
                        idUsuario,

                    fechaHora =
                        fechaActual,

                    dosisAdministrada =
                        dosis,

                    viaAdministracion =
                        via,

                    estado =
                        true,

                    observacion =
                        observacion,

                    cantidadAplicada =
                        cantidadAplicada
                )


                val resultadoAplicacion =
                    repository.registrarAplicacionMedicamento(
                        request
                    )


                if (resultadoAplicacion.isFailure) {

                    _registroAplicacionState.value =
                        Result.failure(
                            resultadoAplicacion.exceptionOrNull()
                                ?: Exception(
                                    "Error al registrar la aplicación"
                                )
                        )

                    return@launch
                }


                // =============================================
                // ACTUALIZAR INVENTARIO
                // =============================================

                val inventarioActualizado =
                    repository.actualizarStockInventario(
                        idInventarioValido,
                        stockRestante
                    )


                // =============================================
                // ACTUALIZAR ELEMENTO DEL PACIENTE
                // =============================================

                val elementoActualizado =
                    repository.actualizarCantidadElemento(
                        idElementoPaciente,
                        stockRestante
                    )


                Log.d(
                    "MEDICAMENTO_STOCK",
                    """
                Aplicación registrada
                Medicamento: $nombreMedicamento
                Stock anterior: $cantidadActual
                Cantidad aplicada: $cantidadAplicada
                Stock restante: $stockRestante
                Inventario actualizado: $inventarioActualizado
                Elemento actualizado: $elementoActualizado
                """.trimIndent()
                )


                // =============================================
                // COMPROBAR ACTUALIZACIONES
                // =============================================

                if (!inventarioActualizado ||
                    !elementoActualizado
                ) {

                    _registroAplicacionState.value =
                        Result.failure(
                            Exception(
                                "La aplicación fue registrada, " +
                                        "pero no se pudo actualizar todo el inventario"
                            )
                        )

                    return@launch
                }


                // =============================================
// STOCK BAJO
// =============================================

                if (stockRestante <= 3) {

                    try {

                        val notificacionEnviada =
                            StockNotificacionesRepository()
                                .enviarAlertaStockBajo(
                                    nombreInsumo = nombreMedicamento,
                                    stockActual = stockRestante
                                )

                        if (!notificacionEnviada) {
                            Log.e(
                                "NOTIF_ERROR",
                                "No se pudo enviar la alerta de stock bajo"
                            )
                        }

                    } catch (e: Exception) {

                        Log.e(
                            "NOTIF_ERROR",
                            "Error al enviar alerta de stock bajo",
                            e
                        )
                    }
                }

                // =============================================
                // RECARGAR STOCK
                // =============================================

                cargarElementosPaciente(
                    idPaciente
                )


                // =============================================
                // ÉXITO
                // =============================================

                _registroAplicacionState.value =
                    Result.success(
                        "Aplicación registrada. Stock: " +
                                "$cantidadActual → $stockRestante"
                    )


            } catch (e: Exception) {

                Log.e(
                    "APLICACION_MEDICAMENTO",
                    "Error registrando medicamento",
                    e
                )

                _registroAplicacionState.value =
                    Result.failure(e)
            }
        }
    }
    fun cargarCatalogoMedicamentos() {
        viewModelScope.launch {
            val lista = repository.getMedicamentos()
            _medicamentosCatalogo.value = lista
        }
    }
    fun actualizarMedicamentosPorPacientesSeleccionados() {

        val formulaciones =
            _formulacionesMedicamentos.value ?: emptyList()

        val grupos =
            _gruposMedicacion.value ?: emptyList()

        val idsSeleccionados =
            _pacientesSeleccionadosHome.value
                ?.mapNotNull { it.idPaciente }
                ?: emptyList()

        val formulacionesSeleccionadas =
            formulaciones.filter { formulacion ->

                formulacion.idPaciente != null &&
                        formulacion.idPaciente in idsSeleccionados &&
                        formulacion.actualAdministrado != false
            }

        separarMedicamentosPorHorario(
            formulacionesSeleccionadas,
            grupos
        )
    }
    fun separarMedicamentosPorHorario(
        lista: List<FormulacionMedicamento>,
        grupos: List<GrupoMedicacion> = emptyList()
    ) {

        val horaActual = 8

        val medicamentosAhora = lista.filter { medicamento ->

            val grupo = grupos.find {
                it.idGrupo == medicamento.idGrupo
            } ?: return@filter false

            val horaBase = grupo.horaAdministracion
                ?.substringBefore(":")
                ?.toIntOrNull()
                ?: return@filter false

            when (medicamento.idGrupo) {

                // Ayunas -> hora configurada, actualmente 07:00
                1 -> {
                    horaActual == horaBase
                }

                // Cada 12 horas -> hora base y 12 horas después
                // Si base es 08:00 => 08:00 y 20:00
                2 -> {
                    val segundaHora = (horaBase + 12) % 24

                    horaActual == horaBase ||
                            horaActual == segundaHora
                }

                // Cada 24 horas -> una vez al día
                3 -> {
                    horaActual == horaBase
                }

                // Anticoagulados -> hora configurada
                4 -> {
                    horaActual == horaBase
                }

                else -> {
                    horaActual == horaBase
                }
            }
        }


        val manana = medicamentosAhora.filter { medicamento ->

            val grupo = grupos.find {
                it.idGrupo == medicamento.idGrupo
            } ?: return@filter false

            val horaBase = grupo.horaAdministracion
                ?.substringBefore(":")
                ?.toIntOrNull()
                ?: return@filter false

            val horaCorrespondiente =
                if (
                    medicamento.idGrupo == 2 &&
                    horaActual == ((horaBase + 12) % 24)
                ) {
                    (horaBase + 12) % 24
                } else {
                    horaBase
                }

            horaCorrespondiente in 6..11
        }


        val tarde = medicamentosAhora.filter { medicamento ->

            val grupo = grupos.find {
                it.idGrupo == medicamento.idGrupo
            } ?: return@filter false

            val horaBase = grupo.horaAdministracion
                ?.substringBefore(":")
                ?.toIntOrNull()
                ?: return@filter false

            val horaCorrespondiente =
                if (
                    medicamento.idGrupo == 2 &&
                    horaActual == ((horaBase + 12) % 24)
                ) {
                    (horaBase + 12) % 24
                } else {
                    horaBase
                }

            horaCorrespondiente in 12..17
        }


        val noche = medicamentosAhora.filter { medicamento ->

            val grupo = grupos.find {
                it.idGrupo == medicamento.idGrupo
            } ?: return@filter false

            val horaBase = grupo.horaAdministracion
                ?.substringBefore(":")
                ?.toIntOrNull()
                ?: return@filter false

            val horaCorrespondiente =
                if (
                    medicamento.idGrupo == 2 &&
                    horaActual == ((horaBase + 12) % 24)
                ) {
                    (horaBase + 12) % 24
                } else {
                    horaBase
                }

            horaCorrespondiente >= 18 ||
                    horaCorrespondiente < 6
        }


        medicamentosManana.value = manana
        medicamentosTarde.value = tarde
        medicamentosNoche.value = noche


        android.util.Log.d(
            "HORARIO_FINAL",
            "HORA=$horaActual | MAÑANA=$manana | TARDE=$tarde | NOCHE=$noche"
        )
    }

    fun cargarFormulacionesMedicamentos() {

        viewModelScope.launch {

            try {

                val lista = repository.getFormulacionesMedicamentos()
                val listaMedicamentos = lista ?: emptyList()

                _formulacionesMedicamentos.value = listaMedicamentos

                actualizarMedicamentosPorPacientesSeleccionados()

            } catch (e: Exception) {

                android.util.Log.e(
                    "FORMULACION_ERROR",
                    "Error cargando formulaciones",
                    e
                )

                _formulacionesMedicamentos.value = emptyList()

                medicamentosManana.value = emptyList()
                medicamentosTarde.value = emptyList()
                medicamentosNoche.value = emptyList()
            }
        }
    }
    fun cargarGruposMedicacion() {

        viewModelScope.launch {

            try {

                val lista = repository.getGrupoMedicacion()

                _gruposMedicacion.value = lista ?: emptyList()

                actualizarMedicamentosPorPacientesSeleccionados()

            } catch (e: Exception) {

                android.util.Log.e(
                    "GRUPO_MEDICACION_ERROR",
                    "Error cargando grupos",
                    e
                )

                _gruposMedicacion.value = emptyList()
            }
        }
    }


    fun cargarTiposInsumos() {
        viewModelScope.launch {
            val lista = repository.getTiposInsumos()
            _tiposInsumosCatalogo.value = lista
        }
    }

    fun cargarInsumosPorTipo(idTipoInsumo: Int) {
        viewModelScope.launch {
            val lista = repository.getInsumosPorTipo(idTipoInsumo)
            _insumosCatalogo.value = lista
        }
    }
}
