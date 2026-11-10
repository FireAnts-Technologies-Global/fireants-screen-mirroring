package com.hyper.castwave.di

import android.content.Context
import com.hyper.castwave.data.pref.AppSharedPref
import com.hyper.castwave.data.pref.AppSharedPreferencesApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Singleton
    @Provides
    fun provideSharedPref(
        @ApplicationContext context: Context
    ): AppSharedPref = AppSharedPreferencesApp(context)

}