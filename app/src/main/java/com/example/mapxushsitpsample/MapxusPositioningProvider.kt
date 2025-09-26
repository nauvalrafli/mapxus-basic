package com.example.mapxushsitpsample

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.api.services.model.floor.Floor
import com.mapxus.map.mapxusmap.api.services.model.floor.SharedFloor
import com.mapxus.map.mapxusmap.overlay.navi.RouteAdsorber
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider
import com.mapxus.positioning.positioning.api.ErrorInfo
import com.mapxus.positioning.positioning.api.FloorType
import com.mapxus.positioning.positioning.api.MapxusLocation
import com.mapxus.positioning.positioning.api.MapxusPositioningClient
import com.mapxus.positioning.positioning.api.MapxusPositioningListener
import com.mapxus.positioning.positioning.api.PositioningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.abs

class MapxusPositioningProvider(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context
) : IndoorLocationProvider() {
    private var started = false
    var isInHeadingMode: Boolean = false

    lateinit var mapxusPositioningClient : MapxusPositioningClient

    override fun supportsFloor(): Boolean {
        return true
    }

    override fun start() {
        mapxusPositioningClient = MapxusPositioningClient.getInstance(lifecycleOwner, context)
        mapxusPositioningClient.addPositioningListener(mapxusPositioningListener)
        mapxusPositioningClient.start()
        started = true
    }

    override fun stop() {
        if (mapxusPositioningClient != null) {
            mapxusPositioningClient.stop()
            mapxusPositioningClient.removePositioningListener(mapxusPositioningListener)
        }
        started = false
    }

    override fun isStarted(): Boolean {
        return started
    }

    val mapxusPositioningListener = object : MapxusPositioningListener {
        override fun onStateChange(positionerState: PositioningState) {
            when (positionerState) {
                PositioningState.STOPPED -> {
                    dispatchOnProviderStopped()
                }

                PositioningState.RUNNING -> {
                    dispatchOnProviderStarted()
                }

                else -> {}
            }
        }

        override fun onError(errorInfo: ErrorInfo) {
            dispatchOnProviderError(
                com.mapxus.map.mapxusmap.positioning.ErrorInfo(
                    errorInfo.errorCode,
                    errorInfo.errorMessage
                )
            )
        }

        override fun onOrientationChange(orientation: Float, sensorAccuracy: Int) {
            if (isInHeadingMode) {
                if (abs((orientation - lastCompass).toDouble()) > 10) {
                    dispatchCompassChange(orientation, sensorAccuracy)
                }
            } else {
                dispatchCompassChange(orientation, sensorAccuracy)
            }
        }

        override fun onLocationChange(mapxusLocation: MapxusLocation) {
            if(mapxusLocation == null) {
                return;
            }
            val location = Location("MapxusPositioning")
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

            dispatchIndoorLocationChange(indoorLocation)
        }
    }

    companion object {
        private val TAG: String = MapxusPositioningProvider::class.java.simpleName
    }
}