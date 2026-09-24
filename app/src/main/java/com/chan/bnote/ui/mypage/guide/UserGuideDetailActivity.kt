package com.chan.bnote.ui.mypage.guide

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R

/** 카테고리 하나를 골랐을 때 그 안의 항목 "목록"만 보여주는 중간 페이지. 항목 하나를 누르면
 * UserGuideItemDetailActivity로 넘어가 그 항목만(사진 포함) 따로 본다. */
class UserGuideDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CATEGORY_ID = "extra_category_id"

		fun createIntent(context: Context, categoryId: String): Intent {
			return Intent(context, UserGuideDetailActivity::class.java).apply {
				putExtra(EXTRA_CATEGORY_ID, categoryId)
			}
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_user_guide_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.user_guide_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val categoryId = intent.getStringExtra(EXTRA_CATEGORY_ID)
		val category = categoryId?.let { UserGuideContent.findCategory(it) }

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.text_top_bar_title).text = category?.name ?: "사용 가이드"

		if (category == null) {
			finish()
			return
		}

		val container = findViewById<LinearLayout>(R.id.container_guide_detail)
		for ((index, item) in category.items.withIndex()) {
			if (index > 0) {
				val divider = View(this).apply {
					layoutParams =
						LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
					setBackgroundColor(
						ContextCompat.getColor(this@UserGuideDetailActivity, R.color.divider_light)
					)
				}
				container.addView(divider)
			}

			val row = LayoutInflater.from(this)
				.inflate(R.layout.item_guide_category, container, false)
			row.findViewById<TextView>(R.id.text_category_name).text = item.title
			row.setOnClickListener {
				startActivity(
					UserGuideItemDetailActivity.createIntent(this, category.id, item.title)
				)
			}
			container.addView(row)
		}
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}