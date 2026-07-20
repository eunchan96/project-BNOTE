package com.chan.bnote.ui.mypage.memorization

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import com.chan.bnote.ui.sermon.addsermon.BibleRangePickerBottomSheet
import kotlinx.coroutines.launch

class MemorizationVerseListActivity : AppCompatActivity() {

	private lateinit var groupListContainer: View
	private lateinit var groupDetailContainer: View
	private lateinit var groupRecycler: RecyclerView
	private lateinit var verseRecycler: RecyclerView
	private lateinit var detailGroupNameText: TextView

	private var selectedGroup: MemorizationGroup? = null

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
		findViewById<TextView>(R.id.btn_manage_groups).setOnClickListener {
			startActivity(android.content.Intent(this, MemorizationGroupManageActivity::class.java))
		}
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

	private fun showGroupDetailScreen(group: MemorizationGroup) {
		groupListContainer.visibility = View.GONE
		groupDetailContainer.visibility = View.VISIBLE
		selectedGroup = group
		detailGroupNameText.text = group.name
		loadGroupVerses(group)
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val groups = db.memorizationVerseDao().getAllGroups()
			val rows = groups.map { group ->
				MemorizationGroupRow(group, db.memorizationVerseDao().getByGroup(group.id).size)
			}

			findViewById<TextView>(R.id.text_group_list_empty).visibility =
				if (groups.isEmpty()) View.VISIBLE else View.GONE

			groupRecycler.adapter = MemorizationGroupRowAdapter(rows) { group ->
				showGroupDetailScreen(group)
			}
		}
	}

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