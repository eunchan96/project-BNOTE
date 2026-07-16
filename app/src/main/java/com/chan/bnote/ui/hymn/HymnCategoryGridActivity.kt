package com.chan.bnote.ui.hymn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.hymn.HymnCategory
import kotlinx.coroutines.launch

class HymnCategoryGridActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_PARENT_ID = "extra_parent_id"
		private const val EXTRA_PARENT_NAME = "extra_parent_name"

		/** 대분류 그리드 (최상위). */
		fun startMajor(context: Context) {
			context.startActivity(Intent(context, HymnCategoryGridActivity::class.java))
		}

		/** 특정 대분류 안의 소분류 그리드. */
		private fun startMinor(context: Context, majorId: Long, majorName: String) {
			val intent = Intent(context, HymnCategoryGridActivity::class.java)
			intent.putExtra(EXTRA_PARENT_ID, majorId)
			intent.putExtra(EXTRA_PARENT_NAME, majorName)
			context.startActivity(intent)
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_hymn_category_grid)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.hymn_category_grid_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val parentId = intent.getLongExtra(EXTRA_PARENT_ID, -1L)
		val parentName = intent.getStringExtra(EXTRA_PARENT_NAME)
		val isMinorLevel = parentId != -1L

		findViewById<TextView>(R.id.text_top_bar_title).text = parentName ?: "찬송 분류"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_hymn_categories)
		recyclerView.layoutManager = GridLayoutManager(this, 3)

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)

			if (isMinorLevel) {
				val minors = db.hymnDao().getMinorCategories(parentId)
				val cells = minors.map { minor ->
					val range = db.hymnDao().getRangeForMinorCategory(minor.id)
					CategoryCell(minor, formatRangeLabel(range))
				}
				recyclerView.adapter = HymnCategoryAdapter(cells) { minor ->
					HymnListActivity.startForCategory(
						this@HymnCategoryGridActivity,
						minor.id,
						minor.name
					)
				}
			} else {
				val majors = db.hymnDao().getMajorCategories()
				val cells = majors.map { major ->
					val range = db.hymnDao().getRangeForMajorCategory(major.id)
					CategoryCell(major, formatRangeLabel(range))
				}
				recyclerView.adapter = HymnCategoryAdapter(cells) { major ->
					lifecycleScope.launch {
						val minors = db.hymnDao().getMinorCategories(major.id)
						if (minors.size == 1) {
							// 소분류가 하나뿐이면 (이름도 대분류와 동일) 바로 찬송 목록으로 이동
							HymnListActivity.startForCategory(
								this@HymnCategoryGridActivity, minors[0].id, minors[0].name
							)
						} else {
							startMinor(this@HymnCategoryGridActivity, major.id, major.name)
						}
					}
				}
			}
		}
	}

	private fun formatRangeLabel(range: com.chan.bnote.data.hymn.HymnNumberRange?): String {
		if (range == null) return ""
		return if (range.minNumber == range.maxNumber) {
			"(${range.minNumber})"
		} else {
			"(${range.minNumber}~${range.maxNumber})"
		}
	}
}

private data class CategoryCell(
	val category: HymnCategory,
	val rangeLabel: String
)

private class HymnCategoryAdapter(
	private val cells: List<CategoryCell>,
	private val onClick: (HymnCategory) -> Unit
) : RecyclerView.Adapter<HymnCategoryAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val name: TextView = view.findViewById(R.id.text_category_name)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context)
			.inflate(R.layout.item_hymn_category_cell, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = cells.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val cell = cells[position]
		holder.name.text = if (cell.rangeLabel.isNotEmpty()) {
			"${cell.category.name}\n${cell.rangeLabel}"
		} else {
			cell.category.name
		}
		holder.itemView.setOnClickListener { onClick(cell.category) }
	}
}