package com.chan.bnote.ui.mypage.guide

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R

class UserGuideActivity : AppCompatActivity() {

	private lateinit var categoryListContainer: LinearLayout
	private lateinit var searchResultsContainer: LinearLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_user_guide)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_guide_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "사용 가이드"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		categoryListContainer = findViewById(R.id.container_category_list)
		searchResultsContainer = findViewById(R.id.container_search_results)

		renderCategoryList()

		findViewById<EditText>(R.id.edit_guide_search).addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				val query = s?.toString()?.trim() ?: ""
				if (query.length >= 2) {
					showSearchResults(query)
				} else {
					showCategoryList()
				}
			}
		})
	}

	private fun showCategoryList() {
		categoryListContainer.visibility = View.VISIBLE
		searchResultsContainer.visibility = View.GONE
	}

	private fun showSearchResults(query: String) {
		categoryListContainer.visibility = View.GONE
		searchResultsContainer.visibility = View.VISIBLE
		searchResultsContainer.removeAllViews()

		val matches =
			mutableListOf<Pair<UserGuideContent.GuideCategory, UserGuideContent.GuideItem>>()
		for (category in UserGuideContent.categories) {
			for (item in category.items) {
				if (item.title.contains(query, ignoreCase = true) ||
					item.description.contains(query, ignoreCase = true)
				) {
					matches.add(category to item)
				}
			}
		}

		if (matches.isEmpty()) {
			val emptyView = TextView(this).apply {
				text = "\"$query\"에 대한 검색 결과가 없어요"
				textSize = 14f
				setTextColor(ContextCompat.getColor(this@UserGuideActivity, R.color.text_hint))
				setPadding(dp(16), dp(24), dp(16), dp(24))
			}
			searchResultsContainer.addView(emptyView)
			return
		}

		for ((index, match) in matches.withIndex()) {
			val (category, item) = match
			if (index > 0) {
				val divider = View(this).apply {
					layoutParams =
						LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
					setBackgroundColor(
						ContextCompat.getColor(
							this@UserGuideActivity,
							R.color.divider_light
						)
					)
				}
				searchResultsContainer.addView(divider)
			}

			val row = LayoutInflater.from(this)
				.inflate(R.layout.item_guide_search_result, searchResultsContainer, false)
			row.findViewById<TextView>(R.id.text_result_title).text = item.title
			row.findViewById<TextView>(R.id.text_result_category).text = category.name
			row.setOnClickListener {
				startActivity(UserGuideDetailActivity.createIntent(this, category.id, item.title))
			}
			searchResultsContainer.addView(row)
		}
	}

	private fun renderCategoryList() {
		categoryListContainer.removeAllViews()
		for ((index, category) in UserGuideContent.categories.withIndex()) {
			if (index > 0) {
				val divider = View(this).apply {
					layoutParams =
						LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
					setBackgroundColor(
						ContextCompat.getColor(
							this@UserGuideActivity,
							R.color.divider_light
						)
					)
				}
				categoryListContainer.addView(divider)
			}

			val row = LayoutInflater.from(this)
				.inflate(R.layout.item_guide_category, categoryListContainer, false)
			row.findViewById<TextView>(R.id.text_category_name).text = category.name
			row.setOnClickListener {
				startActivity(UserGuideDetailActivity.createIntent(this, category.id, null))
			}
			categoryListContainer.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}