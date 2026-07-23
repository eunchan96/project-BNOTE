package com.chan.bnote.ui.mypage.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R
import com.chan.bnote.data.bible.BibleBooks
import com.chan.bnote.data.bible.SeedReconciliationReport

class SeedReconciliationActivity : AppCompatActivity() {

	private lateinit var container: LinearLayout

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_seed_reconciliation)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.seed_reconciliation_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "본문 수정 확인 필요"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		container = findViewById(R.id.container_reconciliation_issues)
		render()
	}

	private fun render() {
		container.removeAllViews()
		val issues = SeedReconciliationReport.getAll(this)

		val emptyText = findViewById<TextView>(R.id.text_reconciliation_empty)
		emptyText.visibility = if (issues.isEmpty()) View.VISIBLE else View.GONE

		for (issue in issues) {
			val row = LayoutInflater.from(this)
				.inflate(R.layout.item_reconciliation_issue, container, false)

			val unit = BibleBooks.chapterUnit(issue.bookId)
			val location =
				"${BibleBooks.nameOf(issue.bookId)} ${issue.chapter}${unit} ${issue.verse}절"
			row.findViewById<TextView>(R.id.text_issue_location).text = location

			val detail = if (issue.type == "word_memo") {
				"단어 메모가 가리키던 \"${issue.oldSnippet}\"을(를) 새 본문에서 못 찾았어요.\n메모 내용: ${issue.memoText ?: ""}"
			} else {
				"부분 하이라이트가 가리키던 \"${issue.oldSnippet}\"을(를) 새 본문에서 못 찾았어요."
			}
			row.findViewById<TextView>(R.id.text_issue_detail).text = detail

			row.findViewById<TextView>(R.id.btn_dismiss_issue).setOnClickListener {
				SeedReconciliationReport.dismiss(this, issue)
				render()
			}

			container.addView(row)
		}
	}
}