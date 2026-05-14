package com.arogya.sahaya.viewmodel

import androidx.lifecycle.*
import com.arogya.sahaya.data.model.*
import com.arogya.sahaya.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repo: ProfileRepository) : ViewModel() {
    val profile: LiveData<MedicalProfile?> = repo.profile
    fun saveProfile(p: MedicalProfile) = viewModelScope.launch { repo.saveProfile(p) }
}

@HiltViewModel
class PillViewModel @Inject constructor(private val repo: PillRepository) : ViewModel() {
    val activePills: LiveData<List<Pill>> = repo.activePills
    private val _toast = MutableLiveData<String>()
    val toast: LiveData<String> = _toast

    fun addPill(pill: Pill) = viewModelScope.launch {
        repo.insertPill(pill); _toast.value = "✅ ${pill.name} added"
    }
    fun updatePill(pill: Pill) = viewModelScope.launch { repo.updatePill(pill) }
    fun deletePill(pill: Pill) = viewModelScope.launch { repo.deletePill(pill) }

    fun markTaken(pillId: Long, doseTime: String) = viewModelScope.launch {
        repo.logIntake(PillLog(pillId = pillId, doseTime = doseTime, wasTaken = true))
        repo.decrementStock(pillId)
        _toast.value = "💊 Marked as taken"
    }
    fun markSkipped(pillId: Long, doseTime: String) = viewModelScope.launch {
        repo.logIntake(PillLog(pillId = pillId, doseTime = doseTime, wasTaken = false))
    }
    fun getLogsForPill(id: Long) = repo.getLogsForPill(id)

    private val _adherence = MutableLiveData<Int>()
    val adherence: LiveData<Int> = _adherence

    fun loadAdherence() = viewModelScope.launch {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        _adherence.value = repo.getAdherencePercent(weekAgo)
    }
}

@HiltViewModel
class VitalsViewModel @Inject constructor(private val repo: VitalsRepository) : ViewModel() {
    val allVitals: LiveData<List<VitalEntry>> = repo.allVitals
    val last7Days: LiveData<List<VitalEntry>> = repo.last7Days
    private val _latest = MutableLiveData<VitalEntry?>()
    val latestVital: LiveData<VitalEntry?> = _latest

    init { loadLatest() }

    private fun loadLatest() = viewModelScope.launch { _latest.value = repo.getLatestVital() }

    fun addVital(entry: VitalEntry) = viewModelScope.launch {
        repo.insertVital(entry); loadLatest()
    }
    fun deleteVital(entry: VitalEntry) = viewModelScope.launch { repo.deleteVital(entry) }

    fun getBpStatus(sys: Int, dia: Int): Pair<String, Int> = when {
        sys == 0 -> Pair("Not recorded", 0xFF9E9E9E.toInt())
        sys < 90 || dia < 60 -> Pair("Low BP ⚠️", 0xFF1565C0.toInt())
        sys <= 120 && dia <= 80 -> Pair("Normal ✅", 0xFF2E7D32.toInt())
        sys <= 129 && dia < 80 -> Pair("Elevated ⚠️", 0xFFF57F17.toInt())
        sys <= 139 || dia <= 89 -> Pair("High BP Stage 1 ⚠️", 0xFFE65100.toInt())
        else -> Pair("High BP Stage 2 🚨", 0xFFC62828.toInt())
    }

    fun getGlucoseStatus(glucose: Float): Pair<String, Int> = when {
        glucose == 0f -> Pair("Not recorded", 0xFF9E9E9E.toInt())
        glucose < 70 -> Pair("Low Sugar 🚨", 0xFF1565C0.toInt())
        glucose <= 100 -> Pair("Normal ✅", 0xFF2E7D32.toInt())
        glucose <= 125 -> Pair("Pre-diabetic ⚠️", 0xFFF57F17.toInt())
        else -> Pair("High Sugar 🚨", 0xFFC62828.toInt())
    }
}

@HiltViewModel
class HealthEventViewModel @Inject constructor(private val repo: HealthEventRepository) : ViewModel() {
    val upcomingEvents = repo.getUpcomingEvents()
    fun addEvent(event: HealthEvent) = viewModelScope.launch { repo.insertEvent(event) }
    fun updateEvent(event: HealthEvent) = viewModelScope.launch { repo.updateEvent(event) }
    fun deleteEvent(event: HealthEvent) = viewModelScope.launch { repo.deleteEvent(event) }
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceRepo: VoiceRepository,
    private val tipRepo: HealthTipRepository
) : ViewModel() {
    val voiceHistory = voiceRepo.recentHistory
    val latestTip = tipRepo.latestTip

    private val _isListening = MutableLiveData(false)
    val isListening: LiveData<Boolean> = _isListening

    private val _isSpeaking = MutableLiveData(false)
    val isSpeaking: LiveData<Boolean> = _isSpeaking

    private val _currentQuery = MutableLiveData("")
    val currentQuery: LiveData<String> = _currentQuery

    private val _currentResponse = MutableLiveData("")
    val currentResponse: LiveData<String> = _currentResponse

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun setListening(v: Boolean) { _isListening.value = v }
    fun setSpeaking(v: Boolean) { _isSpeaking.value = v }
    fun setQuery(v: String) { _currentQuery.value = v }
    fun setResponse(v: String) { _currentResponse.value = v }
    fun setLoading(v: Boolean) { _isLoading.value = v }

    fun saveTip(tip: HealthTip) = viewModelScope.launch { tipRepo.saveTip(tip) }
}
