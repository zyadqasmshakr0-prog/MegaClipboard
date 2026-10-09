package com.zayad.megaclipboard
import android.content.SharedPreferences

object KeyboardLayoutManager {
    const val ENGLISH_INDEX = 10
    const val CUSTOM_INDEX = 11
    private const val PREF_CUSTOM = "custom_arabic_keyboard_layout"

    private val base = listOf(
        "ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج","د",
        "ش","س","ي","ب","ل","ا","ت","ن","م","ك","ط",
        "ئ","ء","ؤ","ر","لا","ى","ة","و","ز","ظ"
    )
    
    private val baseCustom = listOf(
        "ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج","د",
        "ش","س","ي","ب","ل","ا","ت","ن","م","ك","ط","⌫",
        "⇧","ئ","ء","ؤ","ر","لا","ى","ة","و","ز","ظ","↵",
        "?123","🌐","☺","مسافة",".",","
    )

    fun getName(index: Int): String {
        return when (index) {
            0 -> "🇾🇪 العربي القياسي (كيبورد Gboard)"
            1 -> "⚡ العربي السريع"
            10 -> "🇺🇸 English QWERTY"
            11 -> "✋ ترتيبي الخاص"
            else -> "تخطيط $index"
        }
    }

    fun getNames(): List<String> = listOf(getName(0), getName(1), getName(10), getName(11))

    fun getCustom(prefs: SharedPreferences): List<String> {
        val saved = prefs.getString(PREF_CUSTOM, null)
        if (saved.isNullOrBlank()) return baseCustom
        val result = saved.split("|").filter { it.isNotEmpty() }
        return if (result.size == baseCustom.size) result else baseCustom
    }

    fun saveCustom(prefs: SharedPreferences, layout: List<String>) {
        prefs.edit().putString(PREF_CUSTOM, layout.joinToString("|")).apply()
    }

    fun resetCustom(prefs: SharedPreferences) {
        prefs.edit().remove(PREF_CUSTOM).apply()
    }

    fun getRows(index: Int, prefs: SharedPreferences): List<List<String>> {
        if (index == CUSTOM_INDEX) {
            val c = getCustom(prefs)
            return listOf(c.take(12), c.drop(12).take(12), c.drop(24).take(12), c.drop(36).take(6))
        }
        if (index == ENGLISH_INDEX) {
            return listOf(
                listOf("q","w","e","r","t","y","u","i","o","p"),
                listOf("a","s","d","f","g","h","j","k","l"),
                listOf("⇧","z","x","c","v","b","n","m","⌫"),
                listOf("?123","🌐","☺","مسافة",".","↵")
            )
        }
        
        // التخطيط الافتراضي تم إصلاحه ليطابق Gboard تماماً (المسح في الصف الثالث، والإدخال في الرابع)
        val letters = if (index == 1) listOf(
            "ق","و","ع","ر","ت","ي","ب","ل","ا","د","س","م",
            "ن","ك","ط","ح","ض","ص","ث","خ","ج","ف","غ",
            "ش","ه","ة","و","ز","ظ","ذ","ء","ئ","ؤ","لا"
        ) else base

        val row1 = letters.take(12)
        val row2 = letters.drop(12).take(11)
        val row3 = listOf("⇧") + letters.drop(23).take(10) + listOf("⌫")
        val row4 = listOf("?123", "🌐", "☺", "مسافة", ".", "↵")
        
        return listOf(row1, row2, row3, row4)
    }
}