package com.chan.bnote.ui.mypage

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.prayer.PrayerRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PrayerRequestActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyStateText: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_prayer_request)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.prayer_request_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		recyclerView = findViewById(R.id.recycler_prayer_requests)
		emptyStateText = findViewById(R.id.text_empty_state)
		recyclerView.layoutManager = LinearLayoutManager(this)

		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.btn_add_prayer).setOnClickListener { showAddDialog() }

		loadItems()
	}

	private fun loadItems() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val items = db.prayerRequestDao().getAll()

			emptyStateText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
			recyclerView.adapter = PrayerRequestAdapter(
				items = items,
				onToggleAnswered = { item -> toggleAnswered(item) },
				onDelete = { item -> confirmDelete(item) }
			)
		}
	}

	private fun toggleAnswered(item: PrayerRequest) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val updated = if (item.isAnswered) {
				item.copy(isAnswered = false, answeredAt = null)
			} else {
				item.copy(isAnswered = true, answeredAt = System.currentTimeMillis())
			}
			db.prayerRequestDao().update(updated)
			loadItems()
		}
	}

	private fun showAddDialog() {
		val editText = EditText(this).apply {
			hint = "기도제목을 적어보세요"
			setPadding(48, 32, 48, 32)
			textSize = 15f
			minLines = 3
			gravity = Gravity.TOP or Gravity.START
			background =
				ContextCompat.getDrawable(this@PrayerRequestActivity, R.drawable.bg_book_button)
		}
		val container = FrameLayout(this).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("기도제목 추가")
			.setView(container)
			.setPositiveButton("추가") { _, _ ->
				val text = editText.text.toString().trim()
				if (text.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.prayerRequestDao().insert(PrayerRequest(content = text))
						loadItems()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun confirmDelete(item: PrayerRequest) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("기도제목 삭제")
			.setMessage("삭제하면 되돌릴 수 없어요. 계속할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.prayerRequestDao().delete(item)
					loadItems()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}