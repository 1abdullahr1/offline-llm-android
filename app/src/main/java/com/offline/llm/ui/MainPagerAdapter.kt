package com.offline.llm.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.offline.llm.ui.chat.ChatFragment
import com.offline.llm.ui.diagnostics.DiagnosticsFragment
import com.offline.llm.ui.models.ModelsFragment

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ChatFragment()
            1 -> ModelsFragment()
            2 -> DiagnosticsFragment()
            else -> ChatFragment()
        }
    }
}
