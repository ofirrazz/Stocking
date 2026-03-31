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
        if (!isFirebaseConfigured()) {
            Log.w(TAG, "Firebase is not configured. Add app/google-services.json from the Firebase console.")
            return
        }
        configureFirestorePersistence()
        hookFirebase()
    }

    private fun isFirebaseConfigured(): Boolean = FirebaseApp.getApps(this).isNotEmpty()

    private fun configureFirestorePersistence() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false)
                .build()
            firestore.firestoreSettings = settings
        } catch (e: Exception) {
            Log.w(TAG, "Firestore settings could not be applied (already started?)", e)
        }
    }

    private fun hookFirebase() {
        if (!isFirebaseConfigured()) {
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
