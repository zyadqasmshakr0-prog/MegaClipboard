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
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class ClipboardKeyboard : InputMethodService() {
    private lateinit var clipboardManager: ClipboardManager
    private var historyContainer: LinearLayout? = null
    
    private var dialogOverlay: LinearLayout? = null
    private var btnPin: Button? = null
    private var btnDelete: Button? = null
    private var btnCancel: Button? = null
    private var currentSelectedIndex = -1
    
    private fun isAutoPinEnabled(): Boolean {
        val prefs = getSharedPreferences("MegaPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("auto_pin", false)
    }
    
    // دالة تحويل الوقت إلى نص مقروء بالعربية
    private fun formatTimeAgo(timestampSeconds: Long): String {
        val date = Date(timestampSeconds * 1000L)
        val format = SimpleDateFormat("hh:mm a", Locale("ar"))
        val timeStr = format.format(date)
        
        val diff = (System.currentTimeMillis() / 1000L) - timestampSeconds
        val ago = when {
            diff < 60 -> "الآن"
            diff < 3600 -> "منذ {diff / 60} دقيقة"
            diff < 86400 -> "منذ {diff / 3600} ساعة"
            else -> "منذ {diff / 86400} يوم"
        }
        return "🕒 ago • الساعة timeStr"
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
        
        view.findViewById<Button>(R.id.btn_delete).apply {
            text = "⌫"
            setOnClickListener {
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
            }
        }
        view.findViewById<Button>(R.id.btn_enter).apply {
            text = "↵"
            setOnClickListener {
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
        }
        view.findViewById<Button>(R.id.btn_settings).apply {
            text = "⚙"
            setOnClickListener {
                val intent = Intent(this@ClipboardKeyboard, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }
        
        historyContainer = view.findViewById(R.id.history_container)
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
                val timestamp = EngineManager.getTimestampFromEngine(i)
                val timeString = formatTimeAgo(timestamp)
                
                val btn = Button(this@ClipboardKeyboard)
                val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, 0, 10)
                btn.layoutParams = params
                btn.isAllCaps = false
                btn.setBackgroundColor(Color.parseColor("#FFFFFF"))
                btn.setTextColor(Color.parseColor("#000000"))
                
                val prefix = if (isPinned) "📌 " else "⏳ "
                val mainText = if (type == 0) if (content.length > 60) content.substring(0, 60) + "..." else content else "🖼️ صورة / ملف"
                
                // دمج النص مع التاريخ في سطرين
                btn.text = "prefix mainText\ntimeString"
                
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
