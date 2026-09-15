# Plan de pruebas - ITANES World Travel

1. **Inicio sin conexión:** desactivar Wi‑Fi/datos y abrir la app. Debe mostrarse la última ruta guardada (la primera instalación incluye Lima como ejemplo base).
2. **Búsqueda mundial:** con internet, escribir `París, Francia`, `Tokio, Japón`, `Roma, Italia` u otro destino y pulsar **Explorar destino**. Debe geocodificar el lugar y cargar hasta 5 puntos turísticos cercanos.
3. **Mapa interactivo:** pulsar **Ver mapa interactivo y ruta**. Deben aparecer el destino, marcadores de las paradas, zoom/pan con gestos y una línea que conecta el itinerario.
4. **Ruta en automóvil:** desde el mapa pulsar **Abrir ruta real en automóvil**. Debe abrir Google Maps o el navegador con origen, destino y paradas intermedias.
5. **Detalle:** tocar una tarjeta. Debe abrir `DetailActivity` mediante `Intent` con `EXTRA_PLACE_ID`, mostrando descripción, clima, favorito, mapa y compartir.
6. **Favoritos offline:** guardar un lugar como favorito, cerrar y volver a abrir. Debe persistir en SQLite incluso sin internet.
7. **Clima:** con internet, tocar **Actualizar clima**. Retrofit consulta Open‑Meteo para las paradas actuales y guarda el último valor en SQLite.
8. **Recuperación de red:** si una API falla, la ruta local no debe borrarse. WorkManager volverá a intentar la actualización cuando haya red.
9. **Búsqueda sin internet:** escribir un destino nuevo sin red. La app debe avisar que una búsqueda nueva necesita conexión, pero mantener disponible la ruta guardada.
10. **Tablet:** en pantalla >= 600dp, la lista debe usar dos columnas; en smartphone, una.

> Para la entrega final se recomienda tomar capturas de estas pruebas desde el emulador o un dispositivo real y adjuntarlas como evidencias.
