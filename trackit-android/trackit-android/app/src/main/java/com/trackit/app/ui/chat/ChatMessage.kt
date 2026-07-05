package com.trackit.app.ui.chat

enum class Sender { USER, BOT }

data class ChatMessage(
    val id: Long,
    val sender: Sender,
    val text: String,
    val time: String
)
