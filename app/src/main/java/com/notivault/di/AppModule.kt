package com.notivault.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.notivault.data.NotiVaultDatabase
import com.notivault.data.dao.*
import com.notivault.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFS_NAME)

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotiVaultDatabase {
        return Room.databaseBuilder(
            context,
            NotiVaultDatabase::class.java,
            Constants.DB_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNotificationDao(db: NotiVaultDatabase): NotificationDao = db.notificationDao()

    @Provides
    fun provideRuleDao(db: NotiVaultDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideDailyCountDao(db: NotiVaultDatabase): DailyCountDao = db.dailyCountDao()

    @Provides
    fun provideSuggestionDao(db: NotiVaultDatabase): SuggestionDao = db.suggestionDao()

    @Provides
    fun provideServiceHealthDao(db: NotiVaultDatabase): ServiceHealthDao = db.serviceHealthDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
