package com.zayad.megaclipboard
import android.content.SharedPreferences

object KeyboardLayoutManager {
    // تم تصحيح الفهرسة برمجياً لضمان عمل كافة الخرائط بشكل مثالي
    const val ENGLISH_INDEX = 2
    const val CUSTOM_INDEX = 3
    private const val PREF_CUSTOM = "custom_arabic_keyboard_layout"
    
    private val base = listOf("ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج","د","ش","س","ي","ب","ل","ا","ت","ن","م","ك","ط","ئ","ء","ؤ","ر","لا","ى","ة","و","ز","ظ")
    private val baseCustom = listOf("ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج","د","ش","س","ي","ب","ل","ا","ت","ن","م","ك","ط","⌫","⇧","ئ","ء","ؤ","ر","لا","ى","ة","و","ز","ظ","↵","?123","🌐","☺","مسافة",".",",")
    
    fun getName(index: Int): String {
        return when (index) {
            0 -> "🇾🇪 العربي القياسي (12 حرف)"
            1 -> "📱 كيبورد جوجل المألوف (نفس الصورة)"
            2 -> "🇺🇸 English QWERTY"
            3 -> "✋ ترتيبي الخاص (مفتوح بالكامل)"
            4 -> "😀 إيموجي (وجوه ومشاعر)"
            5 -> "🍕 إيموجي (أشياء وحيوانات)"
            6 -> "🎉 إيموجي (أعلام ورموز)"
            else -> "تخطيط $index"
        }
    }
    
    fun getNames(): List<String> = (0..6).map { getName(it) }
    
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
        if (index == 1) { // التخطيط المألوف كما في الصورة تماماً
            return listOf(
                listOf("ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج"),
                listOf("ش","س","ي","ب","ل","ا","ت","ن","م","ك","ط"),
                listOf("ذ","ء","ؤ","ر","ى","ة","و","ز","ظ","د","⌫"),
                listOf("?123","🌐","☺","مسافة",".","↵")
            )
        }
        if (index == 4) { // Emoji Faces
            return listOf(
                listOf("😂","❤️","😍","🤣","😊","🙏","💕","😭","😘","👍","🔥"),
                listOf("🥰","😁","✨","🥺","😅","😎","🙌","🎉","✅","🤔","🌹"),
                listOf("💔","😉","🤦‍♂️","🎶","👀","🤷‍♂️","✨","😴","🤝","✌️","⌫"),
                listOf("?123","🌐","☺","مسافة",".","↵")
            )
        }
        if (index == 5) { // Emoji Objects & Animals
            return listOf(
                listOf("🐶","🐱","🍎","🍓","🍕","🍔","☕","🚗","✈️","⚽","🏀"),
                listOf("🏆","🎮","📱","💻","💡","💸","💣","🔫","🎁","🎈","🧸"),
                listOf("☀️","🌙","⭐","🌟","🔥","💧","🌈","⛄","🎃","🎄","⌫"),
                listOf("?123","🌐","☺","مسافة",".","↵")
            )
        }
        if (index == 6) { // Emoji Flags & Symbols
            return listOf(
                listOf("🇾🇪","🇸🇦","🇵🇸","🇪🇬","🇮🇶","🇸🇾","🇲🇦","🇩🇿","🇱🇧","🇴🇲","🇯🇴"),
                listOf("✔️","❌","❗","❓","💯","🛑","⚠️","✅","❎","💠","🌀"),
                listOf("1️⃣","2️⃣","3️⃣","4️⃣","5️⃣","6️⃣","7️⃣","8️⃣","9️⃣","🔟","⌫"),
                listOf("?123","🌐","☺","مسافة",".","↵")
            )
        }
        
        // Default Arabic (index 0)
        val row1 = base.take(12)
        val row2 = base.drop(12).take(11) + listOf("⌫")
        val row3 = listOf("⇧") + base.drop(23).take(10) + listOf("↵")
        val row4 = listOf("?123", "🌐", "☺", "مسافة", ".", ",")
        
        return listOf(row1, row2, row3, row4)
    }
}