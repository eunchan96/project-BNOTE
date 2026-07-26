package com.chan.bnote.ui.mypage.memorization

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
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
import com.chan.bnote.data.mypage.memorization.MemorizationGroup
import com.chan.bnote.data.mypage.memorization.MemorizationVerse
import com.chan.bnote.data.sermon.SermonBibleRef
import com.chan.bnote.ui.bible.picker.BibleRangePickerBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MemorizationVerseListActivity : AppCompatActivity() {

	private lateinit var groupListContainer: View
	private lateinit var groupDetailContainer: View
	private lateinit var groupRecycler: RecyclerView
	private lateinit var verseRecycler: RecyclerView
	private lateinit var detailGroupNameText: TextView
	private lateinit var btnManageGroups: TextView
	private lateinit var btnAddGroup: TextView

	private var selectedGroup: MemorizationGroup? = null
	private var isGroupManageMode = false
	private var groups: List<MemorizationGroup> = emptyList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_verse_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.memorization_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		groupListContainer = findViewById(R.id.container_group_list)
		groupDetailContainer = findViewById(R.id.container_group_detail)
		groupRecycler = findViewById(R.id.recycler_groups)
		verseRecycler = findViewById(R.id.recycler_group_verses)
		detailGroupNameText = findViewById(R.id.text_detail_group_name)

		groupRecycler.layoutManager = LinearLayoutManager(this)
		verseRecycler.layoutManager = LinearLayoutManager(this)

		findViewById<ImageView>(R.id.btn_back_from_list).setOnClickListener { finish() }
		findViewById<ImageView>(R.id.btn_back_from_detail).setOnClickListener { showGroupListScreen() }
		findViewById<TextView>(R.id.btn_practice).setOnClickListener {
			startActivity(MemorizationPracticeActivity.allVersesIntent(this))
		}
		btnManageGroups = findViewById(R.id.btn_manage_groups)
		btnAddGroup = findViewById(R.id.btn_add_group)
		btnManageGroups.setOnClickListener { toggleGroupManageMode() }
		btnAddGroup.setOnClickListener { showAddGroupDialog() }
		findViewById<TextView>(R.id.btn_add_verse).setOnClickListener { showAddPicker() }
		findViewById<TextView>(R.id.btn_practice_group).setOnClickListener {
			val group = selectedGroup ?: return@setOnClickListener
			startActivity(MemorizationPracticeActivity.groupIntent(this, group.id))
		}

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

	private fun showGroupDetailScreen(group: MemorizationGroup) {
		groupListContainer.visibility = View.GONE
		groupDetailContainer.visibility = View.VISIBLE
		selectedGroup = group
		detailGroupNameText.text = group.name
		loadGroupVerses(group)
	}

	private fun toggleGroupManageMode() {
		isGroupManageMode = !isGroupManageMode
		btnManageGroups.text = if (isGroupManageMode) "완료" else "관리"
		btnAddGroup.visibility = if (isGroupManageMode) View.VISIBLE else View.GONE
		(groupRecycler.adapter as? MemorizationGroupRowAdapter)?.setEditMode(isGroupManageMode)
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			groups = db.memorizationVerseDao().getAllGroups()
			val rows = groups.map { group ->
				MemorizationGroupRow(group, db.memorizationVerseDao().getByGroup(group.id).size)
			}

			findViewById<TextView>(R.id.text_group_list_empty).visibility =
				if (groups.isEmpty()) View.VISIBLE else View.GONE

			groupRecycler.adapter = MemorizationGroupRowAdapter(
				rows = rows,
				isEditMode = isGroupManageMode,
				onClick = { group -> showGroupDetailScreen(group) },
				onEdit = { group -> showRenameGroupDialog(group) },
				onDelete = { group -> confirmDeleteGroup(group) }
			)
		}
	}

	private fun showAddGroupDialog() {
		val editText = buildDialogEditText("그룹 이름")
		val container = wrapInDialogContainer(editText)
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("새 그룹 추가")
			.setView(container)
			.setPositiveButton("추가") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.memorizationVerseDao()
							.insertGroup(MemorizationGroup(name = name, sortOrder = groups.size))
						loadGroups()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun showRenameGroupDialog(group: MemorizationGroup) {
		val editText = buildDialogEditText("그룹 이름").apply { setText(group.name) }
		val container = wrapInDialogContainer(editText)
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("그룹 이름 수정")
			.setView(container)
			.setPositiveButton("저장") { _, _ ->
				val newName = editText.text.toString().trim()
				if (newName.isNotEmpty()) {
					lifecycleScope.launch {
						val db = BibleDatabase.getInstance(applicationContext)
						db.memorizationVerseDao().updateGroup(group.copy(name = newName))
						loadGroups()
					}
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun confirmDeleteGroup(group: MemorizationGroup) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("그룹 삭제")
			.setMessage("'${group.name}' 그룹과 그 안의 암송 구절이 전부 삭제돼요. 계속할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.memorizationVerseDao().deleteByGroup(group.id)
					db.memorizationVerseDao().deleteGroup(group)
					loadGroups()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun buildDialogEditText(hintText: String): EditText {
		return EditText(this).apply {
			hint = hintText
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = ContextCompat.getDrawable(
				this@MemorizationVerseListActivity,
				R.drawable.bg_book_button
			)
		}
	}

	private fun wrapInDialogContainer(view: EditText): FrameLayout {
		return FrameLayout(this).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(view)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

	private fun loadGroupVerses(group: MemorizationGroup) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val verses = db.memorizationVerseDao().getByGroup(group.id)

			findViewById<TextView>(R.id.text_detail_empty).visibility =
				if (verses.isEmpty()) View.VISIBLE else View.GONE

			verseRecycler.adapter = MemorizationVerseAdapter(
				items = verses,
				onClick = { item ->
					startActivity(
						MemorizationVerseDetailActivity.createIntent(
							this@MemorizationVerseListActivity,
							item.id
						)
					)
				}
			)
		}
	}

	private fun showAddPicker() {
		val group = selectedGroup ?: return
		val rangePicker = BibleRangePickerBottomSheet()
		rangePicker.onRangeSelected = { ref -> addVerse(group, ref) }
		rangePicker.show(supportFragmentManager, "memorization_verse_picker")
	}

	private fun addVerse(group: MemorizationGroup, ref: SermonBibleRef) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val alreadyExists = db.memorizationVerseDao().existsCount(
				ref.startBookId, ref.startChapter, ref.startVerse,
				ref.endBookId, ref.endChapter, ref.endVerse
			) > 0
			if (alreadyExists) {
				Toast.makeText(
					this@MemorizationVerseListActivity,
					"이미 등록된 구절이에요",
					Toast.LENGTH_SHORT
				).show()
				return@launch
			}

			val verseText = buildVerseText(ref)
			db.memorizationVerseDao().insert(
				MemorizationVerse(
					groupId = group.id,
					startBookId = ref.startBookId,
					startChapter = ref.startChapter,
					startVerse = ref.startVerse,
					endBookId = ref.endBookId,
					endChapter = ref.endChapter,
					endVerse = ref.endVerse,
					verseText = verseText
				)
			)
			loadGroupVerses(group)
		}
	}

	private suspend fun buildVerseText(ref: SermonBibleRef): String {
		val db = BibleDatabase.getInstance(applicationContext)
		val parts = mutableListOf<String>()
		for (chapter in ref.startChapter..ref.endChapter) {
			val verses = db.bibleDao().getVerses("NKRV", ref.startBookId, chapter)
			val filtered = verses.filter { v ->
				when {
					ref.startChapter == ref.endChapter -> v.verse in ref.startVerse..ref.endVerse
					chapter == ref.startChapter -> v.verse >= ref.startVerse
					chapter == ref.endChapter -> v.verse <= ref.endVerse
					else -> true
				}
			}
			parts.addAll(filtered.map { it.text })
		}
		// 구절이 여러 개면 구절 단위로 줄바꿈해서 저장한다 (단일 구절이면 그냥 한 줄).
		return parts.joinToString("\n")
	}
}