package com.example.routeguidance.complication

import com.example.routeguidance.complication.dataclass.POIResponse
import com.example.routeguidance.complication.dataclass.RoutePathResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface TMapApiService {
    // TMap POI 검색 API 요청
    @GET("tmap/pois")
    suspend fun searchPOIs(
        @Query("version") version: Int = 1,
        @Query("searchKeyword") keyword: String,
        @Query("poiGroupYn") poiGroupYn: String = "Y",
        @Query("centerLat") centerLat: Float? = null,
        @Query("centerLon") centerLon: Float? = null,
        @Query("radius") radius: Int = 0,
        @Query("page") page: Int = 1,
        @Header("appKey") appKey: String = com.example.routeguidance.BuildConfig.TMAP_API_KEY // TMap API Key
    ): POIResponse

    // TMap 보행자 경로 요청
    @POST("tmap/routes/pedestrian")
    suspend fun routePedestrian(
        @Query("version") version: Int = 1,
        @Body startX: Float,
        @Body startY: Float,
        @Body endX: Float,
        @Body endY: Float,
        @Body startName: String = "출발",
        @Body endName: String = "도착",
        @Header("appKey") appKey: String = com.example.routeguidance.BuildConfig.TMAP_API_KEY // TMap API Key
    ): RoutePathResponse
}