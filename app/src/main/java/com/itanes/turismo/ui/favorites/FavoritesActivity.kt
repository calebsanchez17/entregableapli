package com.itanes.turismo.ui.favorites

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.itanes.turismo.databinding.ActivityFavoritesBinding
import com.itanes.turismo.ui.detail.DetailActivity
import com.itanes.turismo.ui.main.TouristAdapter

class FavoritesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoritesBinding
    private val viewModel: FavoritesViewModel by viewModels()
    private lateinit var adapter: TouristAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        adapter = TouristAdapter { place ->
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_PLACE_ID, place.id)
            )
        }
        val columns = if (resources.configuration.screenWidthDp >= 600) 2 else 1
        binding.recyclerFavorites.layoutManager = GridLayoutManager(this, columns)
        binding.recyclerFavorites.adapter = adapter

        viewModel.favorites.observe(this) { favorites ->
            adapter.submitList(favorites)
            binding.txtEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerFavorites.visibility = if (favorites.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.load()
    }
}
