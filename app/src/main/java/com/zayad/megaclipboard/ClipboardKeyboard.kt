package com.zayad.megaclipboard

import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button

class ClipboardKeyboard : InputMethodService() {
    
    init { System.loadLibrary("megaclipboard") }
    
    // تعريف الدوال الجديدة المتصلة بـ C++
    external fun initEngine(path: String)
    external fun addToEngine(data: String, type: Int)
    external fun getDataFromEngine(index: Int): String
    external fun getTypeFromEngine(index: Int): Int

    private lateinit var clipboardManager: ClipboardManager
    
    // المستشعر الذكي: يفرق بين النصوص والصور
    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        val clip = clipboardManager.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val item = clip.getItemAt(0)
            
            if (item.text != null && item.text.isNotEmpty()) {
                addToEngine(item.text.toString(), 0) // إرسال كنص
            } else if (item.uri != null) {
                addToEngine(item.uri.toString(), 1) // إرسال كصورة/ملف
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        // تهيئة C++ وإعطائه المسار السري في ذاكرة الهاتف لحفظ الملفات للأبد
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
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        val btnPaste = view.findViewById<Button>(R.id.btn_paste)

        btnPaste.setOnClickListener {
            // جلب أحدث عنصر تم نسخه من محرك C++
            val content = getDataFromEngine(0)
            val type = getTypeFromEngine(0)
            
            if (content.isNotEmpty()) {
                if (type == 0) {
                    // إذا كان نصاً، الصقه فوراً
                    currentInputConnection?.commitText(content, 1)
                } else {
                    // إذا كان صورة، نعرض رابط مسارها المحفوظ كإثبات لنجاح العملية
                    currentInputConnection?.commitText("🖼️ تم حفظ الصورة بنجاح! الرابط: " + content, 1)
                }
            }
        }
        return view
    }
}
