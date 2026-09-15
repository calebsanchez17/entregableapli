package com.itanes.turismo.ui.detail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.itanes.turismo.R
import com.itanes.turismo.data.model.TouristPlace
import com.itanes.turismo.databinding.ActivityDetailBinding
import com.itanes.turismo.ui.map.MapActivity
import com.itanes.turismo.util.NetworkUtils
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class DetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailBinding
    private val viewModel: DetailViewModel by viewModels()
    private var currentPlace: TouristPlace? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val placeId = intent.getIntExtra(EXTRA_PLACE_ID, -1)
        if (placeId == -1) { finish(); return }

        viewModel.place.observe(this) { place -> if (place != null) { currentPlace = place; render(place) } }
        viewModel.message.observe(this) { msg -> if (!msg.isNullOrBlank()) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() } }

        binding.btnFavorite.setOnClickListener { viewModel.toggleFavorite() }
        binding.btnMap.setOnClickListener { startActivity(Intent(this, MapActivity::class.java)) }
        binding.btnRoute.setOnClickListener { currentPlace?.let(::openNavigation) }
        binding.btnShare.setOnClickListener { currentPlace?.let(::sharePlace) }

        viewModel.load(placeId)
        if (NetworkUtils.isOnline(this)) viewModel.refreshWeather()
    }

    private fun render(place: TouristPlace) = with(binding) {
        toolbar.title = place.name
        txtName.text = place.name
        txtDistrict.text = place.district
        txtDescription.text = place.description
        txtTip.text = place.tip
        btnFavorite.text = if (place.favorite) getString(R.string.remove_favorite) else getString(R.string.save_favorite)

        val localRes = resources.getIdentifier(place.imageName, "drawable", packageName)
        val source: Any = when {
            !place.imageUrl.isNullOrBlank() -> place.imageUrl!!
            localRes != 0 -> localRes
            else -> R.drawable.bg_world_placeholder
        }
        Glide.with(imgPlace).load(source).centerCrop().placeholder(R.drawable.bg_world_placeholder).error(R.drawable.bg_world_placeholder).into(imgPlace)

        txtWeather.text = place.temperature?.let { String.format(Locale.getDefault(), "%.1f °C", it) } ?: "Sin datos guardados"
        txtWeatherNote.text = place.weatherUpdatedAt?.let {
            "Última sincronización: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))}"
        } ?: "Con internet se consulta Open-Meteo; sin conexión se mantiene el último dato."
    }

    private fun openNavigation(place: TouristPlace) {
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${place.latitude},${place.longitude}&travelmode=driving")
        startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    private fun sharePlace(place: TouristPlace) {
        val mapUrl = "https://www.google.com/maps/search/?api=1&query=${place.latitude},${place.longitude}"
        val text = "${place.name}\n${place.district}\n${place.description}\n$mapUrl"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "Destino recomendado por ITANES"); putExtra(Intent.EXTRA_TEXT, text)
        }, "Compartir destino"))
    }

    companion object { const val EXTRA_PLACE_ID = "extra_place_id" }
}
