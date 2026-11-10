package com.hyper.castwave.di

import com.hyper.castwave.navigation.AppNavigator
import com.hyper.castwave.navigation.AppNavigatorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppNavigatorModule {

    @Binds
    abstract fun bindAppNavigator(impl: AppNavigatorImpl): AppNavigator
}
