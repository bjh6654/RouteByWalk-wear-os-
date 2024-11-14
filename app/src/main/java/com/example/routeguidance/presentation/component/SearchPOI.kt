package com.example.routeguidance.presentation.component

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import com.example.routeguidance.R
import com.example.routeguidance.complication.RetrofitClient
import com.example.routeguidance.complication.dataclass.Poi

@Composable
fun ButtonSearchPOI(
    width: Dp,
    height: Dp,
    onClick: (List<Poi>) -> Unit
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
            val response = RetrofitClient.apiService.searchPOIs(keyword = keyword)
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
            keyword = "푸르지오 하이엔드 1차"
        }
    ) {
        Icon(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            painter = painterResource(R.drawable.baseline_search_24),
            contentDescription = "Close view",
        )
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

