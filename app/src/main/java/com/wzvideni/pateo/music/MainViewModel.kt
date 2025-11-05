package com.wzvideni.pateo.music

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.highcapable.yukihookapi.hook.log.YLog
import com.wzvideni.pateo.music.data.Lyric
import com.wzvideni.pateo.music.data.qqMusicLyricRequest
import com.wzvideni.pateo.music.expansion.set
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
    fun setSongName(songName: String?) = _songName.set(songName)

    private val _singerName: MutableStateFlow<String?> = MutableStateFlow(null)
    val singerName: StateFlow<String?> = _singerName
    fun setSingerName(singerName: String?) = _singerName.set(singerName)

    // 专辑名
    private val _albumName: MutableStateFlow<String?> = MutableStateFlow(null)
    val albumName: StateFlow<String?> = _albumName
    fun setAlbumName(albumName: String?) = _albumName.set(albumName)

    // 专辑图片
    private val _albumPic: MutableStateFlow<String?> = MutableStateFlow(null)
    val albumPic: StateFlow<String?> = _albumPic
    fun setAlbumPic(albumPic: String?) = _albumPic.set(albumPic)

    // 音乐播放位置
    private val _musicPlayingPosition: MutableStateFlow<Long> = MutableStateFlow(0L)
    val musicPlayingPosition: StateFlow<Long> = _musicPlayingPosition
    fun setMusicPlayingPosition(value: Long) = _musicPlayingPosition.set(value)

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
    fun setMusicLyricsIndex(value: Int) = _musicLyricsIndex.set(value)

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
        // 没有找到就设置歌词索引为-1并返回单例空列表
        setMusicLyricsIndex(0)
        return null
    }
}