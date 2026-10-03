package com.example.molvigeryapp.ui.cuidador.llegada

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.molvigeryapp.R
import com.example.molvigeryapp.databinding.FragmentVerificacionLlegadaBinding
import com.example.molvigeryapp.ui.cuidador.pacientes.PacientesListFragment
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

class VerificacionLlegadaFragment : Fragment() {

    private var _binding: FragmentVerificacionLlegadaBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationOverlay: MyLocationNewOverlay

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

        verificarPermisoUbicacion()

        configurarBotonContinuar()
    }

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

    private fun configurarPantalla() {

        binding.tvEstadoLlegada.text =
            "Esperando llegada..."

        binding.tvEsperandoUbicacion.text =
            "Obteniendo ubicación..."

        binding.tvDistancia.text =
            "Calculando..."
    }

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

        if (permisoPreciso || permisoAproximado) {

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

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacion() {

        val currentBinding = _binding ?: return

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

                val safeBinding = _binding ?: return@addOnSuccessListener

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

                val safeBinding = _binding ?: return@addOnFailureListener

                safeBinding.tvEsperandoUbicacion.text =
                    "Error al obtener ubicación"

                safeBinding.tvEstadoLlegada.text =
                    "Verifica que el GPS esté activado"
            }
    }

    override fun onDestroyView() {

        if (::locationOverlay.isInitialized) {

            locationOverlay.disableMyLocation()
        }

        _binding = null

        super.onDestroyView()
    }
}