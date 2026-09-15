CREATE TABLE tourist_places (
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
);
CREATE INDEX idx_route_key ON tourist_places(route_key);
