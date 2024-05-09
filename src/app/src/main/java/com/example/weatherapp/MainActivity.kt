package com.example.weatherapp

import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.widget.*
import org.json.JSONArray

class MainActivity : AppCompatActivity() {

    var CITY: String = "Rzeszów,PL" // Domyślne miasto
    val API: String = "7b02db76a019da40323a7ba8c275a0d9" // Klucz API

    fun hideKeyboard(activity: Activity) {
        val inputMethodManager = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = activity.currentFocus
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.address).setOnClickListener {

            findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
        }

        findViewById<Button>(R.id.cancelButton).setOnClickListener {
            hideKeyboard(this)
            findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
        }

        findViewById<Button>(R.id.confirmButton).setOnClickListener {

            hideKeyboard(this)
            findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.loader).visibility = View.GONE

            val cityInput = findViewById<TextView>(R.id.input_city).text.toString()
            findViewById<TextView>(R.id.input_city).text = ""
            if (cityInput.isNotEmpty()) {
                CITY = cityInput
                weatherTask().execute()
            } else {
                Toast.makeText(this@MainActivity, "Wprowadź nazwę miasta", Toast.LENGTH_SHORT).show()
            }
        }

        weatherTask().execute()
    }

    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            // Pokaż ProgressBar, ukryj główny layout
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorText).visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            var geoResponse:String?
            var response:String?
            try{
                geoResponse = URL("https://api.openweathermap.org/geo/1.0/direct?q=$CITY&limit=1&appid=$API").
                readText(Charsets.UTF_8)

                val jsonGeoObject = JSONArray(geoResponse).getJSONObject(0)
                val name = jsonGeoObject.getString("name")
                val country = jsonGeoObject.getString("country")
                val lat = jsonGeoObject.getString("lat")
                val lon = jsonGeoObject.getString("lon")

                response = URL("https://api.openweathermap.org/data/3.0/onecall?lon=$lon&lat=$lat&units=metric&lang=pl&exclude=minutely,alerts&appid=$API").
                readText(Charsets.UTF_8)

                val jsonResponse = JSONObject(response)
                jsonResponse.put("name", name)
                jsonResponse.put("country", country)
                response = jsonResponse.toString()

            }catch (e: Exception){
                response = null
            }
            return response
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {


                if (result == null) {
                    // Jeśli miasto nie zostało znalezione, wyświetlamy komunikat dla użytkownika
                    Toast.makeText(
                        applicationContext,
                        "Miasto nie zostało znalezione",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Pokaż główny layout, ukryj ProgressBar
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
                }else{
                    val jsonObj = JSONObject(result)
                    val current = jsonObj.getJSONObject("current")
                    val dailyJsonArray = jsonObj.getJSONArray("daily")
                    val today = dailyJsonArray.getJSONObject(0)

                    val currentWeatherPictureName = "weather_icon_"+current.getJSONArray("weather").getJSONObject(0).getString("icon")
                    val currentWeatherIcon = resources.getIdentifier(currentWeatherPictureName, "drawable", packageName)

                    val updatedAt:Long = current.getLong("dt")
                    val temp = Math.round(current.getString("temp").toDouble()).toString()+"°"
                    val tempMin = Math.round(today.getJSONObject("temp").getString("min").toDouble()).toString()+"° / "+
                                  Math.round(today.getJSONObject("temp").getString("max").toDouble()).toString()+"° Odczucie "+
                                  Math.round(current.getString("feels_like").toDouble()).toString()+"°"
                    val tempMax = "Max Temp: " + Math.round(today.getJSONObject("temp").getString("max").toDouble().toDouble()).toString()+"°C"
                    val pressure = current.getString("pressure")
                    val humidity = current.getString("humidity")

                    val sunrise:Long = current.getLong("sunrise")
                    val sunset:Long = current.getLong("sunset")
                    val windSpeed = current.getString("wind_speed")

                    val weatherDescription = current.getJSONArray("weather").getJSONObject(0).getString("description")

                    val address = jsonObj.getString("name")+", "+jsonObj.getString("country")

                    // Aktualizacja widoków
                    findViewById<TextView>(R.id.address).text = address
                    findViewById<TextView>(R.id.status).text = weatherDescription
                    findViewById<ImageView>(R.id.mainWeatherImage).setImageResource(currentWeatherIcon)
                    findViewById<TextView>(R.id.temp).text = temp
                    findViewById<TextView>(R.id.temp_min).text = tempMin
                    findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).
                    format(Date(sunrise*1000))
                    findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).
                    format(Date(sunset*1000))
                    findViewById<TextView>(R.id.wind).text = windSpeed
                    findViewById<TextView>(R.id.pressure).text = pressure
                    findViewById<TextView>(R.id.humidity).text = humidity

                    findViewById<TextView>(R.id.TodaySummaryLine1).text = "Przeważnie "+weatherDescription.lowercase()+"."

                    findViewById<TextView>(R.id.TodaySummaryLine2).text =
                            "Maks. temp. "+Math.round(today.getJSONObject("temp").getString("max").toDouble()).toString()+
                            "°C, min.temp "+Math.round(today.getJSONObject("temp").getString("min").toDouble()).toString()+"°C."

                    //vvv TUTAJ BĘDZIE OGARNIĘTE WPROWADZANIE DANYCH POGODOWYCH GODZINOWYCH vvv
                    for (i in 1..24) {

                    }

                    // Pokaż główny layout, ukryj ProgressBar
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                // Pokaż komunikat błędu
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.GONE
                findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
                //findViewById<TextView>(R.id.errorText).text = jsonObj.getString("cod").toString()
            }

        }

    }
}

