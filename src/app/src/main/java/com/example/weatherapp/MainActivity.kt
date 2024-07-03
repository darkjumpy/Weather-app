package com.example.weatherapp

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Rect
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.tianma8023.model.Time
import com.github.tianma8023.ssv.SunriseSunsetView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    val viewModel = MainActivityViewModel()

    var sunAnimated = true
    var firstBoot = true

    private val PERMISSION_REQUEST_LOCATION = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    var phoneLat: String = ""
    var phoneLon: String = ""

    var CITY: String = "Rzeszów,PL" // Domyślne miasto

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_LOCATION)
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                phoneLat = location.latitude.toString()
                phoneLon = location.longitude.toString()
                weatherTask().execute()
            } else {
                Toast.makeText(this, "Nie można uzyskać lokalizacji", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLastLocation()
            } else {
                Toast.makeText(this, "Uprawnienia do lokalizacji są wymagane", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        checkLocationPermission()
    }

    inner class weatherTask() : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            findViewById<ProgressBar>(R.id.loader).visibility = View.VISIBLE
            findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
            findViewById<TextView>(R.id.errorText).visibility = View.GONE
        }

        override fun doInBackground(vararg params: String?): String? {
            return viewModel.getApiData(phoneLat, phoneLon, CITY, !firstBoot)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            try {
                if (result == null) {
                    Toast.makeText(applicationContext, "Miasto nie zostało znalezione", Toast.LENGTH_SHORT).show()
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
                } else {
                    val jsonObj = JSONObject(result)
                    val current = jsonObj.getJSONObject("current")
                    val dailyJsonArray = jsonObj.getJSONArray("daily")
                    val hourlyJsonArray = jsonObj.getJSONArray("hourly")
                    val today = dailyJsonArray.getJSONObject(0)

                    val currentWeatherPictureName = "weather_icon_" + current.getJSONArray("weather").getJSONObject(0).getString("icon")
                    val currentWeatherIcon = resources.getIdentifier(currentWeatherPictureName, "drawable", packageName)

                    var isDay = true

                    if(currentWeatherPictureName[15] == 'd'){isDay = true}else{isDay = false}

                    val updatedAt: Long = current.getLong("dt")
                    val temp = Math.round(current.getString("temp").toDouble()).toString() + "°"
                    val tempMin = Math.round(today.getJSONObject("temp").getString("min").toDouble()).toString() + "° / " +
                            Math.round(today.getJSONObject("temp").getString("max").toDouble()).toString() + "° Odczucie " +
                            Math.round(current.getString("feels_like").toDouble()).toString() + "°"
                    val tempMax = "Max Temp: " + Math.round(today.getJSONObject("temp").getString("max").toDouble().toDouble()).toString() + "°C"
                    val pressure = current.getString("pressure")
                    val humidity = current.getString("humidity")
                    val uvi = current.getString("uvi")
                    val dewPoint = Math.round(current.getString("dew_point").toDouble()).toString()

                    val sunrise: Long = current.getLong("sunrise")
                    val sunset: Long = current.getLong("sunset")
                    val windSpeed = current.getString("wind_speed")

                    val weatherDescription = current.getJSONArray("weather").getJSONObject(0).getString("description")

                    val address = jsonObj.getString("name") + ", " + jsonObj.getString("country")

                    // Aktualizacja widoków

                    if(!isDay) {
                        findViewById<RelativeLayout>(R.id.wholeAppContainer).setBackgroundResource(R.drawable.night_gradient_bg)
                    }
                    findViewById<TextView>(R.id.address).text = address
                    findViewById<TextView>(R.id.status).text = weatherDescription
                    findViewById<ImageView>(R.id.mainWeatherImage).setImageResource(currentWeatherIcon)
                    findViewById<TextView>(R.id.temp).text = temp
                    findViewById<TextView>(R.id.temp_min).text = tempMin
                    findViewById<TextView>(R.id.pressureValue).text = uvi
                    findViewById<TextView>(R.id.humidityValue).text = humidity + "%"
                    findViewById<TextView>(R.id.pressureValue).text = pressure + " hPa"
                    findViewById<TextView>(R.id.dewPointValue).text = dewPoint + "°"

                    findViewById<TextView>(R.id.TodaySummaryLine1).text = "Przeważnie " + weatherDescription.lowercase() + "."
                    findViewById<TextView>(R.id.TodaySummaryLine2).text = "Maks. temp. " + Math.round(today.getJSONObject("temp").getString("max").toDouble()).toString() +
                            "°C, min.temp " + Math.round(today.getJSONObject("temp").getString("min").toDouble()).toString() + "°C."

                    // Pogoda godzinowa

                    // Czyszczenie rodzica
                    findViewById<LinearLayout>(R.id.weatherHourlyMainCells).removeAllViews()

                    for (i in 1..24) {

                        val thisHourWeather = hourlyJsonArray.getJSONObject(i)

                        // Tworzenie nowego widoku LinearLayout
                        val linearLayout = LinearLayout(this@MainActivity)
                        linearLayout.id = i
                        linearLayout.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                        )
                        linearLayout.orientation = LinearLayout.VERTICAL

                        // Tworzenie nowego widoku TextView
                        val hourTextView = TextView(this@MainActivity)
                        hourTextView.id = View.generateViewId()
                        hourTextView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        val thisHourDate:Long = thisHourWeather.getLong("dt")
                        hourTextView.text = SimpleDateFormat("HH:mm", Locale.ENGLISH).
                        format(Date(thisHourDate*1000))

                        hourTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER

                        // Tworzenie nowego widoku ImageView
                        val imageImageView = ImageView(this@MainActivity)
                        imageImageView.id = View.generateViewId()
                        val imageLayoutParams = LinearLayout.LayoutParams(
                            (40 * resources.displayMetrics.density).toInt(),
                            (40 * resources.displayMetrics.density).toInt()
                        )
                        imageLayoutParams.setMargins(
                            (5 * resources.displayMetrics.density).toInt(),
                            (10 * resources.displayMetrics.density).toInt(),
                            (5 * resources.displayMetrics.density).toInt(),
                            (10 * resources.displayMetrics.density).toInt()
                        )
                        imageImageView.layoutParams = imageLayoutParams

                        val hourlyWeatherPictureName = "weather_icon_"+thisHourWeather.getJSONArray("weather").getJSONObject(0).getString("icon")
                        val hourlyWeatherIcon = resources.getIdentifier(hourlyWeatherPictureName, "drawable", packageName)

                        imageImageView.setImageResource(hourlyWeatherIcon)

                        // Tworzenie nowego widoku TextView
                        val tempTextView = TextView(this@MainActivity)
                        tempTextView.id = View.generateViewId()
                        tempTextView.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )
                        tempTextView.text = Math.round(thisHourWeather.getString("temp").toDouble()).toString()+"°"
                        tempTextView.textAlignment = View.TEXT_ALIGNMENT_CENTER

                        // Dodawanie widoków do widoku LinearLayout
                        linearLayout.addView(hourTextView)
                        linearLayout.addView(imageImageView)
                        linearLayout.addView(tempTextView)

                        // Dodawanie widoku LinearLayout do rodzica
                        findViewById<LinearLayout>(R.id.weatherHourlyMainCells).addView(linearLayout)
                    }
                    updateFiveDayForecast(dailyJsonArray)

                    // Wschód i zachód słońca
                    val sunView = findViewById<SunriseSunsetView>(R.id.ssv)
                    val sunViewAnimationTrigger = findViewById<LinearLayout>(R.id.SunsetAnimationTrigger)
                    sunView.setSunriseTime(Time(
                        SimpleDateFormat("HH", Locale.ENGLISH).format(Date(sunrise * 1000)).toInt(),
                        SimpleDateFormat("mm", Locale.ENGLISH).format(Date(sunrise * 1000)).toInt()
                    ))
                    sunView.setSunsetTime(Time(
                        SimpleDateFormat("HH", Locale.ENGLISH).format(Date(sunset * 1000)).toInt(),
                        SimpleDateFormat("mm", Locale.ENGLISH).format(Date(sunset * 1000)).toInt()
                    ))

                    /// Listener przewijania
                    val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
                        val sunViewRect = Rect()
                        sunViewAnimationTrigger.getGlobalVisibleRect(sunViewRect)

                        val mainContainerRect = Rect()
                        findViewById<RelativeLayout>(R.id.mainContainer).getGlobalVisibleRect(mainContainerRect)

                        if (Rect.intersects(sunViewRect, mainContainerRect) && sunAnimated) {
                            sunAnimated = false
                            sunView.startAnimate()
                        }
                    }

                    // Dodanie listenera przewijania
                    sunView.viewTreeObserver.addOnScrollChangedListener(scrollChangedListener)

                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE


                    // Pokaż główny layout, ukryj ProgressBar
                    findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE

                    // Faza księżyca i czasy wschodu i zachodu
                    val moonPhase = today.getDouble("moon_phase")
                    val moonrise = today.getLong("moonrise")
                    val moonset = today.getLong("moonset")

                    val moonriseTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(moonrise * 1000))
                    val moonsetTime = SimpleDateFormat("HH:mm", Locale.ENGLISH).format(Date(moonset * 1000))

                    findViewById<TextView>(R.id.moonriseTime).text = moonriseTime
                    findViewById<TextView>(R.id.moonsetTime).text = moonsetTime
                    updateMoonPhaseUI(moonPhase)
                }
            } catch (e: Exception) {
                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<LinearLayout>(R.id.cityChangeContainer).visibility = View.GONE
                findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
            }
        }

        private fun updateFiveDayForecast(dailyJsonArray: JSONArray) {
            val forecastContainer = findViewById<LinearLayout>(R.id.sevenDayForecastContainer)
            forecastContainer.removeAllViews()

            val dayNames = arrayOf("Niedziela", "Poniedziałek", "Wtorek", "Środa", "Czwartek", "Piątek", "Sobota")
            val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_WEEK) - 1

            for (i in 1..7) {
                val dayWeather = dailyJsonArray.getJSONObject(i)
                val dayIndex = (today + i) % 7
                val dayName = when (i) {
                    1 -> "Jutro"
                    else -> dayNames[dayIndex]
                }

                val tempMax = Math.round(dayWeather.getJSONObject("temp").getDouble("max")).toString() + "°"
                val tempMin = Math.round(dayWeather.getJSONObject("temp").getDouble("min")).toString() + "°"
                val weatherIcon = dayWeather.getJSONArray("weather").getJSONObject(0).getString("icon")

                val dayLayout = LinearLayout(this@MainActivity)
                dayLayout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                dayLayout.orientation = LinearLayout.HORIZONTAL
                dayLayout.setPadding(0, 16, 0, 16)

                val dayNameTextView = TextView(this@MainActivity)
                dayNameTextView.layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                dayNameTextView.text = dayName
                dayNameTextView.textSize = 16f

                val weatherImageView = ImageView(this@MainActivity)
                val imageLayoutParams = LinearLayout.LayoutParams(
                    (40 * resources.displayMetrics.density).toInt(),
                    (40 * resources.displayMetrics.density).toInt()
                )

                imageLayoutParams.setMargins(
                    0,
                    0,
                    (20 * resources.displayMetrics.density).toInt(),
                    0
                )

                weatherImageView.layoutParams = imageLayoutParams
                val weatherIconRes = resources.getIdentifier("weather_icon_$weatherIcon", "drawable", packageName)
                weatherImageView.setImageResource(weatherIconRes)

                val tempMaxTextView = TextView(this@MainActivity)
                tempMaxTextView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tempMaxTextView.text = tempMax
                tempMaxTextView.textSize = 16f
                tempMaxTextView.width = (40 * resources.displayMetrics.density).toInt()

                val tempMinTextView = TextView(this@MainActivity)
                tempMinTextView.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                tempMinTextView.text = tempMin
                tempMinTextView.textSize = 16f
                tempMinTextView.width = (26 * resources.displayMetrics.density).toInt()

                dayLayout.addView(dayNameTextView)
                dayLayout.addView(weatherImageView)
                dayLayout.addView(tempMaxTextView)
                dayLayout.addView(tempMinTextView)

                forecastContainer.addView(dayLayout)
            }
        }

        private fun updateMoonPhaseUI(moonPhase: Double) {
            val moonImageView: ImageView = findViewById(R.id.moonImageView)
            val moonPhaseTextView: TextView = findViewById(R.id.moonPhaseTextView)

            val (moonPhaseImageResource, moonPhaseDescription) = when {
                moonPhase == 0.0 || moonPhase == 1.0 -> Pair(R.drawable.new_moon, "Nów")
                moonPhase == 0.25 -> Pair(R.drawable.first_quarter, "Pierwsza kwadra")
                moonPhase == 0.5 -> Pair(R.drawable.full_moon, "Pełnia")
                moonPhase == 0.75 -> Pair(R.drawable.last_quarter, "Ostatnia kwadra")
                moonPhase < 0.25 -> Pair(R.drawable.waxing_crescent, "Przybywający sierp")
                moonPhase < 0.5 -> Pair(R.drawable.waxing_gibbous, "Przybywający garb")
                moonPhase < 0.75 -> Pair(R.drawable.waning_gibbous, "Ubywający garb")
                else -> Pair(R.drawable.waning_crescent, "Ubywający sierp")
            }

            moonImageView.setImageResource(moonPhaseImageResource)
            moonPhaseTextView.text = moonPhaseDescription
        }
    }
}
