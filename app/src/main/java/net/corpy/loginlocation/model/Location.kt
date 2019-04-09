package net.corpy.loginlocation.model

data class Location(
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var title: String = "",
    var radius: Double = 0.0
)