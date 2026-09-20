package com.chan.bnote.ui.knowledge

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.R
import com.chan.bnote.data.knowledge.BiblePlace
import com.chan.bnote.data.knowledge.BiblePlaceRepository
import kotlinx.coroutines.launch

class BiblePlaceListActivity : AppCompatActivity() {

	// 목록에서 이 순서대로 카테고리 섹션을 보여준다.
	private val categoryOrder = listOf("도시", "지역", "산", "강", "바다", "나라")

	private lateinit var container: LinearLayout
	private lateinit var emptyText: TextView
	private var allPlaces: List<BiblePlace> = emptyList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_bible_place_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.place_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "지도 (지명사전)"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		container = findViewById(R.id.container_list)
		emptyText = findViewById(R.id.text_empty)

		findViewById<EditText>(R.id.edit_search).addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				renderList(filter(s?.toString().orEmpty()))
			}
		})

		loadPlaces()
	}

	private fun loadPlaces() {
		lifecycleScope.launch {
			allPlaces = BiblePlaceRepository.getAll(applicationContext)
			renderList(allPlaces)
		}
	}

	private fun filter(query: String): List<BiblePlace> {
		val trimmed = query.trim()
		if (trimmed.isEmpty()) return allPlaces
		return allPlaces.filter {
			it.name.contains(trimmed) || it.otherNames.contains(trimmed) || it.summary.contains(
				trimmed
			)
		}
	}

	private fun renderList(places: List<BiblePlace>) {
		container.removeAllViews()
		if (places.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			return
		}
		emptyText.visibility = View.GONE

		val grouped = places.groupBy { it.category }
		val orderedCategories = categoryOrder.filter { grouped.containsKey(it) } +
				grouped.keys.filter { it !in categoryOrder }

		for (category in orderedCategories) {
			addHeader(category)
			grouped[category]?.forEach { place -> addRow(place) }
		}
	}

	private fun addHeader(category: String) {
		val header = TextView(this).apply {
			text = category
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@BiblePlaceListActivity, R.color.brown_primary))
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	private fun addRow(place: BiblePlace) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@BiblePlaceListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener {
				startActivity(
					BiblePlaceDetailActivity.createIntent(
						this@BiblePlaceListActivity,
						place.id
					)
				)
			}
		}
		val nameView = TextView(this).apply {
			text = place.name
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@BiblePlaceListActivity, R.color.text_primary))
		}
		val summaryView = TextView(this).apply {
			text = place.summary
			textSize = 13f
			setTextColor(
				ContextCompat.getColor(
					this@BiblePlaceListActivity,
					R.color.text_secondary
				)
			)
			setPadding(0, dp(2), 0, 0)
		}
		row.addView(nameView)
		row.addView(summaryView)
		container.addView(row)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}