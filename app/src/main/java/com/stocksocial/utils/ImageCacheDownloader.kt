package com.stocksocial.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

object ImageCacheDownloader {

    private val client = OkHttpClient()

    suspend fun download(context: Context, url: String, subDir: String, fileName: String): String? {
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url).build()
            val call = client.newCall(request)

            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: java.io.IOException) {
                    if (continuation.isActive) continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            if (continuation.isActive) continuation.resume(null)
                            return
                        }
                        val dir = File(context.cacheDir, subDir).apply { mkdirs() }
                        val file = File(dir, fileName)
                        val saved = try {
                            FileOutputStream(file).use { out ->
                                it.body?.byteStream()?.copyTo(out)
                            }
                            file.absolutePath
                        } catch (_: Exception) {
                            null
                        }
                        if (continuation.isActive) continuation.resume(saved)
                    }
                }
            })
        }
    }
}
