package com.wzvideni.pateo.music.accessibility

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo

class AccessibilitySettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AccessibilitySettingsScreen() }
    }

    private fun runSuCommand(command: String): Boolean {
        return runCatching {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exit = proc.waitFor()
            exit == 0
        }.getOrElse { false }
    }

    private fun ensureAccessibilityEnabledLocked(context: Context, componentIds: Set<String>): Boolean {
        val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val accEnabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        val parts = current.split(":").filter { it.isNotBlank() }.toMutableSet()
        parts.addAll(componentIds)
        val newList = parts.joinToString(":")
        val ok1 = runSuCommand("cmd settings put secure enabled_accessibility_services ${newList}")
                || runSuCommand("settings put secure enabled_accessibility_services ${newList}")
        val ok2 = if (accEnabled != 1) {
            runSuCommand("cmd settings put secure accessibility_enabled 1") || runSuCommand("settings put secure accessibility_enabled 1")
        } else true
        return ok1 && ok2
    }

    @Composable
    private fun RowWithSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

    @Composable
    private fun AccessibilitySettingsScreen() {
        val ctx = this
        val prefs = getSharedPreferences("accessibility_prefs", MODE_PRIVATE)
        val initialSet = prefs.getStringSet("locked_service_ids", emptySet()) ?: emptySet()
        var selectedIds by remember { mutableStateOf(initialSet.toSet()) }
        var autoEnable by remember { mutableStateOf(prefs.getBoolean("auto_enable", true)) }
        var monitorEnabled by remember { mutableStateOf(prefs.getBoolean("monitor_enabled", true)) }
        var installed by remember { mutableStateOf(listOf<AccessibilityServiceInfo>()) }
        var allEnabled by remember { mutableStateOf(false) }

        // 兼容旧版单选配置
        LaunchedEffect(Unit) {
            val legacy = prefs.getString("locked_service_id", "") ?: ""
            if (selectedIds.isEmpty() && legacy.isNotBlank()) {
                selectedIds = selectedIds + legacy
            }
            val am = getSystemService(AccessibilityManager::class.java)
            installed = am.installedAccessibilityServiceList ?: emptyList()
            allEnabled = areServicesEnabled(selectedIds)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "无障碍服务自动开启设置", fontWeight = FontWeight.Bold)
                    RowWithSwitch(
                        label = "桌面启动后自动检测并开启",
                        checked = autoEnable,
                        onCheckedChange = { value ->
                            autoEnable = value
                            prefs.edit().putBoolean("auto_enable", value).apply()
                        }
                    )
                    RowWithSwitch(
                        label = "持续监控无障碍状态",
                        checked = monitorEnabled,
                        onCheckedChange = { value ->
                            monitorEnabled = value
                            prefs.edit().putBoolean("monitor_enabled", value).apply()
                            if (value) AccessibilityMonitorService.startIfEnabled(ctx) else AccessibilityMonitorService.stop(ctx)
                        }
                    )
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "选择锁定的无障碍服务", fontWeight = FontWeight.Bold)
                    installed.forEach { info ->
                        val ri = info.resolveInfo
                        val comp = ComponentName(ri.serviceInfo.packageName, ri.serviceInfo.name)
                        val id = comp.flattenToString()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.clickable {
                                selectedIds = if (selectedIds.contains(id)) selectedIds - id else selectedIds + id
                            }
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(id),
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + id else selectedIds - id
                                }
                            )
                            Column(Modifier.weight(1f)) {
                                Text(text = ri.loadLabel(packageManager)?.toString() ?: ri.serviceInfo.name, fontWeight = FontWeight.Medium)
                                Text(text = id, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            prefs.edit().putStringSet("locked_service_ids", HashSet(selectedIds)).apply()
                            // 清理旧版键以避免混淆
                            prefs.edit().remove("locked_service_id").apply()
                            Toast.makeText(ctx, "已保存设置", Toast.LENGTH_SHORT).show()
                        }) { Text(text = "保存设置") }

                        Button(onClick = {
                            if (selectedIds.isEmpty()) {
                                Toast.makeText(ctx, "请至少选择一个无障碍服务", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val success = ensureAccessibilityEnabledLocked(ctx, selectedIds)
                            allEnabled = areServicesEnabled(selectedIds)
                            Toast.makeText(ctx, if (success && allEnabled) "已尝试开启并检测成功" else "开启失败，请在系统无障碍设置中手动开启", Toast.LENGTH_SHORT).show()
                        }) { Text(text = "立即检测并自动开启") }

                        Button(onClick = {
                            runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                                .onFailure { Toast.makeText(ctx, "无法打开无障碍设置页面", Toast.LENGTH_SHORT).show() }
                        }) { Text(text = "打开系统无障碍设置") }

                        Button(onClick = { finish() }) { Text(text = "返回") }
                    }
                }
            }

            Text(text = "当前服务状态：${if (allEnabled) "全部已启用" else "未全部启用"}", style = MaterialTheme.typography.bodySmall)
        }
    }

    private fun areServicesEnabled(componentIds: Set<String>): Boolean {
        if (componentIds.isEmpty()) return false
        val current = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val accEnabled = Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        val set = current.split(":").filter { it.isNotBlank() }.map { it.trim() }.toSet()
        return accEnabled == 1 && componentIds.all { set.contains(it) }
    }
}