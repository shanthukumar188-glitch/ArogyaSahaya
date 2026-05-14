package com.arogya.sahaya.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.arogya.sahaya.data.model.*

@Dao
interface MedicalProfileDao {
    @Query("SELECT * FROM medical_profile WHERE id = 1")
    fun getProfile(): LiveData<MedicalProfile?>
    @Query("SELECT * FROM medical_profile WHERE id = 1")
    suspend fun getProfileOnce(): MedicalProfile?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: MedicalProfile)
}

@Dao
interface PillDao {
    @Query("SELECT * FROM pills WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActivePills(): LiveData<List<Pill>>
    @Query("SELECT * FROM pills ORDER BY name ASC")
    fun getAllPills(): LiveData<List<Pill>>
    @Query("SELECT * FROM pills WHERE id = :id")
    suspend fun getPillById(id: Long): Pill?
    @Query("SELECT * FROM pills WHERE isActive = 1")
    suspend fun getActivePillsOnce(): List<Pill>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPill(pill: Pill): Long
    @Update
    suspend fun updatePill(pill: Pill)
    @Delete
    suspend fun deletePill(pill: Pill)
    @Query("UPDATE pills SET isActive = 0 WHERE id = :id")
    suspend fun deactivatePill(id: Long)
    @Query("UPDATE pills SET stock = stock - 1 WHERE id = :id AND stock > 0")
    suspend fun decrementStock(id: Long)
}

@Dao
interface PillLogDao {
    @Query("SELECT * FROM pill_logs WHERE takenAt >= :from AND takenAt <= :to ORDER BY takenAt DESC")
    fun getLogsInRange(from: Long, to: Long): LiveData<List<PillLog>>
    @Query("SELECT * FROM pill_logs WHERE pillId = :pillId ORDER BY takenAt DESC LIMIT 30")
    fun getLogsForPill(pillId: Long): LiveData<List<PillLog>>
    @Query("SELECT COUNT(*) FROM pill_logs WHERE wasTaken = 1 AND takenAt >= :from")
    suspend fun getTakenCountSince(from: Long): Int
    @Query("SELECT COUNT(*) FROM pill_logs WHERE takenAt >= :from")
    suspend fun getTotalCountSince(from: Long): Int
    @Insert
    suspend fun insertLog(log: PillLog)
}

@Dao
interface VitalsDao {
    @Query("SELECT * FROM vitals ORDER BY recordedAt DESC")
    fun getAllVitals(): LiveData<List<VitalEntry>>
    @Query("SELECT * FROM vitals WHERE recordedAt >= :from ORDER BY recordedAt ASC")
    fun getVitalsSince(from: Long): LiveData<List<VitalEntry>>
    @Query("SELECT * FROM vitals ORDER BY recordedAt DESC LIMIT 7")
    fun getLast7Vitals(): LiveData<List<VitalEntry>>
    @Query("SELECT * FROM vitals ORDER BY recordedAt DESC LIMIT 1")
    suspend fun getLatestVital(): VitalEntry?
    @Insert
    suspend fun insertVital(entry: VitalEntry): Long
    @Update
    suspend fun updateVital(entry: VitalEntry)
    @Delete
    suspend fun deleteVital(entry: VitalEntry)
}

@Dao
interface HealthEventDao {
    @Query("SELECT * FROM health_events ORDER BY eventDate ASC")
    fun getAllEvents(): LiveData<List<HealthEvent>>
    @Query("SELECT * FROM health_events WHERE eventDate >= :from ORDER BY eventDate ASC")
    fun getUpcomingEvents(from: Long): LiveData<List<HealthEvent>>
    @Query("SELECT COUNT(*) FROM health_events WHERE isCompleted = 0")
    suspend fun getPendingCount(): Int
    @Insert
    suspend fun insertEvent(event: HealthEvent): Long
    @Update
    suspend fun updateEvent(event: HealthEvent)
    @Delete
    suspend fun deleteEvent(event: HealthEvent)
}

@Dao
interface VoiceHistoryDao {
    @Query("SELECT * FROM voice_history ORDER BY timestamp DESC LIMIT 50")
    fun getRecentHistory(): LiveData<List<VoiceHistory>>
    @Insert
    suspend fun insertHistory(history: VoiceHistory)
    @Query("DELETE FROM voice_history WHERE id NOT IN (SELECT id FROM voice_history ORDER BY timestamp DESC LIMIT 100)")
    suspend fun pruneOld()
}

@Dao
interface HealthTipDao {
    @Query("SELECT * FROM health_tips ORDER BY date DESC LIMIT 1")
    fun getLatestTip(): LiveData<HealthTip?>
    @Insert
    suspend fun insertTip(tip: HealthTip)
    @Query("DELETE FROM health_tips WHERE id NOT IN (SELECT id FROM health_tips ORDER BY date DESC LIMIT 10)")
    suspend fun pruneOld()
}

@Database(
    entities = [MedicalProfile::class, Pill::class, PillLog::class,
        VitalEntry::class, HealthEvent::class, VoiceHistory::class, HealthTip::class],
    version = 1,
    exportSchema = false
)
abstract class ArogyaDatabase : RoomDatabase() {
    abstract fun medicalProfileDao(): MedicalProfileDao
    abstract fun pillDao(): PillDao
    abstract fun pillLogDao(): PillLogDao
    abstract fun vitalsDao(): VitalsDao
    abstract fun healthEventDao(): HealthEventDao
    abstract fun voiceHistoryDao(): VoiceHistoryDao
    abstract fun healthTipDao(): HealthTipDao
}
