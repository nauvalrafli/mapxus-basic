package com.example.mapxushsitpsample

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.mapxus.map.mapxusmap.api.services.model.building.FloorInfo
import com.mapxus.map.mapxusmap.overlay.navi.RouteAdsorber
import com.mapxus.map.mapxusmap.positioning.IndoorLocation
import com.mapxus.map.mapxusmap.positioning.IndoorLocationProvider
import com.mapxus.positioning.positioning.api.ErrorInfo
import com.mapxus.positioning.positioning.api.MapxusLocation
import com.mapxus.positioning.positioning.api.MapxusPositioningClient
import com.mapxus.positioning.positioning.api.MapxusPositioningListener
import com.mapxus.positioning.positioning.api.PositioningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.abs

class MapxusPositioningProvider(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val positioningClient: MapxusPositioningClient
) : IndoorLocationProvider() {
    private var started = false
    var isInHeadingMode: Boolean = false

    override fun supportsFloor(): Boolean {
        return true
    }

    override fun start() {
        positioningClient.start()
        started = true
    }

    override fun stop() {
        if (positioningClient != null) {
            positioningClient.stop()
        }
        started = false
    }

    override fun isStarted(): Boolean {
        return started
    }

    companion object {
        private val TAG: String = MapxusPositioningProvider::class.java.simpleName
    }
}