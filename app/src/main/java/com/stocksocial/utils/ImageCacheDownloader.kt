package com.stocksocial.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import okhttp3.OkHttpClient
import okhttp3.Request

object ImageCacheDownloader {

    private val client = OkHttpClient()

    fun download(context: Context, url: String, subDir: String, fileName: String): String? {
        return try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val dir = File(context.cacheDir, subDir).apply { mkdirs() }
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    response.body?.byteStream()?.copyTo(out)
                }
                file.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }
}
