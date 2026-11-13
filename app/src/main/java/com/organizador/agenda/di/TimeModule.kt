package com.organizador.agenda.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Singleton

/**
 * Relógio injetável: o código de produção usa o do sistema; os testes passam um `Clock.fixed`.
 * O fuso é lido a cada uso ([SystemZoneClock]): um `Clock.systemDefaultZone()` guardado como
 * singleton ficaria com o fuso de quando o processo abriu, e os lembretes errariam após uma viagem.
 */
@Module
@InstallIn(SingletonComponent::class)
object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = SystemZoneClock
}

object SystemZoneClock : Clock() {
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = system(zone)
    override fun instant(): Instant = Instant.now()
    override fun millis(): Long = System.currentTimeMillis()
}
