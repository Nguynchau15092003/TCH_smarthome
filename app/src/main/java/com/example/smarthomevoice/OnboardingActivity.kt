package com.example.smarthomevoice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.smarthomevoice.R
import com.example.smarthomevoice.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val onboardingItems = listOf(
        OnboardingItem(R.drawable.onboarding1, "Control\nall devices", "Easily access and manage the smart devices in your home."),
        OnboardingItem(R.drawable.onboarding2, "Integrated\nhigh technology", "With AI support and data analysis capabilities, you can easily set up automation."),
        OnboardingItem(R.drawable.onboarding3, "Easy one-touch \noperation", "Interact with smart devices with just a single tap.")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup ViewPager2
        binding.viewPagerOnboarding.adapter = OnboardingAdapter(onboardingItems)
        binding.viewPagerOnboarding.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                addDotsIndicator(position)
                if (position == onboardingItems.lastIndex) {
                    binding.btnGetStarted.visibility = View.VISIBLE
                    binding.btnGetStarted.text = "Continue with Email"
                } else {
                    binding.btnGetStarted.visibility = View.GONE
                }
            }
        })

        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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