package com.stocksocial.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.stocksocial.R
import com.stocksocial.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        // NavHostFragment inside FragmentContainerView is not attached yet during Activity.onCreate().
        connectNavigation()
    }

    private fun connectNavigation() {
        if (navController != null) return
        val navHost =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: return
        val controller = navHost.navController
        navController = controller

        val authDestinations = setOf(
            R.id.welcomeFragment,
            R.id.loginFragment,
            R.id.registerFragment
        )
        if (FirebaseAuth.getInstance().currentUser != null) {
            val dest = controller.currentDestination?.id
            if (dest != null && dest in authDestinations) {
                controller.navigate(R.id.action_global_feedFragment)
            }
        }

        binding.bottomNavigation.setupWithNavController(controller)

        controller.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = destination.id in setOf(
                R.id.feedFragment,
                R.id.portfolioFragment,
                R.id.notificationsFragment,
                R.id.profileFragment
            )
            binding.bottomNavigation.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        }
    }
}
