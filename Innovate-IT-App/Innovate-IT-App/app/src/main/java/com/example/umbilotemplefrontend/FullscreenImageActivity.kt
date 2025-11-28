package com.example.umbilotemplefrontend

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullscreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_image)

        val imageView = findViewById<ImageView>(R.id.ivFullscreen)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        val imageUrl = intent.getStringExtra("IMAGE_URL")

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .into(imageView)
        }

        btnClose.setOnClickListener { finish() }
        imageView.setOnClickListener { finish() }
    }
}


