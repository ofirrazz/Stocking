package com.stocksocial.model.cache

import com.stocksocial.model.Article
import com.stocksocial.model.Post
import com.stocksocial.model.User

fun CachedArticleEntity.toArticle(): Article = Article(
    id = id,
    category = category,
    title = title,
    summary = summary,
    author = author,
    content = content,
    source = source,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    url = url,
    localImagePath = localImagePath
)

fun Article.toEntity(localImagePathOverride: String? = null): CachedArticleEntity = CachedArticleEntity(
    id = id,
    category = category,
    title = title,
    summary = summary,
    author = author,
    content = content,
    source = source,
    imageUrl = imageUrl,
    publishedAt = publishedAt,
    url = url,
    localImagePath = localImagePathOverride ?: localImagePath
)

fun CachedPostEntity.toPost(): Post = Post(
    id = id,
    author = User(
        id = authorId,
        username = authorUsername,
        email = "",
        avatarUrl = null,
        bio = null
    ),
    content = content,
    createdAt = createdAt,
    likesCount = likesCount,
    likedByCurrentUser = likedByCurrentUser,
    commentsCount = commentsCount,
    stockSymbol = stockSymbol,
    stockPrice = stockPrice,
    imageUrl = imageUrl,
    videoUrl = videoUrl,
    localImagePath = localImagePath
)

fun Post.toEntity(localImagePathOverride: String? = null): CachedPostEntity {
    val millis = try {
        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).parse(createdAt)?.time ?: 0L
    } catch (_: Exception) {
        0L
    }
    return CachedPostEntity(
        id = id,
        authorId = author.id,
        authorUsername = author.username,
        content = content,
        createdAt = createdAt,
        createdAtMillis = millis,
        likesCount = likesCount,
        likedByCurrentUser = likedByCurrentUser,
        commentsCount = commentsCount,
        stockSymbol = stockSymbol,
        stockPrice = stockPrice,
        imageUrl = imageUrl,
        videoUrl = videoUrl,
        localImagePath = localImagePathOverride ?: localImagePath
    )
}
