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
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.SimpleLocationOverlay

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
                binding.tvEsperandoUbicacion.text =
                    "Permiso de ubicación denegado"
                binding.tvEstadoLlegada.text =
                    "Necesitamos tu ubicación para verificar la llegada"
            }
        }

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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(
                requireActivity()
            )
        Configuration.getInstance().load(requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().setUserAgentValue ( "GerIApp/1.0")
        val stadiaMap = XYTileSource(
            "stadiaMaps",
            0,
            20,
            256,
            ".png?api_key=c308f0bd-c5a9-4d85-8b32-5896d3b10f9c",
            arrayOf(
                "http://tiles.stadiamaps.com/tiles/osm_bright/"
            ),"© Stadia Maps © OpenStreetMap contributors © OpenMapTiles"
        )
        binding.mapa.setTileSource(stadiaMap)
        binding.mapa.setMultiTouchControls(true)
        val locationProvider =
            GpsMyLocationProvider(requireContext())

        locationOverlay =
            MyLocationNewOverlay(
                locationProvider,
                binding.mapa
            )

        locationOverlay.enableMyLocation()

        binding.mapa.overlays.add(locationOverlay)
        configurarPantalla()
        verificarPermisoUbicacion()

        binding.btnContinuarTemporal.setOnClickListener {

            // Mostramos nuevamente el BottomNavigation
            requireActivity()
                .findViewById<View>(R.id.bottomNavigation)
                .visibility = View.VISIBLE

            // Vamos a la lista de pacientes
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    PacientesListFragment()
                )
                .commit()
        }
    }
    private fun configurarPantalla() {

        binding.tvEstadoLlegada.text =
            "Esperando llegada..."
        binding.tvEsperandoUbicacion.text =
            "Obteniendo ubicación..."
        binding.tvDistancia.text =
            "Calculando..."
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

        binding.tvEsperandoUbicacion.text =
            "Obteniendo ubicación..."

        val cancellationTokenSource =
            CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->

            if (location != null) {

                val latitud = location.latitude
                val longitud = location.longitude

                val puntoActual = GeoPoint(latitud,longitud)
                binding.mapa.controller.setZoom(17.0)
                binding.mapa.controller.setCenter(puntoActual)

                binding.tvEsperandoUbicacion.visibility = View.GONE

                binding.tvEstadoLlegada.text =
                    "Ubicación obtenida correctamente"


                // MOSTRAMOS LAS COORDENADAS
                // SOLO PARA HACER LA PRUEBA
                binding.tvDistancia.text =
                    "Lat: %.5f\nLon: %.5f".format(
                        latitud,
                        longitud
                    )
            } else {

                binding.tvEsperandoUbicacion.text =
                    "No se pudo obtener la ubicación"

                binding.tvEstadoLlegada.text =
                    "Intenta activar el GPS"
            }

        }.addOnFailureListener {

            binding.tvEsperandoUbicacion.text =
                "Error al obtener ubicación"

            binding.tvEstadoLlegada.text =
                "Verifica que el GPS esté activado"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}