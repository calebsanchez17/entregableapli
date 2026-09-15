package com.itanes.turismo.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.itanes.turismo.data.model.TouristPlace

class TourismDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PLACES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                remote_key TEXT NOT NULL UNIQUE,
                route_key TEXT NOT NULL,
                name TEXT NOT NULL,
                district TEXT NOT NULL,
                description TEXT NOT NULL,
                tip TEXT NOT NULL,
                image_name TEXT NOT NULL DEFAULT '',
                image_url TEXT,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                position INTEGER NOT NULL,
                favorite INTEGER NOT NULL DEFAULT 0,
                temperature REAL,
                weather_updated_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_route_key ON $TABLE_PLACES(route_key)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PLACES")
        onCreate(db)
    }

    private fun seed(db: SQLiteDatabase) {
        seedPlaces.forEach { upsert(db, it) }
    }

    fun getPlacesByRoute(routeKey: String): List<TouristPlace> = readPlaces(
        "SELECT * FROM $TABLE_PLACES WHERE route_key = ? ORDER BY position ASC",
        arrayOf(routeKey)
    )

    fun getFavoritePlaces(): List<TouristPlace> = readPlaces(
        "SELECT * FROM $TABLE_PLACES WHERE favorite = 1 ORDER BY name COLLATE NOCASE ASC",
        emptyArray()
    )

    fun getPlace(id: Int): TouristPlace? = readPlaces(
        "SELECT * FROM $TABLE_PLACES WHERE id = ? LIMIT 1",
        arrayOf(id.toString())
    ).firstOrNull()

    fun saveRoute(routeKey: String, places: List<TouristPlace>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // Archiva favoritos de una búsqueda anterior para que la ruta actual siempre tenga solo sus paradas.
            val archived = ContentValues().apply { put("route_key", "favorites-archive") }
            db.update(TABLE_PLACES, archived, "favorite = 1 AND route_key = ? AND remote_key LIKE 'osm:%'", arrayOf(routeKey))
            db.delete(TABLE_PLACES, "favorite = 0 AND remote_key LIKE 'osm:%'", null)
            places.forEach { upsert(db, it) }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setFavorite(id: Int, favorite: Boolean) {
        val values = ContentValues().apply { put("favorite", if (favorite) 1 else 0) }
        writableDatabase.update(TABLE_PLACES, values, "id = ?", arrayOf(id.toString()))
    }

    fun updateWeather(id: Int, temperature: Double, updatedAt: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply {
            put("temperature", temperature)
            put("weather_updated_at", updatedAt)
        }
        writableDatabase.update(TABLE_PLACES, values, "id = ?", arrayOf(id.toString()))
    }

    private fun upsert(db: SQLiteDatabase, place: TouristPlace) {
        val oldFavorite = db.rawQuery(
            "SELECT favorite FROM $TABLE_PLACES WHERE remote_key = ? LIMIT 1",
            arrayOf(place.remoteKey)
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else null }

        val values = ContentValues().apply {
            put("remote_key", place.remoteKey)
            put("route_key", place.routeKey)
            put("name", place.name)
            put("district", place.district)
            put("description", place.description)
            put("tip", place.tip)
            put("image_name", place.imageName)
            if (place.imageUrl == null) putNull("image_url") else put("image_url", place.imageUrl)
            put("latitude", place.latitude)
            put("longitude", place.longitude)
            put("position", place.position)
            put("favorite", oldFavorite ?: if (place.favorite) 1 else 0)
            place.temperature?.let { put("temperature", it) }
            place.weatherUpdatedAt?.let { put("weather_updated_at", it) }
        }

        if (oldFavorite == null) {
            db.insert(TABLE_PLACES, null, values)
        } else {
            db.update(TABLE_PLACES, values, "remote_key = ?", arrayOf(place.remoteKey))
        }
    }

    private fun readPlaces(sql: String, args: Array<String>): List<TouristPlace> {
        val result = mutableListOf<TouristPlace>()
        readableDatabase.rawQuery(sql, args).use { cursor ->
            while (cursor.moveToNext()) {
                result += TouristPlace(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    remoteKey = cursor.getString(cursor.getColumnIndexOrThrow("remote_key")),
                    routeKey = cursor.getString(cursor.getColumnIndexOrThrow("route_key")),
                    name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                    district = cursor.getString(cursor.getColumnIndexOrThrow("district")),
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                    tip = cursor.getString(cursor.getColumnIndexOrThrow("tip")),
                    imageName = cursor.getString(cursor.getColumnIndexOrThrow("image_name")),
                    imageUrl = if (cursor.isNull(cursor.getColumnIndexOrThrow("image_url"))) null else cursor.getString(cursor.getColumnIndexOrThrow("image_url")),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                    position = cursor.getInt(cursor.getColumnIndexOrThrow("position")),
                    favorite = cursor.getInt(cursor.getColumnIndexOrThrow("favorite")) == 1,
                    temperature = cursor.getColumnIndexOrThrow("temperature").let { if (cursor.isNull(it)) null else cursor.getDouble(it) },
                    weatherUpdatedAt = cursor.getColumnIndexOrThrow("weather_updated_at").let { if (cursor.isNull(it)) null else cursor.getLong(it) }
                )
            }
        }
        return result
    }

    companion object {
        private const val DATABASE_NAME = "itanes_turismo.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_PLACES = "tourist_places"

        private val seedPlaces = listOf(
            TouristPlace(0,"seed:lima:1","lima-peru","Plaza Mayor de Lima","Centro Histórico · Lima, Perú","Centro histórico de la capital peruana, rodeado por edificios emblemáticos y espacios culturales.","Visita por la mañana y recorre a pie las calles del centro histórico.","plaza_mayor",null,-12.0453,-77.0308,1,false,null,null),
            TouristPlace(0,"seed:lima:2","lima-peru","Circuito Mágico del Agua","Parque de la Reserva · Lima, Perú","Complejo de fuentes ornamentales, iluminación y espectáculos de agua, ideal para una visita de tarde o noche.","Consulta los horarios del espectáculo principal antes de salir.","circuito_agua",null,-12.0709,-77.0341,2,false,null,null),
            TouristPlace(0,"seed:lima:3","lima-peru","Huaca Pucllana","Miraflores · Lima, Perú","Complejo arqueológico prehispánico de adobe ubicado en Miraflores, integrado dentro del entorno urbano.","Reserva tiempo para el recorrido guiado y revisa el horario de atención.","huaca_pucllana",null,-12.1107,-77.0332,3,false,null,null),
            TouristPlace(0,"seed:lima:4","lima-peru","Parque del Amor","Miraflores · Lima, Perú","Parque frente al océano Pacífico, conocido por su vista del litoral y sus espacios para caminar y descansar.","La puesta de sol suele ser uno de los mejores momentos para visitarlo.","parque_amor",null,-12.1263,-77.0363,4,false,null,null),
            TouristPlace(0,"seed:lima:5","lima-peru","Museo Larco","Pueblo Libre · Lima, Perú","Museo de arte precolombino con una importante colección arqueológica peruana y jardines visitables.","Revisa el tiempo disponible para recorrer las salas y los jardines sin apuro.","museo_larco",null,-12.0725,-77.0708,5,false,null,null)
        )
    }
}
