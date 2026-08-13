package com.chan.bnote.ui.knowledge

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
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
import com.chan.bnote.data.knowledge.BibleUnit
import com.chan.bnote.data.knowledge.UnitRepository
import kotlinx.coroutines.launch

/** UnitListActivity(카테고리 허브)에서 카테고리 하나를 골랐을 때 그 카테고리의 단위들을 보여주는
 * 화면. 두 단계로 소제목을 붙인다.
 *
 * 1) 구약/신약 — 같은 이름의 단위라도 시대에 따라 크기가 달라지는 경우가 많아서(예: 규빗이
 *    구약엔 약 46cm, 신약엔 약 55cm), keyBookId를 기준으로 구약(1~39권)/신약(40~66권)을 나눈다.
 * 2) subcategory — "부피"의 액체/마른 곡물, "화폐"의 은화/동전/주조화폐/무게 단위, "거리·길이"의
 *    거리/길이처럼 카테고리 안에 더 세분화된 갈래가 있으면 그 안에서 한 번 더 나눈다.
 *
 * 두 경우 다, 실제로 나뉠 값이 두 가지 이상 있을 때만 소제목을 보여준다 — 한쪽뿐이면 굳이
 * 나눌 필요가 없어서 소제목 없이 바로 목록만 나온다. */
class UnitCategoryListActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CATEGORY = "extra_category"
		private const val OLD_TESTAMENT_LAST_BOOK_ID = 39

		// 소제목이 붙을 때 화면에 보여줄 순서(원본 자료의 순서를 그대로 따름).
		// 이 목록에 없는 subcategory 값이 나오면, 처음 등장한 순서대로 뒤에 붙는다.
		private val SUBCATEGORY_ORDER = listOf(
			"거리", "길이", "액체", "마른 곡물",
			"무게를 달아 값을 치르는 경우의 단위(은화)", "무게를 달아 값을 치르는 경우의 단위(금화)",
			"주조화폐", "무게를 달아 값을 치르는 경우의 단위", "은화 단위", "동전 단위"
		)

		fun createIntent(context: Context, category: String): Intent =
			Intent(context, UnitCategoryListActivity::class.java).putExtra(EXTRA_CATEGORY, category)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_unit_category_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.unit_category_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val category = intent.getStringExtra(EXTRA_CATEGORY)
		if (category == null) {
			finish()
			return
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = category
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		lifecycleScope.launch {
			val units = UnitRepository.getAll(applicationContext).filter { it.category == category }
			renderList(units)
		}
	}

	private fun renderList(units: List<BibleUnit>) {
		val container = findViewById<LinearLayout>(R.id.container_list)
		container.removeAllViews()

		val oldTestament = units.filter { it.keyBookId <= OLD_TESTAMENT_LAST_BOOK_ID }
		val newTestament = units.filter { it.keyBookId > OLD_TESTAMENT_LAST_BOOK_ID }

		if (oldTestament.isNotEmpty() && newTestament.isNotEmpty()) {
			addEraHeader(container, "구약")
			renderBySubcategory(container, oldTestament)
			addEraHeader(container, "신약")
			renderBySubcategory(container, newTestament)
		} else {
			renderBySubcategory(container, units)
		}
	}

	/** 넘겨받은 단위들을 subcategory별로 묶어서 그린다. 실제로 나뉠 값이 두 가지 이상일 때만
	 * 소제목을 붙이고, 한 가지뿐이거나(예: OT만 있는 "거리·길이") subcategory 자체가 없는
	 * 카테고리(무게, 시간)는 소제목 없이 바로 목록만 그린다. */
	private fun renderBySubcategory(container: LinearLayout, units: List<BibleUnit>) {
		val distinctSubcategories = units.mapNotNull { it.subcategory }.distinct()
		if (distinctSubcategories.size <= 1) {
			units.forEach { addRow(container, it) }
			return
		}

		val grouped = units.groupBy { it.subcategory }
		val orderedKeys = grouped.keys.sortedBy { key ->
			val index = SUBCATEGORY_ORDER.indexOf(key)
			if (index >= 0) index else SUBCATEGORY_ORDER.size
		}
		for (subcategory in orderedKeys) {
			if (subcategory != null) addSubHeader(container, subcategory)
			grouped[subcategory]?.forEach { addRow(container, it) }
		}
	}

	private fun addEraHeader(container: LinearLayout, era: String) {
		val header = TextView(this).apply {
			text = era
			textSize = 13f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(
				ContextCompat.getColor(
					this@UnitCategoryListActivity,
					R.color.brown_primary
				)
			)
			setPadding(dp(16), dp(16), dp(16), dp(6))
		}
		container.addView(header)
	}

	/** era 소제목보다 글자를 살짝 작고 연하게 해서 구분한다. 들여쓰기는 하지 않는다(era 헤더와
	 * 같은 좌측 여백을 써서, 목록이 계단식으로 밀려 보이지 않게 한다). */
	private fun addSubHeader(container: LinearLayout, subcategory: String) {
		val header = TextView(this).apply {
			text = subcategory
			textSize = 12f
			setTextColor(ContextCompat.getColor(this@UnitCategoryListActivity, R.color.text_hint))
			setPadding(dp(16), dp(10), dp(16), dp(4))
		}
		container.addView(header)
	}

	private fun addRow(container: LinearLayout, unit: BibleUnit) {
		val row = LinearLayout(this).apply {
			orientation = LinearLayout.VERTICAL
			setPadding(dp(16), dp(10), dp(16), dp(10))
			background = ContextCompat.getDrawable(
				this@UnitCategoryListActivity, android.R.drawable.list_selector_background
			)
			isClickable = true
			isFocusable = true
			setOnClickListener {
				startActivity(
					UnitDetailActivity.createIntent(
						this@UnitCategoryListActivity,
						unit.id
					)
				)
			}
		}
		val titleView = TextView(this).apply {
			text = unit.title
			textSize = 15f
			setTypeface(typeface, Typeface.BOLD)
			setTextColor(
				ContextCompat.getColor(
					this@UnitCategoryListActivity,
					R.color.text_primary
				)
			)
		}
		val summaryView = TextView(this).apply {
			text = unit.summary
			textSize = 13f
			setTextColor(
				ContextCompat.getColor(
					this@UnitCategoryListActivity,
					R.color.text_secondary
				)
			)
			setPadding(0, dp(2), 0, 0)
		}
		row.addView(titleView)
		row.addView(summaryView)
		container.addView(row)
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}