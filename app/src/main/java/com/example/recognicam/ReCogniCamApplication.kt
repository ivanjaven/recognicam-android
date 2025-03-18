package com.example.recognicam

import android.app.Application
import com.example.recognicam.core.ServiceLocator

class ReCogniCamApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}