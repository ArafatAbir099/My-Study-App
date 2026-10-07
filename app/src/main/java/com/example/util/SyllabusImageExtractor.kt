package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object SyllabusImageExtractor {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Saves uploaded image locally to app files directory.
     */
    fun saveImageLocally(context: Context, uri: Uri, originalName: String): File? {
        return try {
            val imagesDir = File(context.filesDir, "syllabus_images").apply { mkdirs() }
            val cleanName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(imagesDir, "${System.currentTimeMillis()}_$cleanName")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts text lines from an image file/URI using Gemini AI Vision.
     * Extracts numbered and bulleted lists, unit headings, chapters, and topics
     * faithfully without inventing or hallucinating missing content.
     */
    suspend fun extractTextFromImageUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeSampledBitmap(context, uri, 1200, 1200) ?: return@withContext ""

        // Compress to JPEG Base64
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val base64Data = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        // Try Gemini Vision API if key is available
        val apiKey = try {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isNotBlank() && key != "MY_GEMINI_API_KEY") key else null
        } catch (_: Exception) {
            null
        }

        if (apiKey != null) {
            try {
                val jsonPayload = JSONObject().apply {
                    val contents = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val parts = JSONArray().apply {
                                val promptPart = JSONObject().apply {
                                    put(
                                        "text",
                                        "You are a strict academic syllabus text extractor. Analyze this syllabus image and extract every topic, chapter title, and curriculum subject exactly as written in the original order. Do NOT invent, assume, or add topics that are not present in the image. Return only the extracted topics, one per line. Do not include markdown bullet points, symbols, numbers, or introductory text."
                                    )
                                }
                                val imagePart = JSONObject().apply {
                                    val inlineData = JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", base64Data)
                                    }
                                    put("inlineData", inlineData)
                                }
                                put(promptPart)
                                put(imagePart)
                            }
                            put("parts", parts)
                        }
                        put(contentObj)
                    }
                    put("contents", contents)
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                // Use gemini-2.5-flash or gemini-3.5-flash
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val respJson = JSONObject(responseBody)
                        val candidates = respJson.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                if (text.isNotBlank()) {
                                    return@withContext text
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // If API call fails (e.g. offline or quota), continue to ML Kit fallback
            }
        }

        // Run On-Device ML Kit Text Recognition fallback (zero API key needed, offline-ready)
        val mlKitText = runMlKitOcr(bitmap)
        if (mlKitText.isNotBlank()) {
            return@withContext mlKitText
        }

        // Return empty string if no text was found in image
        ""
    }

    private suspend fun runMlKitOcr(bitmap: Bitmap): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener {
                    continuation.resume("")
                }
        } catch (_: Exception) {
            continuation.resume("")
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }
            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses structured topics directly from raw text (either typed, OCR-extracted, or from multiple images/pages).
     * Never invents topics not present in the input.
     */
    fun parseStructuredTopics(rawText: String): List<ExtractedTopic> {
        val topics = mutableListOf<ExtractedTopic>()
        val lines = rawText.lines()
            .map { cleanOcrLine(it) }
            .filter { it.isNotBlank() }

        val topicBulletRegex = Regex(
            "^(?:[•\\-*▪▫▶✓✔]|\\d+[\\.\\)]|[a-zA-Z][\\.\\)]|Topic\\s*\\d*[:\\-]?|Module\\s*\\d*[:\\-]?|Unit\\s*\\d*[:\\-]?)\\s*(.+)",
            RegexOption.IGNORE_CASE
        )

        for (line in lines) {
            val trimmed = line.trim()

            // Skip page headers or trivial noise
            if (trimmed.length < 2 || trimmed.startsWith("Page ", ignoreCase = true) ||
                trimmed.equals("Syllabus", ignoreCase = true) ||
                trimmed.equals("Course Outline", ignoreCase = true) ||
                trimmed.equals("Table of Contents", ignoreCase = true)
            ) {
                continue
            }

            // Check if line has a bullet or number: e.g. "1. Introduction to Algorithms" or "- Time Complexity"
            val match = topicBulletRegex.find(trimmed)
            val candidate = if (match != null) {
                match.groupValues[1].trim()
            } else {
                trimmed
            }

            // Clean trailing punctuation or lecture hours (e.g., "(3 hours)", "[2 Lectures]")
            val cleanedName = candidate
                .replace(Regex("\\s*\\(\\s*\\d+\\s*(?:hours|hrs|lectures|periods)?\\s*\\)", RegexOption.IGNORE_CASE), "")
                .replace(Regex("\\s*\\[\\s*\\d+\\s*(?:hours|hrs|lectures)?\\s*\\]", RegexOption.IGNORE_CASE), "")
                .trim()

            if (cleanedName.length in 3..100) {
                // Check if multiple comma-separated items on a single line
                if (cleanedName.contains(",") && !cleanedName.contains("e.g.", ignoreCase = true) && cleanedName.length < 80) {
                    val subParts = cleanedName.split(",").map { it.trim() }.filter { it.length in 3..50 }
                    for (part in subParts) {
                        if (!topics.any { it.name.equals(part, ignoreCase = true) }) {
                            topics.add(ExtractedTopic(name = part))
                        }
                    }
                } else {
                    if (!topics.any { it.name.equals(cleanedName, ignoreCase = true) }) {
                        topics.add(ExtractedTopic(name = cleanedName))
                    }
                }
            }
        }

        return topics
    }

    private fun cleanOcrLine(line: String): String {
        return line
            .replace(Regex("[\\p{Cntrl}&&[^\r\n\t]]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
