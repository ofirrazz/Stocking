@file:Suppress("DEPRECATION")
// Google Sign-In v4 is intentionally used here: the new Credential Manager API is overkill for
// this academic project and adds significant lifecycle complexity. The legacy client is still
// fully supported by Firebase Auth and Google Play Services.

package com.stocksocial.utils

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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

/** Maps Google Sign-In [ApiException] codes to actionable messages for the UI. */
fun googleSignInErrorMessage(context: Context, e: ApiException): String = when (e.statusCode) {
    10 -> context.getString(R.string.google_signin_developer_error)
    12501 -> context.getString(R.string.google_signin_cancelled)
    else -> context.getString(
        R.string.google_signin_failed,
        e.statusCode,
        e.message ?: context.getString(R.string.google_signin_no_token)
    )
}
