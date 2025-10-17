package com.wzvideni.pateo.music

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.wzvideni.pateo.music.expansion.toComposeColor
import com.wzvideni.pateo.music.expansion.toLongValue
import com.wzvideni.pateo.music.protobuf.proto.MainSettings
import com.wzvideni.pateo.music.protobuf.proto.mainSettings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

val Context.MainSettingsDataStore: DataStore<MainSettings> by dataStore(
    fileName = "MainSettings.pb",
    serializer = MainSettingsSerializer
)

object MainSettingsSerializer : Serializer<MainSettings> {

    // MainSettings.getDefaultInstance()
    override val defaultValue: MainSettings = mainSettings {
        autoSearch = MainDataStore.defaultAutoSearch
        lyricsColor = MainDataStore.defaultLyricsColor.toLongValue()
        translationColor = MainDataStore.defaultTranslationColor.toLongValue()
        lyricsSize = MainDataStore.defaultLyricsSize.value
        translationSize = MainDataStore.defaultTranslationSize.value
        lyricsWeight = MainDataStore.defaultLyricsWeight.weight
        translationWeight = MainDataStore.defaultTranslationWeight.weight
        lyricsVisibleLines = MainDataStore.defaultLyricsVisibleLines
        lyricsLineSpacing = MainDataStore.defaultLyricsLineSpacing.value
        otherLyricsColor = MainDataStore.defaultOtherLyricsColor.toLongValue()
        otherLyricsSize = MainDataStore.defaultOtherLyricsSize.value
        otherLyricsWeight = MainDataStore.defaultOtherLyricsWeight.weight
    }

    override suspend fun readFrom(input: InputStream): MainSettings {
        return MainSettings.parseFrom(input)
    }

    override suspend fun writeTo(t: MainSettings, output: OutputStream) {
        t.writeTo(output)
    }
}

class MainDataStore(context: Context) {

    companion object {
        val defaultAutoSearch by lazy { true }
        val defaultLyricsColor by lazy { Color.Red }
        val defaultTranslationColor by lazy { Color.Cyan }
        val defaultLyricsSize by lazy { 16.sp }
        val defaultTranslationSize by lazy { 15.sp }
        val defaultLyricsWeight by lazy { FontWeight.Normal }
        val defaultTranslationWeight by lazy { FontWeight.Normal }
        val defaultLyricsVisibleLines by lazy { 3 }
        val defaultLyricsLineSpacing by lazy { 15.dp }
        val defaultOtherLyricsColor by lazy { Color.White }
        val defaultOtherLyricsSize by lazy { 14.sp }
        val defaultOtherLyricsWeight by lazy { FontWeight.Normal }
    }

    private val dataStore = context.MainSettingsDataStore

    private val safeDataStore: Flow<MainSettings>
        get() = dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(MainSettings.getDefaultInstance())
            } else {
                throw exception
            }
        }


    private suspend inline fun updateSettings(crossinline block: MainSettings.Builder.() -> Unit) {
        dataStore.updateData { it.toBuilder().apply(block).build() }
    }


    // 自动搜索
    val autoSearch: Flow<Boolean> = safeDataStore.map { it.autoSearch }
    suspend fun getAutoSearch(): Boolean = autoSearch.first()
    suspend fun setAutoSearch(enabled: Boolean) =
        updateSettings { autoSearch = enabled }


    // 歌词颜色
    val lyricsColor: Flow<Color> = safeDataStore.map { it.lyricsColor.toComposeColor() }
    suspend fun getLyricsColor(): Color = lyricsColor.first()
    suspend fun setLyricsColor(color: Color) = updateSettings {
        lyricsColor = color.toLongValue()
    }

    // 翻译颜色
    val translationColor: Flow<Color> = safeDataStore.map { it.translationColor.toComposeColor() }
    suspend fun getTranslationColor(): Color = translationColor.first()
    suspend fun setTranslationColor(color: Color) =
        updateSettings { translationColor = color.toLongValue() }

    // 歌词大小
    val lyricsSize: Flow<TextUnit> = safeDataStore.map { it.lyricsSize.sp }
    suspend fun getLyricsSize(): TextUnit = lyricsSize.first()
    suspend fun setLyricsSize(size: Float) = updateSettings { lyricsSize = size }


    // 翻译大小
    val translationSize: Flow<TextUnit> = safeDataStore.map { it.translationSize.sp }
    suspend fun getTranslationSize(): TextUnit = translationSize.first()
    suspend fun setTranslationSize(size: Float) = updateSettings { translationSize = size }

    // 歌词粗细
    val lyricsWeight: Flow<FontWeight> = safeDataStore.map { FontWeight(it.lyricsWeight) }
    suspend fun getLyricsWeight(): FontWeight = lyricsWeight.first()
    suspend fun setLyricsWeight(weight: FontWeight) =
        updateSettings { lyricsWeight = weight.weight }

    // 翻译粗细
    val translationWeight: Flow<FontWeight> = safeDataStore.map { FontWeight(it.translationWeight) }
    suspend fun getTranslationWeight(): FontWeight = translationWeight.first()
    suspend fun setTranslationWeight(weight: FontWeight) =
        updateSettings { translationWeight = weight.weight }

    // 歌词可见行数
    val lyricsVisibleLines: Flow<Int> = safeDataStore.map { it.lyricsVisibleLines }
    suspend fun getLyricsVisibleLines(): Int = lyricsVisibleLines.first()
    suspend fun setLyricsVisibleLines(lines: Int) = updateSettings { lyricsVisibleLines = lines }

    // 歌词行间距
    val lyricsLineSpacing: Flow<Dp> = safeDataStore.map { it.lyricsLineSpacing.dp }
    suspend fun getLyricsLineSpacing(): Dp = lyricsLineSpacing.first()
    suspend fun setLyricsLineSpacing(spacing: Dp) =
        updateSettings { lyricsLineSpacing = spacing.value }

    // 其他歌词颜色
    val otherLyricsColor: Flow<Color> = safeDataStore.map { it.otherLyricsColor.toComposeColor() }
    suspend fun getOtherLyricsColor(): Color = otherLyricsColor.first()
    suspend fun setOtherLyricsColor(color: Color) = updateSettings {
        otherLyricsColor = color.toLongValue()
    }

    // 其他歌词字体大小
    val otherLyricsSize: Flow<TextUnit> = safeDataStore.map { it.otherLyricsSize.sp }
    suspend fun getOtherLyricsSize(): TextUnit = otherLyricsSize.first()
    suspend fun setOtherLyricsSize(size: Float) = updateSettings { otherLyricsSize = size }

    // 其他歌词字体粗细
    val otherLyricsWeight: Flow<FontWeight> = safeDataStore.map { FontWeight(it.otherLyricsWeight) }
    suspend fun getOtherLyricsWeight(): FontWeight = otherLyricsWeight.first()
    suspend fun setOtherLyricsWeight(weight: FontWeight) = updateSettings {
        otherLyricsWeight = weight.weight
    }
}
