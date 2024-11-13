package com.example.routeguidance.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources.getSystem
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.TimeText
import com.example.routeguidance.R
import com.example.routeguidance.complication.dataclass.Poi
import com.example.routeguidance.presentation.component.ButtonSearchPOI
import com.example.routeguidance.presentation.component.PoiItem
import com.example.routeguidance.presentation.theme.RouteGuidanceTheme
import com.skt.tmap.TMapGpsManager
import com.skt.tmap.TMapPoint
import com.skt.tmap.TMapView
import com.skt.tmap.overlay.TMapMarkerItem
import com.skt.tmap.poi.TMapPOIItem

val floatButtonHeight: Dp = 35.dp

class MainActivity : ComponentActivity(), TMapGpsManager.OnLocationChangedListener {
    var tMapView: TMapView? = null
    var tMapManager: TMapGpsManager? = null
    var userLocation: Location? = null
    val lastLocationMarker = TMapMarkerItem()
    val Int.dp: Int get() = (this * getSystem().displayMetrics.density).toInt()

    override fun onLocationChange(location: Location) {
//        this.userLocation = location
//        moveToCurrentLocation(tMapView!!)
//        tMapView?.setLocationPoint(location.latitude, location.longitude)
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
        tMapView?.setSKTMapApiKey(com.example.routeguidance.BuildConfig.TMAP_API_KEY)

        tMapManager = TMapGpsManager(this)
        tMapManager?.provider = "passive"
        tMapManager?.openGps()

        tMapView?.setOnMapReadyListener {
            // 맵이 준비되면 현재 위치로 이동

            // 현재 위치 아이콘 변경(Tracking)
            var bitmap = ResourcesCompat.getDrawable(resources, R.drawable.rounded_my_location_24, null)
            tMapView?.setIcon(bitmap?.toBitmap(20.dp, 20.dp))
            // 현재 위치 아이콘 변경(lastKnown)
            bitmap = ResourcesCompat.getDrawable(resources, R.drawable.rounded_last_location_24, null)
            lastLocationMarker.id = "lastLocation"
            lastLocationMarker.icon = bitmap?.toBitmap(20.dp, 20.dp)
            lastLocationMarker.setPosition(0.5f,0.5f)

            moveToCurrentLocation(tMapView, lastLocationMarker)
        }


        setContent {
            var poiItems = remember { mutableStateOf<List<Poi>>(emptyList()) }

            RouteGuidanceTheme {
                MapView(tMapView)
                TimeTextWithBackground()
                Box (
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    CenterRowView(tMapView, poiItems)
                    BottomRowView(tMapView, poiItems.value)
//                  VoiceInputScreen()
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
fun CenterRowView(tMapView: TMapView?, poiItems: MutableState<List<Poi>>) {
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
            Btn_SearchPOI(tMapView, poiItems)
            Btn_TrackCurrentLocation(tMapView)
        }
    }
}

@Composable
fun BottomRowView(tMapView: TMapView?, poiItems: List<Poi>) {
    Box (
        Modifier
            .fillMaxSize()
            .padding(bottom = 20.dp)
        ,contentAlignment = Alignment.BottomCenter
    ) {
        POIIemView(tMapView, poiItems)
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
fun Btn_TrackCurrentLocation(tMapView: TMapView?) {
    val isToggled = remember { mutableStateOf(false) }

    val icon = if (isToggled.value) {
        painterResource(id = R.drawable.baseline_my_location_24)
    } else {
        painterResource(id = R.drawable.baseline_location_searching_24)
    }

    // 버튼 UI
    FloatingActionButton(
        modifier = Modifier
            .absoluteOffset(x = -10.dp)
            .size(floatButtonHeight)
            .alpha(0.7f)
        ,onClick = {
            isToggled.value = !isToggled.value

            Log.v("btn", "clicked")
            if (isToggled.value) {
                Log.v("btn", "on")
                tMapView?.isCompassMode = true
                tMapView?.isTrackingMode = true
                tMapView?.setSightVisible(true)
                // 기존 위치 마커 삭제
                tMapView?.getMarkerItemFromId("lastLocation")?.visible = false
                tMapView?.zoomLevel = 15
                // 출발, 도착 경로 검색
//                val startPoint = TMapPoint(35.262492,128.872322)
//                val endPoint = TMapPoint(35.238107, 128.913391)
//
//                TMapData().findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint,
//                    object: TMapData.OnFindPathDataWithTypeListener {
//                        override fun onFindPathDataWithType(polyline: TMapPolyLine) {
//                            Log.v("search-result", polyline.toString())
//                            tMapView?.addTMapPolyLine(polyline)
//                        }
//                    }
//                )
            } else {
                Log.v("btn", "off")
                tMapView?.isCompassMode = false
                tMapView?.isTrackingMode = false
                tMapView?.setSightVisible(false)
                val lastLocation = tMapView?.locationPoint
                val lastLocationMarker = tMapView?.getMarkerItemFromId("lastLocation")
                if (lastLocation != null) {
                    lastLocationMarker?.setTMapPoint(lastLocation.latitude, lastLocation.longitude)
                }
                lastLocationMarker?.visible = true
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

@Composable
fun Btn_SearchPOI(tMapView: TMapView?, poiItems: MutableState<List<Poi>>) {
    val isToggled = remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    // 버튼 크기를 애니메이션으로 조정할 수 있도록 상태 변수 선언
    val buttonSize by animateDpAsState(
        targetValue = if (isToggled.value) screenWidthDp else floatButtonHeight, // 확장될 크기
        animationSpec = tween(durationMillis = 300) // 애니메이션 설정
    )

    val backGroundAlpha by animateFloatAsState(
        targetValue = if (isToggled.value) 0.9f else 0.0f, // 확장될 크기
        animationSpec = tween(durationMillis = 300) // 애니메이션 설정
    )

    // 버튼의 아이콘 설정
    val icon = if (isToggled.value) {
        painterResource(id = R.drawable.outline_cancel_24)
    } else {
        painterResource(id = R.drawable.twotone_manage_search_24)
    }

    Box (
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures {  }
            }
            .background(Color.Black.copy(alpha = backGroundAlpha))
            .size(buttonSize),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                modifier = Modifier
                    .size(floatButtonHeight)
                    .absoluteOffset(x = 10.dp)
                    .alpha(0.7f),
                onClick = {
                    isToggled.value = !isToggled.value

                    // 로그로 상태를 출력 (디버깅 용도)
                    Log.v("btn", "clicked")
                    if (isToggled.value) {
                        Log.v("btn", "on")
                    } else {
                        Log.v("btn", "off")
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
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().padding(top = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    ButtonSearchPOI(
                        width = 55.dp,
                        height = floatButtonHeight,
                        onClick = { poiList ->
                            poiItems.value = poiList
                            tMapView!!.addTMapPOIItem(poiList.map { poi ->
                                val poiItem = TMapPOIItem()
                                poiItem.setID(poi.id)
                                poiItem.pkey = poi.pkey
                                poiItem.name = poi.name
                                poiItem.telNo = poi.telNo
                                poiItem.name = poi.name
                                poiItem.frontLat = poi.frontLat
                                poiItem.frontLon = poi.frontLon
                                poiItem.noorLat = poi.noorLat
                                poiItem.noorLon = poi.noorLon
                                poiItem
                            } as ArrayList<TMapPOIItem>)

                            val pointList = ArrayList<TMapPoint>()
                            pointList.add(tMapView.locationPoint)
                            poiList.forEach { poi ->
                                pointList.add(TMapPoint(poi.noorLat.toDouble(), poi.noorLon.toDouble()))
                            }
                            val optView = tMapView.getDisplayTMapInfo(pointList)
                            tMapView.zoomLevel = optView!!.zoom-1
                            tMapView.setCenterPoint(optView.point.latitude,optView.point.longitude, true)
                            tMapView.enableClustering = true
                            // 검색 결과로 각각 마커 생성 후 지도로
                            isToggled.value = false
                        }
                    )
                }
                FloatingActionButton(
                    modifier = Modifier
                        .size(floatButtonHeight)
                        .absoluteOffset(x = -10.dp)
                        .alpha(0.7f),
                    onClick = {
                        isToggled.value = false
                    }
                ) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        painter = painterResource(R.drawable.baseline_mic_24),
                        contentDescription = "Close view",
                    )
                }
            }
        }

    }
}

@Composable
fun POIIemView(tMapView: TMapView?, poiItems: List<Poi>) {
    val listState = rememberLazyListState()
    var closestItemIndex by remember { mutableStateOf(0) }
    var isScrollOut by remember { mutableStateOf(false) }

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
            tMapView?.setCenterPoint(poiItems.get(closestItemIndex).noorLat.toDouble(), poiItems.get(closestItemIndex).noorLon.toDouble(), true)
            isScrollOut = false
        }
    }

    // LazyRow 구성
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            flingBehavior = object : androidx.compose.foundation.gestures.FlingBehavior {
                override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
                    findClosestItem()
                    return initialVelocity
                }
            }
        ) {
            itemsIndexed(poiItems) { index, item ->
                PoiItem(poi = item)
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
                detectTapGestures {  }
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
                    .background(Color.White)
                ,
                painter = painterResource(R.drawable.baseline_mic_24),
                contentDescription = "음성 입력",
            )
        }
    }
}