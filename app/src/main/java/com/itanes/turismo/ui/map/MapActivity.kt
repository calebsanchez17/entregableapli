package com.itanes.turismo.ui.map

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.itanes.turismo.R
import com.itanes.turismo.data.model.DestinationMeta
import com.itanes.turismo.data.model.TouristPlace
import com.itanes.turismo.data.repository.TouristRepository
import com.itanes.turismo.databinding.ActivityMapBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

class MapActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMapBinding
    private val repository by lazy { TouristRepository(this) }
    private var destination: DestinationMeta? = null
    private var places: List<TouristPlace> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.minZoomLevel = 3.0
        binding.map.maxZoomLevel = 20.0

        binding.btnGoogleRoute.setOnClickListener { openFullRoute() }

        lifecycleScope.launch {
            val data = withContext(Dispatchers.IO) { repository.getCurrentDestination() to repository.getCurrentPlaces() }
            destination = data.first
            places = data.second
            renderMap(data.first, data.second)
        }
    }

    private fun renderMap(destination: DestinationMeta, places: List<TouristPlace>) {
        binding.txtDestination.text = destination.title
        binding.txtMapInfo.text = "${places.size} paradas · toca los marcadores para ver el nombre"
        binding.map.overlays.clear()

        val points = mutableListOf<GeoPoint>()
        val start = GeoPoint(destination.latitude, destination.longitude)
        points += start

        Marker(binding.map).apply {
            position = start
            title = "Destino buscado"
            snippet = destination.title
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@MapActivity, R.drawable.ic_location_pin)
            binding.map.overlays.add(this)
        }

        places.forEach { place ->
            val point = GeoPoint(place.latitude, place.longitude)
            points += point
            Marker(binding.map).apply {
                position = point
                title = "${place.position}. ${place.name}"
                snippet = place.district
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { marker, _ ->
                    marker.showInfoWindow()
                    true
                }
                binding.map.overlays.add(this)
            }
        }

        if (places.isNotEmpty()) {
            Polyline().apply {
                setPoints(points)
                outlinePaint.strokeWidth = 7f
                outlinePaint.color = ContextCompat.getColor(this@MapActivity, R.color.cyan_500)
                binding.map.overlays.add(this)
            }
        }

        binding.map.post {
            if (points.size > 1) {
                val north = points.maxOf { it.latitude }
                val south = points.minOf { it.latitude }
                val east = points.maxOf { it.longitude }
                val west = points.minOf { it.longitude }
                binding.map.zoomToBoundingBox(BoundingBox(north, east, south, west), true, 90)
            } else {
                binding.map.controller.setZoom(12.0)
                binding.map.controller.setCenter(start)
            }
        }
        binding.map.invalidate()
    }

    private fun openFullRoute() {
        val dest = destination ?: return
        if (places.isEmpty()) {
            Toast.makeText(this, "No hay paradas para crear la ruta.", Toast.LENGTH_SHORT).show(); return
        }
        val last = places.last()
        val waypoints = places.dropLast(1).joinToString("|") { "${it.latitude},${it.longitude}" }
        val uri = Uri.Builder()
            .scheme("https")
            .authority("www.google.com")
            .appendPath("maps")
            .appendPath("dir")
            .appendPath("")
            .appendQueryParameter("api", "1")
            .appendQueryParameter("origin", "${dest.latitude},${dest.longitude}")
            .appendQueryParameter("destination", "${last.latitude},${last.longitude}")
            .appendQueryParameter("travelmode", "driving")
            .apply { if (waypoints.isNotBlank()) appendQueryParameter("waypoints", waypoints) }
            .build()
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    override fun onResume() { super.onResume(); binding.map.onResume() }
    override fun onPause() { binding.map.onPause(); super.onPause() }
}
