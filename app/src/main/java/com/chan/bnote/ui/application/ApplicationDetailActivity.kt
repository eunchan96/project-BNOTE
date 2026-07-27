package com.chan.bnote.ui.application

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.MainActivity
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.sermon.Sermon
import com.chan.bnote.ui.application.addapplication.AddApplicationActivity
import com.chan.bnote.ui.sermon.detail.SermonDetailActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ApplicationDetailActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_APPLICATION_ID = "extra_application_id"

		fun start(context: Context, applicationId: Long) {
			context.startActivity(createIntent(context, applicationId))
		}

		fun createIntent(context: Context, applicationId: Long): Intent {
			return Intent(context, ApplicationDetailActivity::class.java)
				.putExtra(EXTRA_APPLICATION_ID, applicationId)
		}
	}

	private var applicationId: Long = -1L
	private var changed = false

	private val editLauncher = registerForActivityResult(
		ActivityResultContracts.StartActivityForResult()
	) { result ->
		if (result.resultCode == Activity.RESULT_OK) {
			changed = true
			loadApplication()
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_application_detail)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.application_detail_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		applicationId = intent.getLongExtra(EXTRA_APPLICATION_ID, -1L)

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener {
			setResult(if (changed) Activity.RESULT_OK else Activity.RESULT_CANCELED)
			finish()
		}
		findViewById<ImageView>(R.id.btn_edit_application).setOnClickListener {
			editLauncher.launch(AddApplicationActivity.editIntent(this, applicationId))
		}
		findViewById<ImageView>(R.id.btn_delete_application).setOnClickListener { confirmDelete() }

		loadApplication()
	}

	private fun loadApplication() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			val application = db.applicationDao().getById(applicationId) ?: run {
				finish()
				return@launch
			}

			val category = application.categoryId?.let { db.applicationCategoryDao().getById(it) }
			val dateLabel = DateUtils.formatDate(application.applicationDate)
			findViewById<TextView>(R.id.text_detail_date_category).text =
				if (category != null) "$dateLabel [${category.name}]" else dateLabel

			val titleView = findViewById<TextView>(R.id.text_detail_title)
			if (application.title.isBlank()) {
				titleView.visibility = View.GONE
			} else {
				titleView.visibility = View.VISIBLE
				titleView.text = "제목 : ${application.title}"
			}

			val refs = db.applicationBibleRefDao().getByApplication(application.id)
			val refsText = findViewById<TextView>(R.id.text_detail_refs)
			if (refs.isNotEmpty()) {
				refsText.visibility = View.VISIBLE
				refsText.text = "본문 : " + refs.joinToString(", ") { it.toDisplayLabel() }
				refsText.setOnClickListener {
					val first = refs.first()
					val mainIntent =
						Intent(this@ApplicationDetailActivity, MainActivity::class.java).apply {
							putExtra(MainActivity.EXTRA_NAVIGATE_BOOK_ID, first.startBookId)
							putExtra(MainActivity.EXTRA_NAVIGATE_CHAPTER, first.startChapter)
							if (!first.isChapterOnly) putExtra(
								MainActivity.EXTRA_NAVIGATE_VERSE,
								first.startVerse
							)
							flags =
								Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
						}
					startActivity(mainIntent)
				}
			} else {
				refsText.visibility = View.GONE
			}

			val links = db.applicationSermonLinkDao().getByApplication(application.id)
			val sermons = links.mapNotNull { db.sermonDao().getById(it.sermonId) }
			renderSermonChips(sermons)

			findViewById<TextView>(R.id.text_detail_meditation).text =
				application.meditationMemo.ifBlank { "묵상 내용이 없어요" }
			findViewById<TextView>(R.id.text_detail_prayer).text =
				application.prayerMemo.ifBlank { "기도 내용이 없어요" }
			findViewById<TextView>(R.id.text_detail_obedience).text =
				application.obedienceMemo.ifBlank { "순종 내용이 없어요" }
		}
	}

	private fun renderSermonChips(sermons: List<Sermon>) {
		val scroll = findViewById<View>(R.id.scroll_detail_sermon_chips)
		val container = findViewById<LinearLayout>(R.id.container_detail_sermon_chips)
		container.removeAllViews()
		scroll.visibility = if (sermons.isEmpty()) View.GONE else View.VISIBLE

		for (sermon in sermons) {
			val chip = TextView(this).apply {
				text = sermon.title
				textSize = 13f
				setPadding(dp(10), dp(6), dp(10), dp(6))
				setTextColor(
					ContextCompat.getColor(
						this@ApplicationDetailActivity,
						R.color.text_primary
					)
				)
				background = ContextCompat.getDrawable(
					this@ApplicationDetailActivity,
					R.drawable.bg_book_button
				)
				gravity = Gravity.CENTER
				isClickable = true
				isFocusable = true
				layoutParams = LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
				).apply { marginEnd = dp(6) }
				setOnClickListener {
					SermonDetailActivity.start(
						this@ApplicationDetailActivity,
						sermon.id
					)
				}
			}
			container.addView(chip)
		}
	}

	private fun confirmDelete() {
		MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_BNOTE_Dialog)
			.setTitle("적용 삭제")
			.setMessage("이 적용을 삭제할까요?")
			.setPositiveButton("삭제") { _, _ ->
				lifecycleScope.launch {
					val db = BibleDatabase.getInstance(applicationContext)
					db.applicationDao().getById(applicationId)?.let { application ->
						db.applicationBibleRefDao().deleteByApplication(application.id)
						db.applicationSermonLinkDao().deleteByApplication(application.id)
						db.applicationDao().delete(application)
					}
					setResult(Activity.RESULT_OK)
					finish()
				}
			}
			.setNegativeButton("취소", null)
			.show()
	}

	private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}