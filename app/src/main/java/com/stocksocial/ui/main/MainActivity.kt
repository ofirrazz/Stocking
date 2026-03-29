package com.stocksocial.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.stocksocial.R
import com.stocksocial.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        if (FirebaseAuth.getInstance().currentUser != null) {
            val options = NavOptions.Builder()
                .setPopUpTo(R.id.loginFragment, true)
                .build()
            navController.navigate(R.id.feedFragment, null, options)
        }

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = destination.id in setOf(
                R.id.feedFragment,
                R.id.stocksFragment,
                R.id.articlesFragment,
                R.id.profileFragment
            )
            binding.bottomNavigation.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        }
    }
}
