package com.organizador.agenda.di

import com.organizador.agenda.domain.time.ClockNowProvider
import com.organizador.agenda.domain.time.NowProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Escopo que vive enquanto o processo viver: para ações que não podem morrer com a tela (ex.: "Desfazer"). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppScopeModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NowModule {
    @Binds abstract fun bindNow(impl: ClockNowProvider): NowProvider
}
