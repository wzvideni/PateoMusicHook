package com.wzvideni.traccar.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.URLUtil
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import org.traccar.client.MainFragment
import org.traccar.client.TrackingService
import org.traccar.client.StatusActivity

class TraccarActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { TraccarScreen() }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TraccarScreen() {
        val context = this
        val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

        var deviceId by remember { mutableStateOf(prefs.getString(MainFragment.KEY_DEVICE, "") ?: "") }
        var url by remember { mutableStateOf(prefs.getString(MainFragment.KEY_URL, "") ?: "") }
        var interval by remember { mutableStateOf(prefs.getString(MainFragment.KEY_INTERVAL, "600") ?: "600") }
        var distance by remember { mutableStateOf(prefs.getString(MainFragment.KEY_DISTANCE, "0") ?: "0") }
        var angle by remember { mutableStateOf(prefs.getString(MainFragment.KEY_ANGLE, "0") ?: "0") }
        var accuracy by remember { mutableStateOf(prefs.getString(MainFragment.KEY_ACCURACY, "medium") ?: "medium") }
        var trackingOn by remember { mutableStateOf(prefs.getBoolean(MainFragment.KEY_STATUS, false)) }

        val permissionsLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            val fineGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (fineGranted) {
                startTrackingService()
            }
        }

        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 头部与状态，风格对齐 MQTT 控制台
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "traccar跟踪控制台".uppercase(), style = MaterialTheme.typography.titleMedium)
                    val statusColor = if (trackingOn) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                    Box(Modifier.size(10.dp).background(statusColor))
                    androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    TextButton(onClick = { finish() }) { Text("返回".uppercase()) }
                }

                // 配置卡片
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = deviceId,
                            onValueChange = { deviceId = it },
                            label = { Text("设备ID".uppercase()) },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        OutlinedTextField(
                            value = url,
                            onValueChange = { url = it },
                            label = { Text("服务器 URL".uppercase()) },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = interval,
                                onValueChange = { interval = it.filter { ch -> ch.isDigit() } },
                                label = { Text("上报间隔（秒）".uppercase()) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = distance,
                                onValueChange = { distance = it.filter { ch -> ch.isDigit() } },
                                label = { Text("最小距离（米）".uppercase()) },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = angle,
                                onValueChange = { angle = it.filter { ch -> ch.isDigit() } },
                                label = { Text("最小方位变化（度）".uppercase()) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        AccuracyDropdown(accuracy) { accuracy = it }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (deviceId.isBlank()) return@Button
                                if (!validateServerURL(url)) return@Button
                                prefs.edit()
                                    .putString(MainFragment.KEY_DEVICE, deviceId)
                                    .putString(MainFragment.KEY_URL, url)
                                    .putString(MainFragment.KEY_INTERVAL, interval.ifBlank { "600" })
                                    .putString(MainFragment.KEY_DISTANCE, distance.ifBlank { "0" })
                                    .putString(MainFragment.KEY_ANGLE, angle.ifBlank { "0" })
                                    .putString(MainFragment.KEY_ACCURACY, accuracy)
                                    .apply()
                            }) { Text("保存设置".uppercase()) }
                            Button(onClick = {
                                val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (!fineGranted) {
                                    permissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                                } else {
                                    startTrackingService()
                                    trackingOn = true
                                }
                            }, enabled = !trackingOn) { Text("启动服务".uppercase()) }
                            Button(onClick = {
                                stopService(Intent(context, TrackingService::class.java))
                                prefs.edit().putBoolean(MainFragment.KEY_STATUS, false).apply()
                                trackingOn = false
                            }, enabled = trackingOn) { Text("停止服务".uppercase()) }
                            TextButton(onClick = { startActivity(Intent(context, StatusActivity::class.java)) }) { Text("查看状态".uppercase()) }
                        }
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            // 预设默认值（使用 Traccar 的 preferences.xml）
            PreferenceManager.setDefaultValues(context, org.traccar.client.R.xml.preferences, false)

            // 默认打开 Traccar 的启动服务：在具备配置与权限的前提下自动启动
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val alreadyOn = prefs.getBoolean(MainFragment.KEY_STATUS, false)
            val currentDeviceId = prefs.getString(MainFragment.KEY_DEVICE, null)?.trim().orEmpty()
            val currentUrl = prefs.getString(MainFragment.KEY_URL, null)?.trim().orEmpty()

            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val canStart = currentDeviceId.isNotBlank() && validateServerURL(currentUrl)
            if (canStart && !alreadyOn) {
                if (!fineGranted) {
                    // 自动请求定位权限，授权后在回调里会启动服务
                    permissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                } else {
                    startTrackingService()
                    trackingOn = true
                }
            }
        }
    }

    private fun startTrackingService() {
        val context = this
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putBoolean(MainFragment.KEY_STATUS, true).apply()
        ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
    }

    private fun validateServerURL(userUrl: String): Boolean {
        val port = Uri.parse(userUrl).port
        return URLUtil.isValidUrl(userUrl) && (port == -1 || port in 1..65535) &&
                (URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))
    }

    @Composable
    private fun AccuracyDropdown(current: String, onSelect: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val options = listOf("high" to "高精度 (GPS)", "medium" to "中精度 (网络)", "low" to "低精度 (被动)")
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = { expanded = true }) { Text((options.find { it.first == current }?.second ?: "选择精度").uppercase()) }
            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (key, label) ->
                    DropdownMenuItem(
                        text = { Text(label.uppercase()) },
                        onClick = { onSelect(key); expanded = false }
                    )
                }
            }
        }
    }
}