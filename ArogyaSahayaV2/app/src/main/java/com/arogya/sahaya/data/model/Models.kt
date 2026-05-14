package com.arogya.sahaya.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medical_profile")
data class MedicalProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 0,
    val gender: String = "",
    val bloodGroup: String = "",
    val weight: Float = 0f,
    val height: Float = 0f,
    val chronicConditions: String = "",
    val allergies: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val doctorName: String = "",
    val doctorPhone: String = "",
    val preferredLanguage: String = "English",
    val avatarColor: Int = 0
)

@Entity(tableName = "pills")
data class Pill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosage: String,
    val instructions: String = "",
    val pillType: String = "TABLET",
    val doseTimesMorning: Boolean = false,
    val doseTimesAfternoon: Boolean = false,
    val doseTimesNight: Boolean = false,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val reminderHourMorning: Int = 8,
    val reminderMinMorning: Int = 0,
    val reminderHourAfternoon: Int = 13,
    val reminderMinAfternoon: Int = 0,
    val reminderHourNight: Int = 21,
    val reminderMinNight: Int = 0,
    val colorIndex: Int = 0,
    val stock: Int = 30,
    val refillAlert: Int = 5
)

@Entity(tableName = "pill_logs")
data class PillLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pillId: Long,
    val takenAt: Long = System.currentTimeMillis(),
    val doseTime: String,
    val wasTaken: Boolean = true,
    val note: String = ""
)

@Entity(tableName = "vitals")
data class VitalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recordedAt: Long = System.currentTimeMillis(),
    val systolic: Int = 0,
    val diastolic: Int = 0,
    val heartRate: Int = 0,
    val glucoseLevel: Float = 0f,
    val oxygenLevel: Float = 0f,
    val temperature: Float = 0f,
    val weight: Float = 0f,
    val notes: String = "",
    val mood: String = "NORMAL"
)

@Entity(tableName = "health_events")
data class HealthEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val location: String = "",
    val eventDate: Long,
    val eventTime: String = "",
    val organizer: String = "",
    val eventType: String = "HEALTH_CAMP",
    val isCompleted: Boolean = false
)

@Entity(tableName = "voice_history")
data class VoiceHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis(),
    val category: String = "GENERAL"
)

@Entity(tableName = "health_tips")
data class HealthTip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tip: String,
    val category: String,
    val date: Long = System.currentTimeMillis()
)
