package com.itanes.turismo.data.model

data class TouristPlace(
    val id: Int,
    val remoteKey: String,
    val routeKey: String,
    val name: String,
    val district: String,
    val description: String,
    val tip: String,
    val imageName: String,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val position: Int,
    val favorite: Boolean,
    val temperature: Double?,
    val weatherUpdatedAt: Long?
)

data class DestinationMeta(
    val routeKey: String,
    val title: String,
    val latitude: Double,
    val longitude: Double
)

data class WorldSearchResult(
    val destination: DestinationMeta,
    val places: List<TouristPlace>
)
