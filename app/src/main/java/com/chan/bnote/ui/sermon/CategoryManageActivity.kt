package com.chan.bnote.ui.sermon

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory
import com.chan.bnote.ui.common.ColorPickerBottomSheet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class CategoryManageActivity : AppCompatActivity() {

	private lateinit var recyclerView: RecyclerView
	private var categories: List<SermonCategory> = emptyList()

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

		findViewById<TextView>(R.id.btn_add_category).setOnClickListener {
			showEditDialog(null)
		}

		loadCategories()
	}

	private fun loadCategories() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			categories = db.sermonCategoryDao().getAll()
			recyclerView.adapter = CategoryAdapter(
				categories,
				onClick = { showEditDialog(it) },
				onLongClick = { confirmDelete(it) }
			)
		}
	}

	private fun showEditDialog(existing: SermonCategory?) {
		val editText = EditText(this).apply {
			hint = "카테고리 이름"
			setText(existing?.name ?: "")
			setPadding(48, 32, 48, 32)
			textSize = 15f
			background = androidx.core.content.ContextCompat.getDrawable(
				this@CategoryManageActivity,
				R.drawable.bg_book_button
			)
		}
		val container = android.widget.FrameLayout(this).apply {
			setPadding(dp(24), dp(16), dp(24), dp(0))
			addView(editText)
		}
		var selectedColor = existing?.colorHex ?: ColorPickerBottomSheet.palette.first()

		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle(if (existing == null) "카테고리 추가" else "카테고리 수정")
			.setView(container)
			.setPositiveButton("색상 선택 및 저장") { _, _ ->
				val colorPicker = ColorPickerBottomSheet()
				colorPicker.onColorSelected = { color ->
					selectedColor = color
					saveCategory(existing, editText.text.toString().trim(), selectedColor)
				}
				colorPicker.show(supportFragmentManager, "color_picker")
			}
			.setNegativeButton("취소", null)
			.show()
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

	private fun confirmDelete(category: SermonCategory) {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("카테고리 삭제")
			.setMessage("'${category.name}'을(를) 삭제할까요? 이 카테고리를 쓰던 설교는 카테고리 미지정 상태가 돼요.")
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

	private class CategoryAdapter(
		private val items: List<SermonCategory>,
		private val onClick: (SermonCategory) -> Unit,
		private val onLongClick: (SermonCategory) -> Unit
	) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val dot: View = view.findViewById(R.id.color_dot)
			val name: TextView = view.findViewById(R.id.text_category_name)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view =
				LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val category = items[position]
			val drawable = GradientDrawable()
			drawable.shape = GradientDrawable.OVAL
			drawable.setColor(Color.parseColor(category.colorHex))
			holder.dot.background = drawable
			holder.name.text = category.name

			holder.itemView.setOnClickListener { onClick(category) }
			holder.itemView.setOnLongClickListener { onLongClick(category); true }
		}

		override fun getItemCount() = items.size
	}
}