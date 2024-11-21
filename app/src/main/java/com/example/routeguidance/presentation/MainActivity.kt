package com.example.routeguidance.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources.getSystem
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import com.example.routeguidance.BuildConfig
import com.example.routeguidance.R
import com.example.routeguidance.complication.dataclass.Poi
import com.example.routeguidance.complication.state.globalState
import com.example.routeguidance.presentation.component.ButtonSearchPOI
import com.example.routeguidance.presentation.component.PoiItem
import com.example.routeguidance.presentation.component.SearchButtonWithAnimation
import com.example.routeguidance.presentation.theme.RouteGuidanceTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.skt.tmap.TMapData
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.overlay.TMapPolyLine
import kotlinx.coroutines.delay
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.text.SimpleDateFormat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

val floatButtonHeight: Dp = 35.dp

class MainActivity : ComponentActivity(), TMapView.OnDisableScrollWithZoomLevelCallback {
    var tMapView: TMapView? = null
    var userLocation: Location? = null
    val Int.dp: Int get() = (this * getSystem().displayMetrics.density).toInt()

    private var isCenter = mutableStateOf(false)

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onDisableScrollWidthZoomLevel(zoom: Float, point: TMapPoint) {
        if (isCenter.value)
            isCenter.value = false
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): Location? {
        val locationManager = this.getSystemService("location") as LocationManager
        return locationManager.getLastKnownLocation("passive")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        tMapView = TMapView(this)
        // Set TmapView
        tMapView?.setSKTMapApiKey(BuildConfig.TMAP_API_KEY)

        tMapView?.setOnMapReadyListener {
            tMapView?.mapType = TMapView.MapType.NIGHT
            // 현재 위치 아이콘 변경(Tracking)
            var bitmap = ResourcesCompat.getDrawable(resources, R.drawable.rounded_my_location_24, null)
            tMapView?.setIcon(bitmap?.toBitmap(20.dp, 20.dp))

            val dotItem = TMapMarkerItem()
            dotItem.id = "vsmLocationIcon"
            dotItem.tMapPoint = tMapView?.locationPoint
            dotItem.icon = bitmap?.toBitmap(20.dp, 20.dp)
            dotItem.setPosition(0.5f, 0.5f)
            dotItem.visible = false
            tMapView?.addTMapMarkerItem(dotItem)

            // 현재 위치 아이콘 변경(lastKnown)
            val lastLocationMarker = TMapMarkerItem()
            bitmap = ResourcesCompat.getDrawable(resources, R.drawable.rounded_last_location_24, null)
            lastLocationMarker.id = "lastLocation"
            lastLocationMarker.icon = bitmap?.toBitmap(20.dp, 20.dp)
            lastLocationMarker.setPosition(0.5f,0.5f)

            moveToCurrentLocation(tMapView, lastLocationMarker)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            val poiItems = remember { mutableStateOf<List<Poi>>(emptyList()) }
            val isRoute = remember { mutableStateOf(false) }
            val state: globalState = viewModel()

            // Google Service의 fusedLocationService를 통해 위치 정보 갱신
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

            val balanceLocationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 2000).apply {
                setMinUpdateDistanceMeters(3F)
                setMinUpdateIntervalMillis(1000L)
                setMaxUpdateDelayMillis(5000L)
                setWaitForAccurateLocation(false)
            }.build()
            val highLocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000).apply {
                setMinUpdateDistanceMeters(3F)
                setMinUpdateIntervalMillis(1000L)
                setMaxUpdateDelayMillis(5000L)
                setWaitForAccurateLocation(true)
            }.build()
            val locationCallback = object : LocationCallback() {
                // 위치 정보 수신 가능 여부가 변경
                override fun onLocationAvailability(p0: LocationAvailability) {
                    super.onLocationAvailability(p0)

                    try {
                        if (!p0.isLocationAvailable) {
                            tMapView?.let {
                                if (it.mapType != null) {
                                    // 현재 위치 마커 삭제
                                    val locationMarker = it.getMarkerItemFromId("vsmLocationIcon")
                                    locationMarker.visible = false
                                    it.removeTMapMarkerItem(locationMarker.id)
                                    it.addTMapMarkerItem(locationMarker)
                                    // 기존 위치 마커 삭제
                                    val lastLocationMarker = it.getMarkerItemFromId("lastLocation")
                                    lastLocationMarker.visible = true
                                    lastLocationMarker.setTMapPoint(it.locationPoint.latitude, it.locationPoint.longitude)
                                    locationMarker.setPosition(0.5f, 0.5f)
                                    it.removeTMapMarkerItem(lastLocationMarker.id)
                                    it.addTMapMarkerItem(lastLocationMarker)
                                }
                            }
                        }
                    } catch (e: Exception) {

                    }
                }
                // 위치 정보가 변경되면
                override fun onLocationResult(p0: LocationResult) {
                    super.onLocationResult(p0)

                    try {
                        val location: Location = p0.locations
                            .minBy { loc -> loc.accuracy }
                        tMapView?.let {
                            it.setLocationPoint(location.latitude, location.longitude)
                            state.updateLastlocationUpdatedTime()
                            Log.v("updatedLocation", location.provider.toString()+": " +location.accuracy.toString())
                            if (it.mapType != null) {
                                if (!it.isTrackingMode) {
                                    val locationMarker = it.getMarkerItemFromId("vsmLocationIcon")
                                    locationMarker?.visible = true
                                    locationMarker.setTMapPoint(location.latitude, location.longitude)
                                    locationMarker.setPosition(0.5f, 0.5f)
                                    it.removeTMapMarkerItem(locationMarker?.id)
                                    it.addTMapMarkerItem(locationMarker)
                                    if (isCenter.value)
                                        it.setCenterPoint(location.latitude, location.longitude, true)
                                }
                                // 기존 위치 마커 삭제
                                val lastLocationMarker = it.getMarkerItemFromId("lastLocation")
                                if (lastLocationMarker.visible) {
                                    lastLocationMarker.visible = false
                                    it.removeTMapMarkerItem(lastLocationMarker.id)
                                    it.addTMapMarkerItem(lastLocationMarker)
                                }
                            }
                        }
                    } catch (e: Exception) {

                    }
                }
            }
//            fusedLocationProviderClient.requestLocationUpdates(balanceLocationRequest, locationCallback, Looper.getMainLooper())

            LaunchedEffect(isRoute.value) {
                if (isRoute.value) {
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                    fusedLocationProviderClient.requestLocationUpdates(highLocationRequest, locationCallback, Looper.getMainLooper())
                } else {
                    fusedLocationProviderClient.removeLocationUpdates(locationCallback)
                    fusedLocationProviderClient.requestLocationUpdates(balanceLocationRequest, locationCallback, Looper.getMainLooper())
                }
            }

            RouteGuidanceTheme {
                MapView(tMapView)
                TimeTextOverlay()
                Box (
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CenterRowView(tMapView, poiItems, isCenter, isRoute)
                    if (isRoute.value) {
                        BottomRowView(tMapView, poiItems, isRoute)
                    } else {
                        BottomRowView(tMapView, poiItems, isRoute)
                    }
                }
                DestinationDetail(tMapView, isRoute)
            }

        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {

        }
    }

    private fun moveToCurrentLocation(tMapView: TMapView?, lastLocationMarker: TMapMarkerItem) {
        // 마지막 위치를 받아옴
        userLocation = getLastKnownLocation()
        userLocation?.let { tMapView?.setLocationPoint(it.latitude, it.longitude) }

        // 마지막 위치로 맵 이동
        userLocation?.let { lastLocationMarker.setTMapPoint(it.latitude, it.longitude) }
        tMapView?.addTMapMarkerItem(lastLocationMarker)
        tMapView?.setCenterPoint(lastLocationMarker.tMapPoint.latitude, lastLocationMarker.tMapPoint.longitude, true)
        tMapView?.zoomLevel = 15
    }
}

@Composable
fun MapView(tmapview: TMapView?) {
    val context = LocalContext.current
    val screenWidth = context.resources.displayMetrics.widthPixels
    val screenHeight = context.resources.displayMetrics.heightPixels

    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    val cur = change.position
                    val pre = change.previousPosition

                    val left = tmapview!!.leftTopPoint.latitude
                    val top = tmapview.leftTopPoint.longitude
                    val right = tmapview.rightBottomPoint.latitude
                    val bottom = tmapview.rightBottomPoint.longitude

                    val newCenterLat =
                        (cur.y - pre.y) / screenWidth * (bottom - top) + tmapview.centerPoint.latitude
                    val newCenterLon =
                        (cur.x - pre.x) / screenHeight * (right - left) + tmapview.centerPoint.longitude
                    tmapview.setCenterPoint(newCenterLat, newCenterLon)
                }
            }
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context ->
                tmapview!!
            }
        )
    }
}

@Composable
fun CenterRowView(tMapView: TMapView?, poiItems: MutableState<List<Poi>>, isCenter: MutableState<Boolean>, isRoute: MutableState<Boolean>) {
    Box (
        Modifier
            .fillMaxSize()
        ,contentAlignment = Alignment.Center
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
            , horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Btn_SearchPOI(tMapView, poiItems, isCenter, isRoute)
            Btn_TrackCurrentLocation(tMapView, isCenter, isRoute)
        }
    }
}

@Composable
fun BottomRowView(tMapView: TMapView?, poiItems: MutableState<List<Poi>>, isRoute: MutableState<Boolean>) {
    Box (
        Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp)
        ,contentAlignment = Alignment.BottomCenter
    ) {
        POIIemView(tMapView, poiItems, isRoute)
    }
}

@Composable
fun TimeTextOverlay(modifier: Modifier = Modifier) {
    // 3. 현재 시간 가져오기
    val currentTime = remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime.value = System.currentTimeMillis()
            delay(1000L) // 1초마다 업데이트
        }
    }

    // 4. TimeText UI
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color.Black.copy(alpha = 0.6f), // 반투명 배경
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = SimpleDateFormat("HH:mm").format(currentTime.value),
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DestinationDetail(tMapView: TMapView?, isRoute: MutableState<Boolean>, state: globalState = viewModel()) {
    val doc = state.destinationDocument
    val updatedTime = state.lastlocationUpdatedTime

    data class Point(val latitude: Double, val longitude: Double)

    fun Node.getChildNode(tagName: String): Node? {
        val children = this.childNodes
        for (i in 0 until children.length) {
            if (children.item(i).nodeName == tagName) {
                return children.item(i)
            }
        }
        return null
    }

    fun haversineDistance(start: Point, end: Point): Double {
        val R = 6371000.0 // 지구 반지름 (meters)
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dlat = lat2 - lat1
        val dlon = lon2 - lon1

        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun parseRouteFromXML(document: Document?): List<Point> {
        if (document == null) return emptyList<Point>()

        val route = mutableListOf<Point>()
        val nodeList = document.getElementsByTagName("Placemark")

        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            val lineString = node.getChildNode("LineString")
            if (lineString != null) {
                val coordinates = lineString.getChildNode("coordinates")?.textContent
                if (!coordinates.isNullOrEmpty()) {
                    coordinates.split(" ").forEach { coordinate ->
                        val parts = coordinate.split(",")
                        if (parts.size >= 2) {
                            val lon = parts[0].toDouble()
                            val lat = parts[1].toDouble()
                            route.add(Point(lat, lon))
                        }
                    }
                }
            }
        }

        return route
    }

    fun calculateRemainingDistance(currentLocation: Point, route: List<Point>): String {
        var remainingDistance = 0.0
        var foundCurrentLocation = false
        var closestDistance = Double.MAX_VALUE // 가장 가까운 경로 지점까지의 거리
        var closestPointIndex = -1 // 가장 가까운 지점의 인덱스

        for (i in route.indices) {
            val point = route[i]
            val distanceToCurrent = haversineDistance(currentLocation, point)

            if (!foundCurrentLocation) {
                // 현재 위치와 경로 지점 간의 거리 확인
                if (distanceToCurrent < closestDistance) {
                    closestDistance = distanceToCurrent
                    closestPointIndex = i
                }

                // 현재 위치가 경로 상이라고 간주할 수 있는 경우
                if (distanceToCurrent < 30.0) {
                    foundCurrentLocation = true
                    // 현재 위치 이후부터 거리 계산
                    for (j in i until route.size - 1) {
                        remainingDistance += haversineDistance(route[j], route[j + 1])
                    }
                    break
                }
            }
        }

        // 현재 위치가 경로 상에 없을 경우, 가장 가까운 지점까지의 거리 추가
        if (!foundCurrentLocation && closestPointIndex != -1) {
            remainingDistance = closestDistance // 가장 가까운 지점까지의 거리 추가
            // 가장 가까운 지점 이후부터 거리 계산
            for (j in closestPointIndex until route.size - 1) {
                remainingDistance += haversineDistance(route[j], route[j + 1])
            }
        }

        return if (remainingDistance >= 1000) (String.format("%.1f", remainingDistance.toInt()/1000.0) + " km")
        else "${remainingDistance.toInt()} m"
    }

//    fun sumDistanceForLinePlacemarks(doc: Document?): String {
//        // 네임스페이스를 고려하여 "tmap:nodeType"이 "LINE"인 <Placemark> 요소 찾기
//        if (doc == null) return "0 m"
//        val nodeList = doc.getElementsByTagName("tmap:nodeType")
//        var totalDistance = 0
//        for (i in 0 until nodeList.length) {
//            val nodeTypeElement = nodeList.item(i) as Element
//
//            // "LINE"인 경우에만 처리
//            if (nodeTypeElement.textContent.toString() == "LINE") {
//                // 해당 Placemark 요소 찾기
//                var placemark = nodeTypeElement.parentNode
//                while (placemark != null && placemark is Element && placemark.tagName != "Placemark") {
//                    placemark = placemark.parentNode
//                }
//
//                if (placemark != null && placemark is Element) {
//                    // Placemark 내에서 tmap:distance 값 찾기
//                    val distanceElement = placemark.getElementsByTagName("tmap:distance").item(0)
//                    if (distanceElement != null) {
//                        val distance = distanceElement.textContent.toIntOrNull()
//                        if (distance != null) {
//                            totalDistance += distance
//                        }
//                    }
//                }
//            }
//        }
//            return if (totalDistance >= 1000) (String.format("%.1f", totalDistance/1000.0) + " km")
//            else "$totalDistance m"
//    }

    if (isRoute.value) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box (
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.6f), // 반투명 배경
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(bottom = 5.dp)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Column (verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = tMapView?.getMarkerItemFromId("destination")!!.name,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text(
                            text = "남은 거리 : ",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
//                            text = sumDistanceForLinePlacemarks(doc),
                            text = calculateRemainingDistance(currentLocation = Point(tMapView!!.locationPoint.latitude, tMapView.locationPoint.longitude), route = parseRouteFromXML(doc)),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Btn_TrackCurrentLocation(tMapView: TMapView?, isCenter: MutableState<Boolean>, isRoute: MutableState<Boolean>) {
    val isCompass = remember { mutableStateOf(false) }

    if (!isCenter.value) {
        isCompass.value = false
        if (tMapView?.mapType != null) {
            tMapView.isCompassMode = false
            if (tMapView.isTrackingMode) {
                tMapView.isTrackingMode = false

                val locationMarker = tMapView.getMarkerItemFromId("tmpLocationIcon")
                tMapView.removeTMapMarkerItem("tmpLocationIcon")
                tMapView.removeTMapMarkerItem("vsmLocationIcon")
                locationMarker.visible = true
                locationMarker.id = "vsmLocationIcon"
                tMapView.addTMapMarkerItem(locationMarker)
            }
        }
    }

    val icon = if (isCompass.value) {
        painterResource(R.drawable.baseline_explore_off_24)
    } else if (isCenter.value) {
        painterResource(R.drawable.outline_explore_24)
    } else {
        painterResource(R.drawable.baseline_my_location_24)
    }

    // 버튼 UI
    FloatingActionButton(
        modifier = Modifier
            .absoluteOffset(x = -10.dp)
            .size(floatButtonHeight)
            .alpha(0.7f)
        ,onClick = {
            if (isCenter.value) {
                isCompass.value = !isCompass.value

                if (isCompass.value) {
                    val locationMarker = tMapView!!.getMarkerItemFromId("vsmLocationIcon")
                    tMapView.removeTMapMarkerItem("vsmLocationIcon")
                    locationMarker.visible = false
                    locationMarker.id = "tmpLocationIcon"
                    tMapView.addTMapMarkerItem(locationMarker)

                    tMapView.isCompassMode = true
                    tMapView.isTrackingMode = true
                    if (isRoute.value) {
                        tMapView.zoomLevel = max(tMapView.zoomLevel, 16)
                    }
                } else {
                    tMapView?.isCompassMode = false
                    tMapView?.isTrackingMode = false

                    val locationMarker = tMapView!!.getMarkerItemFromId("tmpLocationIcon")
                    tMapView.removeTMapMarkerItem("tmpLocationIcon")
                    tMapView.removeTMapMarkerItem("vsmLocationIcon")
                    locationMarker.visible = true
                    locationMarker.id = "vsmLocationIcon"
                    tMapView.addTMapMarkerItem(locationMarker)
                }

                try {
                    tMapView.setSightVisible(isCompass.value)
                } catch (e: Exception) {

                }

            } else {
                tMapView?.setCenterPoint(tMapView.locationPoint.latitude, tMapView.locationPoint.longitude, true)
                isCenter.value = true
            }
        }
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            painter = icon,
            contentDescription = "Action Button",
        )
    }
}

@Composable
fun Btn_SearchPOI(tMapView: TMapView?, poiItems: MutableState<List<Poi>>, isCenter: MutableState<Boolean>, isRoute: MutableState<Boolean>, state: globalState = viewModel()) {
    val isToggled = remember { mutableStateOf(false) }
    val isItemAdded = remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    var selectedDistance by remember { mutableStateOf("10 km") }

    fun getRadiusFromSelected(): Int {
        return when (selectedDistance) {
            "1 km" -> 1
            "3 km" -> 3
            "5 km" -> 5
            "10 km" -> 10
            "30 km" -> 30
            else -> 0
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // 버튼을 애니메이션으로 조정할 수 있도록 상태 변수 선언
    val buttonSize by animateDpAsState(
        targetValue = if (isToggled.value) screenWidthDp else floatButtonHeight,
        animationSpec = tween(durationMillis = 300)
    )
    val backGroundAlpha by animateFloatAsState(
        targetValue = if (isToggled.value) 0.9f else 0.0f,
        animationSpec = tween(durationMillis = 300)
    )

    isItemAdded.value = poiItems.value.isNotEmpty() || isRoute.value

    // 버튼의 아이콘 설정
    val icon = if (isToggled.value) {
        painterResource(id = R.drawable.outline_cancel_24)
    } else {
        if (isItemAdded.value) {
            painterResource(id = R.drawable.outline_cancel_24)
        } else {
            painterResource(id = R.drawable.baseline_travel_explore_24)
        }
    }

    Box (
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures { }
            }
            .background(Color.Black.copy(alpha = backGroundAlpha))
            .size(buttonSize),
    ) {
        if (isToggled.value) {
            Column (
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row (
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { }
                        }
                        .padding(top = 20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    SearchButtonWithAnimation(
                        onClick = { poiList ->
                            poiItems.value = poiList
                            isCenter.value = false
                            isToggled.value = false
                        },
                        location = tMapView!!.locationPoint,
                        radius = getRadiusFromSelected()
                    )
                }
                Row (
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(top = 40.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val distances = listOf("1 km", "3 km", "5 km", "10 km", "30 km", "전국")
                    Box (
                        modifier = Modifier
                            .width(45.dp)
                            .height(35.dp)
                    ) {
                        Button(
                            onClick = { isExpanded = true }
                        ) { Text(selectedDistance, style = TextStyle(fontWeight = FontWeight.Bold)) }
                        DropdownMenu(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 40.dp),

                            expanded = isExpanded,
                            onDismissRequest = { isExpanded = false }
                        ) {
                            distances.forEach { distance ->
                                DropdownMenuItem(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        selectedDistance = distance
                                        isExpanded = false
                                    },
                                    text = {
                                        Text(text = distance, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                    }
                                )
                            }
                        }
                    }

                }
                Row (
                    modifier = Modifier
                        .fillMaxWidth()
                    ,verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = Modifier.width((screenWidthDp-floatButtonHeight)/2))
                    Box (
                        modifier = Modifier
                            .padding(bottom = 15.dp)
                            .wrapContentSize(),
                    ) {
                        ButtonSearchPOI(
                            width = floatButtonHeight,
                            height = floatButtonHeight,
                            onClick = { poiList ->
                                poiItems.value = poiList
                                // 검색 결과로 각각 마커 생성 후 지도로
                                isCenter.value = false
                                isToggled.value = false
                            },
                            location = tMapView!!.locationPoint,
                            icon = painterResource(R.drawable.baseline_coffee_24),
                            searchKeyword = "카페",
                            radius = getRadiusFromSelected()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(bottom = 35.dp, start = 10.dp)
                            .wrapContentSize(),
                    ) {
                        ButtonSearchPOI(
                            width = floatButtonHeight,
                            height = floatButtonHeight,
                            onClick = { poiList ->
                                poiItems.value = poiList
                                // 검색 결과로 각각 마커 생성 후 지도로
                                isCenter.value = false
                                isToggled.value = false
                            },
                            location = tMapView!!.locationPoint,
                            icon = painterResource(R.drawable.baseline_restaurant_24),
                            searchKeyword = "식당",
                            radius = getRadiusFromSelected()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(bottom = 70.dp, start = 0.dp)
                            .wrapContentSize(),
                    ) {
                        ButtonSearchPOI(
                            width = floatButtonHeight,
                            height = floatButtonHeight,
                            onClick = { poiList ->
                                poiItems.value = poiList
                                // 검색 결과로 각각 마커 생성 후 지도로
                                isCenter.value = false
                                isToggled.value = false
                            },
                            location = tMapView!!.locationPoint,
                            icon = painterResource(R.drawable.baseline_fastfood_24),
                            searchKeyword = "패스트푸드",
                            radius = getRadiusFromSelected()
                        )
                    }
                }
            }
        }
        Box (
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .size(floatButtonHeight)
                    .absoluteOffset(x = 10.dp)
                    .alpha(0.7f),
                onClick = {
                    if (isItemAdded.value) {
                        if (isCenter.value) isCenter.value = false

                        tMapView!!.removeAllTMapPolyLine()
                        if (isRoute.value) {
                            tMapView.removeTMapMarkerItem("destination")
                        }
                        if (poiItems.value.isNotEmpty()) {
                            poiItems.value.forEach { poi ->
                                tMapView.removeTMapMarkerItem(poi.id)
                            }
                        }

                        poiItems.value = emptyList()
                        isRoute.value = false
                    } else {
                        isToggled.value = !isToggled.value
                    }
                }
            ) {
                Icon(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp),
                    painter = icon,
                    contentDescription = if (isToggled.value) "Toggled On" else "Toggled Off",
                )
            }
        }
    }
}

@Composable
fun POIIemView(tMapView: TMapView?, poiItems: MutableState<List<Poi>>, isRoute: MutableState<Boolean>, state: globalState = viewModel()) {
    val listState = rememberLazyListState()
    var closestItemIndex by remember { mutableStateOf(0) }
    var isScrollOut by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun vectorDrawableToBitmap(id: Int): Bitmap {
        val drawable: Drawable? = ContextCompat.getDrawable(context, id)
        val width = drawable!!.intrinsicWidth
        val height = drawable.intrinsicHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    if (poiItems.value.isNotEmpty()) {
        LaunchedEffect(Unit) {
            var focusedItem: TMapMarkerItem? = null
            poiItems.value.forEachIndexed { index, poi ->
                val poiItem = TMapMarkerItem()
                poiItem.id = poi.id
                poiItem.setTMapPoint(poi.noorLat.toDouble(), poi.noorLon.toDouble())
                poiItem.name = poi.name
                poiItem.setPosition(0.5f, 0.5f)
                var icon = vectorDrawableToBitmap(R.drawable.poi_unselect)
                if (index == 0) {
                    focusedItem = poiItem
                    icon = vectorDrawableToBitmap(R.drawable.poi_selected)
                    poiItem.setPosition(0.5f, 1.0f)
                }
                poiItem.icon = icon

                tMapView?.addTMapMarkerItem(poiItem)
            }
            tMapView?.bringMarkerToFront(focusedItem!!)
            val pointList = ArrayList<TMapPoint>(poiItems.value.map { poi ->
                TMapPoint(poi.noorLat.toDouble(), poi.noorLon.toDouble())
            })
            val optView = tMapView!!.getDisplayTMapInfo(pointList)
            tMapView.zoomLevel = optView.zoom-1
            tMapView.setCenterPoint(poiItems.value[0].noorLat.toDouble(), poiItems.value[0].noorLon.toDouble(), true)
        }
    }


    // 가장 중앙에 가까운 아이템을 찾는 함수
    fun findClosestItem() {
        val visibleItems = listState.layoutInfo.visibleItemsInfo
        Log.v("items", visibleItems.size.toString())
        if (visibleItems.isNotEmpty()) {
            // 화면 중앙 위치 계산
            val viewportSize = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            val screenCenter = listState.layoutInfo.viewportStartOffset + viewportSize / 2

            var closestItem = visibleItems.first()
            val offset = (200.dp).value.toInt()
            var minDistance = Math.abs(closestItem.offset + offset - screenCenter)

            // 보이는 아이템 중 가장 중앙에 가까운 아이템 찾기
            for (item in visibleItems) {
                val distance = Math.abs(item.offset + offset - screenCenter)
                if (distance < minDistance) {
                    minDistance = distance
                    closestItem = item
                }
            }

            closestItemIndex = closestItem.index
            isScrollOut = true
        }
    }

    LaunchedEffect(isScrollOut) {
        if (isScrollOut) {
            listState.animateScrollToItem(closestItemIndex)
            tMapView!!.setCenterPoint(poiItems.value.get(closestItemIndex).noorLat.toDouble(), poiItems.value.get(closestItemIndex).noorLon.toDouble(), true)

            poiItems.value.forEachIndexed { index, poi ->
                val focusedItem = tMapView.getMarkerItemFromId(poi.id)
                focusedItem.setPosition(0.5f, 0.5f)
                var icon = vectorDrawableToBitmap(R.drawable.poi_unselect)
                if (index == closestItemIndex) {
                    icon = vectorDrawableToBitmap(R.drawable.poi_selected)
                    focusedItem.setPosition(0.5f, 1.0f)
                }
                tMapView.removeTMapMarkerItem(poi.id)
                focusedItem.icon = icon
                tMapView.addTMapMarkerItem(focusedItem)
            }
            tMapView.bringMarkerToFront(tMapView.getMarkerItemFromId(poiItems.value[closestItemIndex].id))

            isScrollOut = false
        }
    }

    // LazyRow 구성
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            flingBehavior = object : FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    findClosestItem()
                    return initialVelocity
                }
            }
        ) {
            itemsIndexed(poiItems.value) { index, item ->
                PoiItem(poi = item, onClick = {
                    // 출발, 도착 경로 검색
                    val startPoint = tMapView?.locationPoint
                    val endPoint = TMapPoint(item.noorLat.toDouble(), item.noorLon.toDouble())

                    // 선택된 마커 제외 모두 삭제
                    poiItems.value.forEach { poi ->
                        if (item.id != poi.id) {
                            tMapView?.removeTMapMarkerItem(poi.id)
                        } else {
                            val destMarker = tMapView?.getMarkerItemFromId(poi.id)
                            tMapView?.removeTMapMarkerItem(poi.id)
                            destMarker?.id = "destination"
                            destMarker?.name = poi.name
                            tMapView?.addTMapMarkerItem(destMarker)
                        }
                    }

                    TMapData().findPathDataWithType(
                        TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint,
                        object: TMapData.OnFindPathDataWithTypeListener {
                            override fun onFindPathDataWithType(polyline: TMapPolyLine) {
                                polyline.setID("route")
                                polyline.lineColor = 0xFFA500
                                polyline.outLineWidth = 6f
                                polyline.outLineColor = 0xFFFFFF
                                tMapView!!.addTMapPolyLine(polyline)
                                // 현재 위치를 최상단에 다시 그림
                                tMapView.bringMarkerToFront(tMapView.getMarkerItemFromId("vsmLocationIcon"))
                                val pointList = arrayListOf(startPoint, endPoint)
                                val optView = tMapView.getDisplayTMapInfo(pointList)
                                tMapView.zoomLevel = optView.zoom - 1
                                tMapView.setCenterPoint(optView.point.latitude, optView.point.longitude, true)
                            }
                        }
                    )

                    TMapData().findPathDataAllType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint, object: TMapData.OnFindPathDataAllTypeListener {
                        override fun onFindPathDataAllType(p0: Document?) {
                            state.updateDestinationDocument(p0!!)
                            poiItems.value = emptyList()
                            isRoute.value = true
                        }
                    })
                })
            }
        }
    }
}