package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.stocksocial.data.local.ArticleDao
import com.stocksocial.data.local.PostDao
import com.stocksocial.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val auth: FirebaseAuth?,
    private val firestore: FirebaseFirestore?,
    private val postDao: PostDao? = null,
    private val articleDao: ArticleDao? = null
) {

    suspend fun login(emailOrUsername: String, password: String): RepositoryResult<User> =
        withContext(Dispatchers.IO) {
            val firebaseAuth = auth
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            try {
                val emailToUse = resolveEmail(emailOrUsername)
                val result = firebaseAuth.signInWithEmailAndPassword(emailToUse, password).await()
                val u = result.user ?: return@withContext RepositoryResult.Error("Sign-in failed")
                RepositoryResult.Success(mapFirebaseUser(u))
            } catch (e: FirebaseAuthInvalidUserException) {
                RepositoryResult.Error("User not found. Please register first.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                RepositoryResult.Error("Incorrect email/username or password.")
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Login failed", e)
            }
        }

    suspend fun register(username: String, email: String, password: String): RepositoryResult<User> =
        withContext(Dispatchers.IO) {
            val firebaseAuth = auth
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            try {
                val firebaseFirestore = firestore
                    ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
                val trimmedName = username.trim()
                when (val availability = UserFirestoreHelper.checkUsernameAvailability(firebaseFirestore, trimmedName)) {
                    UserFirestoreHelper.UsernameAvailability.Taken ->
                        return@withContext RepositoryResult.Error("This username is already taken.")
                    is UserFirestoreHelper.UsernameAvailability.Unknown ->
                        return@withContext RepositoryResult.Error(
                            "Could not verify username availability. Check your connection and try again.",
                            availability.cause
                        )
                    UserFirestoreHelper.UsernameAvailability.Available -> Unit
                }
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val u = result.user ?: return@withContext RepositoryResult.Error("Registration failed")
                u.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(trimmedName).build()
                ).await()
                UserFirestoreHelper.ensureUserProfileDocument(
                    firestore = firebaseFirestore,
                    user = u,
                    preferredUsername = trimmedName
                )
                RepositoryResult.Success(
                    User(
                        id = u.uid,
                        username = trimmedName,
                        email = email,
                        avatarUrl = null,
                        bio = null,
                        displayName = trimmedName
                    )
                )
            } catch (e: FirebaseAuthWeakPasswordException) {
                RepositoryResult.Error("Password must be at least 6 characters.")
            } catch (e: FirebaseAuthUserCollisionException) {
                RepositoryResult.Error("This email is already registered.")
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                RepositoryResult.Error("Invalid email format.")
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Registration failed", e)
            }
        }

    /**
     * Sends a password reset email to [email] via Firebase Auth. Returns an error if the
     * email is invalid or Firebase is not configured.
     */
    suspend fun sendPasswordReset(email: String): RepositoryResult<Unit> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth
            ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
        val trimmed = email.trim()
        if (trimmed.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return@withContext RepositoryResult.Error("Enter a valid email address.")
        }
        try {
            firebaseAuth.sendPasswordResetEmail(trimmed).await()
            RepositoryResult.Success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            // We intentionally do not reveal whether the email is registered.
            RepositoryResult.Success(Unit)
        } catch (e: Exception) {
            RepositoryResult.Error(e.message ?: "Failed to send reset email", e)
        }
    }

    suspend fun signInWithGoogle(idToken: String): RepositoryResult<User> =
        withContext(Dispatchers.IO) {
            val firebaseAuth = auth
                ?: return@withContext RepositoryResult.Error(FIREBASE_NOT_CONFIGURED_MESSAGE)
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val result = firebaseAuth.signInWithCredential(credential).await()
                val u = result.user ?: return@withContext RepositoryResult.Error("Google sign-in failed")
                ensureFirestoreUserDoc(u)
                RepositoryResult.Success(mapFirebaseUser(u))
            } catch (e: Exception) {
                RepositoryResult.Error(e.message ?: "Google sign-in failed", e)
            }
        }

    private suspend fun ensureFirestoreUserDoc(u: FirebaseUser) {
        val firebaseFirestore = firestore ?: return
        try {
            UserFirestoreHelper.ensureUserProfileDocument(firebaseFirestore, u)
        } catch (_: Exception) {
        }
    }

    private suspend fun resolveEmail(emailOrUsername: String): String {
        val normalized = emailOrUsername.trim()
        if (normalized.contains("@")) return normalized
        val firebaseFirestore = firestore
            ?: throw IllegalArgumentException("Username login is unavailable right now. Please login with email.")

        val snapshot = try {
            firebaseFirestore.collection("users")
                .whereEqualTo("username", normalized)
                .limit(1)
                .get()
                .await()
        } catch (_: Exception) {
            throw IllegalArgumentException("Username login is unavailable right now. Please login with email.")
        }

        val userDoc = snapshot.documents.firstOrNull()
            ?: throw IllegalArgumentException("Username not found. Try email or register first.")

        val email = userDoc.getString("email")
            ?: throw IllegalArgumentException("Account email is missing. Please login with email.")

        return email
    }

    private suspend fun mapFirebaseUser(u: FirebaseUser): User {
        val firebaseFirestore = firestore
        val doc = try {
            if (firebaseFirestore != null) {
                UserFirestoreHelper.ensureUserProfileDocument(firebaseFirestore, u)
            } else {
                null
            }
        } catch (_: Exception) {
            try {
                firebaseFirestore?.collection("users")?.document(u.uid)?.get()?.await()
            } catch (_: Exception) {
                null
            }
        }
        val username = doc?.getString("username")
            ?: u.displayName
            ?: u.email?.substringBefore("@")
            ?: "user"
        val displayName = doc?.getString("displayName")?.takeIf { it.isNotBlank() } ?: username
        return User(
            id = u.uid,
            username = username,
            email = u.email.orEmpty(),
            avatarUrl = doc?.getString("photoUrl")?.takeIf { it.isNotBlank() },
            bio = doc?.getString("bio"),
            displayName = displayName
        )
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        auth?.signOut()
        postDao?.deleteAll()
        articleDao?.deleteAll()
    }

    companion object {
        private const val FIREBASE_NOT_CONFIGURED_MESSAGE =
            "Firebase is not configured on this build. Add app/google-services.json to enable authentication."
    }
}
