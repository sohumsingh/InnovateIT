package com.example.umbilotemplefrontend

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.adapters.ChatMessageAdapter
import com.example.umbilotemplefrontend.models.ChatMessage
import com.example.umbilotemplefrontend.utils.FaqBot
import com.example.umbilotemplefrontend.utils.LocalDatabase
import com.example.umbilotemplefrontend.utils.NavigationHandler

class ChatbotActivity : AppCompatActivity() {

	private lateinit var rv: RecyclerView
	private lateinit var et: EditText
	private lateinit var btn: Button
	private lateinit var adapter: ChatMessageAdapter
    private lateinit var bot: FaqBot
    private lateinit var localDatabase: LocalDatabase
    private lateinit var navigationHandler: NavigationHandler

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_chatbot)

		// Initialize database and navigation
		localDatabase = LocalDatabase(this)
		navigationHandler = NavigationHandler(this)

		// Check if user is logged in
		if (!navigationHandler.checkUserLoggedIn()) {
			return
		}

		// Set up navigation
		navigationHandler.setupNavigation()

		// Set header title
		findViewById<TextView>(R.id.tvHeaderTitle)?.text = "FAQ / Help"

		// Set up back button
		findViewById<ImageButton>(R.id.btnBack)?.setOnClickListener {
			finish()
		}

		rv = findViewById(R.id.rvChat)
		et = findViewById(R.id.etInput)
		btn = findViewById(R.id.btnSend)

        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
		adapter = ChatMessageAdapter(mutableListOf())
		rv.adapter = adapter

        val isAdmin = localDatabase.getCurrentUser()?.isAdmin == true
        bot = FaqBot(resources, isAdmin)

		pushBotGreeting()

		btn.setOnClickListener { submit() }
		et.setOnEditorActionListener { _, actionId, _ ->
			if (actionId == EditorInfo.IME_ACTION_SEND) {
				submit(); true
			} else false
		}
	}

	private fun submit() {
		val text = et.text?.toString()?.trim().orEmpty()
		if (text.isEmpty()) return
		val userMsg = ChatMessage(id = System.nanoTime(), text = text, isUser = true)
		adapter.submit(userMsg)
		et.setText("")
		rv.scrollToPosition(adapter.itemCount - 1)
		val reply = bot.answer(text)
		val botMsg = ChatMessage(id = System.nanoTime(), text = reply, isUser = false)
		adapter.submit(botMsg)
		rv.scrollToPosition(adapter.itemCount - 1)
	}

	private fun pushBotGreeting() {
		val greet = ChatMessage(
			id = System.nanoTime(),
			text = "Hi! I can help with FAQs like opening hours, donations, events, and bookings.",
			isUser = false
		)
		adapter.submit(greet)
	}
}


