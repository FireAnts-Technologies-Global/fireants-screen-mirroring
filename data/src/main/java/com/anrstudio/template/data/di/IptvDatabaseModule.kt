package com.anrstudio.template.data.di

import android.content.Context
import androidx.room.Room
import com.anrstudio.template.data.iptv.database.TVChannelDatabase
import com.anrstudio.template.data.iptv.database.TVChannelDatabase.Companion.DATABASE_TV_CHANNEL_NAME
import com.anrstudio.template.data.iptv.database.dao.ChannelDao
import com.anrstudio.template.data.iptv.database.repository.ChannelDaoRepository
import com.anrstudio.template.data.iptv.database.repository.ChannelDaoRepositoryInterface
import com.anrstudio.template.data.iptv.pref.AppSharedPref
import com.anrstudio.template.data.iptv.pref.AppSharedPreferencesApp
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object IptvDatabaseModule {

    @Provides
    @Singleton
    fun provideAppSharedPref(@ApplicationContext context: Context): AppSharedPref =
        AppSharedPreferencesApp(context)

    @Provides
    @Singleton
    fun provideTVChannelDatabase(@ApplicationContext context: Context): TVChannelDatabase =
        Room.databaseBuilder(context, TVChannelDatabase::class.java, DATABASE_TV_CHANNEL_NAME)
            .addMigrations(TVChannelDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideChannelDao(database: TVChannelDatabase): ChannelDao = database.channelDao()

    @Provides
    @Singleton
    fun provideChannelDaoRepositoryInterface(
        channelDaoRepository: ChannelDaoRepository,
    ): ChannelDaoRepositoryInterface = channelDaoRepository
}
