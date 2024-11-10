package com.example.routeguidance.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import com.example.routeguidance.R
import com.example.routeguidance.presentation.theme.RouteGuidanceTheme
import com.skt.tmap.TMapView
import com.skt.tmap.TMapPoint

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // 권한이 허용되면 현재 위치를 가져오고 맵을 이동시킵니다.
            tMapView?.let { moveToCurrentLocation(it) }
        }
    }

    var tMapView: TMapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(tMapView)
        }

        tMapView = TMapView(this)
        tMapView?.setSKTMapApiKey("9fa74b98-aec6-4555-95a8-61ce351346f7")

        tMapView?.setOnMapReadyListener {
            Log.v("print", "loaded")
            // 맵이 준비되면 현재 위치로 이동
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                moveToCurrentLocation(tMapView!!)
            } else {
                // 권한 요청
                Log.v("print", "try get permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun moveToCurrentLocation(tMapView: TMapView) {
        // 현재 위치 가져오기
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        val listener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d("map_test,","${location.latitude}, ${location.longitude}, ${location.accuracy}")
            }
        }
//        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000L, 10f, listener)
//        // (.. 생략 ..) //
//        manager.removeUpdates(listener)
//        Log.v("print", currentLocation.toString())
//        currentLocation?.let {
//            // 현재 위치로 맵의 중심을 이동
//            val tMapPoint = TMapPoint(it.latitude, it.longitude)
//            tMapView.setCenterPoint(tMapPoint.longitude, tMapPoint.latitude)
//        }
    }
}

@Composable
fun WearApp(tmapview: TMapView?) {
    RouteGuidanceTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            // 지도 컴포넌트를 여기에 배치
            MapView(tmapview)

//            // 시간 표시를 검은 배경에 흰색 글씨로 표시
            TimeTextWithBackground()
//
//            // 인사 메시지
//            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun MapView(tmapview: TMapView?) {
    AndroidView(
        factory = { context ->
            tmapview!!
        }
    )
}

@Composable
fun TimeTextWithBackground() {
    Box(
        modifier = Modifier
            .background(Color.Transparent)  // 검은색 배경 설정
        ,contentAlignment = Alignment.Center
    ) {
        TimeText(
            modifier = Modifier.padding(8.dp),
        )
    }
}