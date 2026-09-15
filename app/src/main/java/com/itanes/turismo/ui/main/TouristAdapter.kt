package com.itanes.turismo.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.itanes.turismo.R
import com.itanes.turismo.data.model.TouristPlace
import com.itanes.turismo.databinding.ItemTouristPlaceBinding
import java.util.Locale

class TouristAdapter(private val onClick: (TouristPlace) -> Unit) : RecyclerView.Adapter<TouristAdapter.PlaceViewHolder>() {
    private val items = mutableListOf<TouristPlace>()

    fun submitList(newItems: List<TouristPlace>) {
        items.clear(); items.addAll(newItems); notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlaceViewHolder(
        ItemTouristPlaceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )
    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount() = items.size

    inner class PlaceViewHolder(private val binding: ItemTouristPlaceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(place: TouristPlace) = with(binding) {
            txtPosition.text = place.position.toString()
            txtName.text = place.name
            txtDistrict.text = place.district
            txtSummary.text = place.description
            imgFavorite.visibility = if (place.favorite) View.VISIBLE else View.GONE
            txtWeather.text = place.temperature?.let { String.format(Locale.getDefault(), "Clima guardado: %.1f °C", it) } ?: "Clima disponible al sincronizar"

            val localRes = root.resources.getIdentifier(place.imageName, "drawable", root.context.packageName)
            val source: Any = when {
                !place.imageUrl.isNullOrBlank() -> place.imageUrl!!
                localRes != 0 -> localRes
                else -> R.drawable.bg_world_placeholder
            }
            Glide.with(imgPlace).load(source).centerCrop().placeholder(R.drawable.bg_world_placeholder).error(R.drawable.bg_world_placeholder).into(imgPlace)
            root.setOnClickListener { onClick(place) }
        }
    }
}
