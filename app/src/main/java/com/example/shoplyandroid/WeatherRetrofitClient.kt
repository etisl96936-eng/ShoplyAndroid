package com.example.shoplyandroid

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Singleton המכיל את הגדרת לקוח ה-Retrofit לשירות מזג האוויר.
 * משתמש ב-lazy initialization — נוצר רק בפעם הראשונה שנגשים אליו.
 * ה-BASE_URL מצביע על ה-API החינמי של Open-Meteo.
 */
object WeatherRetrofitClient {

    private const val BASE_URL = "https://api.open-meteo.com/"

    /**
     * מופע יחיד של ה-API service.
     * נוצר בפעם הראשונה שנגשים אליו ונשמר לשימוש חוזר.
     * GsonConverterFactory ממיר אוטומטית את תשובת ה-JSON לאובייקטי Kotlin.
     */
    val apiService: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}