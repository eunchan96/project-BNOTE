package com.chan.bnote.ui.application.category

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
import com.chan.bnote.data.application.ApplicationCategory
import com.chan.bnote.ui.application.ApplicationDetailActivity
import com.chan.bnote.ui.application.ApplicationRowAdapter
import com.chan.bnote.ui.application.ApplicationRowBuilder
import kotlinx.coroutines.launch

/** "적용" 화면의 카테고리 서브탭. 카테고리 목록을 먼저 보여주고, 누르면 그 카테고리의 적용 목록으로. */
class CategoryApplicationFragment : Fragment() {

	private lateinit var listContainer: View
	private lateinit var detailContainer: View
	private lateinit var categoryRecycler: RecyclerView
	private lateinit var applicationRecycler: RecyclerView
	private lateinit var detailNameText: TextView

	private var selectedCategory: ApplicationCategory? = null

	private val detailLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			loadCategories()
			selectedCategory?.let { loadApplicationsForCategory(it) }
		}
	}

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_application_category, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		listContainer = view.findViewById(R.id.container_category_list)
		detailContainer = view.findViewById(R.id.container_category_detail)
		categoryRecycler = view.findViewById(R.id.recycler_categories)
		applicationRecycler = view.findViewById(R.id.recycler_category_applications)
		detailNameText = view.findViewById(R.id.text_category_detail_name)

		categoryRecycler.layoutManager = LinearLayoutManager(requireContext())
		applicationRecycler.layoutManager = LinearLayoutManager(requireContext())

		view.findViewById<ImageView>(R.id.btn_back_from_category_detail).setOnClickListener {
			showCategoryListScreen()
		}

		loadCategories()
	}

	override fun onResume() {
		super.onResume()
		loadCategories()
		selectedCategory?.let { loadApplicationsForCategory(it) }
	}

	private fun showCategoryListScreen() {
		listContainer.visibility = View.VISIBLE
		detailContainer.visibility = View.GONE
		selectedCategory = null
	}

	private fun showCategoryDetailScreen(category: ApplicationCategory) {
		listContainer.visibility = View.GONE
		detailContainer.visibility = View.VISIBLE
		selectedCategory = category
		detailNameText.text = category.name
		loadApplicationsForCategory(category)
	}

	private fun loadCategories() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val categories = db.applicationCategoryDao().getAll()
			val allApplications = db.applicationDao().getAll()

			val rows = categories.map { category ->
				category to allApplications.count { it.categoryId == category.id }
			}

			view?.findViewById<TextView>(R.id.text_category_list_empty)?.visibility =
				if (rows.isEmpty()) View.VISIBLE else View.GONE

			categoryRecycler.adapter = SimpleCategoryAdapter(rows) { category ->
				showCategoryDetailScreen(category)
			}
		}
	}

	private fun loadApplicationsForCategory(category: ApplicationCategory) {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val applications = db.applicationDao().getByCategory(category.id)

			view?.findViewById<TextView>(R.id.text_category_detail_empty)?.visibility =
				if (applications.isEmpty()) View.VISIBLE else View.GONE

			val rows = ApplicationRowBuilder.build(db, applications, useDateLabel = true)
			applicationRecycler.adapter = ApplicationRowAdapter(rows) { application ->
				detailLauncher.launch(
					ApplicationDetailActivity.createIntent(requireContext(), application.id)
				)
			}
		}
	}

	private class SimpleCategoryAdapter(
		private val rows: List<Pair<ApplicationCategory, Int>>,
		private val onClick: (ApplicationCategory) -> Unit
	) : RecyclerView.Adapter<SimpleCategoryAdapter.ViewHolder>() {

		class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
			val dot: View = view.findViewById(R.id.color_dot)
			val name: TextView = view.findViewById(R.id.text_category_name)
			val count: TextView = view.findViewById(R.id.text_category_count)
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
			val view = LayoutInflater.from(parent.context)
				.inflate(R.layout.item_application_category_row, parent, false)
			return ViewHolder(view)
		}

		override fun onBindViewHolder(holder: ViewHolder, position: Int) {
			val (category, count) = rows[position]
			val drawable = GradientDrawable()
			drawable.shape = GradientDrawable.OVAL
			drawable.setColor(
				try {
					Color.parseColor(category.colorHex)
				} catch (e: Exception) {
					Color.GRAY
				}
			)
			holder.dot.background = drawable
			holder.name.text = category.name
			holder.count.text = "${count}개"
			holder.itemView.setOnClickListener { onClick(category) }
		}

		override fun getItemCount() = rows.size
	}
}