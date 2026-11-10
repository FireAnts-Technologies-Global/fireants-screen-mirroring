package com.hyper.castwave.data.iptv.model

import androidx.annotation.Keep

@Keep
sealed class DataState<out T> {
    object None : DataState<Nothing>()
    object Loading : DataState<Nothing>()
    data class Success<T>(val data: List<T>) : DataState<T>()
    data class Error(val message: String = "") : DataState<Nothing>()
}