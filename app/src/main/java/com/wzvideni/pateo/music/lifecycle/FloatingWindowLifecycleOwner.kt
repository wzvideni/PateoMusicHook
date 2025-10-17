package com.wzvideni.pateo.music.lifecycle

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner


/**
 * 悬浮窗生命周期管理
 *
 * INITIALIZED：初始状态，表示 Lifecycle 已经创建，但尚未开始。
 *
 * CREATED：表示组件已经完全创建，但未开始与用户交互。
 *
 * STARTED：组件已可见，已经开始与用户交互，但可能被暂停。
 *
 * RESUMED：组件完全启动，并处于与用户交互的状态。
 *
 * DESTROYED：组件销毁，生命周期结束。
 */
object FloatingWindowLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }
    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }

    init {
        lifecycleRegistry.addObserver(FloatingWindowLifecycleObserver)
        lifecycleRegistry.currentState = Lifecycle.State.INITIALIZED
        savedStateRegistryController.performAttach() // 绑定
        savedStateRegistryController.performRestore(null) // 可选：恢复状态

        Log.d("FloatingWindowLifecycleOwner", "INITIALIZED")
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    fun updateLifecycleState(state: Lifecycle.State) {
        lifecycleRegistry.currentState = state
        Log.d("FloatingWindowLifecycleOwner", "$state")
    }
}