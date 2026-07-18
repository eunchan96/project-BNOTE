package com.chan.bnote.ui.knowledge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R

/** 성경 배경지식 허브 — 인물사전/지도/족보/연대표/상황별 말씀/당시 문화. */
class BibleKnowledgeHubActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_bible_knowledge_hub)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.knowledge_hub_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "성경 배경지식"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		findViewById<LinearLayout>(R.id.menu_figures).setOnClickListener {
			startActivity(Intent(this, BibleFigureListActivity::class.java))
		}
		findViewById<LinearLayout>(R.id.menu_places).setOnClickListener {
			startActivity(Intent(this, BiblePlaceListActivity::class.java))
		}
		findViewById<LinearLayout>(R.id.menu_genealogy).setOnClickListener {
			startActivity(Intent(this, GenealogyListActivity::class.java))
		}
		findViewById<LinearLayout>(R.id.menu_timeline).setOnClickListener {
			startActivity(Intent(this, TimelineListActivity::class.java))
		}
		findViewById<LinearLayout>(R.id.menu_topics).setOnClickListener {
			startActivity(Intent(this, TopicListActivity::class.java))
		}
		findViewById<LinearLayout>(R.id.menu_culture).setOnClickListener {
			startActivity(Intent(this, CultureListActivity::class.java))
		}
	}
}