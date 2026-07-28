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
import com.chan.bnote.data.knowledge.UnitRepository
import kotlinx.coroutines.launch

class UnitDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_UNIT_ID = "extra_unit_id"

		fun createIntent(context: Context, unitId: String): Intent =
			Intent(context, UnitDetailActivity::class.java).putExtra(EXTRA_UNIT_ID, unitId)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_unit_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.unit_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		loadUnit()
	}

	private fun loadUnit() {
		val unitId = intent.getStringExtra(EXTRA_UNIT_ID)
		if (unitId == null) {
			finish()
			return
		}
		lifecycleScope.launch {
			val unit = UnitRepository.getById(applicationContext, unitId)
			if (unit == null) {
				finish()
				return@launch
			}

			findViewById<TextView>(R.id.text_top_bar_title).text = unit.title
			findViewById<TextView>(R.id.text_category).text = unit.category
			findViewById<TextView>(R.id.text_title).text = unit.title
			findViewById<TextView>(R.id.text_summary).text = unit.summary
			findViewById<TextView>(R.id.text_description).text = unit.description

			findViewById<TextView>(R.id.btn_key_verse).apply {
				text = "${unit.keyVerseLabel} 보러 가기"
				setOnClickListener {
					val navIntent =
						Intent(this@UnitDetailActivity, MainActivity::class.java).apply {
							putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, unit.keyBookId)
							putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, unit.keyChapter)
							flags =
								Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
						}
					startActivity(navIntent)
				}
			}
		}
	}
}