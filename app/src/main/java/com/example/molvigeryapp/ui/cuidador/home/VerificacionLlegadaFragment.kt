package com.example.molvigeryapp.ui.cuidador.llegada

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.R
import com.example.molvigeryapp.data.api.RetrofitClient
import com.example.molvigeryapp.data.model.AsignacionTurnoUsuario
import com.example.molvigeryapp.data.model.Turno
import com.example.molvigeryapp.databinding.FragmentVerificacionLlegadaBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class VerificacionLlegadaFragment : Fragment() {

    private var _binding: FragmentVerificacionLlegadaBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationOverlay: MyLocationNewOverlay

    private var turnosDesplegados = false

    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->

            val ubicacionPrecisa =
                permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val ubicacionAproximada =
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (ubicacionPrecisa || ubicacionAproximada) {

                obtenerUbicacion()

            } else {

                val safeBinding = _binding ?: return@registerForActivityResult

                safeBinding.tvEsperandoUbicacion.text =
                    "Permiso de ubicación denegado"

                safeBinding.tvEstadoLlegada.text =
                    "Necesitamos tu ubicación para verificar la llegada"
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentVerificacionLlegadaBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(
                requireActivity()
            )

        configurarMapa()

        configurarPantalla()

        configurarTarjetaTurnos()

        cargarTurnosPendientes()

        verificarPermisoUbicacion()

        configurarBotonContinuar()
    }

    // =========================================================
    // MAPA
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

        val stadiaMap = XYTileSource(
            "stadiaMaps",
            0,
            20,
            256,
            ".png?api_key=c308f0bd-c5a9-4d85-8b32-5896d3b10f9c",
            arrayOf(
                "http://tiles.stadiamaps.com/tiles/osm_bright/"
            ),
            "© Stadia Maps © OpenStreetMap contributors © OpenMapTiles"
        )

        binding.mapa.setTileSource(
            stadiaMap
        )

        binding.mapa.setMultiTouchControls(
            true
        )

        val locationProvider =
            GpsMyLocationProvider(
                requireContext()
            )

        locationOverlay =
            MyLocationNewOverlay(
                locationProvider,
                binding.mapa
            )

        locationOverlay.enableMyLocation()

        binding.mapa.overlays.add(
            locationOverlay
        )
    }

    // =========================================================
    // CONFIGURACIÓN INICIAL
    // =========================================================

    private fun configurarPantalla() {

        binding.tvEstadoLlegada.text =
            "Esperando llegada..."

        binding.tvEsperandoUbicacion.text =
            "Obteniendo ubicación..."

        binding.tvDistancia.text =
            "Calculando..."
    }

    // =========================================================
    // TARJETA DE TURNOS
    // =========================================================

    private fun configurarTarjetaTurnos() {

        binding.containerTurnos.visibility =
            View.GONE

        binding.tvSinTurnos.visibility =
            View.GONE

        binding.btnExpandirTurnos.setImageResource(
            R.drawable.ic_arrow_down
        )

        turnosDesplegados = false

        binding.layoutTituloTurnos.setOnClickListener {

            turnosDesplegados =
                !turnosDesplegados

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

        lifecycleScope.launch {

            try {

                val preferences =
                    requireContext().getSharedPreferences(
                        "SESION",
                        Context.MODE_PRIVATE
                    )

                val idUsuario =
                    preferences.getInt(
                        "ID_USUARIO",
                        -1
                    )

                if (idUsuario <= 0) {

                    mostrarSinTurnos()

                    return@launch
                }

                val datos =
                    withContext(Dispatchers.IO) {

                        val asignaciones =
                            RetrofitClient.apiService
                                .getAsignacionesTurno()

                        val turnos =
                            RetrofitClient.apiService
                                .getTurnos()

                        Pair(
                            asignaciones,
                            turnos
                        )
                    }

                val asignaciones =
                    datos.first

                val turnos =
                    datos.second

                val turnosPendientes =
                    obtenerTurnosPendientes(
                        idUsuario,
                        asignaciones,
                        turnos
                    )

                mostrarTurnos(
                    turnosPendientes
                )

            } catch (e: Exception) {

                e.printStackTrace()

                mostrarSinTurnos()
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
    ): List<Pair<AsignacionTurnoUsuario, Turno>> {

        val formatoFecha =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        val hoy =
            Calendar.getInstance()

        hoy.set(
            Calendar.HOUR_OF_DAY,
            0
        )

        hoy.set(
            Calendar.MINUTE,
            0
        )

        hoy.set(
            Calendar.SECOND,
            0
        )

        hoy.set(
            Calendar.MILLISECOND,
            0
        )

        val fechaInicio =
            hoy.time

        val fechaLimite =
            Calendar.getInstance()

        fechaLimite.time =
            fechaInicio

        fechaLimite.add(
            Calendar.DAY_OF_YEAR,
            14
        )

        val fechaFin =
            fechaLimite.time

        val turnosMap =
            turnos
                .filter {
                    it.id_turno != null
                }
                .associateBy {
                    it.id_turno
                }

        return asignaciones
            .filter { asignacion ->

                asignacion.id_usuario ==
                        idUsuario

            }
            .filter { asignacion ->

                asignacion.estado.equals(
                    "Asignado",
                    ignoreCase = true
                )

            }
            .mapNotNull { asignacion ->

                val fechaTexto =
                    asignacion.fecha
                        ?: return@mapNotNull null

                val fecha =
                    try {

                        formatoFecha.parse(
                            fechaTexto
                        )

                    } catch (
                        e: Exception
                    ) {

                        null
                    }

                if (fecha == null) {
                    return@mapNotNull null
                }

                val turno =
                    turnosMap[
                        asignacion.id_turno
                    ]
                        ?: return@mapNotNull null

                if (!turno.estado) {
                    return@mapNotNull null
                }

                /*
                 * No mostramos el turno de hoy.
                 *
                 * Solo:
                 *
                 * mañana
                 * hasta
                 * 14 días después.
                 */

                if (
                    fecha.after(fechaInicio) &&
                    !fecha.after(fechaFin)
                ) {

                    Pair(
                        asignacion,
                        turno
                    )

                } else {

                    null
                }
            }
            .sortedWith(
                compareBy<Pair<AsignacionTurnoUsuario, Turno>> {

                    it.first.fecha
                        ?: ""

                }.thenBy {

                    it.second.hora_inicio
                }
            )
    }

    // =========================================================
    // MOSTRAR TURNOS
    // =========================================================

    private fun mostrarTurnos(
        turnosPendientes:
        List<Pair<AsignacionTurnoUsuario, Turno>>
    ) {

        binding.containerTurnos.removeAllViews()

        if (turnosPendientes.isEmpty()) {

            mostrarSinTurnos()

            return
        }

        binding.tvSinTurnos.visibility =
            View.GONE

        turnosPendientes.forEach { datos ->

            val asignacion =
                datos.first

            val turno =
                datos.second

            val tarjeta =
                crearTarjetaTurno(
                    asignacion,
                    turno
                )

            binding.containerTurnos.addView(
                tarjeta
            )
        }
    }

    // =========================================================
    // TARJETA INDIVIDUAL
    // =========================================================

    private fun crearTarjetaTurno(
        asignacion: AsignacionTurnoUsuario,
        turno: Turno
    ): View {

        val contenedor =
            LinearLayout(
                requireContext()
            )

        contenedor.orientation =
            LinearLayout.VERTICAL

        contenedor.setPadding(
            16,
            14,
            16,
            14
        )

        contenedor.setBackgroundResource(
            R.drawable.bg_turno_pendiente
        )

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametros.setMargins(
            0,
            0,
            0,
            12
        )

        contenedor.layoutParams =
            parametros

        val tvNombre =
            TextView(
                requireContext()
            )

        tvNombre.text =
            turno.nombre

        tvNombre.textSize =
            16f

        tvNombre.setTextColor(
            Color.parseColor(
                "#202124"
            )
        )

        tvNombre.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        val tvFecha =
            TextView(
                requireContext()
            )

        tvFecha.text =
            "📅 ${formatearFecha(
                asignacion.fecha
            )}"

        tvFecha.textSize =
            14f

        tvFecha.setTextColor(
            Color.parseColor(
                "#4B5563"
            )
        )

        val tvHorario =
            TextView(
                requireContext()
            )

        tvHorario.text =
            "🕐 ${formatearHora(
                turno.hora_inicio
            )} - ${
                formatearHora(
                    turno.hora_fin
                )
            }"

        tvHorario.textSize =
            14f

        tvHorario.setTextColor(
            Color.parseColor(
                "#4B5563"
            )
        )

        val tvDescripcion =
            TextView(
                requireContext()
            )

        tvDescripcion.text =
            turno.descripcion

        tvDescripcion.textSize =
            13f

        tvDescripcion.setTextColor(
            Color.parseColor(
                "#6B7280"
            )
        )

        val tvEstado =
            TextView(
                requireContext()
            )

        tvEstado.text =
            "Estado: ${asignacion.estado}"

        tvEstado.textSize =
            13f

        tvEstado.setTextColor(
            Color.parseColor(
                "#3B5DBB"
            )
        )

        tvEstado.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        contenedor.addView(
            tvNombre
        )

        contenedor.addView(
            tvFecha
        )

        contenedor.addView(
            tvHorario
        )

        contenedor.addView(
            tvDescripcion
        )

        contenedor.addView(
            tvEstado
        )

        return contenedor
    }

    // =========================================================
    // SIN TURNOS
    // =========================================================

    private fun mostrarSinTurnos() {

        binding.containerTurnos.removeAllViews()

        binding.tvSinTurnos.visibility =
            View.VISIBLE

        binding.containerTurnos.addView(
            binding.tvSinTurnos
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
                formatoEntrada.parse(
                    fecha
                )

            formatoSalida.format(
                fechaConvertida!!
            )

        } catch (
            e: Exception
        ) {

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
                formatoEntrada.parse(
                    hora
                )

            formatoSalida.format(
                horaConvertida!!
            )

        } catch (
            e: Exception
        ) {

            hora
                .take(5)
        }
    }

    // =========================================================
    // BOTÓN CONTINUAR
    // =========================================================

    private fun configurarBotonContinuar() {

        binding.btnContinuarTemporal.setOnClickListener {

            /*
             * Flujo del cuidador:
             *
             * Login
             *    ↓
             * Verificación de llegada
             *    ↓
             * PacientesListFragment
             *    ↓
             * HomeFragment
             *
             * En esta pantalla todavía NO mostramos
             * el BottomNavigation.
             */

            parentFragmentManager
                .beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    PacientesListFragment()
                )
                .commit()
        }
    }

    // =========================================================
    // PERMISOS DE UBICACIÓN
    // =========================================================

    private fun verificarPermisoUbicacion() {

        val permisoPreciso =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val permisoAproximado =
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (
            permisoPreciso ||
            permisoAproximado
        ) {

            obtenerUbicacion()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // =========================================================
    // OBTENER UBICACIÓN
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion() {

        val currentBinding =
            _binding ?: return

        currentBinding.tvEsperandoUbicacion.text =
            "Obteniendo ubicación..."

        val cancellationTokenSource =
            CancellationTokenSource()

        fusedLocationClient
            .getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            .addOnSuccessListener { location ->

                val safeBinding =
                    _binding
                        ?: return@addOnSuccessListener

                if (location != null) {

                    val latitud =
                        location.latitude

                    val longitud =
                        location.longitude

                    val puntoActual =
                        GeoPoint(
                            latitud,
                            longitud
                        )

                    safeBinding.mapa.controller.setZoom(
                        17.0
                    )

                    safeBinding.mapa.controller.setCenter(
                        puntoActual
                    )

                    safeBinding.tvEsperandoUbicacion.visibility =
                        View.GONE

                    safeBinding.tvEstadoLlegada.text =
                        "Ubicación obtenida correctamente"

                    safeBinding.tvDistancia.text =
                        "Lat: %.5f\nLon: %.5f".format(
                            latitud,
                            longitud
                        )

                } else {

                    safeBinding.tvEsperandoUbicacion.text =
                        "No se pudo obtener la ubicación"

                    safeBinding.tvEstadoLlegada.text =
                        "Intenta activar el GPS"
                }
            }
            .addOnFailureListener {

                val safeBinding =
                    _binding
                        ?: return@addOnFailureListener

                safeBinding.tvEsperandoUbicacion.text =
                    "Error al obtener ubicación"

                safeBinding.tvEstadoLlegada.text =
                    "Verifica que el GPS esté activado"
            }
    }

    // =========================================================
    // DESTRUIR VISTA
    // =========================================================

    override fun onDestroyView() {

        if (
            ::locationOverlay.isInitialized
        ) {

            locationOverlay.disableMyLocation()
        }
        _binding = null

        super.onDestroyView()
    }
}