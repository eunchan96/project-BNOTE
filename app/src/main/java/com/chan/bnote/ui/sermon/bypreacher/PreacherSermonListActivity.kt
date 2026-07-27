package com.chan.bnote.ui.sermon.bypreacher

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

/** 설교자 관리 화면에서 설교자를 눌렀을 때 열리는, 그 설교자의 설교 목록. */
class PreacherSermonListActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_PREACHER_ID = "extra_preacher_id"
		private const val EXTRA_PREACHER_NAME = "extra_preacher_name"

		fun createIntent(context: Context, preacherId: Long, preacherName: String): Intent {
			return Intent(context, PreacherSermonListActivity::class.java).apply {
				putExtra(EXTRA_PREACHER_ID, preacherId)
				putExtra(EXTRA_PREACHER_NAME, preacherName)
			}
		}
	}

	private var preacherId: Long = -1L

	private val sermonDetailLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) loadSermons()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_preacher_sermon_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.preacher_sermon_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		preacherId = intent.getLongExtra(EXTRA_PREACHER_ID, -1L)

		findViewById<TextView>(R.id.text_top_bar_title).text =
			intent.getStringExtra(EXTRA_PREACHER_NAME) ?: "설교자"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		findViewById<RecyclerView>(R.id.recycler_preacher_sermon_list).layoutManager =
			LinearLayoutManager(this)

		loadSermons()
	}

	private fun loadSermons() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val sermons = db.sermonDao().getByPreacherId(preacherId)
				.sortedByDescending { it.sermonDate }

			val emptyText = findViewById<TextView>(R.id.text_empty_preacher_sermon_list)
			val recyclerView = findViewById<RecyclerView>(R.id.recycler_preacher_sermon_list)
			emptyText.visibility = if (sermons.isEmpty()) View.VISIBLE else View.GONE
			recyclerView.visibility = if (sermons.isEmpty()) View.GONE else View.VISIBLE

			val rows = SermonRowBuilder.build(db, sermons, useDateLabel = true)
			recyclerView.adapter = SermonRowAdapter(rows) { sermon ->
				sermonDetailLauncher.launch(
					SermonDetailActivity.createIntent(this@PreacherSermonListActivity, sermon.id)
				)
			}
		}
	}
}