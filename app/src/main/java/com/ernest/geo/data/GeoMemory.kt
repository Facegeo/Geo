package com.ernest.geo.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

object GeoMemory {

    private const val PREFS_NAME = "geo_long_term_memory"
    private const val KEY_FACTS = "important_facts"
    private val conversationHistory = mutableListOf<String>()

    // Short-term conversation history
    fun addUserMessage(text: String) {
        conversationHistory.add("User: $text")
        if (conversationHistory.size > 20) conversationHistory.removeAt(0)
    }

    fun addGeoMessage(text: String) {
        conversationHistory.add("Geo: $text")
        if (conversationHistory.size > 20) conversationHistory.removeAt(0)
    }

    fun getConversationContext(): String {
        return conversationHistory.joinToString("\n")
    }

    // Long-term facts storage (Preferences, Projects, Deadlines, Facts)
    fun storeFact(context: Context, fact: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val factsJson = prefs.getString(KEY_FACTS, "[]")
        try {
            val array = JSONArray(factsJson)
            array.put(fact)
            prefs.edit().putString(KEY_FACTS, array.toString()).apply()
            Log.d("GEO_MEMORY", "Stored fact: $fact")
        } catch (e: Exception) {
            Log.e("GEO_MEMORY", "Error storing fact", e)
        }
    }

    fun getAllFacts(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val factsJson = prefs.getString(KEY_FACTS, "[]")
        return try {
            val array = JSONArray(factsJson)
            val sb = StringBuilder()
            for (i in 0 until array.length()) {
                sb.append("- ").append(array.getString(i)).append("\n")
            }
            sb.toString()
        } catch (e: Exception) {
            ""
        }
    }
}