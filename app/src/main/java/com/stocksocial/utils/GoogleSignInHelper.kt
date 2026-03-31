package com.stocksocial.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.stocksocial.R

fun buildGoogleSignInClient(context: Context): GoogleSignInClient? {
    val webClientId = context.getString(R.string.default_web_client_id).trim()
    if (webClientId.isEmpty()) return null
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(webClientId)
        .requestEmail()
        .build()
    return GoogleSignIn.getClient(context, gso)
}
