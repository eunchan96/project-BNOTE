package com.chan.bnote.ui.sermon.bybook

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.chan.bnote.R
import com.chan.bnote.ui.FabAddHandler

/**
 * 설교 탭 상단바 메뉴(≡) > "성경별로 보기"로 여는 화면. 예전 설교 탭의 "성경별" 서브탭
 * (SermonByBookFragment)을 그대로 담아서 보여준다 — 책 이동, 장 그리드, 정렬, "+"로 설교 추가 모두 동일.
 */
class SermonByBookActivity : AppCompatActivity() {

	companion object {
		private const val TAG_BY_BOOK = "sermon_by_book"
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_sermon_by_book)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sermon_by_book_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "성경별로 보기"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		// 화면 회전 등으로 다시 만들어질 땐 FragmentManager가 이미 복원해둔 걸 그대로 쓴다.
		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.add(R.id.sermon_by_book_container, SermonByBookFragment(), TAG_BY_BOOK)
				.commit()
		}

		findViewById<TextView>(R.id.fab_add_sermon_by_book).setOnClickListener {
			(supportFragmentManager.findFragmentByTag(TAG_BY_BOOK) as? FabAddHandler)
				?.onFabAddClicked()
		}
	}
}