package com.example.calendar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionEventDao {
    @Query("SELECT * FROM subscription_events WHERE subscriptionId = :subscriptionId ORDER BY date ASC")
    fun getEventsBySubscription(subscriptionId: Long): Flow<List<SubscriptionEvent>>

    @Query(
        """
        SELECT * FROM subscription_events 
        WHERE date >= :startDate AND date < :endDate AND subscriptionId = :subscriptionId
        ORDER BY date ASC
        """
    )
    fun getEventsBetween(startDate: Long, endDate: Long, subscriptionId: Long): Flow<List<SubscriptionEvent>>

    @Query(
        """
        SELECT * FROM subscription_events 
        WHERE date >= :startOfDay AND date < :endOfDay AND subscriptionId = :subscriptionId
        ORDER BY date ASC
        """
    )
    fun getEventsByDate(startOfDay: Long, endOfDay: Long, subscriptionId: Long): Flow<List<SubscriptionEvent>>
    
    @Query("SELECT * FROM subscription_events WHERE date = :date")
    fun getEventsByExactDate(date: Long): Flow<List<SubscriptionEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SubscriptionEvent): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<SubscriptionEvent>): List<Long>

    @Query("DELETE FROM subscription_events WHERE subscriptionId = :subscriptionId")
    suspend fun deleteBySubscriptionId(subscriptionId: Long)

    @Query(
        """
        DELETE FROM subscription_events 
        WHERE subscriptionId = :subscriptionId 
        AND date >= :startDate 
        AND date < :endDate
        """
    )
    suspend fun deleteBySubscriptionIdAndDateRange(
        subscriptionId: Long,
        startDate: Long,
        endDate: Long
    )

    @Query("DELETE FROM subscription_events WHERE date < :beforeDate")
    suspend fun deleteEventsBefore(beforeDate: Long)
}

