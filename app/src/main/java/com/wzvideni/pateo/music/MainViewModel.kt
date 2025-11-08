package com.wzvideni.pateo.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.highcapable.yukihookapi.hook.log.YLog
import com.wzvideni.pateo.music.data.Lyric
import com.wzvideni.pateo.music.data.qqMusicLyricRequest
import com.wzvideni.pateo.music.expansion.set
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _songId: MutableStateFlow<String?> = MutableStateFlow(null)
    val songId: StateFlow<String?> = _songId
    fun setSongId(songId: String?) = _songId.set(songId)

    private val _songMid: MutableStateFlow<String?> = MutableStateFlow(null)
    val songMid: StateFlow<String?> = _songMid
    fun setSongMid(songMid: String?) = _songMid.set(songMid)

    // 歌曲名
    private val _songName: MutableStateFlow<String?> = MutableStateFlow(null)
    val songName: StateFlow<String?> = _songName
    fun setSongName(songName: String?) {
        if (_songName.value == songName) return
        _songName.set(songName)
        com.wzvideni.pateo.music.tasker.HookState.title = songName
        com.wzvideni.pateo.music.tasker.HookState.timestamp = System.currentTimeMillis()
        onMetadataFieldChanged()
    }

    private val _singerName: MutableStateFlow<String?> = MutableStateFlow(null)
    val singerName: StateFlow<String?> = _singerName
    fun setSingerName(singerName: String?) {
        if (_singerName.value == singerName) return
        _singerName.set(singerName)
        com.wzvideni.pateo.music.tasker.HookState.artist = singerName
        com.wzvideni.pateo.music.tasker.HookState.timestamp = System.currentTimeMillis()
        onMetadataFieldChanged()
    }

    // 专辑名
    private val _albumName: MutableStateFlow<String?> = MutableStateFlow(null)
    val albumName: StateFlow<String?> = _albumName
    fun setAlbumName(albumName: String?) {
        if (_albumName.value == albumName) return
        _albumName.set(albumName)
        com.wzvideni.pateo.music.tasker.HookState.album = albumName
        com.wzvideni.pateo.music.tasker.HookState.timestamp = System.currentTimeMillis()
        onMetadataFieldChanged()
    }

    // 专辑图片
    private val _albumPic: MutableStateFlow<String?> = MutableStateFlow(null)
    val albumPic: StateFlow<String?> = _albumPic
    fun setAlbumPic(albumPic: String?) {
        if (_albumPic.value == albumPic) return
        _albumPic.set(albumPic)
        com.wzvideni.pateo.music.tasker.HookState.albumPic = albumPic
        com.wzvideni.pateo.music.tasker.HookState.timestamp = System.currentTimeMillis()
        onMetadataFieldChanged()
    }

    // 音乐播放位置
    private val _musicPlayingPosition: MutableStateFlow<Long> = MutableStateFlow(0L)
    val musicPlayingPosition: StateFlow<Long> = _musicPlayingPosition
    fun setMusicPlayingPosition(value: Long) = _musicPlayingPosition.set(value)
        .also {
            com.wzvideni.pateo.music.tasker.HookState.positionMs = value
            com.wzvideni.pateo.music.tasker.HookState.timestamp = System.currentTimeMillis()
        }

    // 音乐歌词
    private val _musicLyrics: MutableStateFlow<String> = MutableStateFlow("")
    val musicLyrics: StateFlow<String> = _musicLyrics
    fun setMusicLyrics(value: String) = _musicLyrics.set(value)

    // 音乐翻译
    private val _musicTranslation: MutableStateFlow<String> = MutableStateFlow("")
    val musicTranslation: StateFlow<String> = _musicTranslation
    fun setMusicTranslation(value: String) = _musicTranslation.set(value)

    // 音乐歌词索引
    private val _musicLyricsIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val musicLyricsIndex: StateFlow<Int> = _musicLyricsIndex
    fun setMusicLyricsIndex(value: Int) {
        _musicLyricsIndex.set(value)
        // 当歌词索引更新时，触发 Tasker 事件用于外部自动化
        // 仅取原歌词（索引 0），不包含翻译
        val currentLyricText = _musicLyricsList.value.getOrNull(value)?.lyricsList?.getOrNull(0)
        // 查找下一句原歌词（向后查找第一个存在索引 0 的条目），不包含翻译
        val secondLyricText = run {
            val list = _musicLyricsList.value
            var j = value + 1
            var next: String? = null
            while (j < list.size && next == null) {
                next = list.getOrNull(j)?.lyricsList?.getOrNull(0)
                j++
            }
            next
        }
        // 更新 HookState，供 Action 直接读取
        com.wzvideni.pateo.music.tasker.HookState.lyric = currentLyricText
        com.wzvideni.pateo.music.tasker.HookState.nextLyric = secondLyricText
        com.wzvideni.pateo.music.tasker.HookState.index = value
        com.wzvideni.pateo.music.tasker.HookState.timestamp = System.currentTimeMillis()

        com.wzvideni.pateo.music.tasker.LyricsTaskerEvent.trigger(getApplication(), currentLyricText, secondLyricText, value)
    }

    // 音乐歌词列表
    private val _musicLyricsList: MutableStateFlow<List<Lyric>> = MutableStateFlow(emptyList())
    val musicLyricsList: StateFlow<List<Lyric>> = _musicLyricsList
    fun setMusicLyricsList(value: List<Lyric>) = _musicLyricsList.set(value)

    init {
        viewModelScope.launch {
            _songMid.collect { songMid ->
                qqMusicSearch(songMid)
            }
        }

        viewModelScope.launch {
            _musicPlayingPosition.collect {
                findCurrentLyrics(_musicLyricsList.value)
            }
        }
    }

    // 元数据变化合并与去重：短延迟后统一输出一次，避免连续四次 setter 触发四次广播
    private var metadataChanged: Boolean = false
    private var metadataDebounceJob: Job? = null
    private fun onMetadataFieldChanged() {
        metadataChanged = true
        metadataDebounceJob?.cancel()
        metadataDebounceJob = viewModelScope.launch {
            delay(80)
            if (metadataChanged) {
                metadataChanged = false
                com.wzvideni.pateo.music.tasker.MetadataTaskerEvent.trigger(
                    getApplication(),
                    _singerName.value,
                    _songName.value,
                    _albumName.value,
                    _albumPic.value,
                )
            }
        }
    }


    suspend fun qqMusicSearch(songMid: String?) {
        YLog.debug("qqMusicSearch songMid: $songMid")
        if (songMid != null) {
            _musicLyricsList.value = qqMusicLyricRequest(songMid)
        }
    }

    // 查找当前位置的歌词（二分查找）
    private fun findCurrentLyrics(lyricsList: List<Lyric>): List<String>? {
        // 以下变量皆是需要短时间内经常改变的，定义为常量创建的开销可能过大
        // 最后索引
        val lastIndex = lyricsList.lastIndex
        // 若当前位置已经在最后一句时间点之后，直接选中最后一句，避免错过末尾更新
        if (lastIndex >= 0) {
            val positionNow = musicPlayingPosition.value
            val lastLyric = lyricsList[lastIndex]
            if (positionNow >= lastLyric.millisecond) {
                setMusicLyricsIndex(lastIndex)
                return lastLyric.lyricsList
            }
        }
        // 开始索引
        var beginIndex = 0
        // 结束索引
        var endIndex = lastIndex
        // 中间索引
        var midIndex: Int
        // 中间索引歌词
        var midLyric: Lyric
        // 下一个毫秒时间轴
        var nextMillisecond: Int
        // 从开始循环到结束
        while (beginIndex <= endIndex) {
            val position = musicPlayingPosition.value
            // 中间索引
            midIndex = (beginIndex + endIndex) / 2
            // 获取中间索引歌词
            midLyric = lyricsList[midIndex]
            // 判断中间索引是否等于结束索引，是就表示找到了歌词
            if (midIndex == endIndex) {
                setMusicLyricsIndex(midIndex)
                return midLyric.lyricsList
            }
            // 获取下一个毫秒时间轴
            nextMillisecond = lyricsList[midIndex + 1].millisecond
            // 判断播放位置是否在中间索引歌词的毫秒时间轴和下一个时间轴之间，是就表示找到歌词了
            if (position in midLyric.millisecond until nextMillisecond) {
                // 更新歌词索引
                setMusicLyricsIndex(midIndex)
                return midLyric.lyricsList
                // 否则判断播放位置是否小于中间索引歌词的毫秒时间轴，是就表示歌词位于前半部分
            } else if (position < midLyric.millisecond) {
                // 末尾索引移动到中间索引前一个
                endIndex = midIndex - 1
                // 否则判断播放位置是否大于中间索引歌词的毫秒时间轴，是就表示歌词位于后半部分
            } else if (position > midLyric.millisecond) {
                // 开始索引移动到中间索引后一个
                beginIndex = midIndex + 1
            }
        }
        // 没有在区间内找到：若歌词列表不为空且当前位置在最后一句之前，则默认保持当前逻辑返回第一句；
        // 也可按需改为返回最后一句。为兼容现有行为，这里沿用返回第一句。
        setMusicLyricsIndex(0)
        return null
    }
}