package com.ernest.geo.ai

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log

object WhatsAppReplyManager {

    enum class State {
        IDLE,
        WAITING_FOR_APPROVAL,
        WAITING_FOR_EDIT_TEXT
    }

    private var pendingIntent: PendingIntent? = null
    private var remoteInput: RemoteInput? = null
    private var suggestedReply: String? = null
    
    var currentState = State.IDLE
        private set

    fun setPendingReply(intent: PendingIntent?, input: RemoteInput?, suggestion: String) {
        if (intent != null) pendingIntent = intent
        if (input != null) remoteInput = input
        suggestedReply = suggestion
        currentState = State.WAITING_FOR_APPROVAL
        Log.d("GEO_REPLY", "Pending reply set: $suggestion")
    }

    fun updateReplyAndReady(newReply: String) {
        suggestedReply = newReply
        currentState = State.WAITING_FOR_APPROVAL
    }

    fun enterEditMode() {
        currentState = State.WAITING_FOR_EDIT_TEXT
    }

    fun getSuggestedReply(): String? = suggestedReply

    fun updateSuggestedReply(newReply: String) {
        suggestedReply = newReply
    }

    fun sendReply(context: Context): Boolean {
        val intent = pendingIntent
        val input = remoteInput
        val reply = suggestedReply

        if (intent != null && input != null && reply != null) {
            try {
                val fillInIntent = Intent()
                val bundle = Bundle()
                bundle.putCharSequence(input.resultKey, reply)
                RemoteInput.addResultsToIntent(arrayOf(input), fillInIntent, bundle)
                intent.send(context, 0, fillInIntent)
                clear()
                return true
            } catch (e: Exception) {
                Log.e("GEO_REPLY", "Failed to send reply", e)
            }
        }
        return false
    }

    fun clear() {
        pendingIntent = null
        remoteInput = null
        suggestedReply = null
        currentState = State.IDLE
        Log.d("GEO_REPLY", "Pending reply cleared")
    }
}