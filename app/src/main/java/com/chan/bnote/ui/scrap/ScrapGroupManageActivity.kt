package com.chan.bnote.ui.scrap

import android.app.AlertDialog
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.scrap.ScrapGroup
import kotlinx.coroutines.launch

class ScrapGroupManageActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private var groups: List<ScrapGroup> = emptyList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_scrap_group_manage)

		androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.group_manage_root)) { v, insets ->
			val systemBars =
				insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
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
			groups = db.scrapDao().getAllGroups()
			recyclerView.adapter = ScrapGroupManageAdapter(
				groups = groups,
				onEdit = { group -> showRenameDialog(group) },
				onDelete = { group -> confirmDelete(group) }
			)
		}
	}

	private fun showAddDialog() {
		val editText = EditText(this).apply {
			hint = "그룹 이름"
			setPadding(48, 32, 48, 32)
		}
		AlertDialog.Builder(this)
			.setTitle("새 그룹 추가")
			.setView(editText)
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

	private fun showRenameDialog(group: ScrapGroup) {
		val editText = EditText(this).apply {
			setText(group.name)
			setPadding(48, 32, 48, 32)
		}
		AlertDialog.Builder(this)
			.setTitle("그룹 이름 수정")
			.setView(editText)
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

	private fun confirmDelete(group: ScrapGroup) {
		AlertDialog.Builder(this)
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
}