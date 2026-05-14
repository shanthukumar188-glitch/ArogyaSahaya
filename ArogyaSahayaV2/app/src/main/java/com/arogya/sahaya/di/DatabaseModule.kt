package com.arogya.sahaya.di

import android.content.Context
import androidx.room.Room
import com.arogya.sahaya.data.db.ArogyaDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): ArogyaDatabase =
        Room.databaseBuilder(ctx, ArogyaDatabase::class.java, "arogya_v2.db")
            .fallbackToDestructiveMigration().build()

    @Provides fun provideProfileDao(db: ArogyaDatabase) = db.medicalProfileDao()
    @Provides fun providePillDao(db: ArogyaDatabase) = db.pillDao()
    @Provides fun providePillLogDao(db: ArogyaDatabase) = db.pillLogDao()
    @Provides fun provideVitalsDao(db: ArogyaDatabase) = db.vitalsDao()
    @Provides fun provideHealthEventDao(db: ArogyaDatabase) = db.healthEventDao()
    @Provides fun provideVoiceHistoryDao(db: ArogyaDatabase) = db.voiceHistoryDao()
    @Provides fun provideHealthTipDao(db: ArogyaDatabase) = db.healthTipDao()
}
