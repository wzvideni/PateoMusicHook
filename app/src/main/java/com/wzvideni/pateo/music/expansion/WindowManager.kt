package com.wzvideni.pateo.music.expansion

import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager


// 锁定的悬浮窗布局参数
val lockedWindowParams: WindowManager.LayoutParams =
    // 安卓12不受信任的触摸事件都会被屏蔽，目前采用设置透明度的方法
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.S) {
        WindowManager.LayoutParams(
            // 应用程序覆盖窗口显示在所有活动窗口上方
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // 不会获得按键输入焦点，因此用户无法向其发送点击或其他按钮事件
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    // 不能接收触摸事件
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            // 悬浮窗背景
            PixelFormat.TRANSPARENT
        )
    } else {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSPARENT
        ).apply {
            alpha = 0.8f
        }
    }

// 未锁定的悬浮窗布局参数
val unlockedWindowParams: WindowManager.LayoutParams =
    if (Build.VERSION.SDK_INT != Build.VERSION_CODES.S) {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )
    } else {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        ).apply {
            alpha = 0.8f
        }
    }
