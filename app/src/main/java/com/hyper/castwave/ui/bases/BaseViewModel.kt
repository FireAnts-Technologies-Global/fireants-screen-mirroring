package com.hyper.castwave.ui.bases

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren

open class BaseViewModel : ViewModel() {

    protected fun <T> Flow<T>.stateWhileSubscribed(initialValue: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)

    //init ViewModel Scope
    var viewModelJob = SupervisorJob()
    var uiDispatchers = Dispatchers.Main
    var ioDispatchers = Dispatchers.IO
    var defaultDispatchers = Dispatchers.Default
    var uiScope = CoroutineScope(uiDispatchers + viewModelJob)
    var bgScope = CoroutineScope(ioDispatchers + viewModelJob)

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
        uiScope.coroutineContext.cancelChildren()
        bgScope.coroutineContext.cancelChildren()
    }

    fun <T> MutableLiveData<T>.toLiveData(): LiveData<T> = this
}