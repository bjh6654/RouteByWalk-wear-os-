package com.example.routeguidance.complication.dataclass

import com.google.gson.JsonObject

data class RoutePathResponse(
    val features: List<Feature>
)

data class Feature(
    val type: String,
    val geometry: JsonObject,
    val properties: JsonObject
)