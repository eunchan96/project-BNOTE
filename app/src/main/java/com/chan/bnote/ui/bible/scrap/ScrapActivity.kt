package com.chan.bnote.ui.bible.scrap

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.scrap.Scrap
import com.chan.bnote.data.bible.scrap.ScrapGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ScrapActivity : AppCompatActivity() {

	private lateinit var groupListContainer: View
	private lateinit var groupDetailContainer: View
	private lateinit var groupRecycler: RecyclerView
	private lateinit var verseRecycler: RecyclerView
	private lateinit var detailGroupNameText: TextView
	private lateinit var editModeToggle: TextView
	private lateinit var btnManageGroups: TextView
	private lateinit var btnAddGroup: TextView

	private var selectedGroup: ScrapGroup? = null
	private var isEditMode = false
	private var isGroupManageMode = false
	private var groups: List<ScrapGroup> = emptyList()

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
		btnManageGroups = findViewById(R.id.btn_manage_groups)
		btnAddGroup = findViewById(R.id.btn_add_group)

		groupRecycler.layoutManager = LinearLayoutManager(this)
		verseRecycler.layoutManager = LinearLayoutManager(this)

		findViewById<ImageView>(R.id.btn_back_from_list).setOnClickListener { finish() }
		findViewById<ImageView>(R.id.btn_back_from_detail).setOnClickListener { showGroupListScreen() }
		btnManageGroups.setOnClickListener { toggleGroupManageMode() }
		btnAddGroup.setOnClickListener { showAddGroupDialog() }
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
		selectedGroup?.let { loadGroupVerses(it) }
	}

	private fun showGroupListScreen() {
		groupListContainer.visibility = View.VISIBLE
		groupDetailContainer.visibility = View.GONE
		selectedGroup = null
		isGroupManageMode = false
		btnManageGroups.text = "관리"
		btnAddGroup.visibility = View.GONE
		loadGroups()
	}

	private fun showGroupDetailScreen(group: ScrapGroup) {
		groupListContainer.visibility = View.GONE
		groupDetailContainer.visibility = View.VISIBLE
		selectedGroup = group
		isEditMode = false
		editModeToggle.text = "관리"
		detailGroupNameText.text = group.name
		loadGroupVerses(group)
	}

	private fun toggleEditMode() {
		isEditMode = !isEditMode
		editModeToggle.text = if (isEditMode) "완료" else "관리"
		(verseRecycler.adapter as? ScrapVerseAdapter)?.setEditMode(isEditMode)
	}

	private fun toggleGroupManageMode() {
		isGroupManageMode = !isGroupManageMode
		btnManageGroups.text = if (isGroupManageMode) "완료" else "관리"
		btnAddGroup.visibility = if (isGroupManageMode) View.VISIBLE else View.GONE
		(groupRecycler.adapter as? ScrapGroupRowAdapter)?.setEditMode(isGroupManageMode)
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			groups = db.scrapDao().getAllGroups()
			val rows = groups.map { group ->
				ScrapGroupRow(group, db.scrapDao().getScrapsByGroup(group.id).size)
			}

			findViewById<TextView>(R.id.text_group_list_empty).visibility =
				if (groups.isEmpty()) View.VISIBLE else View.GONE

			groupRecycler.adapter = ScrapGroupRowAdapter(
				rows = rows,
				isEditMode = isGroupManageMode,
				onClick = { group -> showGroupDetailScreen(group) },
				onEdit = { group -> showRenameGroupDialog(group) },
				onDelete = { group -> confirmDeleteGroup(group) }
			)
		}
	}

	private fun showAddGroupDialog() {
		val editText = EditText(this).apply {
			hint = "그룹 이름"
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = ContextCompat.getDrawable(this@ScrapActivity, R.drawable.bg_book_button)
		}
		val container = FrameLayout(this).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("새 그룹 추가")
			.setView(container)
			.setPositiveButton("추가") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.scrapDao().insertGroup(ScrapGroup(name = name, sortOrder = groups.size))
						loadGroups()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun showRenameGroupDialog(group: ScrapGroup) {
		val editText = EditText(this).apply {
			setText(group.name)
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = ContextCompat.getDrawable(this@ScrapActivity, R.drawable.bg_book_button)
		}
		val container = FrameLayout(this).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("그룹 이름 수정")
			.setView(container)
			.setPositiveButton("저장") { _, _ ->
				val newName = editText.text.toString().trim()
				if (newName.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.scrapDao().updateGroup(group.copy(name = newName))
						loadGroups()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun confirmDeleteGroup(group: ScrapGroup) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("그룹 삭제")
			.setMessage("'${group.name}' 그룹과 그 안의 스크랩이 전부 삭제돼요. 계속할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.scrapDao().deleteScrapsByGroup(group.id)
					db.scrapDao().deleteGroup(group)
					loadGroups()
				}
			}
			.setNegativeButton("취소", null)
			.show()
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

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}