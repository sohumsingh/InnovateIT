package com.example.umbilotemplefrontend.models

import android.graphics.Color
import java.io.Serializable

data class EventCategory(
    val id: String,
    val name: String,
    val color: Int
) : Serializable {
    companion object {
        val DEFAULT = EventCategory("default", "General", Color.parseColor("#1976D2"))
        val RELIGIOUS = EventCategory("religious", "Religious", Color.parseColor("#FFA000"))
        val FIREWALKING = EventCategory("firewalking", "Firewalking", Color.parseColor("#D32F2F"))
        val COMMUNITY = EventCategory("community", "Community", Color.parseColor("#388E3C"))
        val FESTIVAL = EventCategory("festival", "Festival", Color.parseColor("#7B1FA2"))
        val OTHER = EventCategory("other", "Other", Color.parseColor("#455A64"))
        
        val ALL_CATEGORIES = listOf(DEFAULT, RELIGIOUS, FIREWALKING, COMMUNITY, FESTIVAL, OTHER)
        
        fun getById(id: String): EventCategory {
            return ALL_CATEGORIES.find { it.id == id } ?: DEFAULT
        }
    }
}
