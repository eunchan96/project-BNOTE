package com.chan.bnote.ui.mypage.prayer

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.prayer.PrayerRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class PrayerRequestActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var emptyStateText: TextView
	private lateinit var btnManage: TextView
	private var isManageMode = false

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

		btnManage = findViewById(R.id.btn_manage_prayer)
		val btnAdd = findViewById<ImageView>(R.id.btn_add_prayer)
		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		btnAdd.setOnClickListener { showAddDialog() }
		btnManage.setOnClickListener {
			isManageMode = !isManageMode
			btnManage.text = if (isManageMode) "완료" else "관리"
			loadItems()
		}

		loadItems()
	}

	private fun loadItems() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val items = db.prayerRequestDao().getAll()

			emptyStateText.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
			recyclerView.adapter = PrayerRequestAdapter(
				items = items,
				isManageMode = isManageMode,
				onToggleAnswered = { item -> toggleAnswered(item) },
				onEdit = { item -> showEditDialog(item) },
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
		PrayerRequestEditorBottomSheet().apply {
			onChanged = { loadItems() }
		}.show(supportFragmentManager, "prayer_editor")
	}

	private fun showEditDialog(item: PrayerRequest) {
		PrayerRequestEditorBottomSheet().apply {
			existing = item
			onChanged = { loadItems() }
		}.show(supportFragmentManager, "prayer_editor")
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
}