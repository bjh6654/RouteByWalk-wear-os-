package com.example.routeguidance.presentation.component

//import androidx.compose.foundation.layout.Row
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

    val context = LocalContext.current

    // ActivityResultLauncher 설정
    val speechRecognizerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    inputKeyword = results[0] // 음성 입력 결과를 TextField 값으로 설정
                }
            }
        }

    // 음성 입력 Intent 설정
    val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR") // 한국어 입력
        putExtra(RecognizerIntent.EXTRA_PROMPT, "말씀해주세요")
    }

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
                    modifier = Modifier
                        .background(Color.Black)
                        .fillMaxHeight()
                        .width(searchBarWidth)
                        .clip(RoundedCornerShape(cornerRadius)),
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
                    }
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
        if (isExpanded) {
            Row (
              modifier = Modifier
                  .padding(top = 100.dp)
            ) {
                IconButton(
                    onClick = {
                        // 음성 입력 시작
                        speechRecognizerLauncher.launch(speechRecognizerIntent)
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_mic_24),
                        contentDescription = "음성 입력",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PoiItem(poi: Poi, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .width(200.dp)
            .height(80.dp)
            .padding(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(text = poi.name, style = MaterialTheme.typography.body2)
        }
    }
}

