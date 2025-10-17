package com.wzvideni.pateo.music.expansion

fun <T> MutableList<T>.replace(newData: Collection<T>) {
    clear()
    addAll(newData)
}
