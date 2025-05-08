package com.example.smarthomevoice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.smarthomevoice.databinding.ActivityOnboardingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var auth: FirebaseAuth

    private val onboardingItems = listOf(
        OnboardingItem(R.drawable.onboarding1, "Control All Devices", "Manage your smart devices effortlessly."),
        OnboardingItem(R.drawable.onboarding2, "Smart Automation", "Automate your home with advanced AI features."),
        OnboardingItem(R.drawable.onboarding3, "Easy Operation", "Control your devices with a single tap.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Check if user is already signed in
        if (auth.currentUser != null) {
            // User is signed in, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // Setup ViewPager2
        binding.viewPagerOnboarding.adapter = OnboardingAdapter(onboardingItems)
        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                addDotsIndicator(position)
                // Show "Get Started" button on last page
                if (position == onboardingItems.lastIndex) {
                    binding.btnGetStarted.visibility = View.VISIBLE
                } else {
                    binding.btnGetStarted.visibility = View.GONE
                }
            }
        })

        // "Get Started" button navigates to LoginActivity
        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Ensure we don't go back to OnboardingActivity
        }
    }

    private fun addDotsIndicator(currentPosition: Int) {
        binding.layoutDots.removeAllViews()
        val dots = arrayOfNulls<ImageView>(onboardingItems.size)
        for (i in onboardingItems.indices) {
            dots[i] = ImageView(this)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.setMargins(8, 0, 8, 0)
            dots[i]?.layoutParams = params
            dots[i]?.setImageDrawable(
                ContextCompat.getDrawable(
                    this,
                    if (i == currentPosition) R.drawable.dot_active else R.drawable.dot_inactive
                )
            )
            binding.layoutDots.addView(dots[i])
        }
    }
}