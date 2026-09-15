package com.itanes.turismo.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.itanes.turismo.R
import com.itanes.turismo.databinding.ActivityMainBinding
import com.itanes.turismo.ui.about.AboutActivity
import com.itanes.turismo.ui.detail.DetailActivity
import com.itanes.turismo.ui.favorites.FavoritesActivity
import com.itanes.turismo.ui.map.MapActivity
import com.itanes.turismo.util.NetworkUtils

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: TouristAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TouristAdapter { place ->
            startActivity(Intent(this, DetailActivity::class.java).putExtra(DetailActivity.EXTRA_PLACE_ID, place.id))
        }

        val columns = if (resources.configuration.screenWidthDp >= 600) 2 else 1
        binding.recyclerPlaces.layoutManager = GridLayoutManager(this, columns)
        binding.recyclerPlaces.adapter = adapter

        binding.btnExplore.setOnClickListener { search() }
        binding.editDestination.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { search(); true } else false
        }
        binding.btnMap.setOnClickListener { startActivity(Intent(this, MapActivity::class.java)) }
        binding.btnFavorites.setOnClickListener { startActivity(Intent(this, FavoritesActivity::class.java)) }
        binding.btnAbout.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
        binding.btnSync.setOnClickListener {
            if (NetworkUtils.isOnline(this)) viewModel.syncWeather()
            else Toast.makeText(this, "Sin internet: puedes seguir usando la última ruta guardada.", Toast.LENGTH_SHORT).show()
        }

        viewModel.places.observe(this) { places ->
            adapter.submitList(places)
            binding.txtCount.text = "${places.size} lugares encontrados"
            binding.btnMap.isEnabled = places.isNotEmpty()
        }
        viewModel.destination.observe(this) { destination ->
            binding.txtRouteTitle.text = destination.title
        }
        viewModel.loading.observe(this) { loading ->
            binding.progressSearch.visibility = if (loading) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnExplore.isEnabled = !loading
        }
        viewModel.message.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }

        updateConnectionStatus()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadCurrentRoute()
        updateConnectionStatus()
    }

    private fun search() {
        val query = binding.editDestination.text?.toString().orEmpty()
        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "Necesitas internet para buscar un destino nuevo. La ruta guardada sigue disponible.", Toast.LENGTH_LONG).show()
            return
        }
        viewModel.searchDestination(query)
    }

    private fun updateConnectionStatus() {
        val online = NetworkUtils.isOnline(this)
        binding.txtConnectionStatus.text = if (online) getString(R.string.online) else getString(R.string.offline)
        binding.txtConnectionStatus.setBackgroundResource(if (online) R.drawable.bg_status_online else R.drawable.bg_status_offline)
    }
}
