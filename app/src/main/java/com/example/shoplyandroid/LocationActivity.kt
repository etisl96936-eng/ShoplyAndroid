package com.example.shoplyandroid

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * מסך מיקום ומזג אוויר.
 * מאפשר קבלת מיקום GPS נוכחי ושמירתו, ומציג נתוני מזג אוויר
 * בזמן אמת דרך API חיצוני (Open-Meteo) באמצעות Retrofit.
 */
class LocationActivity : AppCompatActivity() {

    private lateinit var tvLocation: TextView
    private lateinit var tvWeather: TextView

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "נדרשת הרשאת מיקום", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        tvLocation = findViewById(R.id.tvLocation)
        tvWeather = findViewById(R.id.tvWeather)
        val btnGetLocation = findViewById<Button>(R.id.btnGetLocation)

        loadSavedLocation()

        btnGetLocation.setOnClickListener {
            checkLocationPermissionAndFetch()
        }
    }

    /**
     * בודק אם הרשאת מיקום ניתנה.
     * אם כן — מקבל מיקום מיידית, אחרת — מבקש הרשאה מהמשתמש.
     */
    private fun checkLocationPermissionAndFetch() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    /**
     * מקבל את המיקום האחרון הידוע של המכשיר באמצעות FusedLocationProvider.
     * שומר את הקואורדינטות ב-SharedPreferences וטוען מזג אוויר תואם.
     * ה-annotation מדכא אזהרת הרשאה — ההרשאה נבדקת לפני קריאה לפונקציה זו.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    tvLocation.text = "Latitude: $latitude\nLongitude: $longitude"

                    getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
                        .edit()
                        .putString("LAST_LATITUDE", latitude.toString())
                        .putString("LAST_LONGITUDE", longitude.toString())
                        .apply()

                    Toast.makeText(this, "המיקום נשמר בהצלחה", Toast.LENGTH_SHORT).show()
                    fetchWeather(latitude, longitude)
                } else {
                    tvLocation.text = "לא נמצא מיקום נוכחי"
                    tvWeather.text = "לא ניתן לטעון מזג אוויר בלי מיקום"
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה בקבלת מיקום", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * שולח בקשה ל-API של Open-Meteo לקבלת נתוני מזג אוויר נוכחיים.
     * מציג טמפרטורה ומהירות רוח על המסך.
     *
     * @param latitude קו הרוחב של המיקום
     * @param longitude קו האורך של המיקום
     */
    private fun fetchWeather(latitude: Double, longitude: Double) {
        tvWeather.text = "טוען מזג אוויר..."

        WeatherRetrofitClient.apiService
            .getCurrentWeather(latitude, longitude)
            .enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val current = response.body()?.current
                        val temperature = current?.temperature_2m
                        val windSpeed = current?.wind_speed_10m

                        if (temperature != null && windSpeed != null) {
                            tvWeather.text =
                                "מזג אוויר נוכחי:\nטמפרטורה: $temperature°C\nמהירות רוח: $windSpeed קמ״ש"
                        } else {
                            tvWeather.text = "לא התקבלו נתוני מזג אוויר"
                        }
                    } else {
                        tvWeather.text = "שגיאה בטעינת מזג האוויר"
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    tvWeather.text = "כשל בתקשורת עם שירות מזג האוויר"
                }
            })
    }

    /**
     * טוענת מיקום שמור מ-SharedPreferences ומציגה אותו על המסך.
     * אם קיים מיקום שמור — טוענת גם מזג אוויר מתאים.
     */
    private fun loadSavedLocation() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val lat = prefs.getString("LAST_LATITUDE", null)
        val lon = prefs.getString("LAST_LONGITUDE", null)

        if (lat != null && lon != null) {
            tvLocation.text = "מיקום שמור:\nLatitude: $lat\nLongitude: $lon"

            val latitude = lat.toDoubleOrNull()
            val longitude = lon.toDoubleOrNull()

            if (latitude != null && longitude != null) {
                fetchWeather(latitude, longitude)
            } else {
                tvWeather.text = "המיקום השמור אינו תקין"
            }
        } else {
            tvLocation.text = "עדיין לא נשמר מיקום"
            tvWeather.text = "מזג אוויר עדיין לא נטען"
        }
    }
}