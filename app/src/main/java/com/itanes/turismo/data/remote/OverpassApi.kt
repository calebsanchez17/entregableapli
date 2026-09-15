package com.itanes.turismo.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApi {
    @GET("interpreter")
    suspend fun query(@Query("data") data: String): OverpassResponse
}

data class OverpassResponse(val elements: List<OverpassElement> = emptyList())

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenter? = null,
    val tags: Map<String, String>? = null
)

data class OverpassCenter(val lat: Double, val lon: Double)
