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

class ClipboardKeyboard : InputMethodService() {
    private lateinit var clipboardManager: ClipboardManager
    private var historyContainer: LinearLayout? = null
    
    private fun isAutoPinEnabled(): Boolean {
        val prefs = getSharedPreferences("MegaPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_pin", false)
    }
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            if (item.text != null && item.text.isNotEmpty()) {
                EngineManager.addToEngine(item.text.toString(), 0, isAutoPinEnabled())
                refreshHistoryView()
            } else if (item.uri != null) {
                EngineManager.addToEngine(item.uri.toString(), 1, isAutoPinEnabled())
                refreshHistoryView()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        EngineManager.initEngine(applicationContext.filesDir.absolutePath)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::clipboardManager.isInitialized) clipboardManager.removePrimaryClipChangedListener(clipboardListener)
    }

    override fun onCreateInputView(): View {
        EngineManager.cleanupEngine()
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)
        val btnEnter = view.findViewById<Button>(R.id.btn_enter)
        val btnSettings = view.findViewById<Button>(R.id.btn_settings)
        
        btnDelete.text = "⌫"
        btnEnter.text = "↵"
        btnSettings.text = "⚙️"
        
        btnDelete.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
        }
        btnEnter.setOnClickListener {
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        btnSettings.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        historyContainer = view.findViewById(R.id.history_container)
        refreshHistoryView()
        return view
    }

    private fun refreshHistoryView() {
        Handler(Looper.getMainLooper()).post {
            historyContainer?.removeAllViews()
            val count = EngineManager.getCountFromEngine()
            for (i in 0 until count) {
                val content = EngineManager.getDataFromEngine(i)
                val type = EngineManager.getTypeFromEngine(i)
                val isPinned = EngineManager.isItemPinned(i)
                
                val btn = Button(this@ClipboardKeyboard)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 10)
                btn.layoutParams = params
                btn.isAllCaps = false
                btn.setBackgroundColor(Color.parseColor("#FFFFFF"))
                btn.setTextColor(Color.parseColor("#000000"))
                
                val prefix = if (isPinned) "📌 " else "⏳ "
                btn.text = if (type == 0) prefix + if (content.length > 60) content.substring(0, 60) + "..." else content else prefix + "🖼️ صورة / ملف"
                
                btn.setOnClickListener {
                    if (type == 0) currentInputConnection?.commitText(content, 1)
                    else currentInputConnection?.commitText("🖼 " + content, 1)
                }
                
                btn.setOnLongClickListener {
                    val builder = AlertDialog.Builder(this@ClipboardKeyboard)
                    builder.setTitle("خيارات النص")
                    val options = arrayOf(if(isPinned) "❌ إلغاء التثبيت" else "📌 تثبيت للأبد", "🗑️ حذف نهائي")
                    builder.setItems(options) { _, which ->
                        if (which == 0) EngineManager.pinItem(i)
                        if (which == 1) EngineManager.deleteItem(i)
                        refreshHistoryView()
                    }
                    val dialog = builder.create()
                    val window = dialog.window
                    if (window != null) {
                        val lp = window.attributes
                        lp.token = historyContainer?.windowToken
                        lp.type = WindowManager.LayoutParams.TYPE_INPUT_METHOD_DIALOG
                        window.attributes = lp
                        window.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                    }
                    dialog.show()
                    true
                }
                historyContainer?.addView(btn)
            }
        }
    }
}
