package com.example.routeguidance.complication.dataclass

data class POIResponse(
    val searchPoiInfo: SearchPoiInfo
)

data class SearchPoiInfo(
    val pois: Pois
)

data class Pois(
    val poi: List<Poi>
)

data class Poi(
    val id: String,
    val pkey: String,
    val telNo: String,
    val name: String,
    val upperAddrName: String,
    val middleAddrName: String,
    val lowerAddrName: String,
    val detailAddrname: String,
    val frontLat: String,
    val frontLon: String,
    val noorLat: String,
    val noorLon: String
)