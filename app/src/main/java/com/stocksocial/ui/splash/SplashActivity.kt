package com.stocksocial.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.stocksocial.databinding.ActivitySplashBinding
import com.stocksocial.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.splashRoot.setOnClickListener { openMain() }
        binding.splashLogo.setOnClickListener { openMain() }

        binding.splashRoot.postDelayed({
            binding.splashProgress.visibility = View.GONE
            binding.splashHint.animate().alpha(1f).setDuration(400).start()
        }, LOADING_MS)
    }

    private fun openMain() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        private const val LOADING_MS = 700L
    }
}
