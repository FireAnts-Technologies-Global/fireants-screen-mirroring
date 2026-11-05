package com.anrstudio.template.domain.model

sealed interface DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>
    data class Error(val throwable: Throwable, val message: String) : DataResult<Nothing>
}
