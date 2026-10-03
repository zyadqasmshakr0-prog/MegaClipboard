package com.zayad.megaclipboard

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.graphics.Color
import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.ClipData
import android.widget.Toast
import android.view.ViewGroup

class MainActivity : Activity() {
    private var currentTab = 0 
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        EngineManager.initEngine(applicationContext.filesDir.absolutePath)
        val tabTemp = findViewById<Button>(R.id.tab_temp)
        val tabPinned = findViewById<Button>(R.id.tab_pinned)
        
        // هنا قمنا بإضافة النصوص برمجياً لتجنب خطأ الـ XML
        tabTemp.text = "⏳ المؤقتة (24س)"
        tabPinned.text = "📌 المثبتة (دائمة)"
        
        tabTemp.setOnClickListener {
            currentTab = 0
            tabTemp.setBackgroundColor(Color.parseColor("#FFFFFF"))
            tabTemp.setTextColor(Color.parseColor("#2196F3"))
            tabPinned.setBackgroundColor(Color.parseColor("#81D4FA"))
            tabPinned.setTextColor(Color.parseColor("#FFFFFF"))
            refreshUI()
        }
        
        tabPinned.setOnClickListener {
            currentTab = 1
            tabPinned.setBackgroundColor(Color.parseColor("#FFFFFF"))
            tabPinned.setTextColor(Color.parseColor("#2196F3"))
            tabTemp.setBackgroundColor(Color.parseColor("#81D4FA"))
            tabTemp.setTextColor(Color.parseColor("#FFFFFF"))
            refreshUI()
        }
        refreshUI()
    }
    
    private fun refreshUI() {
        val container = findViewById<LinearLayout>(R.id.main_history_container)
        container.removeAllViews()
        val count = EngineManager.getCountFromEngine()
        
        for (i in 0 until count) {
            val isPinned = EngineManager.isItemPinned(i)
            if (currentTab == 0 && isPinned) continue
            if (currentTab == 1 && !isPinned) continue
            
            val content = EngineManager.getDataFromEngine(i)
            val type = EngineManager.getTypeFromEngine(i)
            
            val btn = Button(this)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 15)
            btn.layoutParams = params
            btn.isAllCaps = false
            btn.setBackgroundColor(Color.parseColor("#FFFFFF"))
            btn.setTextColor(Color.parseColor("#000000"))
            
            val prefix = if (isPinned) "📌 " else "⏳ "
            btn.text = if (type == 0) prefix + (if (content.length > 80) content.substring(0, 80) + "..." else content) else prefix + "🖼️ صورة / ملف"
            
            btn.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("MegaClipboard", content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "تم النسخ إلى الهاتف!", Toast.LENGTH_SHORT).show()
            }
            
            btn.setOnLongClickListener {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("خيارات النص")
                val options = arrayOf(if(isPinned) "❌ إلغاء التثبيت" else "📌 تثبيت للأبد", "🗑️ حذف نهائي")
                builder.setItems(options) { _, which ->
                    if (which == 0) EngineManager.pinItem(i)
                    if (which == 1) EngineManager.deleteItem(i)
                    refreshUI()
                }
                builder.show()
                true
            }
            container.addView(btn)
        }
    }
}
