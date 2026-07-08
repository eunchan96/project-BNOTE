package com.chan.bnote.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chan.bnote.R
import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.DateUtils
import com.chan.bnote.data.Sermon
import kotlinx.coroutines.launch

class CalendarSermonFragment : Fragment() {

	private var selectedDate: Long = DateUtils.normalizeToDayStart(System.currentTimeMillis())

	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
	): View {
		return inflater.inflate(R.layout.fragment_sermon_calendar, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)

		val calendarView = view.findViewById<CalendarView>(R.id.calendar_view)
		calendarView.setOnDateChangeListener { _, year, month, day ->
			val cal = java.util.Calendar.getInstance()
			cal.set(year, month, day, 0, 0, 0)
			selectedDate = DateUtils.normalizeToDayStart(cal.timeInMillis)
			loadSermons()
		}

		view.findViewById<TextView>(R.id.fab_add_sermon).setOnClickListener {
			val sheet = AddSermonBottomSheet()
			sheet.onSaved = { loadSermons() }
			sheet.show(parentFragmentManager, "add_sermon")
		}

		loadSermons()
	}

	private fun loadSermons() {
		lifecycleScope.launch {
			val db = BibleDatabase.getInstance(requireContext().applicationContext)
			val sermons = db.sermonDao().getByDate(selectedDate)
			renderList(sermons)
		}
	}

	private fun renderList(sermons: List<Sermon>) {
		val recyclerView = view?.findViewById<RecyclerView>(R.id.recycler_sermons_by_date) ?: return
		val emptyText = view?.findViewById<TextView>(R.id.text_empty_calendar) ?: return

		if (sermons.isEmpty()) {
			emptyText.visibility = View.VISIBLE
			recyclerView.visibility = View.GONE
			return
		}

		emptyText.visibility = View.GONE
		recyclerView.visibility = View.VISIBLE
		recyclerView.layoutManager = LinearLayoutManager(requireContext())

		val labels = sermons.map { "${it.title}  (${it.preacher})" }
		recyclerView.adapter = SimpleListAdapter(labels) { position ->
			SermonDetailBottomSheet(sermons[position]).show(parentFragmentManager, "sermon_detail")
		}
	}
}