package com.chan.bnote.ui.sermon.bycategory

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.sermon.sermoncategory.SermonCategory
import com.chan.bnote.ui.sermon.SermonRowAdapter
import com.chan.bnote.ui.sermon.SermonRowBuilder
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import kotlinx.coroutines.launch

/** 설교를 카테고리별로 모아보는 화면 (설교 탭의 하위 탭 중 하나). */
class SermonByCategoryFragment : Fragment() {

	private data class CategoryRow(val category: SermonCategory?, val count: Int)

	private lateinit var categoryListContainer: View
	private lateinit var sermonListContainer: View
	private lateinit var categoryRecycler: RecyclerView
	private lateinit var sermonRecycler: RecyclerView
	private lateinit var selectedCategoryText: TextView
	private lateinit var emptyText: TextView

	private var selectedCategory: SermonCategory? = null

	private val sermonDetailLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadCategories()
			selectedCategory?.let { loadSermonsForCategory(it) }
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_by_category, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		categoryListContainer = view.findViewById(R.id.container_category_list)
		sermonListContainer = view.findViewById(R.id.container_category_sermons)
		categoryRecycler = view.findViewById(R.id.recycler_categories)
		sermonRecycler = view.findViewById(R.id.recycler_category_sermons)
		selectedCategoryText = view.findViewById(R.id.text_selected_category)
		emptyText = view.findViewById(R.id.text_empty_category_sermons)

		categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
		sermonRecycler.layoutManager = LinearLayoutManager(requireContext())

		view.findViewById<ImageView>(R.id.btn_back_from_category_detail).setOnClickListener {
			categoryListContainer.visibility = View.VISIBLE
			sermonListContainer.visibility = View.GONE
			selectedCategory = null
		}

		loadCategories()
	}

	override fun onResume() {
		super.onResume()
		loadCategories()
		selectedCategory?.let { loadSermonsForCategory(it) }
	}

	private fun loadCategories() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val categories = db.sermonCategoryDao().getAll()
			val allSermons = db.sermonDao().getAll()

			val rows = mutableListOf<CategoryRow>()
			for (category in categories) {
				val count = allSermons.count { it.categoryId == category.id }
				rows.add(CategoryRow(category, count))
			}
			val uncategorizedCount = allSermons.count { it.categoryId == null }
			rows.add(CategoryRow(null, uncategorizedCount))

			categoryRecycler.adapter = object : RecyclerView.Adapter<CategoryViewHolder>() {
				override fun onCreateViewHolder(
					parent: ViewGroup,
					viewType: Int
				): CategoryViewHolder {
					val v = LayoutInflater.from(parent.context)
						.inflate(R.layout.item_category_row_with_count, parent, false)
					return CategoryViewHolder(v)
				}

				override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
					val row = rows[position]
					val drawable = GradientDrawable()
					drawable.shape = GradientDrawable.OVAL
					val colorHex = row.category?.colorHex ?: String.format(
						"#%06X",
						0xFFFFFF and androidx.core.content.ContextCompat.getColor(
							requireContext(), R.color.category_none
						)
					)
					drawable.setColor(Color.parseColor(colorHex))
					holder.dot.background = drawable
					holder.name.text = row.category?.name ?: "미분류"
					holder.count.text = "${row.count}개"
					holder.itemView.setOnClickListener {
						selectedCategory = row.category
						categoryListContainer.visibility = View.GONE
						sermonListContainer.visibility = View.VISIBLE
						selectedCategoryText.text = row.category?.name ?: "미분류"
						loadSermonsForCategory(row.category)
					}
				}

				override fun getItemCount() = rows.size
			}
		}
	}

	private fun loadSermonsForCategory(category: SermonCategory?) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val allSermons = db.sermonDao().getAll()
			val filtered = allSermons.filter { it.categoryId == category?.id }
				.sortedByDescending { it.sermonDate }

			emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
			sermonRecycler.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

			val rows = SermonRowBuilder.build(db, filtered, useDateLabel = true)
			sermonRecycler.adapter = SermonRowAdapter(rows) { sermon ->
				sermonDetailLauncher.launch(
					SermonDetailActivity.createIntent(
						requireContext(),
						sermon.id
					)
				)
			}
		}
	}

	class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val dot: View = view.findViewById(R.id.color_dot)
		val name: TextView = view.findViewById(R.id.text_category_name)
		val count: TextView = view.findViewById(R.id.text_category_count)
	}
}