package com.organizador.agenda.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.organizador.agenda.data.calendar.CalendarProviderRepository
import com.organizador.agenda.data.focus.RoomFocusRepository
import com.organizador.agenda.data.local.FocusDao
import com.organizador.agenda.data.local.PulsoDatabase
import com.organizador.agenda.data.local.TaskDao
import com.organizador.agenda.data.settings.DataStoreSettingsRepository
import com.organizador.agenda.data.task.RoomTaskRepository
import com.organizador.agenda.domain.repository.CalendarRepository
import com.organizador.agenda.domain.repository.FocusRepository
import com.organizador.agenda.domain.repository.SettingsRepository
import com.organizador.agenda.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PulsoDatabase =
        Room.databaseBuilder(context, PulsoDatabase::class.java, PulsoDatabase.NAME).build()

    @Provides fun provideTaskDao(db: PulsoDatabase): TaskDao = db.taskDao()
    @Provides fun provideFocusDao(db: PulsoDatabase): FocusDao = db.focusDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("pulso_settings") }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun bindTasks(impl: RoomTaskRepository): TaskRepository
    @Binds abstract fun bindFocus(impl: RoomFocusRepository): FocusRepository
    @Binds abstract fun bindCalendar(impl: CalendarProviderRepository): CalendarRepository
    @Binds abstract fun bindSettings(impl: DataStoreSettingsRepository): SettingsRepository
}
