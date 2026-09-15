# ITANES World Travel - Trabajo Final Android

Proyecto mejorado para **Android Studio / Empty Views Activity / Kotlin + XML**.

## Qué hace esta versión

- Permite escribir **cualquier destino del mundo**: ciudad, país, monumento o lugar.
- Geocodifica la búsqueda con **OpenStreetMap Nominatim**.
- Busca hasta **5 lugares turísticos cercanos** mediante **Overpass / OpenStreetMap**.
- Muestra un **mapa interactivo dentro de la app** con marcadores y una línea de recorrido.
- Botón para abrir una **ruta real en automóvil en Google Maps** con las paradas sugeridas.
- Consulta clima con **Open-Meteo**.
- Guarda la última ruta en **SQLite + SharedPreferences** para seguir usándola sin conexión.
- Permite guardar favoritos, compartir lugares y abrir el detalle de cada destino.
- Diseño Material 3, responsive para smartphones y tablets.

## Ejemplos de búsqueda

- París, Francia
- Tokio, Japón
- Machu Picchu, Perú
- Nueva York, Estados Unidos
- Roma, Italia
- Buenos Aires, Argentina

## Cómo abrir

1. Descomprime el ZIP.
2. Android Studio > **Open** > selecciona la carpeta `ITANES_Turismo_Mundial`.
3. Espera **Gradle Sync**.
4. Ejecuta en un emulador o celular con Android 8.0 (API 26) o superior.
5. Acepta que Android Studio descargue dependencias si las solicita.

## APIs y librerías

- Retrofit + Gson: consumo REST / JSON.
- Open-Meteo: clima.
- Nominatim: búsqueda geográfica mundial.
- Overpass API: puntos turísticos de OpenStreetMap.
- osmdroid: mapa OpenStreetMap dentro de Android.
- Glide: imágenes.
- SQLiteOpenHelper: datos offline.
- WorkManager: actualización de clima cuando hay red.

## Importante sobre Nominatim

La app hace búsquedas **solo cuando el usuario pulsa “Explorar destino”**, sin autocompletado. También identifica la aplicación con un User-Agent. Esta implementación es adecuada para demostración académica y uso moderado. Para una app comercial o con muchos usuarios conviene usar un proveedor de geocodificación con plan propio o alojar un servicio de geocodificación.

## Funcionamiento offline

- Una búsqueda nueva requiere internet.
- Después de buscar, la ruta y sus lugares quedan guardados localmente.
- Favoritos y datos guardados siguen disponibles sin internet.
- El mapa base y Google Maps necesitan conexión para cargar contenido nuevo, aunque osmdroid puede reutilizar tiles ya almacenados en caché.

## Generar APK

Android Studio > **Build > Generate App Bundles or APKs > Generate APKs**.

Para entrega firmada: **Build > Generate Signed App Bundle or APK > APK** y crea/selecciona un keystore.
