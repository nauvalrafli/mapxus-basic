package com.example.mapxushsitpsample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapxusComposable(
    controller: MapxusController,
    modifier: Modifier
) {
    AndroidView(
        factory = {
            controller.mapView
        },
        modifier = Modifier.fillMaxSize()
    )

}