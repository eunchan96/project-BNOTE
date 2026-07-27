package com.chan.bnote.ui.mypage.gratitude

import com.chan.bnote.data.BibleDatabase
import com.chan.bnote.data.mypage.gratitude.GratitudeNote

object GratitudeRowBuilder {
	suspend fun build(db: BibleDatabase, notes: List<GratitudeNote>): List<GratitudeRowData> {
		return notes.map { note ->
			val entries = db.gratitudeEntryDao().getByNote(note.id)
				.filter { it.text.isNotBlank() }
			val preview = entries.joinToString(" · ") { it.text }
			GratitudeRowData(note = note, previewText = preview)
		}
	}
}