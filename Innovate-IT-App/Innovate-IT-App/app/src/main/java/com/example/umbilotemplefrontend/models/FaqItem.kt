package com.example.umbilotemplefrontend.models

data class FaqItem(
	val question: String,
	val answer: String,
	val keywords: List<String>,
	val adminOnly: Boolean = false
)


