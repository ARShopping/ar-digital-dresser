package com.example.fyp

import android.content.Context
import android.net.Uri
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object CloudinaryManager {

    private const val CLOUD_NAME = "drhdp0t9j"
    private const val API_KEY = "851978611266154"
    private const val API_SECRET = "ZinCg2tp98pVpfVKbn0lcE5A8DA"

    private val cloudinary = Cloudinary(
        mapOf(
            "cloud_name" to CLOUD_NAME,
            "api_key" to API_KEY,
            "api_secret" to API_SECRET
        )
    )

    suspend fun uploadImage(context: Context, fileUri: Uri): String? {
        return withContext(Dispatchers.IO) {
            val inputStream: InputStream? = context.contentResolver.openInputStream(fileUri)
            if (inputStream == null) return@withContext null

            try {
                val uploadResult = cloudinary.uploader().upload(inputStream, ObjectUtils.emptyMap())
                return@withContext uploadResult["secure_url"] as String?
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }
}
