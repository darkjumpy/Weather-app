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

class MainActivity : AppCompatActivity() {

    var CITY: String = "rzeszow,pl" // Domyślne miasto
    val API: String = "f01e80368f05c66b03425d3f08ab1a1c" // Klucz API

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

        findViewById<Button>(R.id.buttonChangeCity).setOnClickListener {

            findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.VISIBLE
            findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
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
            var response:String?
            try{
                response = URL("https://api.openweathermap.org/data/2.5/weather?q=$CITY&units=metric&lang=pl&appid=$API").
                readText(Charsets.UTF_8)

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
                    val main = jsonObj.getJSONObject("main")
                    val sys = jsonObj.getJSONObject("sys")
                    val wind = jsonObj.getJSONObject("wind")
                    val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

                    val updatedAt:Long = jsonObj.getLong("dt")
                    val updatedAtText = "Zaktualizowano: "+ SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).
                    format(Date(updatedAt*1000))
                    val temp = Math.round(main.getString("temp").toDouble()).toString()+"°C"
                    val tempMin = "Min Temp: " + main.getString("temp_min")+"°C"
                    val tempMax = "Max Temp: " + main.getString("temp_max")+"°C"
                    val pressure = main.getString("pressure")
                    val humidity = main.getString("humidity")

                    val sunrise:Long = sys.getLong("sunrise")
                    val sunset:Long = sys.getLong("sunset")
                    val windSpeed = wind.getString("speed")
                    val weatherDescription = weather.getString("description")

                    val address = jsonObj.getString("name")+", "+sys.getString("country")

                    // Aktualizacja widoków
                    findViewById<TextView>(R.id.address).text = address
                    findViewById<TextView>(R.id.updated_at).text =  updatedAtText
                    findViewById<TextView>(R.id.status).text = weatherDescription.capitalize()
                    findViewById<TextView>(R.id.temp).text = temp
                    findViewById<TextView>(R.id.temp_min).text = tempMin
                    findViewById<TextView>(R.id.temp_max).text = tempMax
                    findViewById<TextView>(R.id.sunrise).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).
                    format(Date(sunrise*1000))
                    findViewById<TextView>(R.id.sunset).text = SimpleDateFormat("hh:mm a", Locale.ENGLISH).
                    format(Date(sunset*1000))
                    findViewById<TextView>(R.id.wind).text = windSpeed
                    findViewById<TextView>(R.id.pressure).text = pressure
                    findViewById<TextView>(R.id.humidity).text = humidity

                    // Pokaż główny layout, ukryj ProgressBar
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    //findViewById<RelativeLayout>(R.id.cityChangeContainer).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                // Pokaż komunikat błędu
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                //findViewById<RelativeLayout>(R.id.cityChangeContainer).visibility = View.GONE
                findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
                //findViewById<TextView>(R.id.errorText).text = jsonObj.getString("cod").toString()
            }

        }

    }
}

