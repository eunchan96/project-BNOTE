package com.chan.bnote.data.bible

/**
 * [isNestedBookFormat]이 true면 JSON이 "책 목록 -> 장 목록 -> 절 목록"으로 중첩된 구조라는 뜻이다
 * (NIV.json, ESV.json이 이 형태). false면 기존처럼 절 하나하나가 book(정수 ID)·chapter·verse를
 * 직접 갖고 있는 평평한 배열이다(KJV.json도 이 형태와 그대로 호환된다).
 */
enum class Translation(
	val code: String,
	val displayName: String,
	val assetFileName: String,
	val isNestedBookFormat: Boolean = false
) {
	NKRV("NKRV", "개역개정", "nkrv.json"),
	KRV("KRV", "개역한글", "krv.json"),
	KSB("KSB", "표준새번역", "ksb.json"),
	KLB("KLB", "현대인의성경", "klb.json"),
	EASY("EASY", "쉬운성경", "easy.json"),
	NIV("NIV", "NIV (New International Version)", "niv.json", isNestedBookFormat = true),
	KJV("KJV", "KJV (King James Version)", "kjv.json"),
	ESV("ESV", "ESV (English Standard Version)", "esv.json", isNestedBookFormat = true)
}