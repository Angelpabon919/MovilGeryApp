package com.example.molvigeryapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.molvigeryapp.databinding.ActivitySplashBinding
import com.example.molvigeryapp.ui.auth.LoginActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.core.view.WindowCompat

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    // Conserva las vistas creadas dinámicamente para poder detener sus animaciones.
    private val animacionesIconos = mutableListOf<View>()

    // Cada icono tiene su propia corrutina, independiente de las demas
    private val trabajosIconos = mutableListOf<Job>()

    // Controla la secuencia principal del Splash Screen
    private var animacionPrincipal: Job? = null

    // Tiempo total aproximado de la pantalla de carga
    private val duracionSplash = 3500L

    // Paleta visual de GerIApp.
    private val azulPrincipal = Color.rgb(59, 91, 219)  // #3B5BDB
    private val azulSecundario = Color.rgb(77, 171, 247) // #4DABF7
    private val azulClaro = Color.rgb(220, 237, 255)
    private val fondoClaro = Color.rgb(248, 250, 252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configurarBarrasDelSistema()
        configurarLogo()

        // Espera a que el contenedor tenga sus dimensiones reales
        // Asi distribuimos los iconos dentro del area visible del splash
        binding.contenedorIconos.post {
            if (!isFinishing && !isDestroyed) {
                configurarIconosFlotantes()
                iniciarAnimacion()
            }
        }
    }

    /* Respeta las areas reservadas por las barras del sistema El contenido se mantiene dentro del área segura y evita que
     la onda inferior quede oculta detrás de la navegación del telefono */
    private fun configurarBarrasDelSistema() {
        window.statusBarColor = fondoClaro
        window.navigationBarColor = fondoClaro

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        WindowCompatHelper.configurarBarrasClaras(window)

        ViewCompat.setOnApplyWindowInsetsListener(binding.splashRoot) { vista, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            // Respeta las areas superior e inferior ocupadas por el sistema
            vista.setPadding(
                vista.paddingLeft,
                barras.top,
                vista.paddingRight,
                barras.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(binding.splashRoot)
    }

    /* Prepara el logo para una entrada suave la opacidad inicial es baja y el tamaño empieza ligeramente reducido */
    private fun configurarLogo() {
        binding.imagenLogo.apply {
            alpha = 0.25f
            scaleX = 0.96f
            scaleY = 0.96f
            bringToFront()
        }
    }

    /* Crea los iconos decorativos reutilizando los vectores existentes
     * Cada icono recibe un tono azul y una posición alrededor del logo.
     */
    private fun configurarIconosFlotantes() {
        val recursos = listOf(
            R.drawable.bitacora,
            R.drawable.calendar_dia,
            R.drawable.calendario,
            R.drawable.clock,
            R.drawable.notifications,
            R.drawable.corazon_cierre,
            R.drawable.persona_encargado,
            R.drawable.menu_book
        )

        val coloresIconos = listOf(
            azulPrincipal,
            azulSecundario,
            azulPrincipal,
            Color.rgb(52, 140, 220),
            azulPrincipal,
            Color.rgb(65, 151, 224),
            azulSecundario,
            azulPrincipal
        )

        val densidad = resources.displayMetrics.density
        val ancho = binding.contenedorIconos.width
        val alto = binding.contenedorIconos.height

        if (ancho <= 0 || alto <= 0) return

        val tamano = (30 * densidad).toInt()

        // Las posiciones iniciales se distribuyen por los bordes
        // Evitamos colocar iconos directamente sobre el logo central
        val posiciones = listOf(
            0.10f to 0.15f,
            0.78f to 0.13f,
            0.06f to 0.39f,
            0.84f to 0.36f,
            0.08f to 0.65f,
            0.83f to 0.67f,
            0.24f to 0.84f,
            0.70f to 0.85f
        )

        recursos.forEachIndexed { indice, recurso ->
            val (x, y) = posiciones[indice]

            val icono = ImageView(this).apply {
                setImageResource(recurso)
                scaleType = ImageView.ScaleType.FIT_CENTER

                // Colorea el vector sin modificar el archivo original
                imageTintList = ColorStateList.valueOf(
                    coloresIconos[indice]
                )

                alpha = 0f
                rotation = Random.nextInt(-10, 11).toFloat()
                elevation = 3 * densidad

                // Fondo translucido para separar el icono de las ondas
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 12 * densidad
                    setColor(Color.argb(190, 255, 255, 255))
                    setStroke(
                        (1 * densidad).toInt(),
                        Color.argb(65, azulSecundario ushr 16 and 0xFF,
                            azulSecundario ushr 8 and 0xFF,
                            azulSecundario and 0xFF)
                    )
                }

                val padding = (5 * densidad).toInt()
                setPadding(padding, padding, padding, padding)
                contentDescription = null
            }

            val parametros = FrameLayout.LayoutParams(tamano, tamano).apply {
                leftMargin = (ancho * x).toInt()
                    .coerceIn(0, (ancho - tamano).coerceAtLeast(0))

                topMargin = (alto * y).toInt()
                    .coerceIn(0, (alto - tamano).coerceAtLeast(0))
            }

            binding.contenedorIconos.addView(icono, parametros)
            animacionesIconos.add(icono)
        }
    }

    /* Inicia el pulso repetitivo del logo y los movimientos de los iconos
     * Después de unos 3,5 segundos, abre la pantalla que corresponda
     */
    private fun iniciarAnimacion() {
        animacionPrincipal = lifecycleScope.launch {
            // El logo oscila entre 100 % y 25 % de opacidad
            binding.imagenLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350L)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()

            // Los iconos empiezan a moverse con pequeños desfases
            animarIconos()

            // Pulso suave y continuo del logo durante la carga.
            // La opacidad baja solo hasta el 55 % para conservar su presencia.
            val inicio = android.os.SystemClock.elapsedRealtime()

            while (
                android.os.SystemClock.elapsedRealtime() - inicio <
                duracionSplash
            ) {
                // Desvanecimiento lento, sin hacer desaparecer el logo.
                binding.imagenLogo.animate()
                    .alpha(0.55f)
                    .setDuration(900L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                delay(900L)

                // Recupera el brillo de forma progresiva.
                binding.imagenLogo.animate()
                    .alpha(1f)
                    .setDuration(900L)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()

                delay(900L)
            }


            // Detiene las animaciones antes de cambiar de pantalla.
            detenerAnimacionesIconos()

            if (!isFinishing && !isDestroyed) {
                abrirPantallaInicial()
            }
        }
    }

    /* Anima cada icono con desplazamientos, rotaciones y duraciones variables
      Las corrutinas son hijas del ciclo de vida de la actividad */
    private fun animarIconos() {
        animacionesIconos.forEachIndexed { indice, icono ->
            val trabajo = lifecycleScope.launch {
                delay(indice * 100L)

                while (!isFinishing && !isDestroyed) {
                    val desplazamientoX =
                        Random.nextInt(-35, 36) *
                                resources.displayMetrics.density

                    val desplazamientoY =
                        Random.nextInt(-45, 46) *
                                resources.displayMetrics.density

                    val duracion = Random.nextLong(800L, 1450L)

                    // Movimiento suave hacia una posición aleatoria
                    icono.animate()
                        .alpha(Random.nextDouble(0.45, 0.90).toFloat())
                        .translationX(desplazamientoX)
                        .translationY(desplazamientoY)
                        .rotation(Random.nextInt(-15, 16).toFloat())
                        .setDuration(duracion)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()

                    delay(duracion)

                    // Desaparece parcialmente antes de iniciar otro movimiento
                    icono.animate()
                        .alpha(0.15f)
                        .setDuration(300L)
                        .start()

                    delay(300L)
                }
            }

            trabajosIconos.add(trabajo)
        }
    }

    /* Cancela todas las corrutinas y animaciones de los iconos
     * Evita que sigan trabajando al navegar a Login o MainActivity
     */
    private fun detenerAnimacionesIconos() {
        trabajosIconos.forEach { it.cancel() }
        trabajosIconos.clear()

        animacionesIconos.forEach { icono ->
            icono.animate().cancel()
        }
    }

    /* Conserva el mecanismo de sesión actual
     * No modifica las preferencias ni el sistema de autenticación existente
     */
    private fun abrirPantallaInicial() {
        val preferencias = getSharedPreferences("SESION", MODE_PRIVATE)

        val sesionIniciada = preferencias.getBoolean(
            "SESION_INICIADA",
            false
        )

        val idUsuario = preferencias.getInt("ID_USUARIO", -1)
        val idRol = preferencias.getInt("ID_ROL", -1)

        val haySesion =
            sesionIniciada && idUsuario > 0 && idRol > 0

        val intent = if (haySesion) {
            Intent(this, MainActivity::class.java).apply {
                putExtra("ID_ROL", idRol)
            }
        } else {
            Intent(this, LoginActivity::class.java)
        }

        // Evita volver al Splash al pulsar Atras desde la pantalla de destino
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()

        overridePendingTransition(
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
    }

    /* Limpia las animaciones y corrutinas cuando la actividad se destruye
     */
    override fun onDestroy() {
        animacionPrincipal?.cancel()
        detenerAnimacionesIconos()

        super.onDestroy()
    }
}

/* Configura los colores de las barras del sistema para que sus iconos
 * sean visibles sobre el fondo claro*/
private object WindowCompatHelper {

    fun configurarBarrasClaras(window: android.view.Window) {
        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}
