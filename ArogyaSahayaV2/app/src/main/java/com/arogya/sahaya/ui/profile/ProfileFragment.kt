package com.arogya.sahaya.ui.profile

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.arogya.sahaya.data.model.MedicalProfile
import com.arogya.sahaya.databinding.FragmentProfileBinding
import com.arogya.sahaya.viewmodel.ProfileViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null
    private val binding get() = _b!!
    private val vm: ProfileViewModel by viewModels()
    private var editing = false

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?) =
        FragmentProfileBinding.inflate(i, c, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEditing(false)
        vm.profile.observe(viewLifecycleOwner) { it?.let { p -> populate(p) } }
        binding.btnEdit.setOnClickListener { if (!editing) setEditing(true) else save() }
        binding.btnCancel.setOnClickListener { setEditing(false); vm.profile.value?.let { populate(it) } }
    }

    private fun populate(p: MedicalProfile) {
        binding.etName.setText(p.name)
        binding.etAge.setText(if (p.age > 0) p.age.toString() else "")
        binding.etGender.setText(p.gender)
        binding.etBloodGroup.setText(p.bloodGroup)
        binding.etWeight.setText(if (p.weight > 0f) p.weight.toString() else "")
        binding.etHeight.setText(if (p.height > 0f) p.height.toString() else "")
        binding.etConditions.setText(p.chronicConditions)
        binding.etAllergies.setText(p.allergies)
        binding.etEmergencyName.setText(p.emergencyContactName)
        binding.etEmergencyPhone.setText(p.emergencyContactPhone)
        binding.etDoctorName.setText(p.doctorName)
        binding.etDoctorPhone.setText(p.doctorPhone)
        val initials = p.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
        binding.tvInitials.text = initials.ifBlank { "?" }
    }

    private fun save() {
        val nm = binding.etName.text.toString().trim()
        if (nm.isEmpty()) { Toast.makeText(context, "Name is required", Toast.LENGTH_SHORT).show(); return }
        vm.saveProfile(MedicalProfile(name = nm, age = binding.etAge.text.toString().toIntOrNull() ?: 0,
            gender = binding.etGender.text.toString().trim(), bloodGroup = binding.etBloodGroup.text.toString().trim(),
            weight = binding.etWeight.text.toString().toFloatOrNull() ?: 0f,
            height = binding.etHeight.text.toString().toFloatOrNull() ?: 0f,
            chronicConditions = binding.etConditions.text.toString().trim(),
            allergies = binding.etAllergies.text.toString().trim(),
            emergencyContactName = binding.etEmergencyName.text.toString().trim(),
            emergencyContactPhone = binding.etEmergencyPhone.text.toString().trim(),
            doctorName = binding.etDoctorName.text.toString().trim(),
            doctorPhone = binding.etDoctorPhone.text.toString().trim()))
        setEditing(false)
        Snackbar.make(binding.root, "✅ Profile saved", Snackbar.LENGTH_SHORT).show()
    }

    private fun setEditing(e: Boolean) {
        editing = e
        listOf(binding.etName, binding.etAge, binding.etGender, binding.etBloodGroup,
            binding.etWeight, binding.etHeight, binding.etConditions, binding.etAllergies,
            binding.etEmergencyName, binding.etEmergencyPhone,
            binding.etDoctorName, binding.etDoctorPhone).forEach { it.isEnabled = e }
        binding.btnEdit.text = if (e) "💾 Save Profile" else "✏️ Edit"
        binding.btnCancel.visibility = if (e) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
