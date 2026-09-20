package com.chan.bnote.ui.appendix

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.appendix.AppendixLoader
import com.chan.bnote.ui.common.SimpleListAdapter

class ResponsiveReadingListActivity : AppCompatActivity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_responsive_reading_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.responsive_reading_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		findViewById<TextView>(R.id.text_top_bar_title).text = "교독문"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }

		val readings = AppendixLoader.loadResponsiveReadings(this)
		val labels = readings.map { "${it.number}. ${it.title}" }

		val recyclerView = findViewById<RecyclerView>(R.id.recycler_responsive_readings)
		recyclerView.layoutManager = LinearLayoutManager(this)
		recyclerView.adapter = SimpleListAdapter(labels) { position ->
			val reading = readings[position]
			ResponsiveReadingDetailActivity.start(this, reading.number)
		}
	}
}