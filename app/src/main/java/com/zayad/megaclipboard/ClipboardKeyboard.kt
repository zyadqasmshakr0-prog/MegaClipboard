package com.zayad.megaclipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.ViewGroup
import android.app.AlertDialog
import android.view.WindowManager
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class ClipboardKeyboard : InputMethodService() {
    private lateinit var clipboardManager: ClipboardManager
    
    // عناصر الواجهة
    private var rootKeyboard: LinearLayout? = null
    private var topToolbar: LinearLayout? = null
    private var layoutKeysContainer: LinearLayout? = null
    private var layoutClipboardContainer: LinearLayout? = null
    private var historyContainer: LinearLayout? = null
    private var btnToggleView: Button? = null
    
    // متغيرات النافذة والتبديل
    private var isClipboardMode = false
    private var currentThemeIndex = 0 // 0=Dark, 1=Light, 2=Blue
    
    private var dialogOverlay: LinearLayout? = null
    private var btnPin: Button? = null
    private var btnDelete: Button? = null
    private var btnCancel: Button? = null
    private var currentSelectedIndex = -1
    
    private fun isAutoPinEnabled(): Boolean {
        return getSharedPreferences("MegaPrefs", Context.MODE_PRIVATE).getBoolean("auto_pin", false)
    }
    
    private fun formatTime(ts: Long): String {
        if (ts <= 0L) return ""
        val date = Date(ts * 1000L)
        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
        return sdf.format(date)
    }
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            if (item.text != null && item.text.isNotEmpty()) {
                EngineManager.addToEngine(item.text.toString(), 0, isAutoPinEnabled())
                if(isClipboardMode) refreshHistoryView()
            } else if (item.uri != null) {
                EngineManager.addToEngine(item.uri.toString(), 1, isAutoPinEnabled())
                if(isClipboardMode) refreshHistoryView()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        EngineManager.initEngine(applicationContext.filesDir.absolutePath)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
        
        currentThemeIndex = getSharedPreferences("MegaPrefs", Context.MODE_PRIVATE).getInt("theme_index", 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::clipboardManager.isInitialized) clipboardManager.removePrimaryClipChangedListener(clipboardListener)
    }

    override fun onCreateInputView(): View {
        EngineManager.cleanupEngine()
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        
        rootKeyboard = view.findViewById(R.id.root_keyboard_view)
        topToolbar = view.findViewById(R.id.top_toolbar)
        layoutKeysContainer = view.findViewById(R.id.layout_keys_container)
        layoutClipboardContainer = view.findViewById(R.id.layout_clipboard_container)
        historyContainer = view.findViewById(R.id.history_container)
        btnToggleView = view.findViewById(R.id.btn_toggle_view)
        
        // أزرار شريط الأدوات
        view.findViewById<Button>(R.id.btn_settings_top).setOnClickListener {
            val intent = Intent(this@ClipboardKeyboard, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        
        // زر التبديل بين الحافظة والكيبورد
        btnToggleView?.setOnClickListener {
            isClipboardMode = !isClipboardMode
            if (isClipboardMode) {
                btnToggleView?.text = "⌨️"
                layoutKeysContainer?.visibility = View.GONE
                layoutClipboardContainer?.visibility = View.VISIBLE
                refreshHistoryView()
            } else {
                btnToggleView?.text = "📋"
                layoutClipboardContainer?.visibility = View.GONE
                layoutKeysContainer?.visibility = View.VISIBLE
            }
        }
        
        // زر تغيير المظهر (Theme Engine)
        view.findViewById<Button>(R.id.btn_theme).setOnClickListener {
            currentThemeIndex = (currentThemeIndex + 1) % 3
            getSharedPreferences("MegaPrefs", Context.MODE_PRIVATE).edit().putInt("theme_index", currentThemeIndex).apply()
            applyTheme()
            generateKeyboardLayout() // إعادة رسم الحروف باللون الجديد
            if(isClipboardMode) refreshHistoryView()
        }
        
        // تجهيز النافذة المنبثقة
        dialogOverlay = view.findViewById(R.id.dialog_overlay)
        btnPin = view.findViewById(R.id.dialog_btn_pin)
        btnDelete = view.findViewById(R.id.dialog_btn_delete)
        btnCancel = view.findViewById(R.id.dialog_btn_cancel)
        
        btnCancel?.setOnClickListener { dialogOverlay?.visibility = View.GONE }
        btnDelete?.setOnClickListener {
            if (currentSelectedIndex != -1) {
                EngineManager.deleteItem(currentSelectedIndex)
                dialogOverlay?.visibility = View.GONE
                refreshHistoryView()
            }
        }
        btnPin?.setOnClickListener {
            if (currentSelectedIndex != -1) {
                EngineManager.pinItem(currentSelectedIndex)
                dialogOverlay?.visibility = View.GONE
                refreshHistoryView()
            }
        }

        applyTheme()
        generateKeyboardLayout()
        return view
    }

    // دالة رسم حروف لوحة المفاتيح برمجياً لتخفيف الكود وتسهيل التخصيص
    private fun generateKeyboardLayout() {
        layoutKeysContainer?.removeAllViews()
        
        val rows = listOf(
            listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "د"),
            listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك", "ط"),
            listOf("⇧", "ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "و", "ز", "ظ", "⌫"),
            listOf("?123", "☺", "مسافة", ".", "↵")
        )

        val keyBgColor = when(currentThemeIndex) {
            0 -> Color.parseColor("#333333") // Dark
            1 -> Color.parseColor("#FFFFFF") // Light
            else -> Color.parseColor("#1565C0") // Blue
        }
        val keyTextColor = if(currentThemeIndex == 1) Color.BLACK else Color.WHITE

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            }

            for (key in row) {
                val btn = Button(this).apply {
                    text = key
                    isAllCaps = false
                    setBackgroundColor(keyBgColor)
                    setTextColor(keyTextColor)
                    setPadding(0,0,0,0)

                    var weight = 1f
                    if (key == "مسافة") weight = 4f
                    if (key == "↵" || key == "⌫" || key == "⇧" || key == "?123" || key == "☺") weight = 1.5f

                    val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
                    params.setMargins(4, 4, 4, 4)
                    layoutParams = params

                    setOnClickListener {
                        when (key) {
                            "⌫" -> {
                                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
                            }
                            "↵" -> {
                                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                            }
                            "مسافة" -> currentInputConnection?.commitText(" ", 1)
                            "⇧", "?123", "☺" -> android.widget.Toast.makeText(this@ClipboardKeyboard, "سيتم تفعيلها في التحديث القادم", android.widget.Toast.LENGTH_SHORT).show()
                            else -> currentInputConnection?.commitText(key, 1)
                        }
                    }
                }
                rowLayout.addView(btn)
            }
            layoutKeysContainer?.addView(rowLayout)
        }
    }

    private fun applyTheme() {
        val rootBg = when(currentThemeIndex) {
            0 -> Color.parseColor("#1E1E1E")
            1 -> Color.parseColor("#EAEAEA")
            else -> Color.parseColor("#0D47A1")
        }
        val toolbarBg = when(currentThemeIndex) {
            0 -> Color.parseColor("#121212")
            1 -> Color.parseColor("#D6D6D6")
            else -> Color.parseColor("#002171")
        }
        
        rootKeyboard?.setBackgroundColor(rootBg)
        topToolbar?.setBackgroundColor(toolbarBg)
        
        // تحديث ألوان أيقونات شريط الأدوات إذا كان المظهر فاتحاً
        val iconColor = if(currentThemeIndex == 1) Color.BLACK else Color.WHITE
        for (i in 0 until (topToolbar?.childCount ?: 0)) {
            val child = topToolbar?.getChildAt(i)
            if (child is Button) child.setTextColor(iconColor)
        }
    }

    private fun refreshHistoryView() {
        if (!isClipboardMode) return
        
        Handler(Looper.getMainLooper()).post {
            historyContainer?.removeAllViews()
            val count = EngineManager.getCountFromEngine()
            
            val btnBgColor = if(currentThemeIndex == 1) Color.WHITE else Color.parseColor("#333333")
            val btnTextColor = if(currentThemeIndex == 1) Color.BLACK else Color.WHITE
            
            for (i in 0 until count) {
                val content = EngineManager.getDataFromEngine(i)
                val type = EngineManager.getTypeFromEngine(i)
                val isPinned = EngineManager.isItemPinned(i)
                val timestamp = EngineManager.getTimestampFromEngine(i)
                
                val btn = Button(this@ClipboardKeyboard)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 10)
                btn.layoutParams = params
                btn.isAllCaps = false
                btn.setBackgroundColor(btnBgColor)
                btn.setTextColor(btnTextColor)
                
                val prefix = if (isPinned) "📌 " else "⏳ "
                val mainText = if (type == 0) (if (content.length > 50) content.substring(0, 50) + "..." else content) else "🖼️ صورة / ملف"
                val timeStr = formatTime(timestamp)
                val timeAppend = if (timeStr.length > 0) "  (🕒 " + timeStr + ")" else ""
                
                btn.text = prefix + mainText + timeAppend
                
                btn.setOnClickListener {
                    if (type == 0) currentInputConnection?.commitText(content, 1)
                    else currentInputConnection?.commitText("🖼 " + content, 1)
                }
                
                btn.setOnLongClickListener {
                    currentSelectedIndex = i
                    btnPin?.text = if (isPinned) "❌ إلغاء التثبيت" else "📌 تثبيت للأبد"
                    dialogOverlay?.visibility = View.VISIBLE
                    true
                }
                historyContainer?.addView(btn)
            }
        }
    }
}
