package com.example.routeguidance.presentation.component

//import androidx.compose.foundation.layout.Row
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import com.example.routeguidance.R
import com.example.routeguidance.complication.RetrofitClient
import com.example.routeguidance.complication.dataclass.Poi
import com.skt.tmap.TMapPoint

@Composable
fun ButtonSearchPOI(
    width: Dp,
    height: Dp,
    onClick: (List<Poi>) -> Unit,
    location: TMapPoint,
    icon: Painter,
    searchKeyword: String,
    radius: Int = 0
) {
    var keyword by remember { mutableStateOf("") }
    var textValue by remember { mutableStateOf("") }
    var poiList by remember { mutableStateOf<List<Poi>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(keyword) {
        try {
            if (keyword.isBlank()) return@LaunchedEffect

            loading = true
            errorMessage = null

            Log.v("POI SEARCH", keyword)
            val response = RetrofitClient.apiService.searchPOIs(
                keyword = keyword,
                centerLat = location.latitude.toFloat(),
                centerLon = location.longitude.toFloat(),
                radius = radius)
            poiList = response.searchPoiInfo.pois.poi

            loading = false
        } catch (e: Exception) {
            Log.v("POI ERROR", e.toString())
            errorMessage = "POI 검색에 실패했습니다: ${e.message}"
            loading = false
        }
    }

    LaunchedEffect(poiList) {
        try {
            if (poiList.isEmpty()) return@LaunchedEffect

            onClick(poiList)
        } catch (e: Exception) {
            Log.v("POI ERROR", e.toString())
            errorMessage = "검색 결과 갱신에 실패했습니다.: ${e.message}"
            loading = false
        }
    }

    FloatingActionButton(
        modifier = Modifier
            .width(width)
            .height(height)
            .alpha(0.7f),
        onClick = {
            keyword = searchKeyword
        }
    ) {
        Icon(
            modifier = Modifier
                .background(Color.LightGray)
                .fillMaxSize()
                .padding(3.dp),
            painter = icon,
            contentDescription = "Close view",
        )
    }
}

@Composable
fun SearchButtonWithAnimation(
    onClick: (List<Poi>) -> Unit,
    location: TMapPoint,
    radius: Int = 0
) {
    var isExpanded by remember { mutableStateOf(false) }
    val transition = updateTransition(targetState = isExpanded, label = "SearchButtonAnimation")

    val buttonWidth by transition.animateDp(label = "ButtonWidth") { expanded ->
        if (expanded) 30.dp else 55.dp
    }
    val viewHeight by transition.animateDp(label = "viewHeight") { expanded ->
        if (expanded) LocalConfiguration.current.screenHeightDp.dp else 30.dp
    }
    val buttonOffsetY by transition.animateDp(label = "ButtonOffsetY") { expanded ->
        if (expanded) 30.dp else 0.dp
    }
    val cornerRadius by transition.animateDp(label = "CornerRadius") { expanded ->
        if (expanded) 12.dp else 28.dp
    }
    val searchBarWidth by transition.animateDp(label = "ButtonWidth") { expanded ->
        if (expanded) LocalConfiguration.current.screenWidthDp.dp*2/3 else 0.dp
    }
    var inputKeyword by remember { mutableStateOf("") }
    var searchKeyword by remember { mutableStateOf("") }
    var poiList by remember { mutableStateOf<List<Poi>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context= LocalContext.current

    LaunchedEffect(searchKeyword) {
        try {
            if (searchKeyword.isBlank()) return@LaunchedEffect

            loading = true
            errorMessage = null

            Log.v("POI SEARCH", searchKeyword)
            val response = RetrofitClient.apiService.searchPOIs(
                keyword = searchKeyword,
                centerLat = location.latitude.toFloat(),
                centerLon = location.longitude.toFloat(),
                radius = radius)
            poiList = response.searchPoiInfo.pois.poi
            loading = false

            if (poiList.isEmpty()) {
                Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
                return@LaunchedEffect
            }
            onClick(poiList)
        } catch (e: Exception) {
            Log.v("POI ERROR", e.toString())
            errorMessage = "POI 검색에 실패했습니다: ${e.message}"
            loading = false
        }
    }

//    LaunchedEffect(poiList) {
//        try {
//            if (poiList.isEmpty()) {
//                Toast.makeText(context, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
//                return@LaunchedEffect
//            }
//
//            onClick(poiList)
//        } catch (e: Exception) {
//            Log.v("POI ERROR", e.toString())
//            errorMessage = "검색 결과가 없습니다.: ${e.message}"
//            loading = false
//        }
//    }

    Box (
        modifier = Modifier
            .fillMaxWidth()
            .height(viewHeight)
        , contentAlignment = Alignment.TopCenter
    ) {
        Row (
            modifier = Modifier
                .offset(y = buttonOffsetY)
                .height(30.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(-25.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isExpanded) {
                TextField(
                    value = inputKeyword,
                    onValueChange = { inputKeyword = it },
                    textStyle = TextStyle(
                        color = Color.Blue,
                        fontSize = 20.sp,
                    ),
                    label = {
                        Text(
                            if (inputKeyword== "")
                                "검색어를 입력하세요."
                            else inputKeyword,
                            style = TextStyle(fontSize = 12.sp)
                        )
                    },
                    modifier = Modifier
                        .background(Color.Black)
                        .fillMaxHeight()
                        .width(searchBarWidth)
                        .clip(RoundedCornerShape(cornerRadius)),
                )
            }
            FloatingActionButton(
                modifier = Modifier
                    .width(buttonWidth)
                    .clip(RoundedCornerShape(cornerRadius)),
                onClick = {
                    if (!isExpanded) {
                        isExpanded = true
                    } else {
                        isExpanded = false
                        if (inputKeyword.trim() != "") {
                            searchKeyword = inputKeyword
                        }
                    }
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_search_24),
                    contentDescription = "Search",
                )
            }
        }
    }
}

@Composable
fun PoiItem(poi: Poi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .width(200.dp)  // Card의 너비 설정
            .height(80.dp)  // Card의 높이 설정
            .padding(10.dp),  // Card의 외부 여백 설정
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),  // Card의 그림자 설정
        shape = MaterialTheme.shapes.medium,  // Card의 모서리 곡률 설정
        onClick = onClick
    ) {
        // Card 내부 내용
        Box(
            contentAlignment = Alignment.Center,  // 내용 가운데 정렬
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(text = poi.name, style = MaterialTheme.typography.body2)
        }
    }
}

