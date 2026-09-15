package com.itanes.turismo.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.itanes.turismo.data.model.TouristPlace
import com.itanes.turismo.data.repository.TouristRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TouristRepository(application)
    private val _favorites = MutableLiveData<List<TouristPlace>>()
    val favorites: LiveData<List<TouristPlace>> = _favorites

    fun load() {
        viewModelScope.launch {
            _favorites.value = withContext(Dispatchers.IO) { repository.getFavorites() }
        }
    }
}
