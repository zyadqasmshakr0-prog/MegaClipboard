package com.zayad.megaclipboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.widget.Button

class ClipboardKeyboard : InputMethodService() {
    
    // 1. تشغيل محرك C++
    init { System.loadLibrary("megaclipboard") }
    
    // 2. تعريف الدوال المتصلة بـ C++
    external fun addTextToEngine(text: String)
    external fun getTextFromEngine(index: Int): String

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        val btnPaste = view.findViewById<Button>(R.id.btn_paste)

        // محاكاة: إضافة نص إلى محرك C++ عند فتح الكيبورد
        addTextToEngine("مرحباً بك! هذا النص تمت معالجته وحفظه في ذاكرة C++ ثم حقنه كأنه كيبورد حقيقي.")

        btnPaste.setOnClickListener {
            // جلب النص الأحدث من C++ (الرقم 0)
            val textFromCpp = getTextFromEngine(0)
            
            // حقن النص فورا في مكان المؤشر (واتساب، متصفح، الخ)
            currentInputConnection?.commitText(textFromCpp, 1)
        }
        return view
    }
}
