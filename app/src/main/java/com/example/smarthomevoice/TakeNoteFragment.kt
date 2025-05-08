package com.example.smarthomevoice

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.smarthomevoice.databinding.FragmentTakeNoteBinding
import com.google.android.material.tabs.TabLayoutMediator

class TakeNoteFragment : Fragment() {

    private var _binding: FragmentTakeNoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTakeNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Gán adapter cho ViewPager2
        val adapter = TakeNotePagerAdapter(this)
        binding.viewPager.adapter = adapter

        // Đồng bộ TabLayout với ViewPager2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Event"
                1 -> "Task"
                else -> ""
            }
        }.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
