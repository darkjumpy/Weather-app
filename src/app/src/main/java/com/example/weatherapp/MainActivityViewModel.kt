package com.example.weatherapp

import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class MainActivityViewModel: ViewModel() {

    val API: String = "7b02db76a019da40323a7ba8c275a0d9" // Klucz API

    fun getApiData(lat: String, lon: String, CITY: String, useCity: Boolean): String? {
        var geoResponse: String?
        var response: String?
        try {
            if(useCity==false){
                response = URL("https://api.openweathermap.org/data/3.0/onecall?lon=$lon&lat=$lat&units=metric&lang=pl&exclude=minutely,alerts&appid=$API").readText(Charsets.UTF_8)

                val jsonResponse = JSONObject(response)

                val geoResponse = URL("https://api.openweathermap.org/geo/1.0/reverse?lat=$lat&lon=$lon&limit=1&appid=$API").readText(Charsets.UTF_8)
                val jsonGeoObject = JSONArray(geoResponse).getJSONObject(0)
                val name = jsonGeoObject.getString("name")
                val country = jsonGeoObject.getString("country")

                jsonResponse.put("name", name)
                jsonResponse.put("country", country)
                response = jsonResponse.toString()
            }else{
                geoResponse = URL("https://api.openweathermap.org/geo/1.0/direct?q=$CITY&limit=1&appid=$API").readText(Charsets.UTF_8)

                val jsonGeoObject = JSONArray(geoResponse).getJSONObject(0)
                val name = jsonGeoObject.getString("name")
                val country = jsonGeoObject.getString("country")
                val lat = jsonGeoObject.getString("lat")
                val lon = jsonGeoObject.getString("lon")

                response = URL("https://api.openweathermap.org/data/3.0/onecall?lon=$lon&lat=$lat&units=metric&lang=pl&exclude=minutely,alerts&appid=$API").readText(Charsets.UTF_8)

                val jsonResponse = JSONObject(response)
                jsonResponse.put("name", name)
                jsonResponse.put("country", country)
                response = jsonResponse.toString()
            }

        } catch (e: Exception) {
            response = null
        }
        return response
    }
}