package com.chan.bnote.ui.mypage

import android.os.Bundle
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
import com.chan.bnote.data.mypage.MemorizationGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class MemorizationGroupManageActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private var groups: List<MemorizationGroup> = emptyList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_memorization_group_manage)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.group_manage_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		recyclerView = findViewById(R.id.recycler_group_manage)
		recyclerView.layoutManager = LinearLayoutManager(this)

		findViewById<ImageView>(R.id.btn_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.btn_add_group).setOnClickListener { showAddDialog() }

		loadGroups()
	}

	private fun loadGroups() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			groups = db.memorizationVerseDao().getAllGroups()
			recyclerView.adapter = MemorizationGroupManageAdapter(
				groups = groups,
				onEdit = { group -> showRenameDialog(group) },
				onDelete = { group -> confirmDelete(group) }
			)
		}
	}

	private fun showAddDialog() {
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

	private fun showRenameDialog(group: MemorizationGroup) {
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

	private fun confirmDelete(group: MemorizationGroup) {
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
				this@MemorizationGroupManageActivity,
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
}