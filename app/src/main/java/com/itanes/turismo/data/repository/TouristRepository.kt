package com.itanes.turismo.data.repository

import android.content.Context
import com.itanes.turismo.data.local.TourismDbHelper
import com.itanes.turismo.data.model.DestinationMeta
import com.itanes.turismo.data.model.TouristPlace
import com.itanes.turismo.data.model.WorldSearchResult
import com.itanes.turismo.data.remote.OverpassElement
import com.itanes.turismo.data.remote.RetrofitClient
import java.text.Normalizer
import java.util.Locale
import kotlin.math.*

class TouristRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = TourismDbHelper(appContext)
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getCurrentDestination(): DestinationMeta {
        val routeKey = prefs.getString(KEY_ROUTE, "lima-peru") ?: "lima-peru"
        val title = prefs.getString(KEY_TITLE, "Lima, Perú") ?: "Lima, Perú"
        val lat = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(-12.0464)))
        val lon = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LON, java.lang.Double.doubleToRawLongBits(-77.0428)))
        return DestinationMeta(routeKey, title, lat, lon)
    }

    fun getCurrentPlaces(): List<TouristPlace> = db.getPlacesByRoute(getCurrentDestination().routeKey)
    fun getFavorites(): List<TouristPlace> = db.getFavoritePlaces()
    fun getPlace(id: Int): TouristPlace? = db.getPlace(id)
    fun setFavorite(id: Int, favorite: Boolean) = db.setFavorite(id, favorite)

    suspend fun searchWorldDestination(rawQuery: String): WorldSearchResult {
        val query = rawQuery.trim()
        require(query.length >= 2) { "Escribe una ciudad, país o lugar válido." }

        val geo = RetrofitClient.nominatimApi.search(query).firstOrNull()
            ?: error("No encontramos ese lugar. Prueba escribiendo ciudad y país, por ejemplo: París, Francia.")

        val lat = geo.lat.toDoubleOrNull() ?: error("No se pudo leer la ubicación.")
        val lon = geo.lon.toDoubleOrNull() ?: error("No se pudo leer la ubicación.")
        val routeKey = slug(query)
        val destination = DestinationMeta(routeKey, geo.displayName, lat, lon)

        var elements = emptyList<OverpassElement>()
        for (radius in listOf(8000, 20000, 50000)) {
            elements = RetrofitClient.overpassApi.query(overpassQuery(lat, lon, radius)).elements
            val candidates = elements.toPlaces(destination)
            if (candidates.size >= 5) {
                val selected = candidates.take(5).mapIndexed { index, p -> p.copy(position = index + 1) }
                db.saveRoute(routeKey, selected)
                saveDestination(destination)
                return WorldSearchResult(destination, db.getPlacesByRoute(routeKey))
            }
        }

        val selected = elements.toPlaces(destination).take(5).mapIndexed { index, p -> p.copy(position = index + 1) }
        if (selected.isEmpty()) error("Encontramos la ubicación, pero no hay puntos turísticos cercanos disponibles en OpenStreetMap.")
        db.saveRoute(routeKey, selected)
        saveDestination(destination)
        return WorldSearchResult(destination, db.getPlacesByRoute(routeKey))
    }

    suspend fun refreshWeather(place: TouristPlace): Result<Double> = runCatching {
        val response = RetrofitClient.weatherApi.getCurrentWeather(place.latitude, place.longitude)
        val temperature = response.current?.temperature ?: error("La API no devolvió temperatura")
        db.updateWeather(place.id, temperature)
        temperature
    }

    suspend fun refreshCurrentWeather(): Int {
        var updated = 0
        getCurrentPlaces().forEach { if (refreshWeather(it).isSuccess) updated++ }
        return updated
    }

    private fun saveDestination(destination: DestinationMeta) {
        prefs.edit()
            .putString(KEY_ROUTE, destination.routeKey)
            .putString(KEY_TITLE, destination.title)
            .putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(destination.latitude))
            .putLong(KEY_LON, java.lang.Double.doubleToRawLongBits(destination.longitude))
            .apply()
    }

    private fun List<OverpassElement>.toPlaces(destination: DestinationMeta): List<TouristPlace> {
        val seen = mutableSetOf<String>()
        return asSequence()
            .mapNotNull { element ->
                val tags = element.tags ?: return@mapNotNull null
                val name = tags["name"]?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null
                val lat = element.lat ?: element.center?.lat ?: return@mapNotNull null
                val lon = element.lon ?: element.center?.lon ?: return@mapNotNull null
                val unique = "${name.lowercase(Locale.ROOT)}|${"%.4f".format(Locale.US, lat)}|${"%.4f".format(Locale.US, lon)}"
                if (!seen.add(unique)) return@mapNotNull null

                val category = friendlyCategory(tags)
                val district = listOfNotNull(tags["addr:suburb"], tags["addr:city"], tags["addr:state"], tags["addr:country"])
                    .distinct().joinToString(" · ").ifBlank { destination.title.take(90) }
                val description = tags["description"]
                    ?: tags["description:es"]
                    ?: "$category ubicado cerca de ${destination.title.substringBefore(',')}. Puedes verlo en el mapa, guardar el lugar y abrir una ruta en automóvil."
                val image = tags["image"]?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
                TouristPlace(
                    id = 0,
                    remoteKey = "osm:${element.type}:${element.id}",
                    routeKey = destination.routeKey,
                    name = name,
                    district = district,
                    description = description,
                    tip = travelTip(category),
                    imageName = "",
                    imageUrl = image,
                    latitude = lat,
                    longitude = lon,
                    position = 0,
                    favorite = false,
                    temperature = null,
                    weatherUpdatedAt = null
                )
            }
            .sortedBy { haversine(destination.latitude, destination.longitude, it.latitude, it.longitude) }
            .toList()
    }

    private fun friendlyCategory(tags: Map<String, String>): String = when {
        tags["tourism"] == "museum" -> "Museo"
        tags["tourism"] == "viewpoint" -> "Mirador"
        tags["tourism"] == "gallery" -> "Galería"
        tags["tourism"] == "zoo" -> "Zoológico"
        tags["tourism"] == "aquarium" -> "Acuario"
        tags["tourism"] == "theme_park" -> "Parque temático"
        tags["tourism"] == "artwork" -> "Obra de arte"
        tags["historic"] != null -> "Lugar histórico"
        else -> "Atracción turística"
    }

    private fun travelTip(category: String): String = when (category) {
        "Museo", "Galería" -> "Revisa horarios y entradas antes de salir."
        "Mirador" -> "Consulta el clima y, si puedes, visita con buena visibilidad."
        "Lugar histórico" -> "Reserva tiempo para caminar y leer la información del lugar."
        else -> "Comprueba horarios, accesos y condiciones locales antes de iniciar la ruta."
    }

    private fun overpassQuery(lat: Double, lon: Double, radius: Int): String = """
        [out:json][timeout:25];
        (
          node(around:$radius,$lat,$lon)[tourism~"attraction|museum|viewpoint|gallery|zoo|aquarium|theme_park|artwork"][name];
          way(around:$radius,$lat,$lon)[tourism~"attraction|museum|viewpoint|gallery|zoo|aquarium|theme_park|artwork"][name];
          relation(around:$radius,$lat,$lon)[tourism~"attraction|museum|viewpoint|gallery|zoo|aquarium|theme_park|artwork"][name];
          node(around:$radius,$lat,$lon)[historic][name];
          way(around:$radius,$lat,$lon)[historic][name];
          relation(around:$radius,$lat,$lon)[historic][name];
        );
        out center tags 80;
    """.trimIndent()

    private fun slug(text: String): String {
        val normalized = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
        return normalized.ifBlank { "ruta-${System.currentTimeMillis()}" }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    companion object {
        private const val PREFS = "itanes_prefs"
        private const val KEY_ROUTE = "current_route_key"
        private const val KEY_TITLE = "current_destination_title"
        private const val KEY_LAT = "current_destination_lat"
        private const val KEY_LON = "current_destination_lon"
    }
}
