package com.anrstudio.template.di

import com.anrstudio.template.navigation.AppNavigator
import com.anrstudio.template.navigation.AppNavigatorImpl
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
