package com.zayad.megaclipboard
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
            text = "⌨️ خريطة الكيبورد"
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

        if (intent.getStringExtra("TARGET_TAB") == "KEYBOARD_LAYOUTS") showLayoutsTab() else showGeneralTab()
    }

    // دالة إنشاء أزرار قوية وواضحة جداً لمنع اختفاء النصوص
    private fun createSettingsBtn(title: String, bgColor: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(bgColor))
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 10, 0, 20)
            layoutParams = params
            setPadding(0, 30, 0, 30)
            setOnClickListener { onClick() }
        }
    }

    private fun showErrorDisplay(e: Throwable) {
        contentContainer.removeAllViews()
        val errorText = TextView(this).apply {
            text = "⚠️ حدث خطأ داخلي ولن ينهار التطبيق:\n\n${e.message}\n\n${e.stackTraceToString()}"
            setTextColor(Color.parseColor("#FF5252"))
            textSize = 14f
        }
        contentContainer.addView(errorText)
    }

    private fun showGeneralTab() {
        try {
            contentContainer.removeAllViews()

            val btnEnable = createSettingsBtn("⌨️ تفعيل الكيبورد في النظام", "#1565C0") {
                startActivity(android.content.Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
            contentContainer.addView(btnEnable)

            val btnSelect = createSettingsBtn("✅ اختيار MegaClipboard كافتراضي", "#2E7D32") {
                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                imm.showInputMethodPicker()
            }
            contentContainer.addView(btnSelect)

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            var isSaveEnabled = prefs.getBoolean("permanent_save", false)
            val btnSave = createSettingsBtn(if (isSaveEnabled) "💾 الحفظ الدائم: مفعل" else "💾 الحفظ الدائم: معطل", if (isSaveEnabled) "#1565C0" else "#424242") {
                // سيتم تحديث الزر برمجياً داخل اللمسة
            }
            btnSave.setOnClickListener {
                isSaveEnabled = !isSaveEnabled
                prefs.edit().putBoolean("permanent_save", isSaveEnabled).apply()
                btnSave.text = if (isSaveEnabled) "💾 الحفظ الدائم: مفعل" else "💾 الحفظ الدائم: معطل"
                btnSave.setBackgroundColor(if (isSaveEnabled) Color.parseColor("#1565C0") else Color.parseColor("#424242"))
            }
            contentContainer.addView(btnSave)

            // زر القائمة المثبتة - تم تصميمه بوضوح تام ولون برتقالي جذاب لتراه بوضوح
            val btnPinned = createSettingsBtn("📌 قائمة الحافظة المثبتة", "#E64A19") {
                Toast.makeText(this@MainActivity, "سيتم عرض القائمة هنا قريباً", Toast.LENGTH_SHORT).show()
            }
            contentContainer.addView(btnPinned)

        } catch (t: Throwable) {
            showErrorDisplay(t)
        }
    }

    private fun showLayoutsTab() {
        try {
            contentContainer.removeAllViews()
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            var currentLayoutIndex = prefs.getInt("keyboard_layout", 0)

            val title = TextView(this).apply {
                text = "اختر الترتيب المفضل للحروف:"
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(0, 0, 0, 30)
            }
            contentContainer.addView(title)

            val names = KeyboardLayoutManager.getNames()
            val layoutIds = KeyboardLayoutManager.getLayoutIds()
            val buttons = mutableListOf<Button>()

            names.forEachIndexed { index, name ->
                val layoutId = layoutIds.getOrElse(index) { 0 }
                val isSelected = (layoutId == currentLayoutIndex)
                
                val btn = Button(this).apply {
                    text = if (isSelected) "✓ $name" else name
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setBackgroundColor(if (isSelected) Color.parseColor("#1565C0") else Color.parseColor("#252525"))
                    val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    params.setMargins(0, 10, 0, 10)
                    layoutParams = params
                    setPadding(0, 25, 0, 25)
                    
                    setOnClickListener {
                        currentLayoutIndex = layoutId
                        prefs.edit().putInt("keyboard_layout", layoutId).apply()
                        buttons.forEachIndexed { i, b ->
                            val checkId = layoutIds.getOrElse(i) { 0 }
                            val active = (checkId == currentLayoutIndex)
                            b.text = if (active) "✓ ${names[i]}" else names[i]
                            b.setBackgroundColor(if (active) Color.parseColor("#1565C0") else Color.parseColor("#252525"))
                        }
                        Toast.makeText(this@MainActivity, "تم تفعيل $name", Toast.LENGTH_SHORT).show()
                    }
                }
                buttons.add(btn)
                contentContainer.addView(btn)
            }

            val customTitle = TextView(this).apply {
                text = "✋ ترتيبي الخاص (اسحب لتغيير المكان):"
                setTextColor(Color.WHITE)
                textSize = 18f
                setPadding(0, 50, 0, 30)
            }
            contentContainer.addView(customTitle)

            // تحميل المحرر بأمان
            val editor = KeyboardLayoutEditorView(this, KeyboardLayoutManager.getCustom(prefs))
            val editorParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (320 * resources.displayMetrics.density).toInt())
            editorParams.setMargins(0, 10, 0, 40)
            contentContainer.addView(editor, editorParams)

            val saveBtn = createSettingsBtn("💾 حفظ الترتيب وتفعيله", "#1565C0") {
                KeyboardLayoutManager.saveCustom(prefs, editor.getItems())
                currentLayoutIndex = KeyboardLayoutManager.CUSTOM_INDEX
                prefs.edit().putInt("keyboard_layout", currentLayoutIndex).apply()
                buttons.forEachIndexed { i, b ->
                    val checkId = layoutIds.getOrElse(i) { 0 }
                    val active = (checkId == currentLayoutIndex)
                    b.text = if (active) "✓ ${names[i]}" else names[i]
                    b.setBackgroundColor(if (active) Color.parseColor("#1565C0") else Color.parseColor("#252525"))
                }
                Toast.makeText(this@MainActivity, "تم الحفظ والتفعيل بنجاح!", Toast.LENGTH_SHORT).show()
            }
            contentContainer.addView(saveBtn)

            val resetBtn = createSettingsBtn("↩️ استعادة الترتيب الافتراضي", "#B71C1C") {
                KeyboardLayoutManager.resetCustom(prefs)
                Toast.makeText(this@MainActivity, "تمت الاستعادة", Toast.LENGTH_SHORT).show()
                showLayoutsTab()
            }
            contentContainer.addView(resetBtn)

        } catch (t: Throwable) {
            showErrorDisplay(t)
        }
    }
}