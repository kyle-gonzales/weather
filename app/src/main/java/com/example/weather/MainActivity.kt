package com.example.weather

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import com.example.weather.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var binding : ActivityMainBinding? = null
    private var progressBarDialog : Dialog? = null

    private lateinit var fusedLocationClient : FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val lastLocation : Location = result.lastLocation!!
            val latitude = lastLocation.latitude
            val longitude = lastLocation.longitude

            Log.i("LAST LOCATION", "$latitude, $longitude")
            getLocationWeatherDetails(latitude, longitude)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (!isLocationEnabled()) {
            showLocationServicesAlertDialog()
        } else {
//            Toast.makeText(this, "Location is ON", Toast.LENGTH_SHORT).show()
            getLocationPermission()
        }
    }

    private fun getLocationWeatherDetails(latitude : Double, longitude : Double) {
        if (!Constants.isNetworkAvailable(this)) {
            showNetworkServicesDialog()
            return
        }

//        Toast.makeText(this, "network is available", Toast.LENGTH_SHORT).show()
        //1. create a retrofit instance
        //2. create an interface for the http requests service
        //3. create a list call for the http request
        //4. create an instance of the service interface
        //5. use the service to query the api
        //6. enqueue the call
        val retrofit : Retrofit = Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create()) // converts the data into GSON
            .build()

        val service : WeatherService = retrofit.create(WeatherService::class.java)
        val weather : Call<CurrentWeather> = service.getWeather(latitude, longitude, Constants.METRIC_UNIT, resources.getString(R.string.api_key))
        showProgressDialog()

        weather.enqueue(object : Callback<CurrentWeather> {
            override fun onResponse(call: Call<CurrentWeather>, response: Response<CurrentWeather>) {
                hideProgressDialog()

                if (response.isSuccessful) {
                    val weatherList = response.body()
                    Log.i("WEATHER_RESPONSE", weatherList.toString())

                    updateDisplay(weatherList!!)

                } else {
                    when (response.code()) {
                        in 300 .. 399 -> Log.e("WEATHER_RESPONSE", "redirected")
                        in 400 .. 499 -> Log.e("WEATHER_RESPONSE", "not found")
                        else -> Log.e("WEATHER_RESPONSE", "unforeseen error")
                    }
                }
            }

            override fun onFailure(call: Call<CurrentWeather>, t: Throwable) {
                hideProgressDialog()

                Toast.makeText(this@MainActivity, "failed to get weather", Toast.LENGTH_SHORT).show()
                t.printStackTrace()
            }

        })


    }

    @SuppressLint("SetTextI18n")
    private fun updateDisplay(weather: CurrentWeather) {
        binding?.tvMain?.text = weather.weather[0].main
        binding?.tvDescription?.text = weather.weather[0].description

        when(weather.weather[0].icon.subSequence(0,2)) {
            in arrayOf("09", "10") -> binding?.ivMain?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.rain))
            in arrayOf("02", "03", "04") -> binding?.ivMain?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.cloud))
            "11" -> binding?.ivMain?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.storm))
            "01" -> binding?.ivMain?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.sunny))
            "13" -> binding?.ivMain?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.snowflake))
            else -> binding?.ivMain?.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.cloud))
        }

        binding?.tvTemperature?.text = weather.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())

        binding?.tvHumidity?.text = weather.main.humidity.toString() + "%"

        binding?.tvMaxTemp?.text = weather.main.temp_max.toString() + getUnit(application.resources.configuration.locales.toString()) + " max"
        binding?.tvMinTemp?.text = weather.main.temp_min.toString() + getUnit(application.resources.configuration.locales.toString()) + " min"

        binding?.tvWindSpeed?.text = weather.wind.speed.toString()
        binding?.tvLocation?.text = weather.name
        binding?.tvCountry?.text = weather.sys.country
        //TODO("change the units of measurement")

        binding?.tvSunrise?.text = getUnixTime(weather.sys.sunrise)
        binding?.tvSunset?.text = getUnixTime(weather.sys.sunset)

    }

    private fun getUnixTime(timex: Long) : String? {
        val date = Date(timex * 1000L)
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        sdf.timeZone = TimeZone.getDefault()

        return sdf.format(date)
    }

    private fun getUnit(value : String): String {
        var res = "°C"
        if (value == "US" || value == "LR" || value == "MM")
            res = "°F"
        return res
    }


    private fun showNetworkServicesDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
            .setMessage("Weather App needs a network connection to work properly. Please turn on your network service")
            .setPositiveButton("Turn On Network Services") { dialog, _ ->
                dialog.dismiss()
                val networkSettingsIntent= Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
                startActivity(networkSettingsIntent)
            }
            .setNegativeButton("Close Weather App") {
                    dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationInterval : Long = 0
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, locationInterval)
            .setMaxUpdates(1)
            .setWaitForAccurateLocation(false)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.myLooper())
    }

    private fun getLocationPermission() {
        Dexter.withContext(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    requestLocationData()
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                    if (response!!.isPermanentlyDenied) {
                        showLocationPermissionsRationale()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permissionRequest: PermissionRequest?, token: PermissionToken?) {
                    token?.continuePermissionRequest()
                }

            }).onSameThread().check()
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager : LocationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    private fun showLocationPermissionsRationale() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
            .setMessage("Weather App needs your location permissions. Please allow the permissions in your settings")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                dialog.dismiss()
                try {

                    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    settingsIntent.data = uri
                    startActivity(settingsIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "Could not open settings.", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") {
                    dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }
    private fun showLocationServicesAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
            .setMessage("Weather App needs your location to work properly. Please turn on your location service")
            .setPositiveButton("Turn On Location Services") { dialog, _ ->
                dialog.dismiss()
                val locationSettingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(locationSettingsIntent)
            }
            .setNegativeButton("Close Weather App") {
                dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .show()
    }

    private fun showProgressDialog() {
        progressBarDialog = Dialog(this)
        progressBarDialog!!.setContentView(R.layout.progress_dialog_layout)
        progressBarDialog!!.show()
    }

    private fun hideProgressDialog() {
        if (progressBarDialog != null)
            progressBarDialog!!.cancel()
        progressBarDialog = null
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}