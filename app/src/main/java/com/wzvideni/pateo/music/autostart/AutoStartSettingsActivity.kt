package com.wzvideni.pateo.music.autostart

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class AutoStartSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { SettingsScreen() }
    }

    @Composable
    private fun SettingsScreen() {
        val context = this
        val deviceCtx = remember { context.createDeviceProtectedStorageContext() }
        val dprefs = remember { deviceCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
        val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

        var enabled by remember { mutableStateOf(dprefs.getBoolean(KEY_ENABLED, false) || prefs.getBoolean(KEY_ENABLED, false)) }
        var selectedLabel by remember { mutableStateOf(readLabel(context, dprefs, prefs)) }
        var showPicker by remember { mutableStateOf(false) }
        var apps by remember { mutableStateOf<List<ResolveInfo>>(emptyList()) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "开机自启动设置", fontWeight = FontWeight.Bold)

                    RowWithSwitch(
                        label = "启用开机自启动",
                        checked = enabled,
                        onCheckedChange = { value ->
                    enabled = value
                    putBooleanBoth(dprefs, prefs, KEY_ENABLED, value)
                }
            )

                    Text(text = "当前选择：${selectedLabel ?: "未选择"}")

                    Button(onClick = {
                        val pm = context.packageManager
                        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                        apps = pm.queryIntentActivities(query, 0)
                        showPicker = true
                    }) { Text("选择自启动应用") }

                    Button(onClick = { finish() }) { Text("返回") }
                }
            }

            if (showPicker) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        AppPickerList(apps) { ri ->
                            val pm = context.packageManager
                            val label = ri.loadLabel(pm).toString()
                            val pkg = ri.activityInfo.packageName
                            val comp = ri.activityInfo.name
                    putStringBoth(dprefs, prefs, KEY_PACKAGE, pkg)
                    putStringBoth(dprefs, prefs, KEY_COMPONENT, comp)
                    selectedLabel = label
                    showPicker = false
                }
            }
                }
            }
        }
    }

    private fun putBooleanBoth(dprefs: android.content.SharedPreferences, prefs: android.content.SharedPreferences, key: String, value: Boolean) {
        dprefs.edit().putBoolean(key, value).apply()
        prefs.edit().putBoolean(key, value).apply()
    }

    private fun putStringBoth(dprefs: android.content.SharedPreferences, prefs: android.content.SharedPreferences, key: String, value: String) {
        dprefs.edit().putString(key, value).apply()
        prefs.edit().putString(key, value).apply()
    }

    @Composable
    private fun AppPickerList(apps: List<ResolveInfo>, onPick: (ResolveInfo) -> Unit) {
        val pm = this.packageManager
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(apps) { ri ->
                val label = ri.loadLabel(pm).toString()
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { onPick(ri) },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    @Composable
    private fun RowWithSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

    private fun readLabel(context: Context, dprefs: android.content.SharedPreferences, prefs: android.content.SharedPreferences): String? {
        val pm = context.packageManager
        val pkg = dprefs.getString(KEY_PACKAGE, null) ?: prefs.getString(KEY_PACKAGE, null) ?: return null
        val comp = dprefs.getString(KEY_COMPONENT, null) ?: prefs.getString(KEY_COMPONENT, null)
        return try {
            if (!comp.isNullOrBlank()) {
                val cn = ComponentName(pkg, comp)
                val ri = pm.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(cn), 0)
                ri?.loadLabel(pm)?.toString() ?: pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            } else pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (_: Exception) { null }
    }

    companion object {
        private const val PREFS_NAME = "autostart_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PACKAGE = "package"
        private const val KEY_COMPONENT = "component"
    }
}