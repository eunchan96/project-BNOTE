package com.chan.bnote.ui.knowledge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.knowledge.ParableRepository
import kotlinx.coroutines.launch

class ParableDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_ITEM_ID = "extra_item_id"

		fun createIntent(context: Context, itemId: String): Intent =
			Intent(context, ParableDetailActivity::class.java).putExtra(EXTRA_ITEM_ID, itemId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_parable_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.parable_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		loadItem()
	}

	private fun loadItem() {
		val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
		if (itemId == null) {
			finish()
			return
		}
		lifecycleScope.launch {
			val item = ParableRepository.getById(applicationContext, itemId)
			if (item == null) {
				finish()
				return@launch
			}

			findViewById<TextView>(R.id.text_top_bar_title).text = item.title
			findViewById<TextView>(R.id.text_type).text = item.type
			findViewById<TextView>(R.id.text_title).text = item.title
			findViewById<TextView>(R.id.text_summary).text = item.summary
			findViewById<TextView>(R.id.text_description).text = item.description

			findViewById<TextView>(R.id.btn_key_verse).apply {
				text = "${item.keyVerseLabel} 보러 가기"
				setOnClickListener {
					val navIntent =
						Intent(this@ParableDetailActivity, MainActivity::class.java).apply {
							putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, item.keyBookId)
							putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, item.keyChapter)
							flags =
								Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
						}
					startActivity(navIntent)
				}
			}
		}
	}
}