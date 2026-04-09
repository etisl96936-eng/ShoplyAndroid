package com.example.shoplyandroid

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

class LocationActivity : AppCompatActivity() {

    private lateinit var tvLocation: TextView

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
        val btnGetLocation = findViewById<Button>(R.id.btnGetLocation)

        loadSavedLocation()

        btnGetLocation.setOnClickListener {
            checkLocationPermissionAndFetch()
        }
    }

    private fun checkLocationPermissionAndFetch() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

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
                } else {
                    tvLocation.text = "לא נמצא מיקום נוכחי"
                    Toast.makeText(this, "לא נמצא מיקום נוכחי", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "שגיאה בקבלת מיקום", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSavedLocation() {
        val prefs = getSharedPreferences("ShoplyPrefs", MODE_PRIVATE)
        val lat = prefs.getString("LAST_LATITUDE", null)
        val lon = prefs.getString("LAST_LONGITUDE", null)

        tvLocation.text = if (lat != null && lon != null) {
            "מיקום שמור:\nLatitude: $lat\nLongitude: $lon"
        } else {
            "עדיין לא נשמר מיקום"
        }
    }
}