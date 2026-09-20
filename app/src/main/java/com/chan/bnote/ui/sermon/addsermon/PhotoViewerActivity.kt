package com.chan.bnote.ui.sermon.addsermon

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.load
import com.chan.bnote.R

class PhotoViewerActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_PATH = "extra_photo_path"

		fun start(context: Context, filePath: String) {
			val intent = Intent(context, PhotoViewerActivity::class.java)
			intent.putExtra(EXTRA_PATH, filePath)
			context.startActivity(intent)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_photo_viewer)

		val path = intent.getStringExtra(EXTRA_PATH)
		findViewById<ImageView>(R.id.image_fullscreen_photo).load(path)
		findViewById<ImageView>(R.id.btn_close_photo_viewer).setOnClickListener { finish() }
	}
}