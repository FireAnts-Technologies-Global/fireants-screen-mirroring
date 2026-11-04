package com.anrstudio.template.data.pref

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(
    SingletonComponent::class
)
interface BaseDialogEntryPoint {
    fun appSharedPref(): AppSharedPref
}