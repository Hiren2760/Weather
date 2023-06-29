package com.example.weatherapp.networking

object ApiEndpoint {
    var BASEURL = "https://api.openweathermap.org/data/2.5/"
    var CurrentWeather = "weather?"
    var ListWeather = "forecast?"
    var Daily = "forecast/daily?"
    var UnitsAppid = "&units=metric&appid=fae7190d7e6433ec3a45285ffcf55c86"
    var UnitsAppidDaily = "&units=metric&cnt=15&appid=fae7190d7e6433ec3a45285ffcf55c86"
}
