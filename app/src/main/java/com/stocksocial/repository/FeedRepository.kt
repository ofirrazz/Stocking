package com.stocksocial.repository

import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.stocksocial.model.Post
import com.stocksocial.model.PostDao
import kotlinx.coroutines.tasks.await

class FeedRepository(private val postDao: PostDao) {

    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    // ה-UI יתחבר לזה. LiveData מה-Room תמיד יתעדכן כשנשמור נתונים חדשים.
    val allPosts: LiveData<List<Post>> = postDao.getAllPosts()

    // פונקציה לרענון הנתונים מהשרת
    suspend fun refreshPosts() {
        try {
            // 1. נביא את הנתונים מ-Firebase
            val snapshot = postsCollection.get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val authorId = doc.getString("authorId") ?: ""
                val authorName = doc.getString("authorName") ?: "Unknown"
                val content = doc.getString("content") ?: ""
                val imageUrl = doc.getString("imageUrl")
                val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                
                Post(id, authorId, authorName, content, imageUrl, createdAt)
            }

            // 2. נשמור אותם ב-Room (ה-UI יתעדכן אוטומטית דרך ה-LiveData)
            postDao.insertAll(posts)
        } catch (e: Exception) {
            e.printStackTrace()
            // כאן אפשר לנהל שגיאות (למשל אם אין אינטרנט)
        }
    }

    // הוספת פוסט חדש
    suspend fun addPost(post: Post) {
        // קודם שומרים בשרת
        postsCollection.document(post.id).set(post).await()
        // אחר כך שומרים מקומית
        postDao.insert(post)
    }
}
