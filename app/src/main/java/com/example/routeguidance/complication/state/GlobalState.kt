package com.example.routeguidance.complication.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import org.w3c.dom.Document

class globalState: ViewModel() {
    var destinationDocument by mutableStateOf<Document?>(null)
        private set

    fun updateDestinationDocument(doc: Document) {
        destinationDocument = doc
    }

    var lastlocationUpdatedTime by mutableStateOf(System.currentTimeMillis())
        private set

    fun updateLastlocationUpdatedTime() {
        lastlocationUpdatedTime = System.currentTimeMillis()
    }
}