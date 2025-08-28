package com.example.mapxushsitpsample

import android.app.Application
import com.mapxus.map.mapxusmap.api.map.MapxusMapContext

class BaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MapxusMapContext.init(applicationContext)
    }
}