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

    private val alphabetical = listOf(
        "ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز","س",
        "ش","ص","ض","ط","ظ","ع","غ","ف","ق","ك","ل","م",
        "ن","ه","و","ي","ء","ؤ","ئ","ة","ى","لا"
    )

    private val abjad = listOf(
        "ا","ب","ج","د","ه","و","ز","ح","ط","ي","ك","ل",
        "م","ن","س","ع","ف","ص","ق","ر","ش","ت","ث","خ",
        "ذ","ض","ظ","غ","ء","ؤ","ئ","ة","ى","لا"
    )

    private val frequency = listOf(
        "ا","ل","م","ن","و","ي","ه","ر","ب","ت","ك","ع",
        "د","س","ق","ف","ح","ج","ص","ش","ز","ط","ض","ث",
        "غ","خ","ذ","ظ","ء","ؤ","ئ","ة","ى","لا"
    )

    private fun rotate(list: List<String>, amount: Int): List<String> {
        val n = amount % list.size
        return list.drop(n) + list.take(n)
    }

    private fun alternate(list: List<String>): List<String> {
        val result = mutableListOf<String>()
        var left = 0
        var right = list.lastIndex

        while (left <= right) {
            result.add(list[left])

            if (left != right) {
                result.add(list[right])
            }

            left++
            right--
        }

        return result
    }

    private fun interleave(list: List<String>): List<String> {
        val result = mutableListOf<String>()
        val middle = (list.size + 1) / 2
        val first = list.take(middle)
        val second = list.drop(middle)

        for (i in first.indices) {
            result.add(first[i])
            if (i < second.size) {
                result.add(second[i])
            }
        }

        return result
    }

    fun getName(index: Int): String {
        return when (index) {
            0 -> "🇾🇪 العربي القياسي"
            1 -> "⚡ العربي السريع"
            2 -> "🔤 العربي الأبجدي"
            3 -> "📜 ترتيب أبجد هوز"
            4 -> "🧠 ترتيب شائع الاستخدام"
            5 -> "↩️ الترتيب المعكوس"
            6 -> "⬅️ ترتيب اليد اليسرى"
            7 -> "➡️ ترتيب اليد اليمنى"
            8 -> "🔀 الترتيب المتناوب"
            9 -> "🔄 الترتيب الدائري"
            10 -> "🇺🇸 English QWERTY"
            11 -> "✋ ترتيبي الخاص"
            else -> "تخطيط"
        }
    }

    fun getNames(): List<String> {
        return (0..11).map { getName(it) }
    }

    fun getPreset(index: Int): List<String> {

        return when (index) {

            0 -> base

            1 -> listOf(
                "ق","و","ع","ر","ت","ي","ب","ل","ا","د","س","م",
                "ن","ك","ط","ح","ض","ص","ث","خ","ج","ف","غ",
                "ش","ه","ة","و","ز","ظ","ذ","ء","ئ","ؤ","لا"
            )

            2 -> alphabetical

            3 -> abjad

            4 -> frequency

            5 -> base.reversed()

            6 -> {
                val left = listOf(
                    "ق","ف","غ","ع","ه","خ","ح","ج","د",
                    "ش","س","ي","ب","ل","ا","ت","ن","م",
                    "ك","ط","ظ","ض","ص","ث","ذ","ز","و",
                    "ة","ى","ر","ؤ","ئ","ء","لا"
                )
                left
            }

            7 -> {
                val right = listOf(
                    "ا","ل","م","ن","ت","ب","ي","س","ش",
                    "د","ج","ح","خ","ه","ع","غ","ف","ق",
                    "ط","ك","ظ","ز","و","ة","ى","ر","ؤ",
                    "ئ","ء","ذ","ض","ص","ث","لا"
                )
                right
            }

            8 -> alternate(base)

            9 -> rotate(base, 11)

            else -> base
        }
    }

    fun getCustom(prefs: SharedPreferences): List<String> {

        val saved =
            prefs.getString(PREF_CUSTOM, null)

        if (saved.isNullOrBlank()) {
            return base
        }

        val result =
            saved.split("|")
                .filter { it.isNotEmpty() }

        return if (result.size == base.size)
            result
        else
            base
    }

    fun saveCustom(
        prefs: SharedPreferences,
        layout: List<String>
    ) {
        prefs.edit()
            .putString(
                PREF_CUSTOM,
                layout.joinToString("|")
            )
            .apply()
    }

    fun resetCustom(prefs: SharedPreferences) {
        prefs.edit()
            .remove(PREF_CUSTOM)
            .apply()
    }

    fun getRows(
        index: Int,
        prefs: SharedPreferences
    ): List<List<String>> {

        val letters =
            when (index) {
                11 -> getCustom(prefs)
                in 0..9 -> getPreset(index)
                else -> return emptyList()
            }

        return listOf(
            letters.take(12),
            letters.drop(12).take(11),
            letters.drop(23).take(10)
        )
    }
}
