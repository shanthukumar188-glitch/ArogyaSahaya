package com.arogya.sahaya.ui.emergency

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.arogya.sahaya.R
import com.arogya.sahaya.databinding.FragmentEmergencyBinding
import com.arogya.sahaya.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyFragment : Fragment() {
    private var _b: FragmentEmergencyBinding? = null
    private val binding get() = _b!!
    private val profileVm: ProfileViewModel by viewModels()
    private var emergencyPhone = "112"
    private var countdownTimer: CountDownTimer? = null

    private val callPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { if (it) placeCall() }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentEmergencyBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileVm.profile.observe(viewLifecycleOwner) { p ->
            p?.let {
                if (it.emergencyContactPhone.isNotBlank()) {
                    emergencyPhone = it.emergencyContactPhone
                }
                val eName = it.emergencyContactName.ifBlank { "Emergency Contact" }
                binding.tvEmergencyContact.text = "📞 $eName\n${it.emergencyContactPhone.ifBlank { "Not set — go to Profile" }}"
                binding.tvDoctorContact.text = if (it.doctorPhone.isNotBlank())
                    "🩺 ${it.doctorName}\n${it.doctorPhone}"
                else "Doctor not set — go to Profile to add"
            }
        }

        binding.btnSOS.setOnLongClickListener { startCountdown(); true }
        binding.btnSOS.setOnClickListener {
            val pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse)
            binding.btnSOS.startAnimation(pulse)
            Toast.makeText(context, "Hold the SOS button for 3 seconds to call", Toast.LENGTH_SHORT).show()
        }
        binding.btnCancelSOS.setOnClickListener { cancelCountdown() }
        binding.btnCall112.setOnClickListener { emergencyPhone = "112"; requestCallPermission() }
        binding.btnCallContact.setOnClickListener { requestCallPermission() }
        binding.btnSendSMS.setOnClickListener { sendSOS() }

        binding.tvTip1.text = "🔴 Stay calm and breathe slowly"
        binding.tvTip2.text = "🔴 Lie down if you feel dizzy or faint"
        binding.tvTip3.text = "🔴 Do NOT take extra medicine without doctor advice"
        binding.tvTip4.text = "🔴 Keep your profile card visible for paramedics"
    }

    private fun startCountdown() {
        binding.btnCancelSOS.visibility = View.VISIBLE
        binding.tvCountdown.visibility = View.VISIBLE
        binding.btnSOS.isEnabled = false
        countdownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(ms: Long) { binding.tvCountdown.text = "Calling in ${ms / 1000 + 1}..." }
            override fun onFinish() { binding.tvCountdown.text = "📞 Calling..."; requestCallPermission(); cancelCountdown() }
        }.start()
    }

    private fun cancelCountdown() {
        countdownTimer?.cancel(); countdownTimer = null
        binding.btnCancelSOS.visibility = View.GONE
        binding.tvCountdown.visibility = View.GONE
        binding.btnSOS.isEnabled = true
        binding.btnSOS.clearAnimation()
    }

    private fun requestCallPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE)
            == PackageManager.PERMISSION_GRANTED) placeCall()
        else callPermLauncher.launch(Manifest.permission.CALL_PHONE)
    }

    private fun placeCall() {
        startActivity(Intent(Intent.ACTION_CALL, Uri.parse("tel:$emergencyPhone")))
    }

    private fun sendSOS() {
        val p = profileVm.profile.value
        val msg = "🆘 EMERGENCY! I need immediate help.\nName: ${p?.name ?: "Patient"}\n" +
                  "Conditions: ${p?.chronicConditions ?: "Unknown"}\nPlease come immediately."
        startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$emergencyPhone"))
            .apply { putExtra("sms_body", msg) })
    }

    override fun onDestroyView() { countdownTimer?.cancel(); super.onDestroyView(); _b = null }
}
