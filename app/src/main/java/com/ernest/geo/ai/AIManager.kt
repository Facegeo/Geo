package com.ernest.geo.ai

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.ernest.geo.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.ernest.geo.data.GeoMemory

class AIManager {

    private val model = GenerativeModel(
        modelName = "gemini-3.1-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) return bitmap
        
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (ratio > 1) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    suspend fun extractAndStoreFact(context: Context, command: String) {
        if (BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "null") {
            Log.e("GEO_AI", "Gemini API Key is missing or invalid.")
            return
        }
        val prompt = """
            Analyze the following user input and determine if it contains an important fact, preference, project, or deadline that should be remembered for the future.
            Examples of things to remember: 
            - "My exam is on Friday" -> "User's exam is on Friday"
            - "I love dark chocolate" -> "User loves dark chocolate"
            - "The password to my bike is 1234" -> "The bike password is 1234"
            - "I am working on the Geo project" -> "User is working on the Geo project"
            
            Input: "$command"
            
            If there is a fact to remember, return ONLY the summarized fact.
            If there is nothing important to remember, return ONLY the word "none".
        """.trimIndent()

        try {
            val response = model.generateContent(prompt)
            val fact = response.text?.trim()
            if (fact != null && fact.lowercase() != "none") {
                GeoMemory.storeFact(context, fact)
            }
        } catch (e: Exception) {
            Log.e("GEO_AI", "Error extracting fact: ${e.message}", e)
        }
    }

    suspend fun analyzeImage(context: Context, bitmap: Bitmap, prompt: String): String {
        return try {
            Log.d("GEO_AI", "Starting image analysis. Prompt length: ${prompt.length}")
            
            // Resize bitmap to avoid "Request payload too large" or timeouts
            val processedBitmap = resizeBitmap(bitmap, 1024)
            
            val importantFacts = GeoMemory.getAllFacts(context)
            val userQuestion = if (prompt.trim().isEmpty()) "Describe this image naturally." else prompt
            
            val fullPrompt = """
                You are Geo, Ernest's personal AI assistant.
                You are looking at an image captured by Ernest.
                
                Guidelines for Image Analysis:
                1. Identify the primary subject naturally. Do NOT force references to telecommunications or engineering.
                2. If the image is an everyday object (bed, food, people, pets, buildings, clothing, furniture, landscapes, etc.), describe it naturally as a human would.
                3. ONLY relate the image to Telecommunication and Information Engineering if:
                   - The subject genuinely belongs to that domain (electronics, antennas, routers, cables, circuit boards, network devices, etc.).
                   - OR if Ernest explicitly asks for a technical/engineering explanation in his question.
                4. Maintain a helpful and friendly tone.
                
                Things you remember about Ernest:
                $importantFacts
                
                Ernest's question about this image:
                $userQuestion
            """.trimIndent()

            val inputContent = content {
                image(processedBitmap)
                text(fullPrompt)
            }

            val response = model.generateContent(inputContent)
            val result = response.text ?: "I can see the image, but I can't describe it right now."
            
            Log.d("GEO_AI", "Analysis successful")
            result
            
        } catch (e: Exception) {
            Log.e("GEO_AI", "Vision error: ${e.message}", e)
            "Sorry Ernest, I'm having trouble seeing right now. Please try again."
        }
    }

    suspend fun generateSuggestedReply(sender: String, message: String): String {
        val prompt = """
            You are Geo, Ernest's personal AI assistant.
            Ernest just received a WhatsApp message from $sender.
            
            Message: "$message"
            
            Your task:
            1. Understand the message intent. It may be in English, Kiswahili, Luo, or a mix of these.
            2. Detect the language blend and tone.
            3. Generate a single, short, and natural suggested reply.
            4. Use the same language blend (English/Kiswahili/Luo) as the sender if appropriate.
            5. If you are uncertain about Luo interpretation, preserve the intent and use English/Kiswahili naturally.
            6. Return ONLY the suggested reply text. No explanations.
            
            Suggested reply:
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: "Okay."
        } catch (e: Exception) {
            Log.e("GEO_AI", "Error generating suggestion: ${e.message}", e)
            "Noted."
        }
    }

    suspend fun extractReminder(command: String): String? {
        val now = System.currentTimeMillis()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        val currentTimeStr = sdf.format(java.util.Date(now))
        
        val prompt = """
            You are a precision time-parsing engine for an Android assistant.
            Input Command: "$command"
            
            Reference Current Time: $currentTimeStr
            Reference Current Epoch: $now
            
            Task:
            Identify if the user is requesting a RELATIVE time (e.g., "in 2 minutes", "after 1 hour") 
            or an ABSOLUTE time (e.g., "at 8 PM", "tomorrow at 3", "on Friday").
            
            JSON Structure:
            - "task": (String) The task description.
            - "type": (String) "RELATIVE" or "ABSOLUTE".
            - "relativeDelayMs": (Long) Total duration in milliseconds if type is RELATIVE.
            - "isoDateTime": (String) Target absolute time in "yyyy-MM-dd'T'HH:mm:ss" format.
            - "interval": (Long) 0 for one-time, 86400000 for daily.
            
            Rules:
            1. If RELATIVE (e.g., "in 2 minutes"): calculate relativeDelayMs (e.g., 120000).
            2. If ABSOLUTE (e.g., "at 8 PM"): calculate isoDateTime. If the time has already passed today, assume tomorrow.
            3. CRITICAL: Do not confuse relative words as dates. "in 2 minutes" is RELATIVE.
            4. Return ONLY raw JSON. Return "null" if no reminder is detected.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            val text = response.text?.trim()
            Log.d("GEO_AI", "Reminder raw output: $text")
            if (text == "null" || text == null) null 
            else text.replace("```json", "").replace("```", "").trim()
        } catch (e: Exception) {
            Log.e("GEO_AI", "Reminder extraction error: ${e.message}", e)
            null
        }
    }

    suspend fun generateReminderAnnouncement(task: String): String {
        val prompt = """
            You are Geo, Ernest's personal AI assistant. 
            A reminder for the following task has just triggered: "$task"
            
            Instruction:
            Generate a short, natural, and helpful announcement for this reminder.
            Ernest is a Telecommunication student, so be encouraging and technical when appropriate.
            
            Examples:
            - "Submit assignment" -> "Ernest, it's time to submit your Telecommunications assignment. Good luck!"
            - "Drink water" -> "You've been meaning to drink some water. Stay hydrated."
            - "Call Brian" -> "Don't forget to call Brian."
            - "Medication" -> "It's time for your medication, Ernest. Health first!"
            - "Workout" -> "Time for your evening workout. You've got this!"
            - "Birthday" -> "Today is Mary's birthday. Would you like me to help you send a birthday message?"
            
            Guidelines:
            1. Keep it concise (1-2 sentences).
            2. Be friendly and personalized.
            3. Return ONLY the announcement text.
        """.trimIndent()

        return try {
            val response = model.generateContent(prompt)
            response.text?.trim() ?: "Ernest, this is your reminder to $task"
        } catch (e: Exception) {
            Log.e("GEO_AI", "Error generating announcement: ${e.message}")
            "Ernest, this is your reminder to $task"
        }
    }

    suspend fun askGeo(
        context: Context,
        prompt: String
    ): String {

        return try {
            // Retrieve the active dynamic context history and long term facts
            val conversationContext = GeoMemory.getConversationContext()
            val importantFacts = GeoMemory.getAllFacts(context)

            // Format the full contextual system wrapper injection
            val fullPrompt = """
                You are Geo, Ernest's personal AI assistant.
                Be conversational, helpful, concise, and friendly.
                
                Language Instructions:
                1. Always respond in English.
                2. Even if the user uses Kiswahili, Luo, or a mix, your response must be strictly in English.
                3. Maintain a professional yet friendly tone.
        
                Important things you remember about Ernest:
                $importantFacts

                Previous conversation:
        
                $conversationContext
        
                User:
                $prompt
            """.trimIndent()

            val response = model.generateContent(fullPrompt)

            response.text
                ?: "I couldn't generate a response."

        } catch (e: Exception) {
            Log.e("GEO_AI", "AskGeo error: ${e.message}", e)
            "Sorry Ernest, an error occurred: ${e.message}"
        }
    }
}
