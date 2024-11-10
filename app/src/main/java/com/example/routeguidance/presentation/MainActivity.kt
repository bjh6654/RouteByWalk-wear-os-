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
import com.skt.tmap.BuildConfig
import com.skt.tmap.TMapGpsManager
import com.skt.tmap.TMapView

class MainActivity : ComponentActivity(), TMapGpsManager.OnLocationChangedListener {
    var tMapView: TMapView? = null
    var userLocation: Location? = null

    override fun onLocationChange(location: Location) {
        this.userLocation = location
        moveToCurrentLocation(tMapView!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            WearApp(tMapView)
        }

        tMapView = TMapView(this)
        tMapView?.setSKTMapApiKey(com.example.routeguidance.BuildConfig.TMAP_API_KEY)

        tMapView?.setOnMapReadyListener {
            // 맵이 준비되면 현재 위치로 이동
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                var tMapManager = TMapGpsManager(this)
                tMapManager.openGps()
                moveToCurrentLocation(tMapView!!)
            } else {
                // 권한 요청
                Log.v("print", "try get permission")
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // 권한이 허용되면 현재 위치를 가져오고 맵을 이동시킵니다.
            tMapView?.let { moveToCurrentLocation(it) }
        }
    }

    private fun moveToCurrentLocation(tMapView: TMapView) {
        if (userLocation != null) {
            tMapView.setCenterPoint(userLocation!!.latitude, userLocation!!.longitude, true)
        }
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
            MapView(tmapview)

            TimeTextWithBackground()
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
            .background(Color.Transparent)
        ,contentAlignment = Alignment.Center
    ) {
        TimeText(
            modifier = Modifier.padding(4.dp),
        )
    }
}