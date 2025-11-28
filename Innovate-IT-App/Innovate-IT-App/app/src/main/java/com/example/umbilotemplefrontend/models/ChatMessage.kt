package com.example.umbilotemplefrontend.models

data class ChatMessage(
	val id: Long,
	val text: String,
	val isUser: Boolean,
	val timestampMs: Long = System.currentTimeMillis()
)


