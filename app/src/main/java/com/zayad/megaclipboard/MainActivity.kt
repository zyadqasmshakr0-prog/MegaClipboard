package com.zayad.megaclipboard
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import android.preference.PreferenceManager

class MainActivity : Activity() {
    private lateinit var contentContainer: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#121212"))
        }
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }
        val tabLayouts = Button(this).apply {
            text = "⌨️ خريطة لوحة المفاتيح"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showLayoutsTab() }
        }
        val tabGeneral = Button(this).apply {
            text = "⚙️ إعدادات عامة"
            setTextColor(Color.GRAY)
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { showGeneralTab() }
        }
        tabBar.addView(tabLayouts)
        tabBar.addView(tabGeneral)
        root.addView(tabBar)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }
        scroll.addView(contentContainer)
        root.addView(scroll)
        setContentView(root)

        val targetTab = intent.getStringExtra("TARGET_TAB")
        if (targetTab == "KEYBOARD_LAYOUTS") showLayoutsTab() else showGeneralTab()
    }

    private fun showGeneralTab() {
        contentContainer.removeAllViews()
        val btnEnable = Button(this).apply {
            text = "⌨️ تفعيل الكيبورد في النظام"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1565C0"))
            setOnClickListener { startActivity(android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 10, 0, 20)
        contentContainer.addView(btnEnable, params)

        val btnSelect = Button(this).apply {
            text = "✅ اختيار MegaClipboard كافتراضي"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setOnClickListener {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
        }
        contentContainer.addView(btnSelect, params)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var isSaveEnabled = prefs.getBoolean("permanent_save", false)
        val btnSave = Button(this).apply {
            text = if (isSaveEnabled) "💾 الحفظ الدائم: مفعل" else "💾 الحفظ الدائم: معطل"
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isSaveEnabled) Color.parseColor("#1565C0") else Color.parseColor("#424242"))
            setOnClickListener {
                isSaveEnabled = !isSaveEnabled
                prefs.edit().putBoolean("permanent_save", isSaveEnabled).apply()
                text = if (isSaveEnabled) "💾 الحفظ الدائم: مفعل" else "💾 الحفظ الدائم: معطل"
                setBackgroundColor(if (isSaveEnabled) Color.parseColor("#1565C0") else Color.parseColor("#424242"))
            }
        }
        contentContainer.addView(btnSave, params)

        val btnPinned = Button(this).apply {
            text = "📌 قائمة الحافظة المثبتة"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#424242"))
            setOnClickListener { Toast.makeText(this@MainActivity, "سيتم عرض القائمة المثبتة هنا", Toast.LENGTH_SHORT).show() }
        }
        contentContainer.addView(btnPinned, params)
    }

    private fun showLayoutsTab() {
        contentContainer.removeAllViews()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        var currentLayoutIndex = prefs.getInt("keyboard_layout", 0)

        val title = Button(this).apply {
            text = "اختر الترتيب المفضل للحروف:"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 18f
            setPadding(0, 0, 0, 20)
        }
        contentContainer.addView(title)

        val names = KeyboardLayoutManager.getNames()
        val buttons = mutableListOf<Button>()

        names.forEachIndexed { index, name ->
            val btn = Button(this).apply {
                text = if (index == currentLayoutIndex) "✓ $name" else name
                isAllCaps = false
                setTextColor(Color.WHITE)
                setBackgroundColor(if (index == currentLayoutIndex) Color.parseColor("#1565C0") else Color.parseColor("#252525"))
                setPadding(0, 20, 0, 20)
                setOnClickListener {
                    currentLayoutIndex = index
                    prefs.edit().putInt("keyboard_layout", index).apply()
                    buttons.forEachIndexed { i, b ->
                        b.text = if (i == index) "✓ ${names[i]}" else names[i]
                        b.setBackgroundColor(if (i == index) Color.parseColor("#1565C0") else Color.parseColor("#252525"))
                    }
                    Toast.makeText(this@MainActivity, "تم تفعيل $name", Toast.LENGTH_SHORT).show()
                }
            }
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 8, 0, 8)
            buttons.add(btn)
            contentContainer.addView(btn, params)
        }

        val customTitle = Button(this).apply {
            text = "✋ ترتيبي الخاص (اسحب أي زر لتغيير مكانه):"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 18f
            setPadding(0, 60, 0, 20)
        }
        contentContainer.addView(customTitle)

        val editor = KeyboardLayoutEditorView(this, KeyboardLayoutManager.getCustom(prefs))
        val editorParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (320 * resources.displayMetrics.density).toInt())
        editorParams.setMargins(0, 10, 0, 30)
        contentContainer.addView(editor, editorParams)

        val saveBtn = Button(this).apply {
            text = "💾 حفظ الترتيب وتفعيله"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1565C0"))
            setOnClickListener {
                KeyboardLayoutManager.saveCustom(prefs, editor.getItems())
                currentLayoutIndex = KeyboardLayoutManager.CUSTOM_INDEX
                prefs.edit().putInt("keyboard_layout", currentLayoutIndex).apply()
                buttons.forEachIndexed { i, b ->
                    b.text = if (i == currentLayoutIndex) "✓ ${names[i]}" else names[i]
                    b.setBackgroundColor(if (i == currentLayoutIndex) Color.parseColor("#1565C0") else Color.parseColor("#252525"))
                }
                Toast.makeText(this@MainActivity, "تم الحفظ والتفعيل!", Toast.LENGTH_SHORT).show()
            }
        }
        contentContainer.addView(saveBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        
        val resetBtn = Button(this).apply {
            text = "↩️ استعادة الافتراضي"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#B71C1C"))
            setOnClickListener {
                KeyboardLayoutManager.resetCustom(prefs)
                Toast.makeText(this@MainActivity, "تمت الاستعادة. افتح التبويب مجدداً للتحديث.", Toast.LENGTH_SHORT).show()
            }
        }
        val resetParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        resetParams.setMargins(0, 20, 0, 60)
        contentContainer.addView(resetBtn, resetParams)
    }
}