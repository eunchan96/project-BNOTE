package com.chan.bnote.ui.knowledge

import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R

/** "성경의 단위들" 첫 화면 — 거리·길이·무게·부피·화폐·시간 카테고리를 먼저 고르는 허브.
 * 항목이 늘어나면서 예전처럼 전부 한 화면에 늘어놓으면 너무 길어져서, 성경 배경지식 허브
 * (BibleKnowledgeHubActivity)와 같은 방식으로 카테고리 선택 화면을 앞에 뒀다.
 * 카테고리를 고르면 UnitCategoryListActivity에서 그 카테고리의 단위들을 보여준다. */
class UnitListActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_unit_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.unit_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "성경의 단위들"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		findViewById<LinearLayout>(R.id.menu_category_distance_length).setOnClickListener {
			openCategory("거리·길이")
		}
		findViewById<LinearLayout>(R.id.menu_category_weight).setOnClickListener {
			openCategory("무게")
		}
		findViewById<LinearLayout>(R.id.menu_category_volume).setOnClickListener {
			openCategory("부피")
		}
		findViewById<LinearLayout>(R.id.menu_category_currency).setOnClickListener {
			openCategory("화폐")
		}
		findViewById<LinearLayout>(R.id.menu_category_time).setOnClickListener {
			openCategory("시간")
		}
	}

	private fun openCategory(category: String) {
		startActivity(UnitCategoryListActivity.createIntent(this, category))
	}
}