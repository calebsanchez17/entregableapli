package com.itanes.turismo.ui.detail

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

class DetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TouristRepository(application)

    private val _place = MutableLiveData<TouristPlace?>()
    val place: LiveData<TouristPlace?> = _place

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun load(id: Int) {
        viewModelScope.launch {
            _place.value = withContext(Dispatchers.IO) { repository.getPlace(id) }
        }
    }

    fun toggleFavorite() {
        val current = _place.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.setFavorite(current.id, !current.favorite)
            }
            load(current.id)
            _message.value = if (current.favorite) "Eliminado de favoritos" else "Guardado en favoritos"
        }
    }

    fun refreshWeather() {
        val current = _place.value ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { repository.refreshWeather(current) }
            if (result.isSuccess) {
                load(current.id)
                _message.value = "Clima actualizado"
            } else {
                _message.value = "Sin conexión: mostrando el último clima guardado"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
