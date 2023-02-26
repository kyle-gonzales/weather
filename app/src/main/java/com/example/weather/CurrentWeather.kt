package com.example.weather

import java.io.Serializable

data class CurrentWeather(
    val coord: Coordinates,
    val weather : List<Weather>,
    val base : String,
    val main : MainData,
    val visibility : Int,
    val wind : Wind,
    val rain : Rain,
    val clouds : Clouds,
    val dt : Int,
    val sys : SystemData,
    val timezone :  Int,
    val id : Int,
    val name : String,
    val cod : Int,
) : Serializable

data class Coordinates(
    val lon : Double,
    val lat : Double,
) : Serializable

data class Weather (
    val id : Int,
    val main : String,
    val description : String,
    val icon : String,
) : Serializable

data class MainData (
    val temp : Double,
    val feels_like : Double,
    val pressure: Double,
    val humidity : Double,
    val temp_min : Double,
    val temp_max : Double,
    val sea_level : Double,
    val grnd_level : Double,
) : Serializable

data class Wind (
    val speed : Double,
    val deg : Int,
    val gust : Double
) : Serializable

data class Clouds (
    val all : Double
) : Serializable

data class Rain (
    val hour1: Double,
    val hour3: Double,
) : Serializable

data class SystemData (
    val type : Int,
    val id : Long,
    val country : String,
    val sunrise: Long,
    val sunset : Long,
) : Serializable


