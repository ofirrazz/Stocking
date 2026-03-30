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
        configureFirestorePersistence()
        hookFirebase()
    }

    private fun configureFirestorePersistence() {
        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
        firestore.firestoreSettings = settings
    }

    private fun hookFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            Log.e(TAG, "Firebase is not configured. Add app/google-services.json from the Firebase console.")
            return
        }
        val projectId = FirebaseApp.getInstance().options.projectId
        Log.d(TAG, "Firebase initialized (projectId=$projectId)")

        FirebaseFirestore.getInstance()
            .collection("connection_test")
            .document("app")
            .set(
                mapOf(
                    "lastLaunchAt" to System.currentTimeMillis(),
                    "debug" to BuildConfig.DEBUG
                )
            )
            .addOnSuccessListener { Log.d(TAG, "Firestore write OK — backend reachable") }
            .addOnFailureListener { e ->
                Log.w(TAG, "Firestore write failed (network or security rules)", e)
            }
    }

    companion object {
        private const val TAG = "StockSocialApp"
    }
}
