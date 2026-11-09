package com.organizador.agenda.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Datas e horas "de parede" (LocalDate/LocalTime/LocalDateTime) não dependem de fuso:
 * uma tarefa para "amanhã às 9h" continua às 9h se o fuso mudar. Instantes são epoch millis.
 */
class Converters {
    @TypeConverter fun dateToLong(v: LocalDate?): Long? = v?.toEpochDay()
    @TypeConverter fun longToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)

    @TypeConverter fun timeToInt(v: LocalTime?): Int? = v?.toSecondOfDay()
    @TypeConverter fun intToTime(v: Int?): LocalTime? = v?.let { LocalTime.ofSecondOfDay(it.toLong()) }

    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun dateTimeToString(v: LocalDateTime?): String? = v?.toString()
    @TypeConverter fun stringToDateTime(v: String?): LocalDateTime? = v?.let(LocalDateTime::parse)
}
