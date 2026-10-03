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

class ClipboardKeyboard : InputMethodService() {
    
    init { System.loadLibrary("megaclipboard") }
    
    external fun initEngine(path: String)
    external fun addToEngine(data: String, type: Int)
    external fun getDataFromEngine(index: Int): String
    external fun getTypeFromEngine(index: Int): Int
    external fun getCountFromEngine(): Int
    external fun cleanupEngine()

    private lateinit var clipboardManager: ClipboardManager
    private var historyContainer: LinearLayout? = null
    
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            if (item.text != null && item.text.isNotEmpty()) {
                addToEngine(item.text.toString(), 0)
                refreshHistoryView() // تحديث القائمة فوراً عند النسخ
            } else if (item.uri != null) {
                addToEngine(item.uri.toString(), 1)
                refreshHistoryView() // تحديث القائمة فوراً عند النسخ
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        initEngine(applicationContext.filesDir.absolutePath)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(clipboardListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::clipboardManager.isInitialized) {
            clipboardManager.removePrimaryClipChangedListener(clipboardListener)
        }
    }

    override fun onCreateInputView(): View {
        cleanupEngine() // تفعيل مؤقت الـ 24 ساعة
        
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        val btnDelete = view.findViewById<Button>(R.id.btn_delete)
        val btnEnter = view.findViewById<Button>(R.id.btn_enter)
        val btnSettings = view.findViewById<Button>(R.id.btn_settings)
        historyContainer = view.findViewById<LinearLayout>(R.id.history_container)

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

        refreshHistoryView() // رسم النصوص عند فتح الكيبورد
        return view
    }

    // دالة تقوم برسم كل النصوص المخزنة وتضعها كأزرار يمكن النزول إليها
    private fun refreshHistoryView() {
        Handler(Looper.getMainLooper()).post {
            historyContainer?.removeAllViews()
            val count = getCountFromEngine()
            for (i in 0 until count) {
                val content = getDataFromEngine(i)
                val type = getTypeFromEngine(i)
                
                val btn = Button(this)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 10)
                btn.layoutParams = params
                btn.isAllCaps = false
                btn.setBackgroundColor(Color.parseColor("#FFFFFF"))
                btn.setTextColor(Color.parseColor("#000000"))
                
                // عرض أول 60 حرف فقط لكي لا يكون الزر ضخماً جداً
                if (type == 0) {
                    btn.text = if (content.length > 60) content.substring(0, 60) + "..." else content
                } else {
                    btn.text = "🖼️ مسار صورة/ملف محفوظ"
                }
                
                btn.setOnClickListener {
                    if (type == 0) currentInputConnection?.commitText(content, 1)
                    else currentInputConnection?.commitText("🖼️ " + content, 1)
                }
                
                historyContainer?.addView(btn)
            }
        }
    }
}
