package com.stocksocial.ui.main

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.stocksocial.R
import com.stocksocial.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check Firebase connection
        testFirebaseConnection()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

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

    private fun testFirebaseConnection() {
        val db = FirebaseFirestore.getInstance()
        val testData = hashMapOf(
            "status" to "connected",
            "time" to System.currentTimeMillis()
        )

        db.collection("connection_test")
            .add(testData)
            .addOnSuccessListener { 
                Log.d("FIREBASE_TEST", "Successfully connected to Firebase! ID: ${it.id}")
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_TEST", "Failed to connect to Firebase", e)
            }
    }
}
