package com.example.mediumclone.data.service

import com.example.mediumclone.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RealGenerativeAIService : GenerativeAIService {

    override suspend fun generateArticleContent(topic: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        android.util.Log.d("RealGenerativeAIService", "API Key length: ${apiKey.length}")
        if (apiKey.isBlank()) {
            throw IllegalStateException("API Key not found. Please add GEMINI_API_KEY to local.properties.")
        }

        val generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = apiKey
        )

        val prompt = """
            You are an expert tech writer on Medium. Write a high-quality, engaging article section about "$topic".
            
            Guidelines:
            - Use Markdown formatting (## Headers, **Bold**, *Italic*, `Code`).
            - Do not include the main Title (H1) at the start, just the body content.
            - Include at least one code block example if relevant to the topic.
            - Make it sound professional yet accessible.
            - Keep it around 300-400 words.
        """.trimIndent()

        try {
            val response = generativeModel.generateContent(prompt)
            return@withContext response.text ?: "Sorry, I couldn't generate content for that topic."
        } catch (e: Exception) {
            e.printStackTrace()
             val message = e.message ?: "Unknown error"
             if (message.contains("404")) throw Exception("Model not found (404). Check model name.")
             throw e
        }
    }
}
