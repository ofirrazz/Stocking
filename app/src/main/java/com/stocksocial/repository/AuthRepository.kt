package com.stocksocial.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.stocksocial.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun login(emailOrUsername: String, password: String): RepositoryResult<User> =
        withContext(Dispatchers.IO) {
            try {
                val emailToUse = resolveEmail(emailOrUsername)
                val result = auth.signInWithEmailAndPassword(emailToUse, password).await()
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
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val u = result.user ?: return@withContext RepositoryResult.Error("Registration failed")
                u.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(username).build()
                ).await()
                val trimmedName = username.trim()
                val userDoc = hashMapOf(
                    "username" to trimmedName,
                    "usernameLower" to trimmedName.lowercase(),
                    "displayName" to trimmedName,
                    "email" to email,
                    "photoUrl" to "",
                    "bio" to "",
                    "location" to "",
                    "website" to "",
                    "bannerUrl" to "",
                    "createdAt" to System.currentTimeMillis()
                )
                // Firestore profile is optional for successful auth.
                // If rules/network fail here, user can still continue into the app.
                try {
                    firestore.collection("users").document(u.uid).set(userDoc).await()
                } catch (_: Exception) {
                    // Ignore Firestore profile write failures and rely on FirebaseAuth profile.
                }
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

    private suspend fun resolveEmail(emailOrUsername: String): String {
        val normalized = emailOrUsername.trim()
        if (normalized.contains("@")) return normalized

        val snapshot = try {
            firestore.collection("users")
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
        val doc = try {
            firestore.collection("users").document(u.uid).get().await()
        } catch (_: Exception) {
            null
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

    fun logout() {
        auth.signOut()
    }
}
