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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import com.example.routeguidance.BuildConfig
import com.example.routeguidance.R
import com.example.routeguidance.complication.dataclass.Poi
import com.example.routeguidance.presentation.component.ButtonSearchPOI
import com.example.routeguidance.presentation.component.PoiItem
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
import org.w3c.dom.Document
import org.w3c.dom.Element

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
//    override fun onLocationChange(location: Location) {
//        tMapView?.let {
//            it.setLocationPoint(location.latitude, location.longitude)
//            if (it.mapType != null) {
//                val locationMarker = it.getMarkerItemFromId("vsmLocationIcon")
//                it.removeTMapMarkerItem(locationMarker?.id)
//                locationMarker.setTMapPoint(location.latitude, location.longitude)
//                locationMarker.setPosition(0.5f, 0.5f)
//                it.addTMapMarkerItem(locationMarker)
//            }
//        }
//    }

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

        // Google Service의 fusedLocationService를 통해 위치 정보 갱신
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).apply {
            setMinUpdateDistanceMeters(2F)
            setMinUpdateIntervalMillis(500L)
            setWaitForAccurateLocation(true)
        }.build()
        val locationCallback = object : LocationCallback() {
            // 위치 정보 수신 가능 여부가 변경
            override fun onLocationAvailability(p0: LocationAvailability) {
                super.onLocationAvailability(p0)

                if (!p0.isLocationAvailable) {
                    tMapView?.let {
                        if (it.mapType != null) {
                            // 현재 위치 마커 삭제
                            val locationMarker = it.getMarkerItemFromId("vsmLocationIcon")
                            if (locationMarker.visible) {
                                locationMarker?.visible = false
                                it.removeTMapMarkerItem(locationMarker?.id)
                                it.addTMapMarkerItem(locationMarker)
                            }
                            // 기존 위치 마커 삭제
                            val lastLocationMarker = it.getMarkerItemFromId("lastLocation")
                            lastLocationMarker.visible = true
                            it.removeTMapMarkerItem(lastLocationMarker.id)
                            lastLocationMarker.setTMapPoint(it.locationPoint.latitude, it.locationPoint.longitude)
                            locationMarker.setPosition(0.5f, 0.5f)
                            it.addTMapMarkerItem(lastLocationMarker)
                        }
                    }
                }
            }
            // 위치 정보가 변경되면
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                val location: Location = p0.locations.last()
                tMapView?.let {
                    it.setLocationPoint(location.latitude, location.longitude)
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
            }
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

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

            RouteGuidanceTheme {
                MapView(tMapView)
                TimeTextWithBackground()
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
    Box(
        modifier = Modifier
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
            Btn_TrackCurrentLocation(tMapView, isCenter)
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
fun TimeTextWithBackground() {
    Box (
        modifier = Modifier
            .padding(5.dp)
    ) {
        TimeText(
            timeTextStyle = TextStyle(color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun Btn_TrackCurrentLocation(tMapView: TMapView?, isCenter: MutableState<Boolean>) {
    val isCompass = remember { mutableStateOf(false) }

    if (!isCenter.value) {
        isCompass.value = false
        if (tMapView?.mapType != null) tMapView.isCompassMode = false
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


                    tMapView.isCompassMode = isCompass.value
                    tMapView.isTrackingMode = isCompass.value
                } else {
                    tMapView?.isCompassMode = isCompass.value
                    tMapView?.isTrackingMode = isCompass.value

                    val locationMarker = tMapView!!.getMarkerItemFromId("tmpLocationIcon")
                    locationMarker.visible = true
                    locationMarker.id = "vsmLocationIcon"
                    tMapView.removeTMapMarkerItem("vsmLocationIcon")
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
fun Btn_SearchPOI(tMapView: TMapView?, poiItems: MutableState<List<Poi>>, isCenter: MutableState<Boolean>, isRoute: MutableState<Boolean>) {
    val isToggled = remember { mutableStateOf(false) }
    val isItemAdded = remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // 버튼을 애니메이션으로 조정할 수 있도록 상태 변수 선언
    val buttonSize by animateDpAsState(
        targetValue = if (isToggled.value) screenWidthDp else floatButtonHeight, // 확장될 크기
        animationSpec = tween(durationMillis = 300) // 애니메이션 설정
    )
    val backGroundAlpha by animateFloatAsState(
        targetValue = if (isToggled.value) 0.9f else 0.0f, // 확장될 크기
        animationSpec = tween(durationMillis = 300) // 애니메이션 설정
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
        contentAlignment = Alignment.CenterStart,
    ) {
        FloatingActionButton(
            modifier = Modifier
                .size(floatButtonHeight)
                .absoluteOffset(x = 10.dp)
                .alpha(0.7f),
            onClick = {
//                    if (tMapView?.mapType != null && (poiItems.value.isNotEmpty() || tMapView.getPolyLineFromId("route") != null)) {
                if (isItemAdded.value) {
                    val lastLocationMarker = tMapView!!.getMarkerItemFromId("lastLocation")
                    val locationMarker = tMapView.getMarkerItemFromId("vsmLocationIcon")
                    tMapView.removeAllTMapPolyLine()
                    tMapView.removeAllTMapMarkerItem()
                    tMapView.addTMapMarkerItem(lastLocationMarker)
                    tMapView.addTMapMarkerItem(locationMarker)
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
        if (isToggled.value) {
            Box (
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ButtonSearchPOI(
                        width = 55.dp,
                        height = floatButtonHeight,
                        onClick = { poiList ->
                            poiItems.value = poiList
                            // 검색 결과로 각각 마커 생성 후 지도로
                            isCenter.value = false
                            isToggled.value = false
                        },
                        location = tMapView!!.locationPoint,
                        icon = painterResource(R.drawable.baseline_search_24),
                        searchKeyword = "김해시",
                        radius = 10
                    )
                }
                Box(
                    modifier = Modifier
                        .absoluteOffset(x = -45.dp)
                        .fillMaxSize()
                        .padding(bottom = 30.dp),
                    contentAlignment = Alignment.BottomCenter
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
                        radius = 3
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .absoluteOffset(x = 45.dp)
                        .padding(bottom = 30.dp),
                    contentAlignment = Alignment.BottomCenter
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
                        radius = 3
                    )
                }
            }
        }

    }
}

@Composable
fun POIIemView(tMapView: TMapView?, poiItems: MutableState<List<Poi>>, isRoute: MutableState<Boolean>) {
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
                        if (item.id != poi.id)
                            tMapView?.removeTMapMarkerItem(poi.id)
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
                        fun sumDistanceForLinePlacemarks(doc: Document): Int {
                            // 네임스페이스를 고려하여 "tmap:nodeType"이 "LINE"인 <Placemark> 요소 찾기
                            val nodeList = doc.getElementsByTagName("tmap:nodeType")
                            Log.v("aa", nodeList.length.toString())
                            var totalDistance = 0
                            for (i in 0 until nodeList.length) {
                                val nodeTypeElement = nodeList.item(i) as Element

                                // "LINE"인 경우에만 처리
                                if (nodeTypeElement.textContent.toString() == "LINE") {
                                    // 해당 Placemark 요소 찾기
                                    var placemark = nodeTypeElement.parentNode
                                    while (placemark != null && placemark is Element && placemark.tagName != "Placemark") {
                                        placemark = placemark.parentNode
                                    }

                                    if (placemark != null && placemark is Element) {
                                        // Placemark 내에서 tmap:distance 값 찾기
                                        val distanceElement = placemark.getElementsByTagName("tmap:distance").item(0)
                                        if (distanceElement != null) {
                                            val distance = distanceElement.textContent.toIntOrNull()
                                            if (distance != null) {
                                                totalDistance += distance
                                            }
                                        }
                                    }
                                }
                            }
                            return totalDistance
                        }

                        override fun onFindPathDataAllType(p0: Document?) {
                            Log.v("aaa", sumDistanceForLinePlacemarks(p0!!).toString())
                        }
                    })
                    poiItems.value = emptyList()
                    isRoute.value = true
                })
            }
        }
    }
}


@Composable
fun VoiceInputScreen() {
    var speechText by remember { mutableStateOf(TextFieldValue("")) }
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .pointerInput(Unit) {
                detectTapGestures { }
            }
        ,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("", modifier = Modifier.padding(bottom = 16.dp))

        // 텍스트 필드
        TextField(
            value = speechText,
            onValueChange = { speechText = it },
            label = { Text("텍스트 입력") },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 음성 입력 버튼
        FloatingActionButton (
            modifier = Modifier
                .background(Color.White, shape = CircleShape)
                .size(50.dp)
            ,
            onClick = {
//            startSpeechRecognition()
            }
        ) {
            Icon(
                modifier = Modifier
                    .fillMaxSize()
//                    .padding(5.dp)
                    .background(Color.White),
                painter = painterResource(R.drawable.baseline_mic_24),
                contentDescription = "음성 입력",
            )
        }
    }
}