package com.arogya.sahaya.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.arogya.sahaya.R
import com.arogya.sahaya.data.model.VoiceHistory
import com.arogya.sahaya.databinding.FragmentVoiceBinding
import com.arogya.sahaya.service.VoiceAssistantService
import com.arogya.sahaya.viewmodel.ProfileViewModel
import com.arogya.sahaya.viewmodel.VoiceViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class VoiceFragment : Fragment() {

    private var _binding: FragmentVoiceBinding? = null
    private val binding get() = _binding!!
    private val voiceVm: VoiceViewModel by viewModels()
    private val profileVm: ProfileViewModel by viewModels()
    private lateinit var historyAdapter: VoiceHistoryAdapter
    private lateinit var voiceService: VoiceAssistantService
    private var patientName = "Friend"
    private var conditions = ""

    private val micPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startListening() else showError("Microphone permission denied. Go to Settings to allow.") }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentVoiceBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupVoiceService()
        setupRecycler()
        setupObservers()
        setupQuickQuestions()

        profileVm.profile.observe(viewLifecycleOwner) { p ->
            patientName = p?.name?.split(" ")?.firstOrNull() ?: "Friend"
            conditions = p?.chronicConditions ?: ""
            binding.tvWelcome.text = "Hello $patientName! 👋\nAsk me any health question"
        }

        binding.btnMic.setOnClickListener { onMicClick() }
        binding.btnStop.setOnClickListener { stopAll() }
        binding.btnClearHistory.setOnClickListener {
            binding.recyclerHistory.visibility = View.GONE
            binding.tvHistoryLabel.visibility = View.GONE
        }
    }

    private fun setupVoiceService() {
        voiceService = VoiceAssistantService()
        voiceService.onListeningStarted = {
            requireActivity().runOnUiThread {
                voiceVm.setListening(true); voiceVm.setLoading(false)
                binding.tvStatus.text = "Listening... Speak now 🎙️"
                binding.btnMic.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.pulse))
            }
        }
        voiceService.onSpeechResult = { text ->
            requireActivity().runOnUiThread {
                voiceVm.setQuery(text); voiceVm.setListening(false); voiceVm.setLoading(true)
                binding.tvStatus.text = "Processing your question..."
                binding.btnMic.clearAnimation()
            }
        }
        voiceService.onAIResponse = { response ->
            requireActivity().runOnUiThread {
                voiceVm.setResponse(response); voiceVm.setLoading(false)
                binding.tvStatus.text = "Speaking response..."
            }
        }
        voiceService.onSpeakingStarted = { requireActivity().runOnUiThread { voiceVm.setSpeaking(true) } }
        voiceService.onSpeakingDone = {
            requireActivity().runOnUiThread {
                voiceVm.setSpeaking(false)
                binding.tvStatus.text = "Tap the mic to ask another question"
            }
        }
        voiceService.onError = { msg -> requireActivity().runOnUiThread { showError(msg) } }
    }

    private fun setupRecycler() {
        historyAdapter = VoiceHistoryAdapter()
        binding.recyclerHistory.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
            adapter = historyAdapter
        }
    }

    private fun setupObservers() {
        voiceVm.isListening.observe(viewLifecycleOwner) { listening ->
            binding.btnMic.setImageResource(if (listening) R.drawable.ic_mic_active else R.drawable.ic_mic)
            binding.micRipple.visibility = if (listening) View.VISIBLE else View.GONE
        }
        voiceVm.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressVoice.visibility = if (loading) View.VISIBLE else View.GONE
        }
        voiceVm.isSpeaking.observe(viewLifecycleOwner) { speaking ->
            binding.btnStop.visibility = if (speaking) View.VISIBLE else View.GONE
            binding.waveformView.visibility = if (speaking) View.VISIBLE else View.GONE
        }
        voiceVm.currentQuery.observe(viewLifecycleOwner) { q ->
            binding.tvCurrentQuery.text = if (q.isNotBlank()) "You: $q" else ""
            binding.tvCurrentQuery.visibility = if (q.isNotBlank()) View.VISIBLE else View.GONE
        }
        voiceVm.currentResponse.observe(viewLifecycleOwner) { r ->
            binding.cardResponse.visibility = if (r.isNotBlank()) View.VISIBLE else View.GONE
            binding.tvResponse.text = r
        }
        voiceVm.voiceHistory.observe(viewLifecycleOwner) { history ->
            if (history.isNotEmpty()) {
                historyAdapter.submitList(history)
                binding.recyclerHistory.visibility = View.VISIBLE
                binding.tvHistoryLabel.visibility = View.VISIBLE
                binding.recyclerHistory.smoothScrollToPosition(history.size - 1)
            }
        }
    }

    private fun setupQuickQuestions() {
        val questions = listOf(
            "Is my blood pressure normal?",
            "Tips to manage diabetes",
            "What foods should I avoid?",
            "How to sleep better?",
            "How much water to drink daily?",
            "What is normal blood sugar level?"
        )
        binding.chipGroup.removeAllViews()
        questions.forEach { q ->
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = q; isCheckable = false
                setChipBackgroundColorResource(R.color.chip_bg)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.primary))
                textSize = 13f
                setOnClickListener { askQuestion(q) }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun askQuestion(question: String) {
        voiceVm.setQuery(question); voiceVm.setLoading(true)
        voiceVm.setResponse(""); binding.cardResponse.visibility = View.GONE
        binding.tvStatus.text = "Processing..."
        // Use the service's internal fallback directly for chip questions
        voiceService.processQuery(question, patientName, conditions)
    }

    private fun onMicClick() {
        if (voiceVm.isListening.value == true) {
            voiceService.stopListening(); voiceVm.setListening(false)
            binding.btnMic.clearAnimation()
            binding.tvStatus.text = "Tap mic to ask a question"
        } else { checkMicAndListen() }
    }

    private fun checkMicAndListen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) startListening()
        else micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startListening() {
        voiceVm.setResponse(""); voiceVm.setQuery("")
        binding.cardResponse.visibility = View.GONE
        voiceService.startListening(patientName, conditions)
    }

    private fun stopAll() {
        voiceService.stopSpeaking(); voiceService.stopListening()
        voiceVm.setListening(false); voiceVm.setSpeaking(false)
        binding.btnMic.clearAnimation()
        binding.tvStatus.text = "Tap the mic to ask a question"
    }

    private fun showError(msg: String) {
        voiceVm.setListening(false); voiceVm.setLoading(false)
        binding.btnMic.clearAnimation()
        binding.tvStatus.text = "⚠️ $msg"
    }

    override fun onDestroyView() { stopAll(); super.onDestroyView(); _binding = null }
}

class VoiceHistoryAdapter : ListAdapter<VoiceHistory, VoiceHistoryAdapter.VH>(DiffCB) {
    companion object DiffCB : DiffUtil.ItemCallback<VoiceHistory>() {
        override fun areItemsTheSame(a: VoiceHistory, b: VoiceHistory) = a.id == b.id
        override fun areContentsTheSame(a: VoiceHistory, b: VoiceHistory) = a == b
    }
    inner class VH(val v: View) : RecyclerView.ViewHolder(v)
    override fun onCreateViewHolder(parent: ViewGroup, vt: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_voice_history, parent, false))
    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        h.v.findViewById<TextView>(R.id.tvQuery).text = "You: ${item.query}"
        h.v.findViewById<TextView>(R.id.tvResponse).text = "Arogya Mitra: ${item.response}"
        h.v.findViewById<TextView>(R.id.tvTime).text = sdf.format(Date(item.timestamp))
    }
}
