package com.arogya.sahaya.ui.home

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.arogya.sahaya.R
import com.arogya.sahaya.databinding.FragmentHomeBinding
import com.arogya.sahaya.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val profileVm: ProfileViewModel by viewModels()
    private val pillVm: PillViewModel by viewModels()
    private val vitalsVm: VitalsViewModel by viewModels()
    private val eventVm: HealthEventViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentHomeBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Date
        binding.tvDate.text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

        // Greeting
        profileVm.profile.observe(viewLifecycleOwner) { profile ->
            val first = profile?.name?.split(" ")?.firstOrNull() ?: "Friend"
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val greet = when { hour < 12 -> "Good Morning"; hour < 17 -> "Good Afternoon"; else -> "Good Evening" }
            binding.tvGreeting.text = "$greet, $first 🙏"
            binding.tvSubtitle.text = when {
                profile?.chronicConditions?.isNotBlank() == true -> "Managing: ${profile.chronicConditions.split(",").firstOrNull()?.trim()}"
                else -> "Stay healthy, stay happy"
            }
        }

        // Pills
        pillVm.activePills.observe(viewLifecycleOwner) { pills ->
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val nextDose = when { hour < 8 -> "Morning dose at 8:00 AM"; hour < 13 -> "Afternoon dose at 1:00 PM"; hour < 21 -> "Night dose at 9:00 PM"; else -> "All doses done for today ✅" }
            binding.tvPillSummary.text = "${pills.size} medicine(s)  •  $nextDose"
        }
        pillVm.loadAdherence()
        pillVm.adherence.observe(viewLifecycleOwner) { pct ->
            binding.progressAdherence.progress = pct
            binding.tvAdherenceValue.text = "$pct%"
            binding.tvAdherenceLabel.text = when { pct >= 90 -> "Excellent 🌟"; pct >= 70 -> "Good 👍"; else -> "Needs Improvement ⚠️" }
        }

        // Latest vitals
        vitalsVm.latestVital.observe(viewLifecycleOwner) { v ->
            if (v != null) {
                binding.tvBpValue.text = "${v.systolic}/${v.diastolic}"
                binding.tvHrValue.text = "${v.heartRate}"
                binding.tvGlucoseValue.text = "${v.glucoseLevel.toInt()}"
                val (bpStatus, bpColor) = vitalsVm.getBpStatus(v.systolic, v.diastolic)
                binding.tvBpStatus.text = bpStatus
                binding.tvBpStatus.setTextColor(bpColor)
            } else {
                binding.tvBpValue.text = "--/--"
                binding.tvHrValue.text = "--"
                binding.tvGlucoseValue.text = "--"
                binding.tvBpStatus.text = "Tap + to record"
            }
        }

        // Upcoming event
        eventVm.upcomingEvents.observe(viewLifecycleOwner) { events ->
            val next = events.firstOrNull()
            if (next != null) {
                val sdf = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                binding.tvNextEvent.text = next.title
                binding.tvNextEventDate.text = "${sdf.format(Date(next.eventDate))}  •  ${next.eventTime}"
                binding.cardNextEvent.visibility = View.VISIBLE
            } else {
                binding.cardNextEvent.visibility = View.GONE
            }
        }

        // Nav
        binding.cardPills.setOnClickListener { findNavController().navigate(R.id.pillFragment) }
        binding.cardVitals.setOnClickListener { findNavController().navigate(R.id.vitalsFragment) }
        binding.cardAsha.setOnClickListener { findNavController().navigate(R.id.ashaFragment) }
        binding.cardProfile.setOnClickListener { findNavController().navigate(R.id.profileFragment) }
        binding.btnVoiceAssistant.setOnClickListener { findNavController().navigate(R.id.voiceFragment) }
        binding.btnSOS.setOnClickListener { findNavController().navigate(R.id.emergencyFragment) }
        binding.cardNextEvent.setOnClickListener { findNavController().navigate(R.id.ashaFragment) }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
