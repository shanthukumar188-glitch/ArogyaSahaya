package com.arogya.sahaya.data.repository

import androidx.lifecycle.LiveData
import com.arogya.sahaya.data.db.*
import com.arogya.sahaya.data.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val dao: MedicalProfileDao) {
    val profile: LiveData<MedicalProfile?> = dao.getProfile()
    suspend fun getProfileOnce() = dao.getProfileOnce()
    suspend fun saveProfile(p: MedicalProfile) = dao.upsert(p)
}

@Singleton
class PillRepository @Inject constructor(
    private val pillDao: PillDao,
    private val logDao: PillLogDao
) {
    val activePills: LiveData<List<Pill>> = pillDao.getAllActivePills()
    val allPills: LiveData<List<Pill>> = pillDao.getAllPills()
    suspend fun getActivePillsOnce() = pillDao.getActivePillsOnce()
    suspend fun getPillById(id: Long) = pillDao.getPillById(id)
    suspend fun insertPill(pill: Pill) = pillDao.insertPill(pill)
    suspend fun updatePill(pill: Pill) = pillDao.updatePill(pill)
    suspend fun deletePill(pill: Pill) = pillDao.deletePill(pill)
    suspend fun deactivatePill(id: Long) = pillDao.deactivatePill(id)
    suspend fun decrementStock(id: Long) = pillDao.decrementStock(id)
    fun getLogsInRange(from: Long, to: Long) = logDao.getLogsInRange(from, to)
    fun getLogsForPill(pillId: Long) = logDao.getLogsForPill(pillId)
    suspend fun logIntake(log: PillLog) = logDao.insertLog(log)
    suspend fun getAdherencePercent(fromMillis: Long): Int {
        val taken = logDao.getTakenCountSince(fromMillis)
        val total = logDao.getTotalCountSince(fromMillis)
        return if (total == 0) 100 else ((taken * 100) / total)
    }
}

@Singleton
class VitalsRepository @Inject constructor(private val dao: VitalsDao) {
    val allVitals: LiveData<List<VitalEntry>> = dao.getAllVitals()
    val last7Days: LiveData<List<VitalEntry>> = dao.getLast7Vitals()
    fun getVitalsSince(from: Long) = dao.getVitalsSince(from)
    suspend fun getLatestVital() = dao.getLatestVital()
    suspend fun insertVital(entry: VitalEntry) = dao.insertVital(entry)
    suspend fun deleteVital(entry: VitalEntry) = dao.deleteVital(entry)
}

@Singleton
class HealthEventRepository @Inject constructor(private val dao: HealthEventDao) {
    val allEvents: LiveData<List<HealthEvent>> = dao.getAllEvents()
    fun getUpcomingEvents(from: Long = System.currentTimeMillis()) = dao.getUpcomingEvents(from)
    suspend fun insertEvent(event: HealthEvent) = dao.insertEvent(event)
    suspend fun updateEvent(event: HealthEvent) = dao.updateEvent(event)
    suspend fun deleteEvent(event: HealthEvent) = dao.deleteEvent(event)
    suspend fun getPendingCount() = dao.getPendingCount()
}

@Singleton
class VoiceRepository @Inject constructor(private val dao: VoiceHistoryDao) {
    val recentHistory: LiveData<List<VoiceHistory>> = dao.getRecentHistory()
    suspend fun saveHistory(h: VoiceHistory) { dao.insertHistory(h); dao.pruneOld() }
}

@Singleton
class HealthTipRepository @Inject constructor(private val dao: HealthTipDao) {
    val latestTip: LiveData<HealthTip?> = dao.getLatestTip()
    suspend fun saveTip(tip: HealthTip) { dao.insertTip(tip); dao.pruneOld() }
}
