package com.trackit.app.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trackit.app.data.repository.ObjectRepository
import com.trackit.app.data.repository.PredictRepository
import com.trackit.app.data.repository.ReminderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class ChatViewModel(
    objectRepository: ObjectRepository,
    reminderRepository: ReminderRepository,
    predictRepository: PredictRepository
) : ViewModel() {

    private val engine = ChatEngine(objectRepository, reminderRepository, predictRepository)
    private val idCounter = AtomicLong(1)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val _messages = MutableLiveData<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = idCounter.getAndIncrement(),
                sender = Sender.BOT,
                text = "¡Hola! Soy TrackitBot 📦\n\nEstoy aquí para ayudarte a recordar dónde dejaste tus cosas. Usa /Ayuda para ver los comandos disponibles.",
                time = now()
            )
        )
    )
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _typing = MutableLiveData(false)
    val typing: LiveData<Boolean> = _typing

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        appendMessage(Sender.USER, text)
        _typing.value = true

        viewModelScope.launch {
            val response = engine.handle(text)
            delay(300) // pequeña pausa para que se sienta como una conversación real
            _typing.value = false
            appendMessage(Sender.BOT, response)
        }
    }

    private fun appendMessage(sender: Sender, text: String) {
        val current = _messages.value.orEmpty()
        _messages.value = current + ChatMessage(idCounter.getAndIncrement(), sender, text, now())
    }

    private fun now(): String = timeFormat.format(Date())
}
