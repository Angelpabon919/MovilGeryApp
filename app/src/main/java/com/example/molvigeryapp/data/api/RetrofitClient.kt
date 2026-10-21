package com.example.molvigeryapp.data.api

import android.content.Context
import com.example.molvigeryapp.GerIAppApplication
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL =
        "https://geriapp-backend.onrender.com/api/"

    // Agrega el token de la sesión a las peticiones protegidas.
    private val authInterceptor = Interceptor { chain ->

        val solicitudOriginal = chain.request()

        val token = GerIAppApplication.instancia
            .getSharedPreferences("SESION", Context.MODE_PRIVATE)
            .getString("TOKEN", null)

        val esLogin = solicitudOriginal.url.encodedPath
            .endsWith("/usuarios/login/")

        val solicitud = if (!token.isNullOrBlank() && !esLogin) {
            solicitudOriginal.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            solicitudOriginal
        }

        chain.proceed(solicitud)
    }

    private val logging = HttpLoggingInterceptor().apply {
        // Evita mostrar el token en los registros HTTP.
        redactHeader("Authorization")
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client =
        OkHttpClient.Builder()
            .connectTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()

    val apiService: ApiService =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
}
