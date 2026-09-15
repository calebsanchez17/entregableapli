package com.itanes.turismo.data.remote

import com.itanes.turismo.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "${BuildConfig.APPLICATION_ID}/2.0 (academic tourism app)")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val weatherApi: OpenMeteoApi by lazy {
            retrofit("https://api.open-meteo.com/").create(OpenMeteoApi::class.java)
    }

    val nominatimApi: NominatimApi by lazy {
        retrofit("https://nominatim.openstreetmap.org/").create(NominatimApi::class.java)
    }

    val overpassApi: OverpassApi by lazy {
        retrofit("https://overpass-api.de/api/").create(OverpassApi::class.java)
    }
}
