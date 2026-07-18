package com.chan.bnote.ui.knowledge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.knowledge.BiblePlaceRepository
import kotlinx.coroutines.launch

class BiblePlaceDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_PLACE_ID = "extra_place_id"

		fun createIntent(context: Context, placeId: String): Intent =
			Intent(context, BiblePlaceDetailActivity::class.java)
				.putExtra(EXTRA_PLACE_ID, placeId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_bible_place_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.place_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		loadPlace()
	}

	private fun loadPlace() {
		val placeId = intent.getStringExtra(EXTRA_PLACE_ID)
		if (placeId == null) {
			finish()
			return
		}
		lifecycleScope.launch {
			val place = BiblePlaceRepository.getById(applicationContext, placeId)
			if (place == null) {
				finish()
				return@launch
			}

			findViewById<TextView>(R.id.text_top_bar_title).text = place.name
			findViewById<TextView>(R.id.text_category_region).text =
				"${place.category} · ${place.region}"
			findViewById<TextView>(R.id.text_name).text = place.name

			val otherNamesView = findViewById<TextView>(R.id.text_other_names)
			if (place.otherNames.isNotBlank()) {
				otherNamesView.text = "다른 이름: ${place.otherNames}"
				otherNamesView.visibility = View.VISIBLE
			} else {
				otherNamesView.visibility = View.GONE
			}

			findViewById<TextView>(R.id.text_summary).text = place.summary
			findViewById<TextView>(R.id.text_description).text = place.description

			findViewById<TextView>(R.id.btn_key_verse).apply {
				text = "${place.keyVerseLabel} 보러 가기"
				setOnClickListener {
					val intent =
						Intent(this@BiblePlaceDetailActivity, MainActivity::class.java).apply {
							putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, place.keyBookId)
							putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, place.keyChapter)
							flags =
								Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
						}
					startActivity(intent)
				}
			}
		}
	}
}