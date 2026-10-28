
package com.example.molvigeryapp.data.api

import android.content.Context
import com.example.molvigeryapp.GerIAppApplication
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL =
        "https://geriapp-backend.onrender.com/api/"

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

    /* Desactiva los registros HTTP para evitar trabajo innecesario y no exponer información personal durante las peticiones */
    private val logging = HttpLoggingInterceptor().apply {
        redactHeader("Authorization")
        level = HttpLoggingInterceptor.Level.NONE
    }

    /* Cliente HTTP compartido por toda la aplicación */
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    /* Instancia única de Retrofit y ApiService */
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }
}
