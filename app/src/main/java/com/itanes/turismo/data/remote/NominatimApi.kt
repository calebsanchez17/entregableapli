package com.itanes.turismo.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 1,
        @Query("addressdetails") addressDetails: Int = 1
    ): List<NominatimResult>
}

data class NominatimResult(
    @SerializedName("display_name") val displayName: String,
    val lat: String,
    val lon: String,
    val type: String? = null
)
