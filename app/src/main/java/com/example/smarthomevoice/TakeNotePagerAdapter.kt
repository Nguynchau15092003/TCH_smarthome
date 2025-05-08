package com.example.smarthomevoice

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TakeNotePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> CreateEventFragment()
            1 -> CreateTaskFragment()
            else -> throw IllegalArgumentException("Vị trí không hợp lệ: $position")
        }
    }
}
