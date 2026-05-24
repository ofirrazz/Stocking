package com.stocksocial.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.stocksocial.R
import com.stocksocial.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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

        // Pick the start destination based on auth state instead of always starting on
        // welcomeFragment. This:
        //  - removes the brief welcome flash for already-signed-in users on cold start, and
        //  - prevents the NavController "Ignoring popBackStack to welcomeFragment"
        //    warnings emitted by BottomNavigationView.setupWithNavController(), which
        //    always tries to pop back to the graph's start destination on every tab switch.
        val isSignedIn = isFirebaseConfigured() && try {
            FirebaseAuth.getInstance().currentUser != null
        } catch (_: Exception) {
            false
        }
        val graph = controller.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            if (isSignedIn) R.id.feedFragment else R.id.welcomeFragment
        )
        controller.graph = graph

        binding.bottomNavigation.setupWithNavController(controller)

        controller.addOnDestinationChangedListener { _, destination, _ ->
            val showBottomNav = destination.id in setOf(
                R.id.feedFragment,
                R.id.portfolioFragment,
                R.id.hotStocksFragment,
                R.id.articlesFragment,
                R.id.profileFragment
            )
            binding.bottomNavigation.visibility = if (showBottomNav) View.VISIBLE else View.GONE
        }
    }

    private fun isFirebaseConfigured(): Boolean = FirebaseApp.getApps(this).isNotEmpty()
}
