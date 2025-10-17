package com.wzvideni.pateo.music.lifecycle

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object FloatingWindowLifecycleObserver : DefaultLifecycleObserver {
    private val _floatingWindowLifecycle = MutableStateFlow<FloatingWindowLifecycle?>(null)
    val floatingWindowLifecycle: StateFlow<FloatingWindowLifecycle?> = _floatingWindowLifecycle

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        _floatingWindowLifecycle.value = FloatingWindowLifecycle.CREATED
        Log.d("FloatingWindowLifecycleObserver", "CREATED")
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        _floatingWindowLifecycle.value = FloatingWindowLifecycle.STARTED
        Log.d("FloatingWindowLifecycleObserver", "STARTED")
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        _floatingWindowLifecycle.value = FloatingWindowLifecycle.RESUMED
        Log.d("FloatingWindowLifecycleObserver", "RESUMED")
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        _floatingWindowLifecycle.value = FloatingWindowLifecycle.PAUSED
        Log.d("FloatingWindowLifecycleObserver", "PAUSED")
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        _floatingWindowLifecycle.value = FloatingWindowLifecycle.STOPPED
        Log.d("FloatingWindowLifecycleObserver", "STOPPED")
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        _floatingWindowLifecycle.value = FloatingWindowLifecycle.DESTROYED
        Log.d("FloatingWindowLifecycleObserver", "DESTROYED")
    }
}