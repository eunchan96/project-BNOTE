package com.chan.bnote

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.BibleSeeder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)
		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val db = BibleDatabase.getInstance(applicationContext)
		lifecycleScope.launch {
			BibleSeeder.seedIfEmpty(applicationContext, db)
			// TODO: 여기서 db.bibleDao().getBooks() 호출해서 리스트로 화면에 뿌리기
		}
	}
}