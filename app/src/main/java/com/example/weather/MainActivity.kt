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
import android.content.ActivityNotFoundException
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class MainActivity : AppCompatActivity() {

    private var binding : ActivityMainBinding? = null

    private lateinit var fusedLocationClient : FusedLocationProviderClient

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val lastLocation : Location = result.lastLocation!!
            val latitude = lastLocation.latitude
            val longitude = lastLocation.longitude

            Log.i("LAST LOCATION", "$latitude, $longitude")

            getLocationWeatherDetails()
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

    private fun getLocationWeatherDetails() {
        if (!Constants.isNetworkAvailable(this)) {
            showNetworkServicesDialog()
            return
        }

//        Toast.makeText(this, "network is available", Toast.LENGTH_SHORT).show()

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
        val locationManager : LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
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


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}