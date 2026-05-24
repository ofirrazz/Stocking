package com.stocksocial

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.stocksocial.utils.AppContainer

class StockSocialApp : Application() {

    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.w(TAG, "Firebase is not configured. Add app/google-services.json from the Firebase console.")
            return
        }
        configureFirestore()
        Log.d(TAG, "Firebase initialized (projectId=${FirebaseApp.getInstance().options.projectId})")
    }

    private fun configureFirestore() {
        try {
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
            FirebaseFirestore.getInstance().firestoreSettings = settings
        } catch (e: Exception) {
            Log.w(TAG, "Firestore settings could not be applied (already started?)", e)
        }
    }

    companion object {
        private const val TAG = "StockSocialApp"
    }
}
