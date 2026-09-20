package com.chan.bnote.ui.sermon.category

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.ui.sermon.SermonRowAdapter
import com.chan.bnote.ui.sermon.SermonRowBuilder
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import kotlinx.coroutines.launch

/** 설교 카테고리 관리 화면에서 카테고리를 눌렀을 때 열리는, 그 카테고리의 설교 목록. */
class CategorySermonListActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CATEGORY_ID = "extra_category_id"
		private const val EXTRA_CATEGORY_NAME = "extra_category_name"
		private const val NO_CATEGORY = -1L

		fun createIntent(context: Context, categoryId: Long?, categoryName: String): Intent {
			return Intent(context, CategorySermonListActivity::class.java).apply {
				putExtra(EXTRA_CATEGORY_ID, categoryId ?: NO_CATEGORY)
				putExtra(EXTRA_CATEGORY_NAME, categoryName)
			}
		}
	}

	private var categoryId: Long? = null

	private val sermonDetailLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) loadSermons()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_category_sermon_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.category_sermon_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val rawId = intent.getLongExtra(EXTRA_CATEGORY_ID, NO_CATEGORY)
		categoryId = if (rawId == NO_CATEGORY) null else rawId

		findViewById<TextView>(R.id.text_top_bar_title).text =
			intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: "미분류"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		findViewById<RecyclerView>(R.id.recycler_category_sermons).layoutManager =
			LinearLayoutManager(this)

		loadSermons()
	}

	private fun loadSermons() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val filtered = db.sermonDao().getAll()
				.filter { it.categoryId == categoryId }
				.sortedByDescending { it.sermonDate }

			val emptyText = findViewById<TextView>(R.id.text_empty_category_sermons)
			val recyclerView = findViewById<RecyclerView>(R.id.recycler_category_sermons)
			emptyText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
			recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE

			val rows = SermonRowBuilder.build(db, filtered, useDateLabel = true)
			recyclerView.adapter = SermonRowAdapter(rows) { sermon ->
				sermonDetailLauncher.launch(
					SermonDetailActivity.createIntent(
						this@CategorySermonListActivity,
						sermon.id
					)
				)
			}
		}
	}
}