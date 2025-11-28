package com.example.umbilotemplefrontend.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.umbilotemplefrontend.R
import com.example.umbilotemplefrontend.models.ChatMessage

class ChatMessageAdapter(
	private val items: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

	companion object {
		private const val TYPE_USER = 1
		private const val TYPE_BOT = 2
	}

	override fun getItemViewType(position: Int): Int {
		return if (items[position].isUser) TYPE_USER else TYPE_BOT
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
		val inflater = LayoutInflater.from(parent.context)
		return if (viewType == TYPE_USER) {
			val v = inflater.inflate(R.layout.item_chat_message_user, parent, false)
			UserVH(v)
		} else {
			val v = inflater.inflate(R.layout.item_chat_message_bot, parent, false)
			BotVH(v)
		}
	}

	override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
		val item = items[position]
		if (holder is UserVH) {
			holder.text.text = item.text
		} else if (holder is BotVH) {
			holder.text.text = item.text
		}
	}

	override fun getItemCount(): Int = items.size

	fun submit(message: ChatMessage) {
		items.add(message)
		notifyItemInserted(items.lastIndex)
	}

	class UserVH(v: View) : RecyclerView.ViewHolder(v) {
		val text: TextView = v.findViewById(R.id.tvMessage)
	}

	class BotVH(v: View) : RecyclerView.ViewHolder(v) {
		val text: TextView = v.findViewById(R.id.tvMessage)
	}
}


