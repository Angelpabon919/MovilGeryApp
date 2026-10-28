package com.example.molvigeryapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.data.repository.FcmTokenRepository
import com.example.molvigeryapp.data.repository.NotificacionesRepository
import com.example.molvigeryapp.databinding.ActivityMainBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import com.example.molvigeryapp.ui.cuidador.actualizador.ActualizadorGeriApp
import com.example.molvigeryapp.ui.cuidador.llegada.VerificacionLlegadaFragment
import com.example.molvigeryapp.ui.cuidador.notificaciones.DetalleNotificacionCuidadorFragment
import com.example.molvigeryapp.ui.encargado.home.HomeEncargadoFragment
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.molvigeryapp.ui.encargado.NavegacionEncargado
import com.example.molvigeryapp.ui.encargado.perfil.PerfilEncargadoFragment
import com.example.molvigeryapp.ui.encargado.citas.CitasEncargadoFragment
import com.example.molvigeryapp.ui.encargado.asignarturno.AsignarTurnoEncargadoFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "GERIAPP_FCM"
        private const val TAG_PANTALLA = "GERIAPP_PANTALLA"

        private const val REQUEST_NOTIFICATION_PERMISSION = 1001

        private const val EXTRA_ABRIR_NOTIFICACION =
            "ABRIR_DESDE_NOTIFICACION"

        private const val EXTRA_ID_NOTIFICACION =
            "ID_NOTIFICACION"

        private const val EXTRA_TIPO_NOTIFICACION =
            "TIPO_NOTIFICACION"

        private const val ROL_ENCARGADO = 2
        private const val ROL_CUIDADOR = 3

        ///boton de llamada

        private const val TELEFONO_EMERGENCIA =
            "+573146767639"
    }

    private var idRolActual = -1
    private var procesandoNotificacion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.e(
            TAG_PANTALLA,
            "MainActivity: entrando a onCreate"
        )

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.fabLlamada.setOnClickListener {
            abrirMarcadorEmergencia()
        }

        Log.e(
            TAG_PANTALLA,
            "MainActivity: pantalla principal cargada"
        )

        solicitarPermisoNotificaciones()
        obtenerTokenFCM()

        val preferencias = getSharedPreferences(
            "SESION",
            MODE_PRIVATE
        )

        val rolRecibido = intent.getIntExtra(
            "ID_ROL",
            preferencias.getInt("ID_ROL", -1)
        )

        // Compatibilidad con los roles anteriores.
        idRolActual = when (rolRecibido) {
            5 -> ROL_CUIDADOR
            6 -> ROL_ENCARGADO
            else -> rolRecibido
        }

        if (idRolActual != rolRecibido) {
            preferencias.edit()
                .putInt("ID_ROL", idRolActual)
                .apply()

            Log.d(
                TAG,
                "Rol actualizado de $rolRecibido a $idRolActual"
            )
        }

        Log.d(
            TAG,
            "Rol recibido en MainActivity: $idRolActual"
        )

        Log.e(
            TAG_PANTALLA,
            "Rol utilizado para abrir la pantalla: $idRolActual"
        )

        if (idRolActual == ROL_CUIDADOR) {
            Log.e(
                TAG_PANTALLA,
                "Iniciando actualizador del cuidador"
            )

            iniciarActualizadorGeriApp()
        }

        if (savedInstanceState == null) {
            when (idRolActual) {

                ROL_ENCARGADO -> {
<<<<<<< Updated upstream
                    Log.e(
                        TAG_PANTALLA,
                        "Abriendo HomeEncargadoFragment"
                    )

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            HomeEncargadoFragment()
                        )
                        .commit()
=======
                    navegarPantallaEncargado(
                        NavegacionEncargado.Pantalla.INICIO
                    )
>>>>>>> Stashed changes
                }

                ROL_CUIDADOR -> {
                    Log.e(
                        TAG_PANTALLA,
                        "Abriendo VerificacionLlegadaFragment"
                    )

                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainer,
                            VerificacionLlegadaFragment()
                        )
                        .commit()
                }

                else -> {
                    Log.e(
                        TAG_PANTALLA,
                        "No se abrió ningún fragmento. Rol=$idRolActual"
                    )
                }
            }

            procesarIntentNotificacion(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        procesarIntentNotificacion(intent)
    }

    fun navegarPantallaEncargado(pantalla: NavegacionEncargado.Pantalla) {
        if (idRolActual != ROL_ENCARGADO || isFinishing || isDestroyed) return

        val fm = supportFragmentManager
        if (fm.isStateSaved) return

        val destinos = listOf(
            NavegacionEncargado.Pantalla.INICIO to
                    "encargado_inicio",
            NavegacionEncargado.Pantalla.PERFIL to
                    "encargado_perfil",
            NavegacionEncargado.Pantalla.CITAS to
                    "encargado_citas",
            NavegacionEncargado.Pantalla.ASIGNAR_TURNO to
                    "encargado_turnos"
        )

        val tagDestino = destinos.first { it.first == pantalla }.second

        val transaccion = fm.beginTransaction()
            .setReorderingAllowed(true)

        // Ocultar las pantallas principales que ya existen.
        destinos.forEach { (_, tag) ->
            fm.findFragmentByTag(tag)?.let { fragment ->
                if (fragment.isAdded && !fragment.isHidden) {
                    transaccion.hide(fragment)
                    transaccion.setMaxLifecycle(
                        fragment,
                        Lifecycle.State.CREATED
                    )
                }
            }
        }

        // Recuperar el fragmento existente o crear uno nuevo.
        val fragmento = fm.findFragmentByTag(tagDestino)
            ?: crearFragmentoEncargado(pantalla)

        if (!fragmento.isAdded) {
            transaccion.add(
                R.id.fragmentContainer,
                fragmento,
                tagDestino
            )
        } else {
            transaccion.show(fragmento)
        }

        transaccion.setMaxLifecycle(
            fragmento,
            Lifecycle.State.RESUMED
        )

        transaccion.commit()
    }

    private fun crearFragmentoEncargado(
        pantalla: NavegacionEncargado.Pantalla
    ): Fragment {
        return when (pantalla) {
            NavegacionEncargado.Pantalla.INICIO ->
                HomeEncargadoFragment()

            NavegacionEncargado.Pantalla.PERFIL ->
                PerfilEncargadoFragment()

            NavegacionEncargado.Pantalla.CITAS ->
                CitasEncargadoFragment()

            NavegacionEncargado.Pantalla.ASIGNAR_TURNO ->
                AsignarTurnoEncargadoFragment()
        }
    }


    private fun procesarIntentNotificacion(intent: Intent?) {
        if (
            intent?.getBooleanExtra(
                EXTRA_ABRIR_NOTIFICACION,
                false
            ) != true
        ) {
            return
        }

        if (procesandoNotificacion) return

        val preferencias = getSharedPreferences(
            "SESION",
            MODE_PRIVATE
        )

        val idUsuario = preferencias.getInt(
            "ID_USUARIO",
            -1
        )

        val rolGuardado = preferencias.getInt(
            "ID_ROL",
            idRolActual
        )

        val idRol = when (rolGuardado) {
            5 -> ROL_CUIDADOR
            6 -> ROL_ENCARGADO
            else -> rolGuardado
        }

        val idNotificacion = intent.getIntExtra(
            EXTRA_ID_NOTIFICACION,
            -1
        )

        val tipo = intent.getStringExtra(
            EXTRA_TIPO_NOTIFICACION
        ).orEmpty()

        if (idRol != ROL_CUIDADOR) {
            Log.d(
                TAG,
                "No se abrirá el detalle del cuidador. Rol=$idRol"
            )

            limpiarExtrasNotificacion(intent)
            return
        }

        if (idUsuario <= 0 || idNotificacion <= 0) {
            Log.e(
                TAG,
                "ID_USUARIO o ID_NOTIFICACION inválido. " +
                        "Usuario=$idUsuario, notificación=$idNotificacion"
            )

            limpiarExtrasNotificacion(intent)
            return
        }

        procesandoNotificacion = true

        lifecycleScope.launch {
            try {
                val lista =
                    NotificacionesRepository.obtenerNotificacionesUsuario(
                        idUsuario
                    )

                val notificacion = lista.firstOrNull {
                    it.idNotificacion == idNotificacion
                }

                if (notificacion == null) {
                    Log.w(
                        TAG,
                        "No se encontró la notificación " +
                                "$idNotificacion. Tipo=$tipo"
                    )
                    return@launch
                }

                if (isFinishing || isDestroyed) {
                    return@launch
                }

                val bundle = Bundle().apply {
                    putSerializable(
                        "notificacion",
                        notificacion
                    )
                }

                val detalle =
                    DetalleNotificacionCuidadorFragment().apply {
                        arguments = bundle
                    }

                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.fragmentContainer,
                        detalle
                    )
                    .addToBackStack("detalle_notificacion")
                    .commit()

                if (!notificacion.leida) {
                    val marcada =
                        NotificacionesRepository
                            .marcarCualquierNotificacionLeida(
                                idNotificacion =
                                    notificacion.idNotificacion,
                                idUsuario = idUsuario
                            )

                    Log.d(
                        TAG,
                        "Resultado al marcar notificación como leída: $marcada"
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error abriendo notificación desde push",
                    e
                )
            } finally {
                limpiarExtrasNotificacion(intent)
                procesandoNotificacion = false
            }
        }
    }

    private fun limpiarExtrasNotificacion(intent: Intent) {
        intent.removeExtra(EXTRA_ABRIR_NOTIFICACION)
        intent.removeExtra(EXTRA_ID_NOTIFICACION)
        intent.removeExtra(EXTRA_TIPO_NOTIFICACION)
    }

    private fun iniciarActualizadorGeriApp() {
        lifecycleScope.launch {
            while (isActive) {
                try {
                    ActualizadorGeriApp.actualizar(
                        applicationContext
                    )
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Error actualizando datos de GerIApp",
                        e
                    )
                }

                delay(
                    ActualizadorGeriApp.obtenerIntervalo()
                )
            }
        }
    }

    private fun obtenerTokenFCM() {
        Log.d(
            TAG,
            "Solicitando token FCM..."
        )

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { tarea ->

                if (!tarea.isSuccessful) {
                    Log.e(
                        TAG,
                        "No se pudo obtener el token FCM",
                        tarea.exception
                    )
                    return@addOnCompleteListener
                }

                val token = tarea.result

                Log.d(
                    TAG,
                    "Token FCM obtenido correctamente"
                )

                registrarTokenEnDjango(token)
            }
    }

    private fun registrarTokenEnDjango(token: String) {
        val preferencias = getSharedPreferences(
            "SESION",
            MODE_PRIVATE
        )

        val idUsuario = preferencias.getInt(
            "ID_USUARIO",
            -1
        )

        if (idUsuario <= 0) {
            Log.d(
                TAG,
                "No hay un ID_USUARIO válido en la sesión"
            )
            return
        }

        lifecycleScope.launch {
            try {
                val registrado = FcmTokenRepository.registrarToken(
                    idUsuario = idUsuario,
                    token = token
                )

                if (registrado) {
                    Log.d(
                        TAG,
                        "Token FCM registrado correctamente en Django"
                    )
                } else {
                    Log.e(
                        TAG,
                        "Django rechazó el token FCM"
                    )
                }

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error registrando token FCM en Django",
                    e
                )
            }
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    private fun abrirMarcadorEmergencia() {
        val numero = TELEFONO_EMERGENCIA

        if (
            numero.isBlank() ||
            numero == "REEMPLAZAR_NUMERO_NATALIA"
        ) {
            Toast.makeText(
                this,
                "Configura el número de emergencia.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.fromParts("tel", numero, null)
        }

        try {
            startActivity(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No hay una aplicación de teléfono disponible.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    fun cerrarSesion() {
        getSharedPreferences("SESION", MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}