package com.example.shoplyandroid

/**
 * מודל תשובה מ-API של Open-Meteo למזג אוויר.
 * מכיל את נתוני מזג האוויר הנוכחיים.
 *
 * @param current אובייקט הנתונים הנוכחיים — יכול להיות null אם ה-API לא החזיר נתונים
 */
data class WeatherResponse(
    val current: CurrentWeather?
)

/**
 * נתוני מזג האוויר הנוכחיים המתקבלים מה-API.
 *
 * @param temperature_2m טמפרטורה בגובה 2 מטר מהקרקע ב-°C
 * @param wind_speed_10m מהירות רוח בגובה 10 מטר מהקרקע בקמ"ש
 */
data class CurrentWeather(
    val temperature_2m: Double?,
    val wind_speed_10m: Double?
)