package com.itanes.turismo.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.itanes.turismo.data.model.DestinationMeta
import com.itanes.turismo.data.model.TouristPlace
import com.itanes.turismo.data.repository.TouristRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TouristRepository(application)

    private val _places = MutableLiveData<List<TouristPlace>>()
    val places: LiveData<List<TouristPlace>> = _places

    private val _destination = MutableLiveData<DestinationMeta>()
    val destination: LiveData<DestinationMeta> = _destination

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init { loadCurrentRoute() }

    fun loadCurrentRoute() {
        viewModelScope.launch {
            val data = withContext(Dispatchers.IO) {
                repository.getCurrentDestination() to repository.getCurrentPlaces()
            }
            _destination.value = data.first
            _places.value = data.second
        }
    }

    fun searchDestination(query: String) {
        if (query.trim().length < 2) {
            _message.value = "Escribe el destino que quieres visitar."
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _message.value = "Buscando destino y lugares turísticos..."
            runCatching {
                withContext(Dispatchers.IO) { repository.searchWorldDestination(query) }
            }.onSuccess { result ->
                _destination.value = result.destination
                _places.value = result.places
                _message.value = "Ruta preparada con ${result.places.size} lugares turísticos."
            }.onFailure {
                _message.value = it.message ?: "No se pudo buscar el destino. Revisa tu conexión."
            }
            _loading.value = false
        }
    }

    fun syncWeather() {
        viewModelScope.launch {
            _loading.value = true
            val count = withContext(Dispatchers.IO) { repository.refreshCurrentWeather() }
            _message.value = if (count > 0) "Clima actualizado en $count lugares." else "No se pudo actualizar el clima."
            loadCurrentRoute()
            _loading.value = false
        }
    }

    fun clearMessage() { _message.value = null }
}
