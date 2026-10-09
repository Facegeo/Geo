package com.ernest.geo.data

import androidx.compose.runtime.mutableStateListOf
import com.ernest.geo.model.Message

object NotificationRepository {

    val notifications =
        mutableStateListOf<Message>()

    fun addNotification(text: String) {

        notifications.add(
            Message(
                text = text,
                isUser = false
            )
        )
    }
}