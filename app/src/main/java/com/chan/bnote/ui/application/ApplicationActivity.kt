package com.chan.bnote.ui.application

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.chan.bnote.R
import com.chan.bnote.ui.application.category.ApplicationCategoryManageActivity
import com.chan.bnote.ui.application.category.CategoryApplicationFragment

class ApplicationActivity : AppCompatActivity() {

	private lateinit var subtabCalendar: TextView
	private lateinit var subtabCategory: TextView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_application)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.application_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<TextView>(R.id.btn_manage_categories).setOnClickListener {
			startActivity(Intent(this, ApplicationCategoryManageActivity::class.java))
		}

		subtabCalendar = findViewById(R.id.subtab_calendar)
		subtabCategory = findViewById(R.id.subtab_category)

		subtabCalendar.setOnClickListener {
			switchSubTab(
				CalendarApplicationFragment(),
				subtabCalendar
			)
		}
		subtabCategory.setOnClickListener {
			switchSubTab(
				CategoryApplicationFragment(),
				subtabCategory
			)
		}

		if (savedInstanceState == null) {
			switchSubTab(CalendarApplicationFragment(), subtabCalendar)
		}
	}

	private fun switchSubTab(fragment: Fragment, selected: TextView) {
		supportFragmentManager.beginTransaction()
			.replace(R.id.application_sub_container, fragment)
			.commit()

		listOf(subtabCalendar, subtabCategory).forEach {
			val isSelected = it == selected
			it.setTextColor(
				resources.getColor(
					if (isSelected) R.color.brown_primary else R.color.bottom_nav_unselected,
					null
				)
			)
		}
	}
}