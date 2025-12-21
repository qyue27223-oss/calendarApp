package com.example.calendar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY dtStart ASC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    fun getEventById(id: Long): Flow<Event?>

    @Query(
        """
        SELECT * FROM events 
        WHERE dtStart >= :startTime AND dtStart < :endTime
        ORDER BY dtStart ASC
        """
    )
    fun getEventsBetween(startTime: Long, endTime: Long): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE uid = :uid LIMIT 1")
    suspend fun getEventByUid(uid: String): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>): List<Long>

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)
}


