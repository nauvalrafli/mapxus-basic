package com.example.mapxushsitpsample

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.LifecycleOwner
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapxus.map.mapxusmap.api.map.FollowUserMode
import com.mapxus.map.mapxusmap.api.map.MapxusMap
import com.mapxus.map.mapxusmap.api.map.interfaces.OnMapxusMapReadyCallback
import com.mapxus.map.mapxusmap.api.map.model.MapxusMapOptions
import com.mapxus.map.mapxusmap.api.map.model.SelectorPosition
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.api.services.model.floor.Floor
import com.mapxus.map.mapxusmap.api.services.model.floor.SharedFloor
import com.mapxus.map.mapxusmap.impl.MapboxMapViewProvider
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProviderListener
import com.mapxus.positioning.positioning.api.ErrorInfo
import com.mapxus.positioning.positioning.api.FloorType
import com.mapxus.positioning.positioning.api.MapxusLocation
import com.mapxus.positioning.positioning.api.MapxusPositioningClient
import com.mapxus.positioning.positioning.api.MapxusPositioningListener
import com.mapxus.positioning.positioning.api.PositioningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.abs

class MapxusController(
    val context: Context,
    lifecycleOwner: LifecycleOwner,
    val locale: Locale
) {
    val mapView = MapView(context)
    val mapOptions = MapxusMapOptions().apply {
        floorId = "ad24bdcb0698422f8c8ab53ad6bb2665"
        zoomLevel = 19.0
    }
    val mapViewProvider = MapboxMapViewProvider(context, mapView, mapOptions)
    val mapxusPositioningClient = MapxusPositioningClient.getInstance(lifecycleOwner, context)
    val mapxusPositioningProvider : MapxusPositioningProvider = MapxusPositioningProvider(lifecycleOwner, context, mapxusPositioningClient)
    private var mapxusMap : MapxusMap? = null
    var mapboxMap : MapboxMap? = null

    val coroutineScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

    init {
        mapViewProvider.getMapxusMapAsync(object : OnMapxusMapReadyCallback {
            override fun onMapxusMapReady(p0: MapxusMap?) {
                Log.d("Location", "Mapxus is ready")
                mapxusMap = p0
//                mapxusMap?.mapxusUiSettings?.isBuildingSelectorEnabled = false
//                mapxusMap?.mapxusUiSettings?.isSelectorEnabled = false
                mapxusMap?.mapxusUiSettings?.setSelectorPosition(SelectorPosition.TOP_LEFT)
                mapxusMap?.mapxusUiSettings?.setSelectFontColor(Color.White.hashCode())
                mapxusMap?.mapxusUiSettings?.setSelectBoxColor(Color(0xFF4285F4).hashCode())
                mapView.getMapAsync(object: OnMapReadyCallback {
                    override fun onMapReady(mMap: MapboxMap) {
                        coroutineScope.launch {
                            delay(3000)
                            withContext(Dispatchers.Main) {
                                // we want to make the user location always show which direction its facing
                                // is there a better way to do this? I am unable to find something to check whether the style is fully loaded or not.
                                useDefaultDrawableBearingIcon()
                                mapxusMap?.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
                            }
                        }
                        mMap.setMinZoomPreference(18.0)
                        mapboxMap = mMap
                    }
                })
                mapxusPositioningProvider.addListener(object: IndoorLocationProviderListener {
                    override fun onCompassChanged(angle: Float, sensorAccuracy: Int) {

                    }

                    override fun onIndoorLocationChange(indoorLocation: IndoorLocation?) {
                        useDefaultDrawableBearingIcon()
                    }

                    override fun onProviderError(errorInfo: com.mapxus.map.mapxusmap.positioning.ErrorInfo) {
                        Log.d("Location", "Provider Error: ${errorInfo.errorMessage}")
                    }

                    override fun onProviderStarted() {
                        Log.d("Location", "Started")
                        mapxusMap?.setLocationProvider(mapxusPositioningProvider)
                    }

                    override fun onProviderStopped() {
                        Log.d("Location", "Stopped")
                    }

                })
                mapxusPositioningProvider.start()
                mapxusMap?.setLocationEnabled(true)
                mapxusMap?.setLocationProvider(mapxusPositioningProvider)
            }
        })
        mapxusPositioningClient.addPositioningListener(object : MapxusPositioningListener {
            override fun onStateChange(positionerState: PositioningState) {
                when (positionerState) {
                    PositioningState.STOPPED -> {
                        mapxusPositioningProvider.dispatchOnProviderStopped()
                    }

                    PositioningState.RUNNING -> {
                        mapxusPositioningProvider.dispatchOnProviderStarted()
                    }

                    else -> {}
                }
            }

            override fun onError(errorInfo: ErrorInfo) {
                mapxusPositioningProvider.dispatchOnProviderError(
                    com.mapxus.map.mapxusmap.positioning.ErrorInfo(
                        errorInfo.errorCode,
                        errorInfo.errorMessage
                    )
                )
            }

            override fun onOrientationChange(orientation: Float, sensorAccuracy: Int) {
                if (mapxusPositioningProvider.isInHeadingMode) {
                    if (abs((orientation - mapxusPositioningProvider.lastCompass).toDouble()) > 10) {
                        mapxusPositioningProvider.dispatchCompassChange(orientation, sensorAccuracy)
                    }
                } else {
                    mapxusPositioningProvider.dispatchCompassChange(orientation, sensorAccuracy)
                }
            }

            override fun onLocationChange(mapxusLocation: MapxusLocation) {
                val location = Location("MapxusPositioning")
                coroutineScope.launch {
                    location.latitude = mapxusLocation.latitude
                    location.longitude = mapxusLocation.longitude
                    location.time = System.currentTimeMillis()
                    val building = mapxusLocation.buildingId
                    var floor : Floor? = null;
                    if(mapxusLocation.mapxusFloor != null) {
                        mapxusPositioningClient.changeFloor(mapxusLocation.mapxusFloor)
                        val floorType = mapxusLocation.mapxusFloor.type
                        if(floorType == FloorType.FLOOR) {
                            floor = FloorInfo(
                                mapxusLocation.mapxusFloor.id,
                                mapxusLocation.mapxusFloor.code,
                                mapxusLocation.mapxusFloor.ordinal,
                            )
                        } else {
                            floor = SharedFloor(
                                mapxusLocation.mapxusFloor.id,
                                mapxusLocation.mapxusFloor.code,
                                mapxusLocation.mapxusFloor.ordinal
                            )
                        }
                    }

                    val indoorLocation = IndoorLocation(building, floor, location)
                    indoorLocation.accuracy = mapxusLocation.accuracy

                    withContext(Dispatchers.Main) {
                        mapxusPositioningProvider.dispatchIndoorLocationChange(indoorLocation)
                    }
                }
            }
        })
        mapxusPositioningProvider.start()
        mapxusPositioningClient.start()
        mapxusMap?.setLocationEnabled(true)
        mapxusMap?.followUserMode = FollowUserMode.FOLLOW_USER_AND_HEADING
    }

    fun getMapxusMap(): MapxusMap {
        if (mapxusMap != null) {
            return mapxusMap!!
        }

        var result: MapxusMap? = null
        val latch = CountDownLatch(1)

        mapViewProvider.getMapxusMapAsync {
            result = it
            latch.countDown()
        }

        latch.await() // blocks current thread until latch is released
        return result!!
    }

    fun useDefaultDrawableBearingIcon() {
        // we do this to make it possible to be run from inside a suspended function
        // what we want to achieve is to use custom icon for the gps and bearing, but unable to
        // find the way to do this. Therefore we choose to show compass with custom bearing drawable.
        // Sadly, gpsDrawable only usable in RenderMode.GPS
        coroutineScope.launch {
            withContext(Dispatchers.Main) {
                mapboxMap?.locationComponent?.applyStyle(
                    LocationComponentOptions.builder(context)
                        // .bearingDrawable(R.drawable.user_location)
                        .build()
                )
                mapboxMap?.locationComponent?.renderMode = RenderMode.COMPASS
            }
        }
    }
}