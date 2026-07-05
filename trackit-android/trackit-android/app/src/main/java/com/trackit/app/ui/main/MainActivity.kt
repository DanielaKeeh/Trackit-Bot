package com.trackit.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.trackit.app.databinding.ActivityMainBinding
import com.trackit.app.ui.chat.ChatFragment
import com.trackit.app.ui.dashboard.DashboardFragment
import com.trackit.app.ui.objects.ObjectsFragment
import com.trackit.app.ui.reminders.RemindersFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            showFragment(DashboardFragment())
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                com.trackit.app.R.id.nav_dashboard -> DashboardFragment()
                com.trackit.app.R.id.nav_objects -> ObjectsFragment()
                com.trackit.app.R.id.nav_reminders -> RemindersFragment()
                com.trackit.app.R.id.nav_chat -> ChatFragment()
                else -> DashboardFragment()
            }
            showFragment(fragment)
            true
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(com.trackit.app.R.id.fragmentContainer, fragment)
            .commit()
    }
}
