package com.example.mapxushsitpsample

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mapxushsitpsample.ui.theme.MapxusHSITPSampleTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    var controller : MapxusController? = null

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.light(
            android.graphics.Color.TRANSPARENT,
            android.graphics.Color.TRANSPARENT
        ))
        controller = MapxusController(this, this, locale = Locale.getDefault())
        setContent {
            MapxusHSITPSampleTheme {

                val notificationPermission = rememberMultiplePermissionsState(
                    permissions = listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )

                LaunchedEffect(true) {
                    if (!notificationPermission.allPermissionsGranted) {
                        notificationPermission.launchMultiplePermissionRequest()
                    }
                }

                val coroutineScope = rememberCoroutineScope()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.TopEnd
                    ) {
                        controller?.let { it1 -> MapxusComposable(
                            modifier = Modifier.fillMaxWidth().padding(innerPadding).fillMaxHeight(1F),
                            controller = it1
                        ) }
                        Column(modifier = Modifier.padding(top = 56.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            IconButton(
                                onClick = {
                                    val zoomLevel = controller?.mapboxMap?.cameraPosition?.zoom
                                    controller?.getMapxusMap()?.followUserMode =
                                        FollowUserMode.FOLLOW_USER_AND_HEADING
                                    coroutineScope.launch {
                                        delay(500)
                                        controller?.mapboxMap?.cameraPosition =
                                            com.mapbox.mapboxsdk.camera.CameraPosition.Builder()
                                                .zoom(zoomLevel ?: 19.0).build()
                                    }
                                },
                                modifier = Modifier
                                    .padding(6.dp)
                                    .align(Alignment.End)
                                    .background(color = Color.White, shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        controller?.mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        controller?.mapView?.onResume()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        controller?.mapView?.onLowMemory()
    }

    override fun onStop() {
        super.onStop()
        controller?.mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        controller?.mapView?.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        controller?.mapView?.onPause()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MapxusHSITPSampleTheme {
        Greeting("Android")
    }
}