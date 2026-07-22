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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

/** 설교를 카테고리별로 모아보는 화면. 설교 탭 메뉴(≡) > 카테고리별 보기에서 연다. */
class SermonByCategoryActivity : AppCompatActivity() {

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

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_sermon_by_category)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sermon_by_category_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "카테고리별 보기"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		categoryListContainer = findViewById(R.id.container_category_list)
		sermonListContainer = findViewById(R.id.container_category_sermons)
		categoryRecycler = findViewById(R.id.recycler_categories)
		sermonRecycler = findViewById(R.id.recycler_category_sermons)
		selectedCategoryText = findViewById(R.id.text_selected_category)
		emptyText = findViewById(R.id.text_empty_category_sermons)

		categoryRecycler.layoutManager = LinearLayoutManager(this)
		sermonRecycler.layoutManager = LinearLayoutManager(this)

		findViewById<ImageView>(R.id.btn_back_from_category_detail).setOnClickListener {
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
			val db = BibleDatabase.getInstance(applicationContext)
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
						0xFFFFFF and ContextCompat.getColor(
							this@SermonByCategoryActivity, R.color.category_none
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
			val db = BibleDatabase.getInstance(applicationContext)
			val allSermons = db.sermonDao().getAll()
			val filtered = allSermons.filter { it.categoryId == category?.id }
				.sortedByDescending { it.sermonDate }

			emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
			sermonRecycler.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

			val rows = SermonRowBuilder.build(db, filtered, useDateLabel = true)
			sermonRecycler.adapter = SermonRowAdapter(rows) { sermon ->
				sermonDetailLauncher.launch(
					SermonDetailActivity.createIntent(
						this@SermonByCategoryActivity,
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