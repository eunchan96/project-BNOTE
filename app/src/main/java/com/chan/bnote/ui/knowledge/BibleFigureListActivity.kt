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
import com.chan.bnote.data.knowledge.BibleFigure
import com.chan.bnote.data.knowledge.BibleFigureRepository
import kotlinx.coroutines.launch

class BibleFigureListActivity : AppCompatActivity() {

	// 목록에서 이 순서대로 카테고리 섹션을 보여준다 (목록에 없는 카테고리는 뒤에 그대로 붙는다).
	private val categoryOrder = listOf("족장", "지도자", "사사", "왕", "선지자", "사도", "여성", "기타")

	private lateinit var container: LinearLayout
	private lateinit var emptyText: TextView
	private var allFigures: List<BibleFigure> = emptyList()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_bible_figure_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.figure_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "인물사전"
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

		loadFigures()
	}

	private fun loadFigures() {
		lifecycleScope.launch {
			allFigures = BibleFigureRepository.getAll(applicationContext)
			renderList(allFigures)
		}
	}

	private fun filter(query: String): List<BibleFigure> {
		val trimmed = query.trim()
		if (trimmed.isEmpty()) return allFigures
		return allFigures.filter {
			it.name.contains(trimmed) || it.otherNames.contains(trimmed) || it.summary.contains(
				trimmed
			)
		}
	}

	private fun renderList(figures: List<BibleFigure>) {
		container.removeAllViews()
		if (figures.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			return
		}
		emptyText.visibility = View.GONE

		val grouped = figures.groupBy { it.category }
		val orderedCategories = categoryOrder.filter { grouped.containsKey(it) } +
				grouped.keys.filter { it !in categoryOrder }

		for (category in orderedCategories) {
			addHeader(category)
			grouped[category]?.forEach { figure -> addRow(figure) }
		}
	}

	private fun addHeader(category: String) {
		val header = TextView(this).apply {
			text = category
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(
				ContextCompat.getColor(
					this@BibleFigureListActivity,
					R.color.brown_primary
				)
			)
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	private fun addRow(figure: BibleFigure) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@BibleFigureListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener {
				startActivity(
					BibleFigureDetailActivity.createIntent(
						this@BibleFigureListActivity,
						figure.id
					)
				)
			}
		}
		val nameView = TextView(this).apply {
			text = figure.name
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(ContextCompat.getColor(this@BibleFigureListActivity, R.color.text_primary))
		}
		val summaryView = TextView(this).apply {
			text = figure.summary
			textSize = 13f
			setTextColor(
				ContextCompat.getColor(
					this@BibleFigureListActivity,
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