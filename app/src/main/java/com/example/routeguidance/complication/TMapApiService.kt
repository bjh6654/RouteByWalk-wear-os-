package com.example.routeguidance.complication

import com.example.routeguidance.complication.dataclass.POIResponse
import retrofit2.http.GET
import retrofit2.http.Header
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
//        @Query("radius") radius: Int = 0, // 기본 반경을 500m로 설정
        @Query("page") page: Int = 1,
        @Header("appKey") appKey: String = com.example.routeguidance.BuildConfig.TMAP_API_KEY // TMap API Key
    ): POIResponse
}