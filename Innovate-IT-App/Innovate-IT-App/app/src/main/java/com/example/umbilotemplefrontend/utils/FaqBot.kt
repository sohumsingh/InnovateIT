package com.example.umbilotemplefrontend.utils

import java.util.Locale
import com.example.umbilotemplefrontend.models.FaqItem

import android.content.res.Resources
import com.example.umbilotemplefrontend.R

class FaqBot(
    private val resources: Resources,
    private val isAdmin: Boolean
) {

	private val faqs: List<FaqItem> = listOf(
		FaqItem(
			question = "How can I make a donation?",
			answer = resources.getString(R.string.faq_donations_answer),
			keywords = listOf("donate", "donation", "contribute", "offerings", "money", "bank", "account", "branch", "code", "copy", "eft", "payment", "pay", "transfer")
		),
		// Donation aliases
		FaqItem(
			question = "Where do I find the bank details?",
			answer = resources.getString(R.string.faq_donations_answer),
			keywords = listOf("bank details", "account number", "branch code", "donation", "donate", "eft", "transfer")
		),
		FaqItem(
			question = "How do I copy the account number?",
			answer = resources.getString(R.string.faq_donations_answer),
			keywords = listOf("copy", "account number", "branch", "donate", "bank")
		),
		FaqItem(
			question = "Where can I see upcoming events?",
			answer = resources.getString(R.string.faq_events_answer),
			keywords = listOf("events", "upcoming", "schedule", "calendar", "next", "program", "what's on", "whats on")
		),
		// Events aliases
		FaqItem(
			question = "When is the next event?",
			answer = resources.getString(R.string.faq_events_answer),
			keywords = listOf("next event", "today", "tonight", "this week", "what's on", "whats on")
		),
		FaqItem(
			question = "How do I book a service?",
			answer = resources.getString(R.string.faq_booking_how_answer),
			keywords = listOf("book", "booking", "reserve", "reservation", "service", "request", "rsvp", "apply")
		),
		// Booking aliases
		FaqItem(
			question = "How do I request a booking?",
			answer = resources.getString(R.string.faq_booking_how_answer),
			keywords = listOf("request", "booking request", "reserve", "reservation", "book")
		),
		FaqItem(
			question = "What does Pending mean for my booking?",
			answer = resources.getString(R.string.faq_booking_pending_answer),
			keywords = listOf("pending", "booking", "status", "review", "approved", "rejected")
		),
		FaqItem(
			question = "How do I upload documents or gallery media?",
			answer = resources.getString(R.string.faq_uploads_how_answer),
			keywords = listOf("upload", "document", "pdf", "doc", "gallery", "image", "video", "approve", "approval", "media", "post", "picture", "photo")
		),
		// Upload aliases
		FaqItem(
			question = "How do I post a photo to the gallery?",
			answer = resources.getString(R.string.faq_uploads_how_answer),
			keywords = listOf("post", "photo", "picture", "upload", "gallery")
		),
		FaqItem(
			question = "Why is my gallery upload not visible yet?",
			answer = resources.getString(R.string.faq_gallery_visibility_answer),
			keywords = listOf("gallery", "upload", "pending", "approve", "visible", "appear", "review")
		),
		FaqItem(
			question = "Which file types can I upload?",
			answer = resources.getString(R.string.faq_supported_files_answer),
			keywords = listOf("file", "type", "format", "pdf", "doc", "image", "video", "mp4", "png", "jpg", "gif", "bmp", "avi", "mov")
		),
		FaqItem(
			question = "How do I change between Light and Dark mode?",
			answer = resources.getString(R.string.faq_theme_answer),
			keywords = listOf("dark", "light", "theme", "mode", "appearance")
		),
		FaqItem(
			question = "How do I update my account details?",
			answer = resources.getString(R.string.faq_update_account_answer),
			keywords = listOf("update", "account", "profile", "name", "phone", "edit", "change details")
		),
		FaqItem(
			question = "Where can I see my uploads?",
			answer = resources.getString(R.string.faq_my_uploads_answer),
			keywords = listOf("my", "uploads", "status", "documents", "gallery", "approved", "pending", "submissions")
		),
		FaqItem(
			question = "Where can I see my pending event requests?",
			answer = resources.getString(R.string.faq_my_pending_requests_answer),
			keywords = listOf("pending", "event", "requests", "booking", "status", "submitted")
		),
		FaqItem(
			question = "How do I log out?",
			answer = resources.getString(R.string.faq_logout_answer),
			keywords = listOf("logout", "sign out", "log out", "exit", "signout")
		),
		FaqItem(
			question = "I can't see new events or documents. What can I do?",
			answer = resources.getString(R.string.faq_offline_cache_answer),
			keywords = listOf("offline", "cache", "refresh", "update", "sync", "data", "not updating", "stale", "old")
		),
		FaqItem(
			question = "How do I open the Gallery?",
			answer = resources.getString(R.string.faq_gallery_open_answer),
			keywords = listOf("gallery", "photos", "videos", "images", "media", "pictures")
		),
		FaqItem(
			question = "How do I view temple documents?",
			answer = resources.getString(R.string.faq_documents_view_answer),
			keywords = listOf("documents", "pdf", "forms", "files", "view", "download")
		),
		FaqItem(
			question = "How can I contact or get support?",
			answer = resources.getString(R.string.faq_contact_support_answer),
			keywords = listOf("contact", "support", "help", "assist", "office", "phone", "email", "call")
		),
        FaqItem(
			question = "Who can access the Admin panel?",
            answer = resources.getString(R.string.faq_admin_access_answer),
            keywords = listOf("admin", "panel", "privilege", "permission", "access"),
            adminOnly = true
		),
		FaqItem(
			question = "Where do I find the temple website?",
			answer = resources.getString(R.string.faq_website_answer),
			keywords = listOf("website", "web", "site", "link", "browser", "url")
		),
		FaqItem(
			question = "How are donations handled?",
			answer = resources.getString(R.string.faq_donations_handling_answer),
			keywords = listOf("donation", "donate", "bank", "transfer", "account", "branch", "copy", "payment")
		),
		FaqItem(
			question = "Can I upload videos to the Gallery?",
			answer = resources.getString(R.string.faq_gallery_video_answer),
			keywords = listOf("video", "mp4", "avi", "mov", "gallery", "upload", "clip")
		),
		FaqItem(
			question = "Do I need to be logged in to use the app?",
			answer = resources.getString(R.string.faq_login_required_answer),
			keywords = listOf("login", "register", "sign in", "account", "access", "signin")
		)
	)

	fun answer(userInput: String): String {
		val normalized = normalize(userInput)
		if (normalized.isBlank()) {
			return defaultAnswer()
		}
        val visibleFaqs = faqs.filter { !it.adminOnly || isAdmin }
		val scored = visibleFaqs
			.map { faq -> faq to score(normalized, faq) }
			.sortedByDescending { it.second }
		val top = scored.firstOrNull()
        return if (top != null && top.second >= 0.2) {
			top.first.answer
		} else {
			defaultAnswer()
		}
	}

	private fun defaultAnswer(): String {
		return resources.getString(R.string.faq_default_answer)
	}

	private fun score(input: String, faq: FaqItem): Double {
		val tokens = input.split(" ")
		if (tokens.isEmpty()) return 0.0
		val kw = faq.keywords.map { it.lowercase(Locale.getDefault()) }.toSet()
		val hits = tokens.count { it in kw }
		val keywordRecall = if (kw.isEmpty()) 0.0 else hits.toDouble() / kw.size
		val qOverlap = jaccard(tokens.toSet(), normalize(faq.question).split(" ").toSet())
		// Partial match boost: token contains keyword or keyword contains token (length >= 3)
		val partialHits = tokens.count { tok ->
			val t = tok.trim()
			if (t.length < 3) false else kw.any { k -> t.contains(k) || k.contains(t) }
		}
		val partialBoost = if (tokens.isEmpty()) 0.0 else partialHits.toDouble() / tokens.size
		return (keywordRecall * 0.6) + (qOverlap * 0.25) + (partialBoost * 0.15)
	}

	private fun jaccard(a: Set<String>, b: Set<String>): Double {
		if (a.isEmpty() || b.isEmpty()) return 0.0
		val inter = a.intersect(b).size.toDouble()
		val union = a.union(b).size.toDouble()
		return if (union == 0.0) 0.0 else inter / union
	}

	private fun normalize(s: String): String {
		return s.lowercase(Locale.getDefault())
			.replace("[^a-z0-9 ]+".toRegex(), " ")
			.replace("\n+".toRegex(), " ")
			.replace("\\s+".toRegex(), " ")
			.trim()
	}
}


