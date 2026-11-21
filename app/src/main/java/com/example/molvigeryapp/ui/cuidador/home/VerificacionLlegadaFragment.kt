package com.example.molvigeryapp.ui.cuidador.llegada

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.databinding.FragmentVerificacionLlegadaBinding
import com.example.molvigeryapp.ui.cuidador.CuidadorLocationOverlay
import com.example.molvigeryapp.ui.cuidador.home.BienvenidaSedeCuidadorFragment
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class VerificacionLlegadaFragment : Fragment() {

    private var _binding: FragmentVerificacionLlegadaBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var locationCallback: LocationCallback

    private var turnosDesplegados = false
    private var ubicacionInicializada = false
    private var navegacionRealizada = false
    private var actualizacionesUbicacionActivas = false
    private var asignacionCargada = false
    private var idAsignacionActiva: Int? = null
    private var asignacionActiva: AsignacionTurnoUsuario? = null
    private var ubicacionDentroRadio = false
    private var registroEnProgreso = false

    // =========================================================
    // UBICACIÓN DE PRUEBA
    // =========================================================

    private val latitudDestino = 2.426545
    private val longitudDestino = -76.603616

    private val radioPermitido = 100.0

    private var marcadorDestino: Marker? = null
    private var circuloGeocerca: Polygon? = null

    // =========================================================
    // PERMISOS
    // =========================================================

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->

            val permisoPreciso =
                permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val permisoAproximado =
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (permisoPreciso || permisoAproximado) {

                if (asignacionCargada && idAsignacionActiva != null && !navegacionRealizada) {
                    comprobarEstadoGpsAlRegresar()
                }

            } else {

                val safeBinding = _binding ?: return@registerForActivityResult

                ubicacionDentroRadio = false
                actualizarEstadoBotonLlegada()

                safeBinding.tvEsperandoUbicacion.visibility = View.VISIBLE
                safeBinding.tvEsperandoUbicacion.text =
                    "Permiso de ubicación denegado"

                safeBinding.tvEstadoLlegada.text =
                    "Necesitamos tu ubicación para verificar la llegada"
            }
        }

    private data class TurnoAgrupado(
        val turno: Turno,
        val fechaInicio: String,
        val fechaFin: String,
        val estado: String
    )

    // =========================================================
    // CREAR VISTA
    // =========================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentVerificacionLlegadaBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    // =========================================================
    // INICIALIZAR FRAGMENT
    // =========================================================

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { vista, insets ->

            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.updatePadding(
                left = barras.left,
                right = barras.right,
                bottom = barras.bottom
            )

            insets
        }

        binding.btnZoomMas.setOnClickListener {
            binding.mapa.controller.zoomIn()
        }

        binding.btnZoomMenos.setOnClickListener {
            binding.mapa.controller.zoomOut()
        }

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(
                requireActivity()
            )

        binding.btnMiUbicacion.setOnClickListener {
            centrarEnMiUbicacion()
        }

        binding.btnYaLlegue.setOnClickListener {
            confirmarLlegada()
        }

        configurarMapa()

        configurarPantalla()

        configurarTarjetaTurnos()

        cargarTurnosPendientes()
    }

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onStart() {
        super.onStart()

        if (
            _binding != null &&
            asignacionCargada &&
            idAsignacionActiva != null &&
            !navegacionRealizada &&
            tienePermisoUbicacion()
        ) {
            comprobarEstadoGpsAlRegresar()
        }
    }

    override fun onResume() {
        super.onResume()

        _binding?.mapa?.onResume()

        // Detectar si el cuidador activó el GPS
        // mientras estaba fuera de GerIApp.
        comprobarEstadoGpsAlRegresar()
    }

    override fun onStop() {

        detenerActualizacionesUbicacion()

        super.onStop()
    }

    override fun onPause() {

        _binding?.mapa?.onPause()

        super.onPause()
    }

    // =========================================================
    // CONFIGURAR MAPA
    // =========================================================

    private fun configurarMapa() {

        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(
                requireContext()
            )
        )

        Configuration.getInstance().setUserAgentValue(
            "GerIApp/1.0"
        )

        val cartoApiKey = "cb1_51jx_1_a6886e91076599e58b727063"

        val cartoPositron = XYTileSource(
            "CARTO_Positron",
            0,
            20,
            256,
            ".png?key=$cartoApiKey",
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/light_all/",
                "https://b.basemaps.cartocdn.com/rastertiles/light_all/",
                "https://c.basemaps.cartocdn.com/rastertiles/light_all/",
                "https://d.basemaps.cartocdn.com/rastertiles/light_all/"
            ),
            "© OpenStreetMap contributors © CARTO"
        )

        binding.mapa.setTileSource(cartoPositron)

        // =====================================================
        // INICIALIZAR OVERLAY DE UBICACIÓN GPS
        // =====================================================

        locationOverlay = CuidadorLocationOverlay(
            requireContext(),
            binding.mapa
        ).apply {
            enableMyLocation()
        }

        binding.mapa.overlays.add(locationOverlay)

        binding.mapa.setMultiTouchControls(true)

        binding.mapa.setBuiltInZoomControls(false)

        binding.mapa.setFlingEnabled(true)

        binding.mapa.setMultiTouchControls(true)

        // =====================================================
        // PRIORIDAD DE GESTOS PARA EL MAPA
        // =====================================================

        binding.mapa.setOnTouchListener { vista, evento ->

            when (evento.action) {

                android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_MOVE -> {

                    vista.parent.requestDisallowInterceptTouchEvent(true)
                }

                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {

                    vista.parent.requestDisallowInterceptTouchEvent(false)
                }
            }

            false
        }

        binding.mapa.setMinZoomLevel(5.0)
        binding.mapa.setMaxZoomLevel(20.0)

        binding.mapa.controller.setZoom(17.0)

        // =====================================================
        // DESTINO
        // =====================================================

        val puntoDestino = GeoPoint(
            latitudDestino,
            longitudDestino
        )

        val nuevoMarcador = Marker(binding.mapa).apply {

            position = puntoDestino

            icon = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.marcador_sede
            )

            title = "Sede asignada"

            snippet = "Radio permitido: 100 metros"

            setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            )
        }

        marcadorDestino = nuevoMarcador

        // =====================================================
        // GEOCERCA
        // =====================================================

        val nuevoCirculo = Polygon().apply {

            points = Polygon.pointsAsCircle(
                puntoDestino,
                radioPermitido
            )

            fillPaint.color = Color.argb(
                45,
                59,
                91,
                219
            )

            outlinePaint.color = Color.parseColor(
                "#3B5BDB"
            )

            outlinePaint.strokeWidth = 3f
        }

        circuloGeocerca = nuevoCirculo

        binding.mapa.overlays.add(nuevoCirculo)
        binding.mapa.overlays.add(nuevoMarcador)

        binding.mapa.invalidate()
    }

    // =========================================================
    // CENTRAR EN MI UBICACIÓN
    // =========================================================

    private fun centrarEnMiUbicacion() {

        val ubicacionActual = locationOverlay.myLocation

        if (ubicacionActual != null) {

            binding.mapa.controller.animateTo(
                GeoPoint(
                    ubicacionActual.latitude,
                    ubicacionActual.longitude
                )
            )

            binding.mapa.controller.setZoom(18.0)

        } else {

            Toast.makeText(
                requireContext(),
                "Buscando tu ubicación GPS...",
                Toast.LENGTH_SHORT
            ).show()

            comprobarEstadoGpsAlRegresar()
        }
    }

    // =========================================================
    // CONFIGURACIÓN INICIAL
    // =========================================================

    private fun configurarPantalla() {

        asignacionCargada = false
        idAsignacionActiva = null
        asignacionActiva = null
        ubicacionDentroRadio = false
        registroEnProgreso = false

        binding.tvEstadoLlegada.text =
            "Verificando turno..."

        binding.tvEsperandoUbicacion.visibility =
            View.VISIBLE

        binding.tvEsperandoUbicacion.text =
            "Consultando turno asignado..."

        binding.tvDistancia.text =
            "Calculando..."

        actualizarEstadoBotonLlegada()
    }

    private fun actualizarEstadoBotonLlegada() {
        val safeBinding = _binding ?: return

        val puedeRegistrar =
            asignacionCargada &&
                idAsignacionActiva != null &&
                ubicacionDentroRadio &&
                !registroEnProgreso &&
                !navegacionRealizada

        safeBinding.btnYaLlegue.isEnabled = puedeRegistrar
        safeBinding.btnYaLlegue.isClickable = puedeRegistrar

        if (registroEnProgreso) {
            safeBinding.btnYaLlegue.text = ""
            safeBinding.btnYaLlegue.alpha = 0.85f
            safeBinding.progressRegistroLlegada.visibility = View.VISIBLE
        } else {
            safeBinding.btnYaLlegue.text = "Ya llegué"
            safeBinding.btnYaLlegue.alpha = if (puedeRegistrar) 1f else 0.45f
            safeBinding.progressRegistroLlegada.visibility = View.GONE
        }
    }

    private fun mostrarSinTurnoActivo(mensajeUbicacion: String) {
        val safeBinding = _binding ?: return

        ubicacionDentroRadio = false
        safeBinding.tvHoraProgramada.text = "Sin turno programado"
        safeBinding.tvEstadoLlegada.text = "Sin turno activo"
        safeBinding.tvEsperandoUbicacion.visibility = View.VISIBLE
        safeBinding.tvEsperandoUbicacion.text = mensajeUbicacion
        safeBinding.tvDistancia.text = "--"

        actualizarEstadoBotonLlegada()
    }

    // =========================================================
    // TARJETA DE TURNOS
    // =========================================================

    private fun configurarTarjetaTurnos() {

        binding.containerTurnos.visibility = View.GONE

        binding.tvSinTurnos.visibility = View.GONE

        binding.btnExpandirTurnos.setImageResource(
            R.drawable.ic_arrow_down
        )

        turnosDesplegados = false

        binding.layoutTituloTurnos.setOnClickListener {

            turnosDesplegados = !turnosDesplegados

            if (turnosDesplegados) {

                binding.containerTurnos.visibility =
                    View.VISIBLE

                binding.btnExpandirTurnos.setImageResource(
                    R.drawable.ic_arrow_up
                )

            } else {

                binding.containerTurnos.visibility =
                    View.GONE

                binding.btnExpandirTurnos.setImageResource(
                    R.drawable.ic_arrow_down
                )
            }
        }
    }

    // =========================================================
    // CARGAR TURNOS PENDIENTES
    // =========================================================

    private fun cargarTurnosPendientes() {

        val propietarioVista = viewLifecycleOwner

        asignacionCargada = false
        idAsignacionActiva = null
        asignacionActiva = null
        ubicacionDentroRadio = false
        actualizarEstadoBotonLlegada()

        propietarioVista.lifecycleScope.launch {

            try {

                val preferences =
                    requireContext().getSharedPreferences(
                        "SESION",
                        Context.MODE_PRIVATE
                    )

                val idUsuario =
                    preferences.getInt("ID_USUARIO", -1)

                if (idUsuario <= 0) {

                    asignacionCargada = true

                    if (
                        _binding != null &&
                        propietarioVista.lifecycle.currentState.isAtLeast(
                            Lifecycle.State.STARTED
                        )
                    ) {
                        detenerActualizacionesUbicacion()
                        mostrarSinTurnoActivo("No se identificó el usuario autenticado")
                        mostrarSinTurnos()
                    }

                    return@launch
                }

                val datos = withContext(Dispatchers.IO) {

                    val asignaciones =
                        RetrofitClient.apiService.getAsignacionesTurno()

                    val turnos =
                        RetrofitClient.apiService.getTurnos()

                    Pair(asignaciones, turnos)
                }

                if (
                    _binding == null ||
                    !propietarioVista.lifecycle.currentState.isAtLeast(
                        Lifecycle.State.STARTED
                    )
                ) {
                    return@launch
                }

                val asignacionResuelta =
                    LlegadaTurnoHelper.resolverAsignacionActivaParaLlegada(
                        idUsuario = idUsuario,
                        asignaciones = datos.first,
                        turnos = datos.second
                    )

                asignacionCargada = true
                asignacionActiva = asignacionResuelta?.asignacion
                idAsignacionActiva = asignacionResuelta?.asignacion?.id_asignacion_turno_usuario

                // Actualizar horario programado del turno activo
                actualizarHorarioProgramado(asignacionResuelta)

                // Mostrar turnos pendientes/activos
                val turnosPendientes =
                    obtenerTurnosPendientes(
                        idUsuario,
                        datos.first,
                        datos.second
                    )

                mostrarTurnos(turnosPendientes)

                if (idAsignacionActiva != null) {
                    _binding?.tvEstadoLlegada?.text = "Esperando llegada..."
                    actualizarEstadoBotonLlegada()
                    verificarPermisoUbicacion()
                } else {
                    detenerActualizacionesUbicacion()
                    mostrarSinTurnoActivo("No tienes un turno activo para registrar llegada")
                }

            } catch (e: kotlinx.coroutines.CancellationException) {

                throw e

            } catch (e: Exception) {

                Log.e(
                    "VERIFICACION_LLEGADA",
                    "Error cargando turnos pendientes",
                    e
                )

                asignacionCargada = true
                idAsignacionActiva = null
                asignacionActiva = null

                if (
                    _binding != null &&
                    propietarioVista.lifecycle.currentState.isAtLeast(
                        Lifecycle.State.STARTED
                    )
                ) {
                    detenerActualizacionesUbicacion()
                    mostrarSinTurnoActivo("No se pudo consultar tu turno asignado")
                    mostrarSinTurnos()
                }
            }
        }
    }

    // =========================================================
    // FILTRAR TURNOS
    // =========================================================

    private fun obtenerTurnosPendientes(
        idUsuario: Int,
        asignaciones: List<AsignacionTurnoUsuario>,
        turnos: List<Turno>
    ): List<TurnoAgrupado> {

        val formatoFecha = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )

        val calendarioHoy = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val fechaInicioLimite = calendarioHoy.time

        val calendarioFin = calendarioHoy.clone() as Calendar
        calendarioFin.add(Calendar.DAY_OF_YEAR, 14)

        val fechaFinLimite = calendarioFin.time

        val turnosMap = turnos
            .filter { it.id_turno != null && it.estado }
            .associateBy { it.id_turno }

        val asignacionesValidas = asignaciones
            .filter { it.id_usuario == idUsuario }
            .filter {
                LlegadaTurnoHelper.esEstadoElegibleParaLlegada(it.estado)
            }
            .mapNotNull { asignacion ->

                val fechaTexto = asignacion.fecha?.take(10)
                    ?: return@mapNotNull null

                val fecha = try {
                    formatoFecha.parse(fechaTexto)
                } catch (e: Exception) {
                    null
                } ?: return@mapNotNull null

                val turno = turnosMap[asignacion.id_turno]
                    ?: return@mapNotNull null

                if (
                    fecha.before(fechaInicioLimite) ||
                    fecha.after(fechaFinLimite)
                ) {
                    return@mapNotNull null
                }

                Triple(asignacion, turno, fecha)
            }
            .sortedBy { it.third }

        if (asignacionesValidas.isEmpty()) {
            return emptyList()
        }

        val grupos = mutableListOf<TurnoAgrupado>()

        var turnoActual = asignacionesValidas.first().second
        var fechaInicioGrupo = asignacionesValidas.first().third
        var fechaFinGrupo = asignacionesValidas.first().third
        var estadoGrupo = asignacionesValidas.first().first.estado ?: "Asignado"

        for (i in 1 until asignacionesValidas.size) {

            val actual = asignacionesValidas[i]
            val turno = actual.second
            val fecha = actual.third

            val calendarioAnterior = Calendar.getInstance().apply {
                time = fechaFinGrupo
            }

            calendarioAnterior.add(Calendar.DAY_OF_YEAR, 1)

            val esConsecutivo =
                formatoFecha.format(calendarioAnterior.time) ==
                        formatoFecha.format(fecha)

            val mismoTurno =
                turno.id_turno == turnoActual.id_turno

            if (esConsecutivo && mismoTurno) {

                fechaFinGrupo = fecha

            } else {

                grupos.add(
                    TurnoAgrupado(
                        turno = turnoActual,
                        fechaInicio = formatoFecha.format(fechaInicioGrupo),
                        fechaFin = formatoFecha.format(fechaFinGrupo),
                        estado = estadoGrupo
                    )
                )

                turnoActual = turno
                fechaInicioGrupo = fecha
                fechaFinGrupo = fecha
                estadoGrupo = actual.first.estado ?: "Asignado"
            }
        }

        grupos.add(
            TurnoAgrupado(
                turno = turnoActual,
                fechaInicio = formatoFecha.format(fechaInicioGrupo),
                fechaFin = formatoFecha.format(fechaFinGrupo),
                estado = estadoGrupo
            )
        )

        return grupos
    }

    // =========================================================
    // HORARIO PROGRAMADO DEL DÍA ACTUAL
    // =========================================================

    private fun actualizarHorarioProgramado(
        asignacionResuelta: LlegadaTurnoHelper.AsignacionActivaLlegada?
    ) {

        val safeBinding = _binding ?: return

        if (asignacionResuelta == null) {

            safeBinding.tvHoraProgramada.text =
                "Sin turno programado"

            return
        }

        safeBinding.tvHoraProgramada.text =
            formatearHoraAMPM(asignacionResuelta.turno.hora_inicio)
    }

    // =========================================================
    // MOSTRAR TURNOS
    // =========================================================

    private fun mostrarTurnos(
        turnosPendientes: List<TurnoAgrupado>
    ) {

        val safeBinding = _binding ?: return

        safeBinding.containerTurnos.removeAllViews()

        if (turnosPendientes.isEmpty()) {
            mostrarSinTurnos()
            return
        }

        safeBinding.tvSinTurnos.visibility = View.GONE

        turnosPendientes.forEach { grupo ->

            val tarjeta = crearTarjetaTurno(grupo)

            val bindingActual = _binding ?: return

            bindingActual.containerTurnos.addView(tarjeta)
        }
    }

    // =========================================================
    // CREAR TARJETA DE TURNO
    // =========================================================

    private fun crearTarjetaTurno(
        grupo: TurnoAgrupado
    ): View {

        val contexto = requireContext()
        val turno = grupo.turno

        val tarjeta = LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.parseColor("#E9ECEF"))
            }

            elevation = dp(3).toFloat()
        }

        tarjeta.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }

        // ENCABEZADO

        val encabezado = LinearLayout(contexto).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val horaInicio = turno.hora_inicio ?: ""

        val hora = try {
            SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .parse(horaInicio)
        } catch (e: Exception) {
            try {
                SimpleDateFormat("HH:mm", Locale.getDefault())
                    .parse(horaInicio)
            } catch (e: Exception) {
                null
            }
        }

        val horaDelDia = Calendar.getInstance().apply {
            if (hora != null) time = hora
        }.get(Calendar.HOUR_OF_DAY)

        val esDiurno = horaDelDia >= 6 && horaDelDia < 18

        val tipoTurno = if (esDiurno) "DIURNO" else "NOCTURNO"

        val iconoTurno = if (esDiurno) "☀" else "☾"

        val tvTipo = crearTextoTurno(
            "$iconoTurno  $tipoTurno",
            17f,
            "#3B5BDB",
            true
        )

        encabezado.addView(
            tvTipo,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        val tvEstado = crearTextoTurno(
            grupo.estado,
            11f,
            "#3B5BDB",
            true
        ).apply {
            setPadding(dp(10), dp(6), dp(10), dp(6))

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#EDF3FF"))
                cornerRadius = dp(20).toFloat()
            }
        }

        encabezado.addView(tvEstado)
        tarjeta.addView(encabezado)

        // SEPARADOR

        tarjeta.addView(View(contexto).apply {
            setBackgroundColor(Color.parseColor("#E9ECEF"))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(12)
                bottomMargin = dp(14)
            }
        })

        // FECHAS

        val filaFechas = LinearLayout(contexto).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val columnaInicio = LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
        }

        columnaInicio.addView(
            crearTextoTurno(
                "FECHA INICIO",
                11f,
                "#64748B",
                true
            )
        )

        columnaInicio.addView(
            crearTextoTurno(
                formatearFecha(grupo.fechaInicio),
                15f,
                "#202124",
                true
            ).apply {
                setPadding(0, dp(6), 0, 0)
            }
        )

        val columnaFin = LinearLayout(contexto).apply {
            orientation = LinearLayout.VERTICAL
        }

        columnaFin.addView(
            crearTextoTurno(
                "FECHA FIN",
                11f,
                "#64748B",
                true
            )
        )

        columnaFin.addView(
            crearTextoTurno(
                formatearFecha(grupo.fechaFin),
                15f,
                "#202124",
                true
            ).apply {
                setPadding(0, dp(6), 0, 0)
            }
        )

        filaFechas.addView(
            columnaInicio,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        filaFechas.addView(
            columnaFin,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        tarjeta.addView(filaFechas)

        // HORARIO

        val contenedorHorario = LinearLayout(contexto).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))

            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.parseColor("#EDF3FF"))
                cornerRadius = dp(12).toFloat()
            }
        }

        contenedorHorario.addView(
            crearTextoTurno(
                "◷  HORARIO",
                13f,
                "#3B5BDB",
                true
            ),
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        contenedorHorario.addView(
            crearTextoTurno(
                "${formatearHoraAMPM(turno.hora_inicio)} - " +
                        formatearHoraAMPM(turno.hora_fin),
                14f,
                "#3B5BDB",
                true
            )
        )

        tarjeta.addView(contenedorHorario.apply {

            val parametros = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            parametros.topMargin = dp(14)
            layoutParams = parametros
        })

        return tarjeta
    }

    private fun crearTextoTurno(
        texto: String,
        tamano: Float,
        color: String,
        negrita: Boolean
    ): TextView {

        return TextView(requireContext()).apply {
            text = texto
            textSize = tamano
            setTextColor(Color.parseColor(color))

            if (negrita) {
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }
    }

    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    // =========================================================
    // SIN TURNOS
    // =========================================================

    private fun mostrarSinTurnos() {

        val safeBinding = _binding ?: return

        safeBinding.containerTurnos.removeAllViews()

        safeBinding.tvSinTurnos.visibility =
            View.VISIBLE

        safeBinding.containerTurnos.addView(
            safeBinding.tvSinTurnos
        )
    }

    // =========================================================
    // FORMATEAR FECHA
    // =========================================================

    private fun formatearFecha(
        fecha: String?
    ): String {

        if (fecha.isNullOrBlank()) {
            return "Fecha no disponible"
        }

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
                )

            val formatoSalida =
                SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

            val fechaConvertida =
                formatoEntrada.parse(fecha)

            formatoSalida.format(
                fechaConvertida!!
            )

        } catch (e: Exception) {

            fecha
        }
    }

    // =========================================================
    // FORMATEAR HORA
    // =========================================================

    private fun formatearHora(
        hora: String?
    ): String {

        if (hora.isNullOrBlank()) {
            return "--:--"
        }

        return try {

            val formatoEntrada =
                SimpleDateFormat(
                    "HH:mm:ss",
                    Locale.getDefault()
                )

            val formatoSalida =
                SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

            val horaConvertida =
                formatoEntrada.parse(hora)

            formatoSalida.format(
                horaConvertida!!
            )

        } catch (e: Exception) {

            hora.take(5)
        }
    }

    // =========================================================
    // FORMATO DE HORA AM / PM
    // =========================================================

    private fun formatearHoraAMPM(
        hora: String?
    ): String {

        if (hora.isNullOrBlank()) {
            return "Sin turno programado"
        }

        return try {

            val formatoEntrada = SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            )

            val formatoSalida = SimpleDateFormat(
                "h:mm a",
                Locale.US
            )

            val horaConvertida =
                formatoEntrada.parse(hora)

            formatoSalida.format(
                horaConvertida!!
            )

        } catch (e: Exception) {

            try {

                val formatoEntrada = SimpleDateFormat(
                    "HH:mm",
                    Locale.getDefault()
                )

                val formatoSalida = SimpleDateFormat(
                    "h:mm a",
                    Locale.US
                )

                formatoSalida.format(
                    formatoEntrada.parse(hora)!!
                )

            } catch (e: Exception) {

                "Sin turno programado"
            }
        }
    }

    // =========================================================
    // VERIFICAR PERMISO
    // =========================================================

    private fun verificarPermisoUbicacion() {

        if (tienePermisoUbicacion()) {

            comprobarEstadoGpsAlRegresar()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun tienePermisoUbicacion(): Boolean {

        val contexto = context ?: return false

        val permisoPreciso =
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        return permisoPreciso || permisoAproximado
    }

    // =========================================================
    // COMPROBAR SI EL GPS ESTÁ ACTIVADO
    // =========================================================

    private fun gpsEstaActivado(): Boolean {

        val contexto = context ?: return false

        val locationManager =
            contexto.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        return LocationManagerCompat.isLocationEnabled(
            locationManager
        )
    }

    // =========================================================
    // RECUPERAR GPS AL REGRESAR A LA APLICACIÓN
    // =========================================================

    private fun comprobarEstadoGpsAlRegresar() {

        val safeBinding = _binding ?: return

        if (!asignacionCargada || idAsignacionActiva == null || navegacionRealizada) {
            return
        }

        // Comprobar permisos
        if (!tienePermisoUbicacion()) {

            detenerActualizacionesUbicacion()
            ubicacionDentroRadio = false
            actualizarEstadoBotonLlegada()

            safeBinding.tvEsperandoUbicacion.visibility =
                View.VISIBLE

            safeBinding.tvEsperandoUbicacion.text =
                "Necesitamos permiso para acceder a tu ubicación"

            safeBinding.tvEstadoLlegada.text =
                "Permiso de ubicación requerido"

            return
        }

        // Comprobar GPS
        if (!gpsEstaActivado()) {

            detenerActualizacionesUbicacion()
            ubicacionDentroRadio = false
            actualizarEstadoBotonLlegada()

            safeBinding.tvEsperandoUbicacion.visibility =
                View.VISIBLE

            safeBinding.tvEsperandoUbicacion.text =
                "Activa la ubicación GPS para verificar tu llegada"

            safeBinding.tvEstadoLlegada.text =
                "Esperando activación del GPS"

            safeBinding.tvDistancia.text =
                "Esperando ubicación..."

            return
        }

        // Iniciar nuevamente si no existen actualizaciones activas
        if (!actualizacionesUbicacionActivas) {

            safeBinding.tvEsperandoUbicacion.visibility =
                View.VISIBLE

            safeBinding.tvEsperandoUbicacion.text =
                "GPS activado. Actualizando ubicación..."

            iniciarActualizacionesUbicacion()
        }
    }

    // =========================================================
    // ACTUALIZACIÓN AUTOMÁTICA GPS
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun iniciarActualizacionesUbicacion() {

        val safeBinding = _binding ?: return

        if (!asignacionCargada || idAsignacionActiva == null || navegacionRealizada) return

        if (!tienePermisoUbicacion()) return

        if (!gpsEstaActivado()) {

            ubicacionDentroRadio = false
            actualizarEstadoBotonLlegada()

            safeBinding.tvEsperandoUbicacion.visibility =
                View.VISIBLE

            safeBinding.tvEsperandoUbicacion.text =
                "Activa la ubicación GPS para continuar"

            safeBinding.tvEstadoLlegada.text =
                "Esperando activación del GPS"

            return
        }

        // Evitar solicitudes duplicadas
        if (actualizacionesUbicacionActivas) return

        if (!::locationCallback.isInitialized) {

            locationCallback = object : LocationCallback() {

                override fun onLocationResult(
                    resultado: LocationResult
                ) {

                    if (_binding == null) return

                    if (!gpsEstaActivado()) {

                        detenerActualizacionesUbicacion()
                        ubicacionDentroRadio = false
                        actualizarEstadoBotonLlegada()

                        return
                    }

                    val ubicacion =
                        resultado.lastLocation ?: return

                    actualizarVerificacionLlegada(
                        ubicacion
                    )
                }
            }
        }

        val solicitudUbicacion =
            LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L
            )
                .setMinUpdateIntervalMillis(1500L)
                .build()

        try {

            // Marcar inmediatamente para evitar
            // solicitudes simultáneas desde onStart y onResume.
            actualizacionesUbicacionActivas = true

            fusedLocationClient.requestLocationUpdates(
                solicitudUbicacion,
                locationCallback,
                Looper.getMainLooper()
            ).addOnSuccessListener {

                val bindingActual = _binding
                if (
                    bindingActual == null ||
                    !asignacionCargada ||
                    idAsignacionActiva == null ||
                    navegacionRealizada ||
                    !gpsEstaActivado()
                ) {

                    detenerActualizacionesUbicacion()

                    return@addOnSuccessListener
                }

                bindingActual.tvEsperandoUbicacion.visibility =
                    View.VISIBLE

                bindingActual.tvEsperandoUbicacion.text =
                    "Obteniendo ubicación GPS..."

            }.addOnFailureListener { error ->

                actualizacionesUbicacionActivas = false

                Log.e(
                    "VERIFICACION_LLEGADA",
                    "Error iniciando actualizaciones GPS",
                    error
                )

                _binding?.tvEsperandoUbicacion?.apply {

                    visibility = View.VISIBLE

                    text =
                        "No se pudo obtener la ubicación. Intenta regresar a la aplicación."
                }
            }

        } catch (e: SecurityException) {

            actualizacionesUbicacionActivas = false

            Log.e(
                "VERIFICACION_LLEGADA",
                "Permiso GPS no disponible",
                e
            )
        }
    }

    // =========================================================
    // DETENER ACTUALIZACIONES GPS
    // =========================================================

    private fun detenerActualizacionesUbicacion() {

        if (!actualizacionesUbicacionActivas) return

        if (::locationCallback.isInitialized) {

            fusedLocationClient.removeLocationUpdates(
                locationCallback
            )
        }

        actualizacionesUbicacionActivas = false
    }

    // =========================================================
    // CALCULAR DISTANCIA Y VERIFICAR LLEGADA
    // =========================================================

    private fun actualizarVerificacionLlegada(
        ubicacion: Location
    ) {

        val safeBinding = _binding ?: return

        if (navegacionRealizada || !asignacionCargada || idAsignacionActiva == null) return

        val puntoActual = GeoPoint(
            ubicacion.latitude,
            ubicacion.longitude
        )

        // Coordenadas del destino
        val puntoDestino = Location("destino").apply {

            latitude = latitudDestino
            longitude = longitudDestino
        }

        // Distancia real en metros
        val distancia =
            ubicacion.distanceTo(puntoDestino)

        // Actualizar ubicación del usuario en el mapa
        if (::locationOverlay.isInitialized) {

            locationOverlay.onLocationChanged(
                ubicacion,
                null
            )

            safeBinding.mapa.invalidate()
        }

        // Centrar mapa la primera vez
        if (!ubicacionInicializada) {

            safeBinding.mapa.controller.setZoom(
                18.0
            )

            safeBinding.mapa.controller.setCenter(
                puntoActual
            )

            ubicacionInicializada = true
        }

        // Mostrar distancia
        safeBinding.tvDistancia.text =
            String.format(
                Locale.getDefault(),
                "Distancia: %.1f metros",
                distancia
            )

        // -----------------------------------------------------
        // DENTRO DEL RADIO
        // -----------------------------------------------------

        if (distancia <= radioPermitido) {

            ubicacionDentroRadio = true

            safeBinding.tvEsperandoUbicacion.visibility =
                View.GONE

            if (!registroEnProgreso) {
                safeBinding.tvEstadoLlegada.text =
                    "Ubicación verificada"
            }

            actualizarEstadoBotonLlegada()

        } else {

            // -------------------------------------------------
            // FUERA DEL RADIO
            // -------------------------------------------------

            ubicacionDentroRadio = false

            safeBinding.tvEsperandoUbicacion.visibility =
                View.GONE

            if (!registroEnProgreso) {
                safeBinding.tvEstadoLlegada.text =
                    "Fuera del área permitida"
            }

            actualizarEstadoBotonLlegada()
        }

        safeBinding.mapa.invalidate()
    }

    // =========================================================
    // CONFIRMAR LLEGADA EN BACKEND
    // =========================================================

    private fun confirmarLlegada() {

        if (navegacionRealizada || registroEnProgreso) return

        val idAsignacion = idAsignacionActiva
        if (!asignacionCargada || idAsignacion == null) {
            Toast.makeText(
                requireContext(),
                "No tienes un turno activo para registrar la llegada.",
                Toast.LENGTH_SHORT
            ).show()
            actualizarEstadoBotonLlegada()
            return
        }

        if (!ubicacionDentroRadio) {
            Toast.makeText(
                requireContext(),
                "Debes estar dentro del radio permitido para confirmar tu llegada.",
                Toast.LENGTH_SHORT
            ).show()
            actualizarEstadoBotonLlegada()
            return
        }

        val propietarioVista = viewLifecycleOwner

        registroEnProgreso = true
        _binding?.tvEstadoLlegada?.text = "Registrando llegada..."
        actualizarEstadoBotonLlegada()

        propietarioVista.lifecycleScope.launch {
            val resultado = try {
                withContext(Dispatchers.IO) {
                    val response =
                        RetrofitClient.apiService.registrarLlegadaTurno(idAsignacion)
                    LlegadaTurnoHelper.interpretarRespuestaRegistroLlegada(response)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: IOException) {
                Log.e(
                    "VERIFICACION_LLEGADA",
                    "Error de conexión al registrar llegada (id=$idAsignacion)",
                    e
                )
                LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorConexion(
                    "Error de conexión. Verifica tu internet e intenta nuevamente."
                )
            } catch (e: Exception) {
                Log.e(
                    "VERIFICACION_LLEGADA",
                    "Error inesperado al registrar llegada (id=$idAsignacion)",
                    e
                )
                LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorConexion(
                    "No se pudo conectar con el servidor. Intenta nuevamente."
                )
            }

            val safeBinding = _binding
            if (
                safeBinding == null ||
                !isAdded ||
                !propietarioVista.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            ) {
                registroEnProgreso = false
                return@launch
            }

            when (resultado) {
                is LlegadaTurnoHelper.ResultadoRegistroLlegada.Exito -> {
                    asignacionActiva = resultado.asignacion ?: asignacionActiva
                    navegacionRealizada = true
                    registroEnProgreso = false

                    detenerActualizacionesUbicacion()

                    safeBinding.tvEstadoLlegada.text = "Llegada verificada"
                    actualizarEstadoBotonLlegada()

                    Toast.makeText(
                        requireContext(),
                        resultado.mensaje,
                        Toast.LENGTH_SHORT
                    ).show()

                    // Mostrar pantalla de bienvenida de la sede tras confirmar en el backend
                    parentFragmentManager
                        .beginTransaction()
                        .setCustomAnimations(
                            android.R.anim.fade_in,
                            android.R.anim.fade_out
                        )
                        .replace(
                            R.id.fragmentContainer,
                            BienvenidaSedeCuidadorFragment()
                        )
                        .commit()
                }

                is LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorPermisos -> {
                    registroEnProgreso = false
                    safeBinding.tvEstadoLlegada.text = "Sin autorización"
                    actualizarEstadoBotonLlegada()

                    Toast.makeText(
                        requireContext(),
                        resultado.mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorTurnoFinalizado -> {
                    registroEnProgreso = false
                    idAsignacionActiva = null
                    asignacionActiva = null
                    detenerActualizacionesUbicacion()
                    safeBinding.tvEstadoLlegada.text = "Turno finalizado"
                    actualizarEstadoBotonLlegada()

                    Toast.makeText(
                        requireContext(),
                        resultado.mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorServidor -> {
                    registroEnProgreso = false
                    safeBinding.tvEstadoLlegada.text =
                        if (ubicacionDentroRadio) "Ubicación verificada" else "Fuera del área permitida"
                    actualizarEstadoBotonLlegada()

                    Toast.makeText(
                        requireContext(),
                        resultado.mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }

                is LlegadaTurnoHelper.ResultadoRegistroLlegada.ErrorConexion -> {
                    registroEnProgreso = false
                    safeBinding.tvEstadoLlegada.text =
                        if (ubicacionDentroRadio) "Ubicación verificada" else "Fuera del área permitida"
                    actualizarEstadoBotonLlegada()

                    Toast.makeText(
                        requireContext(),
                        resultado.mensaje,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        detenerActualizacionesUbicacion()

        if (::locationOverlay.isInitialized) {

            locationOverlay.disableMyLocation()
        }

        _binding?.mapa?.overlays?.clear()

        marcadorDestino = null
        circuloGeocerca = null
        registroEnProgreso = false
        ubicacionDentroRadio = false

        _binding = null

        super.onDestroyView()
    }
}