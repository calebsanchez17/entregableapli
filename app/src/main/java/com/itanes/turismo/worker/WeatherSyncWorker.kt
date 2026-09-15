package com.itanes.turismo.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.itanes.turismo.data.repository.TouristRepository

class WeatherSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            TouristRepository(applicationContext).refreshCurrentWeather()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object { const val WORK_NAME = "itanes_weather_sync" }
}
