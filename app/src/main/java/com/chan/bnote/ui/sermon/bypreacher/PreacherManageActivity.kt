package com.chan.bnote.ui.sermon.bypreacher

import android.os.Bundle
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.AppSettings
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.preacher.Preacher
import com.chan.bnote.ui.common.DragReorderHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 설교자 관리 + 설교자별 보기를 합친 화면(설교 카테고리 관리와 같은 방식).
 * 평소엔 설교자를 눌러서 그 설교자의 설교 목록(PreacherSermonListActivity)으로 이동하고,
 * "관리"를 누르면 각 항목에 수정/삭제 아이콘과 순서를 바꿀 수 있는 ≡ 손잡이가 나타난다.
 * 정렬은 이름순 같은 옵션 없이 항상 직접 설정(드래그)한 순서를 쓴다.
 */
class PreacherManageActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private lateinit var btnManage: TextView
	private lateinit var btnAdd: TextView

	private var preachers: List<Preacher> = emptyList()
	private var isManageMode = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_preacher_manage)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.preacher_manage_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		recyclerView = findViewById(R.id.recycler_preachers_manage)
		recyclerView.layoutManager = LinearLayoutManager(this)
		btnManage = findViewById(R.id.btn_manage_preacher)
		btnAdd = findViewById(R.id.btn_add_preacher)

		btnManage.setOnClickListener {
			isManageMode = !isManageMode
			btnManage.text = if (isManageMode) "완료" else "관리"
			btnAdd.visibility = if (isManageMode) View.VISIBLE else View.GONE
			renderPreacherList()
		}
		btnAdd.setOnClickListener { showEditDialog(null) }

		loadPreachers()
	}

	override fun onResume() {
		super.onResume()
		loadPreachers()
	}

	private fun loadPreachers() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val allPreachers = db.preacherDao().getAll()
			val customOrderIds = AppSettings.getPreacherCustomOrderIds(this@PreacherManageActivity)
			val byId = allPreachers.associateBy { it.id }
			val ordered = customOrderIds.mapNotNull { byId[it] }
			val rest = allPreachers.filter { it.id !in customOrderIds }
			preachers = ordered + rest

			if (preachers.isEmpty()) {
				isManageMode = false
				btnManage.visibility = View.GONE
				btnAdd.visibility = View.VISIBLE
			} else {
				btnManage.visibility = View.VISIBLE
				btnAdd.visibility = if (isManageMode) View.VISIBLE else View.GONE
			}

			renderPreacherList()
		}
	}

	private fun renderPreacherList() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val rows = preachers.map { preacher ->
				PreacherRow(preacher, db.sermonDao().getByPreacherId(preacher.id).size)
			}

			var dragHelper: ItemTouchHelper? = null

			val adapter = PreacherManageAdapter(
				initialRows = rows,
				isEditMode = isManageMode,
				onClick = { preacher ->
					startActivity(
						PreacherSermonListActivity.createIntent(
							this@PreacherManageActivity, preacher.id, preacher.name
						)
					)
				},
				onEdit = { preacher -> showEditDialog(preacher) },
				onDelete = { preacher -> confirmDelete(preacher) },
				onStartDrag = { holder -> dragHelper?.startDrag(holder) }
			)
			recyclerView.adapter = adapter

			if (isManageMode) {
				dragHelper = ItemTouchHelper(
					DragReorderHelper(
						onMove = { from, to -> adapter.moveItem(from, to) },
						onDragFinished = {
							AppSettings.setPreacherCustomOrderIds(
								this@PreacherManageActivity, adapter.currentOrderIds()
							)
						}
					)
				)
				dragHelper?.attachToRecyclerView(recyclerView)
			}
		}
	}

	private fun showEditDialog(existing: Preacher?) {
		val editText = EditText(this).apply {
			hint = "설교자 이름"
			setText(existing?.name ?: "")
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background =
				ContextCompat.getDrawable(this@PreacherManageActivity, R.drawable.bg_book_button)
		}
		val container = FrameLayout(this).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}

		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle(if (existing == null) "설교자 추가" else "설교자 이름 수정")
			.setView(container)
			.setPositiveButton(if (existing == null) "추가" else "저장") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isEmpty()) return@setPositiveButton
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					if (existing == null) {
						db.preacherDao().insert(Preacher(name = name, sortOrder = preachers.size))
					} else {
						db.preacherDao().update(existing.copy(name = name))
					}
					loadPreachers()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun confirmDelete(preacher: Preacher) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("설교자 삭제")
			.setMessage("'${preacher.name}'을(를) 삭제할까요? 이 설교자로 등록된 설교는 '미지정' 상태가 돼요.")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.preacherDao().clearPreacherFromSermons(preacher.id)
					db.preacherDao().delete(preacher)
					loadPreachers()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}