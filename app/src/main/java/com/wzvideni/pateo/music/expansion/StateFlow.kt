package com.wzvideni.pateo.music.expansion

import kotlinx.coroutines.flow.MutableStateFlow


fun <T> MutableStateFlow<List<T>>.add(item: T) {
    while (true) {
        val prevValue = value
        val nextValue = prevValue + item
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}

fun <T> MutableStateFlow<T>.set(newValue: T) {
    while (true) {
        val prevValue = value
        val nextValue = newValue
        if (compareAndSet(prevValue, nextValue)) {
            return
        }
    }
}