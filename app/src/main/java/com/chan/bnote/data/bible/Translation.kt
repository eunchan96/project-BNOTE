package com.chan.bnote.data.bible

enum class Translation(val code: String, val displayName: String, val assetFileName: String) {
	NKRV("NKRV", "개역개정", "nkrv.json"),
	KRV("KRV", "개역한글", "krv.json"),
	KSB("KSB", "표준새번역", "ksb.json"),
	KLB("KLB", "현대인의성경", "klb.json")
}