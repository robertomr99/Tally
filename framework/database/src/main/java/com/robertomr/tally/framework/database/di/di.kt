package com.robertomr.tally.framework.database.di

import android.content.Context
import androidx.room.Room
import com.robertomr.tally.domain.expenses.data.PhotoFileStore
import com.robertomr.tally.domain.expenses.data.PhotoProcessor
import com.robertomr.tally.domain.expenses.data.TicketDataSource
import com.robertomr.tally.framework.database.TallyDatabase
import com.robertomr.tally.framework.database.dao.TicketDao
import com.robertomr.tally.framework.database.source.CapturedPhotoProcessor
import com.robertomr.tally.framework.database.source.PhotoFileStoreImpl
import com.robertomr.tally.framework.database.source.TicketLocalDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideTallyDatabase(@ApplicationContext context: Context): TallyDatabase =
        Room.databaseBuilder(context, TallyDatabase::class.java, "tally.db")
            .build()

    @Provides
    fun provideTicketDao(database: TallyDatabase): TicketDao = database.ticketDao()

    @Provides
    fun provideTicketDataSource(ticketDao: TicketDao): TicketDataSource =
        TicketLocalDataSource(ticketDao)

    @Provides
    fun providePhotoFileStore(@ApplicationContext context: Context): PhotoFileStore =
        PhotoFileStoreImpl(context)

    @Provides
    fun providePhotoProcessor(): PhotoProcessor = CapturedPhotoProcessor()
}
