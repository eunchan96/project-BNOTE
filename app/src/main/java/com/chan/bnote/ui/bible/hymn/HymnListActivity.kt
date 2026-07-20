package com.chan.bnote.ui.bible.hymn

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.bible.hymn.Hymn
import kotlinx.coroutines.launch

class HymnListActivity : AppCompatActivity() {

	companion object {
		private const val EXTRA_CATEGORY_ID = "extra_category_id"
		private const val EXTRA_CATEGORY_NAME = "extra_category_name"

		/** 전체 645장 목록 (성경 메뉴에서 '찬송' 눌렀을 때 진입). */
		fun start(context: Context) {
			context.startActivity(Intent(context, HymnListActivity::class.java))
		}

		/** 특정 소분류로 필터링된 목록. */
		fun startForCategory(context: Context, categoryId: Long, categoryName: String) {
			val intent = Intent(context, HymnListActivity::class.java)
			intent.putExtra(EXTRA_CATEGORY_ID, categoryId)
			intent.putExtra(EXTRA_CATEGORY_NAME, categoryName)
			context.startActivity(intent)
		}
	}

	private var allHymns: List<Hymn> = emptyList()
	private lateinit var recyclerView: RecyclerView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_hymn_list)

		ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.hymn_list_root)) { v, insets ->
			val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
			v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
			insets
		}

		val categoryId = intent.getLongExtra(EXTRA_CATEGORY_ID, -1L)
		val categoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME)

		findViewById<TextView>(R.id.text_top_bar_title).text = categoryName ?: "찬송"
		findViewById<ImageView>(R.id.btn_top_bar_back).setOnClickListener { finish() }
		findViewById<ImageView>(R.id.btn_open_categories).setOnClickListener {
			HymnCategoryGridActivity.startMajor(this)
		}

		recyclerView = findViewById(R.id.recycler_hymns)
		recyclerView.layoutManager = LinearLayoutManager(this)

		val editSearch = findViewById<EditText>(R.id.edit_hymn_search)
		editSearch.addTextChangedListener(object : TextWatcher {
			override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
			override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
			override fun afterTextChanged(s: Editable?) {
				applyFilter(s?.toString().orEmpty())
			}
		})

		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(applicationContext)
			allHymns = if (categoryId != -1L) {
				db.hymnDao().getByCategory(categoryId)
			} else {
				db.hymnDao().getAll()
			}
			renderList(allHymns)
		}
	}

	private fun applyFilter(keyword: String) {
		if (keyword.isBlank()) {
			renderList(allHymns)
			return
		}
		val normalized = keyword.replace(" ", "")
		val filtered = allHymns.filter { hymn ->
			hymn.number.toString().contains(normalized) ||
					hymn.title.replace(" ", "").contains(normalized)
		}
		renderList(filtered)
	}

	private fun renderList(hymns: List<Hymn>) {
		recyclerView.adapter = HymnRowAdapter(hymns) { hymn ->
			HymnDetailActivity.start(this, hymn.number)
		}
	}
}

private class HymnRowAdapter(
	private val hymns: List<Hymn>,
	private val onClick: (Hymn) -> Unit
) : RecyclerView.Adapter<HymnRowAdapter.ViewHolder>() {

	class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val number: TextView = view.findViewById(R.id.text_hymn_number)
		val title: TextView = view.findViewById(R.id.text_hymn_title)
	}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view =
			LayoutInflater.from(parent.context).inflate(R.layout.item_hymn_row, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int = hymns.size

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val hymn = hymns[position]
		holder.number.text = hymn.number.toString()
		holder.title.text = hymn.title
		holder.itemView.setOnClickListener { onClick(hymn) }
	}
}