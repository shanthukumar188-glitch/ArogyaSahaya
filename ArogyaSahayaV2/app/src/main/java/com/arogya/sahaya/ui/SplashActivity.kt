package com.arogya.sahaya.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arogya.sahaya.data.repository.ProfileRepository
import com.arogya.sahaya.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject lateinit var profileRepository: ProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            delay(1800)
            val profile = profileRepository.getProfileOnce()
            val dest = if (profile == null || profile.name.isBlank())
                OnboardingActivity::class.java else MainActivity::class.java
            startActivity(Intent(this@SplashActivity, dest))
            finish()
        }
    }
}
