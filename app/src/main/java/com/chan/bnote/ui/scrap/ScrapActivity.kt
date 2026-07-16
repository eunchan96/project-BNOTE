package com.chan.bnote.ui.scrap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.scrap.Scrap
import com.chan.bnote.data.scrap.ScrapGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ScrapActivity : AppCompatActivity() {

	private lateinit var groupListContainer: View
	private lateinit var groupDetailContainer: View
	private lateinit var groupRecycler: RecyclerView
	private lateinit var verseRecycler: RecyclerView
	private lateinit var detailGroupNameText: TextView
	private lateinit var editModeToggle: TextView

	private var selectedGroup: ScrapGroup? = null
	private var isEditMode = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_scrap)

		androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrap_root)) { v, insets ->
			val systemBars =
				insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		groupListContainer = findViewById(R.id.container_group_list)
		groupDetailContainer = findViewById(R.id.container_group_detail)
		groupRecycler = findViewById(R.id.recycler_groups)
		verseRecycler = findViewById(R.id.recycler_group_verses)
		detailGroupNameText = findViewById(R.id.text_detail_group_name)
		editModeToggle = findViewById(R.id.btn_edit_mode_toggle)

		groupRecycler.layoutManager = LinearLayoutManager(this)
		verseRecycler.layoutManager = LinearLayoutManager(this)

		findViewById<ImageView>(R.id.btn_back_from_list).setOnClickListener { finish() }
		findViewById<ImageView>(R.id.btn_back_from_detail).setOnClickListener { showGroupListScreen() }
		findViewById<TextView>(R.id.btn_manage_groups).setOnClickListener {
			startActivity(Intent(this, ScrapGroupManageActivity::class.java))
		}
		editModeToggle.setOnClickListener { toggleEditMode() }

		onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
			override fun handleOnBackPressed() {
				if (groupDetailContainer.visibility == View.VISIBLE) {
					showGroupListScreen()
				} else {
					finish()
				}
			}
		})

		showGroupListScreen()
	}

	override fun onResume() {
		super.onResume()
		// 그룹 관리 화면에서 돌아왔을 때 그룹/개수가 바뀌었을 수 있으니 갱신
		loadGroups()
		selectedGroup?.let { loadGroupVerses(it) }
	}

	private fun showGroupListScreen() {
		groupListContainer.visibility = View.VISIBLE
		groupDetailContainer.visibility = View.GONE
		selectedGroup = null
		loadGroups()
	}

	private fun showGroupDetailScreen(group: ScrapGroup) {
		groupListContainer.visibility = View.GONE
		groupDetailContainer.visibility = View.VISIBLE
		selectedGroup = group
		isEditMode = false
		editModeToggle.text = "수정"
		detailGroupNameText.text = group.name
		loadGroupVerses(group)
	}

	private fun toggleEditMode() {
		isEditMode = !isEditMode
		editModeToggle.text = if (isEditMode) "완료" else "수정"
		(verseRecycler.adapter as? ScrapVerseAdapter)?.setEditMode(isEditMode)
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val groups = db.scrapDao().getAllGroups()
			val rows = groups.map { group ->
				ScrapGroupRow(group, db.scrapDao().getScrapsByGroup(group.id).size)
			}

			findViewById<TextView>(R.id.text_group_list_empty).visibility =
				if (groups.isEmpty()) View.VISIBLE else View.GONE

			groupRecycler.adapter = ScrapGroupRowAdapter(rows) { group ->
				showGroupDetailScreen(group)
			}
		}
	}

	private fun loadGroupVerses(group: ScrapGroup) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val scraps = db.scrapDao().getScrapsByGroup(group.id)

			findViewById<TextView>(R.id.text_detail_empty).visibility =
				if (scraps.isEmpty()) View.VISIBLE else View.GONE

			verseRecycler.adapter = ScrapVerseAdapter(
				scraps = scraps,
				isEditMode = isEditMode,
				fontSize = AppSettings.getFontSize(this@ScrapActivity),
				onClick = { scrap -> navigateToScrap(scrap) },
				onDelete = { scrap -> confirmDeleteScrap(scrap) }
			)
		}
	}

	private fun navigateToScrap(scrap: Scrap) {
		val intent = Intent()
		intent.putExtra("bookId", scrap.bookId)
		intent.putExtra("chapter", scrap.chapter)
		intent.putExtra("verse", scrap.startVerse)
		setResult(RESULT_OK, intent)
		finish()
	}

	private fun confirmDeleteScrap(scrap: Scrap) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("스크랩 삭제")
			.setMessage("이 스크랩을 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.scrapDao().deleteScrap(scrap)
					selectedGroup?.let { loadGroupVerses(it) }
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}
}