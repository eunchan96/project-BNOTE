package com.chan.bnote.ui.sermon.category

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
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory
import com.chan.bnote.ui.common.ColorPickerBottomSheet
import com.chan.bnote.ui.common.DragReorderHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 설교 카테고리 관리 + 카테고리별 보기를 합친 화면.
 * 평소엔 카테고리를 눌러서 그 카테고리의 설교 목록 화면(CategorySermonListActivity)으로 이동하고,
 * "관리"를 누르면 각 항목에 수정/삭제 아이콘과 순서를 바꿀 수 있는 ≡ 손잡이가 나타난다.
 */
class CategoryManageActivity : AppCompatActivity() {

	data class CategoryRow(val category: SermonCategory?, val count: Int)

	private lateinit var recyclerView: RecyclerView
	private lateinit var btnManage: TextView
	private lateinit var btnAdd: TextView

	private var categories: List<SermonCategory> = emptyList()
	private var isManageMode = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_category_manage)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.category_manage_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		recyclerView = findViewById(R.id.recycler_categories)
		recyclerView.layoutManager = LinearLayoutManager(this)
		btnManage = findViewById(R.id.btn_manage_category)
		btnAdd = findViewById(R.id.btn_add_category)

		btnManage.setOnClickListener {
			isManageMode = !isManageMode
			btnManage.text = if (isManageMode) "완료" else "관리"
			btnAdd.visibility = if (isManageMode) View.VISIBLE else View.GONE
			renderCategoryList()
		}
		btnAdd.setOnClickListener { showEditDialog(null) }

		loadCategories()
	}

	override fun onResume() {
		super.onResume()
		loadCategories()
	}

	private fun loadCategories() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			categories = db.sermonCategoryDao().getAll()

			// 실제 카테고리가 없으면(미분류만 있는 상태) 관리 모드 자체가 의미 없으니 추가 버튼만 보여준다.
			if (categories.isEmpty()) {
				isManageMode = false
				btnManage.visibility = View.GONE
				btnAdd.visibility = View.VISIBLE
			} else {
				btnManage.visibility = View.VISIBLE
				btnAdd.visibility = if (isManageMode) View.VISIBLE else View.GONE
			}

			renderCategoryList()
		}
	}

	private fun renderCategoryList() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val allSermons = db.sermonDao().getAll()

			val rows = mutableListOf<CategoryRow>()
			for (category in categories) {
				rows.add(CategoryRow(category, allSermons.count { it.categoryId == category.id }))
			}
			rows.add(CategoryRow(null, allSermons.count { it.categoryId == null }))

			val adapter = CategoryManageAdapter(
				initialRows = rows,
				isEditMode = isManageMode,
				onClick = { category ->
					startActivity(
						CategorySermonListActivity.createIntent(
							this@CategoryManageActivity,
							category?.id,
							category?.name ?: "미분류"
						)
					)
				},
				onEdit = { category -> showEditDialog(category) },
				onDelete = { category ->
					confirmDelete(
						category,
						rows.first { it.category?.id == category.id }.count
					)
				}
			)
			recyclerView.adapter = adapter

			if (isManageMode) {
				// 드래그 중엔 목록을 다시 안 불러오고 어댑터 안에서만 옮기다가, 손을 뗀 순간에만 저장한다.
				val dragHelper = ItemTouchHelper(
					DragReorderHelper(
						onMove = { from, to -> adapter.moveItem(from, to) },
						onDragFinished = {
							lifecycleScope.launch {
								val db2 = BibleDatabase.getInstance(applicationContext)
								adapter.currentCategoryOrder().forEachIndexed { index, category ->
									if (category.sortOrder != index) {
										db2.sermonCategoryDao()
											.update(category.copy(sortOrder = index))
									}
								}
								categories = db2.sermonCategoryDao().getAll()
							}
						}
					)
				)
				dragHelper.attachToRecyclerView(recyclerView)
			}
		}
	}

	private fun showEditDialog(existing: SermonCategory?) {
		val editText = EditText(this).apply {
			hint = "카테고리 이름"
			setText(existing?.name ?: "")
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background =
				ContextCompat.getDrawable(this@CategoryManageActivity, R.drawable.bg_book_button)
		}
		val container = FrameLayout(this).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}

		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle(if (existing == null) "카테고리 추가" else "카테고리 수정")
			.setView(container)
			.setPositiveButton("색상 선택") { _, _ ->
				val name = editText.text.toString().trim()
				if (name.isEmpty()) return@setPositiveButton
				val colorPicker = ColorPickerBottomSheet()
				colorPicker.onColorSelected = { color ->
					if (existing != null) {
						confirmEdit(existing, name, color)
					} else {
						saveCategory(null, name, color)
					}
				}
				colorPicker.show(supportFragmentManager, "color_picker")
			}
			.setNegativeButton("취소", null)
			.show()
	}

	/** 이미 있는 카테고리를 수정할 땐, 그 카테고리를 쓰는 설교에도 영향이 간다는 걸 미리 알려준다. */
	private fun confirmEdit(existing: SermonCategory, name: String, colorHex: String) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val count = db.sermonDao().getAll().count { it.categoryId == existing.id }
			val message = if (count > 0) {
				"이 카테고리를 쓰는 설교 ${count}개에도 변경 내용이 반영돼요. 계속할까요?"
			} else {
				"저장할까요?"
			}
			MaterialAlertDialogBuilder(
				this@CategoryManageActivity,
				R.style.ThemeOverlay_BNOTE_Dialog
			)
				.setTitle("카테고리 수정")
				.setMessage(message)
				.setPositiveButton("저장") { _, _ -> saveCategory(existing, name, colorHex) }
				.setNegativeButton("취소", null)
				.show()
		}
	}

	private fun saveCategory(existing: SermonCategory?, name: String, colorHex: String) {
		if (name.isBlank()) return
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			if (existing == null) {
				db.sermonCategoryDao().insert(
					SermonCategory(name = name, colorHex = colorHex, sortOrder = categories.size)
				)
			} else {
				db.sermonCategoryDao().update(existing.copy(name = name, colorHex = colorHex))
			}
			loadCategories()
		}
	}

	private fun confirmDelete(category: SermonCategory, sermonCount: Int) {
		val message = if (sermonCount > 0) {
			"이 카테고리를 쓰는 설교 ${sermonCount}개가 모두 미분류로 바뀌어요. 삭제할까요?"
		} else {
			"'${category.name}'을(를) 삭제할까요?"
		}
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("카테고리 삭제")
			.setMessage(message)
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.sermonCategoryDao().delete(category)
					loadCategories()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

}