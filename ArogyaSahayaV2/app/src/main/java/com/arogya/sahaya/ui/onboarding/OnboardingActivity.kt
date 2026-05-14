package com.arogya.sahaya.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.arogya.sahaya.R
import com.arogya.sahaya.data.model.MedicalProfile
import com.arogya.sahaya.ui.MainActivity
import com.arogya.sahaya.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OnboardingActivity : AppCompatActivity() {

    private val vm: ProfileViewModel by viewModels()
    private var name = ""; private var age = 0; private var gender = ""
    private var conditions = ""; private var emergencyName = ""; private var emergencyPhone = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val vp = findViewById<ViewPager2>(R.id.viewPager)
        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnSkip = findViewById<Button>(R.id.btnSkip)
        val dots = listOf<View>(
            findViewById(R.id.dot1), findViewById(R.id.dot2),
            findViewById(R.id.dot3), findViewById(R.id.dot4)
        )

        val pages = listOf(R.layout.page_onboarding_welcome, R.layout.page_onboarding_basic,
            R.layout.page_onboarding_conditions, R.layout.page_onboarding_emergency)

        vp.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class VH(v: View) : RecyclerView.ViewHolder(v)
            override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH =
                VH(LayoutInflater.from(parent.context).inflate(pages[vt], parent, false).also { it.tag = "page_$vt" })
            override fun getItemViewType(pos: Int) = pos
            override fun getItemCount() = pages.size
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {}
        }
        vp.isUserInputEnabled = false

        vp.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, d -> d.alpha = if (i == position) 1f else 0.3f }
                btnNext.text = if (position == pages.size - 1) "Get Started 🚀" else "Next →"
            }
        })

        btnNext.setOnClickListener {
            val cur = vp.currentItem
            if (collectPage(cur, vp)) {
                if (cur < pages.size - 1) vp.currentItem = cur + 1
                else { saveAndGo() }
            }
        }
        btnSkip.setOnClickListener {
            vm.saveProfile(MedicalProfile(name = "Patient")); goMain()
        }
    }

    private fun collectPage(page: Int, vp: ViewPager2): Boolean {
        val v = vp.findViewWithTag<View>("page_$page") ?: return true
        return when (page) {
            1 -> {
                name = v.findViewById<EditText>(R.id.etName)?.text?.toString()?.trim() ?: ""
                age = v.findViewById<EditText>(R.id.etAge)?.text?.toString()?.toIntOrNull() ?: 0
                if (name.isEmpty()) { Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show(); false }
                else true
            }
            2 -> { conditions = v.findViewById<EditText>(R.id.etConditions)?.text?.toString()?.trim() ?: ""; true }
            3 -> {
                emergencyName = v.findViewById<EditText>(R.id.etEmergencyName)?.text?.toString()?.trim() ?: ""
                emergencyPhone = v.findViewById<EditText>(R.id.etEmergencyPhone)?.text?.toString()?.trim() ?: ""
                true
            }
            else -> true
        }
    }

    private fun saveAndGo() {
        vm.saveProfile(MedicalProfile(name = name.ifBlank { "Patient" }, age = age,
            chronicConditions = conditions, emergencyContactName = emergencyName,
            emergencyContactPhone = emergencyPhone))
        goMain()
    }

    private fun goMain() { startActivity(Intent(this, MainActivity::class.java)); finish() }
}
