package com.example.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class WeatherReport(
    val locationName: String,
    val country: String = "",
    val latitude: Double,
    val longitude: Double,
    val temperature: Double,
    val feelsLike: Double,
    val condition: String,
    val weatherCode: Int,
    val humidity: Int,
    val windSpeed: Double,
    val tempMax: Double,
    val tempMin: Double,
    val dateText: String,
    val isDay: Boolean = true,
    val localHour: Int = -1
)

object WeatherService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Resolves WMO weather codes to human-readable weather descriptions
     */
    fun decodeWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61 -> "Light Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow Fall"
            77 -> "Snow Grains"
            80, 81, 82 -> "Rain Showers"
            85, 86 -> "Snow Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with Hail"
            else -> "Cloudy"
        }
    }

    /**
     * Fetches real weather report for the device's current location using GPS/Network provider
     */
    suspend fun getCurrentLocationWeather(context: Context): WeatherReport = withContext(Dispatchers.IO) {
        val (lat, lon, detectedCity) = try {
            getDeviceCoordinates(context)
        } catch (e: Exception) {
            Log.e("WeatherService", "Error reading location: ${e.message}")
            Triple(28.6139, 77.2090, "New Delhi") // Default to New Delhi if location not yet available
        }

        val resolvedCity = if (detectedCity.isNotEmpty()) detectedCity else resolveCityName(context, lat, lon)
        fetchWeatherByCoordinates(lat, lon, resolvedCity)
    }

    /**
     * Searches any city name (e.g. "Delhi", "Mumbai", "London", "Tokyo", etc.) and returns real weather
     */
    suspend fun getWeatherForQuery(context: Context, query: String): WeatherReport = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || trimmed.equals("current", ignoreCase = true) || trimmed.equals("here", ignoreCase = true)) {
            return@withContext getCurrentLocationWeather(context)
        }

        // Geocode city query using Open-Meteo Geocoding API
        val encodedCity = URLEncoder.encode(trimmed, "UTF-8")
        val geocodingUrl = "https://geocoding-api.open-meteo.com/v1/search?name=$encodedCity&count=1&language=en&format=json"

        try {
            val req = Request.Builder().url(geocodingUrl).build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()

            if (!resp.isSuccessful || body.isNullOrEmpty()) {
                return@withContext getCurrentLocationWeather(context)
            }

            val parsed = json.parseToJsonElement(body).jsonObject
            val results = parsed["results"]?.jsonArray
            if (results != null && results.isNotEmpty()) {
                val first = results[0].jsonObject
                val lat = first["latitude"]?.jsonPrimitive?.doubleOrNull ?: 28.6139
                val lon = first["longitude"]?.jsonPrimitive?.doubleOrNull ?: 77.2090
                val cityName = first["name"]?.jsonPrimitive?.content ?: trimmed
                val country = first["country"]?.jsonPrimitive?.content ?: ""

                val locationLabel = if (country.isNotEmpty()) "$cityName, $country" else cityName
                return@withContext fetchWeatherByCoordinates(lat, lon, locationLabel)
            }
        } catch (e: Exception) {
            Log.e("WeatherService", "Geocoding error: ${e.message}")
        }

        // Fallback to current location weather
        getCurrentLocationWeather(context)
    }

    /**
     * Fetches exact live weather from Open-Meteo API using latitude and longitude
     */
    private suspend fun fetchWeatherByCoordinates(
        lat: Double,
        lon: Double,
        locationLabel: String
    ): WeatherReport = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&timezone=auto"

        val req = Request.Builder().url(url).build()
        val resp = client.newCall(req).execute()
        val body = resp.body?.string() ?: throw IllegalStateException("Empty weather response")

        val root = json.parseToJsonElement(body).jsonObject
        val current = root["current"]?.jsonObject ?: throw IllegalStateException("No current weather data")
        val daily = root["daily"]?.jsonObject

        val temp = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: 25.0
        val feelsLike = current["apparent_temperature"]?.jsonPrimitive?.doubleOrNull ?: temp
        val humidity = current["relative_humidity_2m"]?.jsonPrimitive?.intOrNull ?: 65
        val windSpeed = current["wind_speed_10m"]?.jsonPrimitive?.doubleOrNull ?: 8.0
        val weatherCode = current["weather_code"]?.jsonPrimitive?.intOrNull ?: 0
        val isDay = (current["is_day"]?.jsonPrimitive?.intOrNull ?: 1) == 1

        val maxTemps = daily?.get("temperature_2m_max")?.jsonArray
        val minTemps = daily?.get("temperature_2m_min")?.jsonArray
        val tempMax = maxTemps?.firstOrNull()?.jsonPrimitive?.doubleOrNull ?: (temp + 3.0)
        val tempMin = minTemps?.firstOrNull()?.jsonPrimitive?.doubleOrNull ?: (temp - 3.0)

        val dateFormatted = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date())

        val timeStr = current["time"]?.jsonPrimitive?.content ?: ""
        val localHour = try {
            if (timeStr.contains("T")) {
                timeStr.substringAfter("T").substringBefore(":").toIntOrNull() ?: -1
            } else -1
        } catch (e: Exception) {
            -1
        }

        WeatherReport(
            locationName = locationLabel,
            latitude = lat,
            longitude = lon,
            temperature = temp,
            feelsLike = feelsLike,
            condition = decodeWeatherCode(weatherCode),
            weatherCode = weatherCode,
            humidity = humidity,
            windSpeed = windSpeed,
            tempMax = tempMax,
            tempMin = tempMin,
            dateText = dateFormatted,
            isDay = isDay,
            localHour = localHour
        )
    }

    private fun getDeviceCoordinates(context: Context): Triple<Double, Double, String> {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return Triple(28.6139, 77.2090, "New Delhi")
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return Triple(28.6139, 77.2090, "New Delhi")

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        var bestLocation: Location? = null

        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null && (bestLocation == null || loc.accuracy < bestLocation.accuracy)) {
                        bestLocation = loc
                    }
                }
            } catch (e: SecurityException) {
                // Ignore and try next
            }
        }

        return if (bestLocation != null) {
            val city = resolveCityName(context, bestLocation.latitude, bestLocation.longitude)
            Triple(bestLocation.latitude, bestLocation.longitude, city)
        } else {
            Triple(28.6139, 77.2090, "New Delhi")
        }
    }

    private fun resolveCityName(context: Context, lat: Double, lon: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Async geocoder supported on T+
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "My Location"
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: "My Location"
            }
        } catch (e: Exception) {
            "My Location"
        }
    }
}
