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
import android.widget.Switch
import android.content.SharedPreferences
import android.content.Intent
import android.provider.Settings
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : Activity() {
    private var currentTab = 0 
    private lateinit var sharedPrefs: SharedPreferences
    
    // دالة تحويل الوقت
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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        EngineManager.initEngine(applicationContext.filesDir.absolutePath)
        sharedPrefs = getSharedPreferences("MegaPrefs", Context.MODE_PRIVATE)
        
        val btnActivate = findViewById<Button>(R.id.btn_activate_keyboard)
        btnActivate.text = "⚙️ تفعيل لوحة المفاتيح"
        btnActivate.setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        
        val switchAutoPin = findViewById<Switch>(R.id.switch_auto_pin)
        switchAutoPin.text = "حفظ تلقائي أبدي (النصوص لا تُحذف)"
        switchAutoPin.isChecked = sharedPrefs.getBoolean("auto_pin", false)
        switchAutoPin.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.edit().putBoolean("auto_pin", isChecked).apply()
            Toast.makeText(this, if(isChecked) "تم تفعيل الحفظ الأبدي" else "تم إيقاف الحفظ الأبدي", Toast.LENGTH_SHORT).show()
        }
        
        val tabTemp = findViewById<Button>(R.id.tab_temp)
        val tabPinned = findViewById<Button>(R.id.tab_pinned)
        
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
    }

    override fun onResume() {
        super.onResume()
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val clip = clipboard.primaryClip
                if (clip != null && clip.itemCount > 0) {
                    val item = clip.getItemAt(0)
                    val autoPin = sharedPrefs.getBoolean("auto_pin", false)
                    if (item.text != null && item.text.isNotEmpty()) {
                        EngineManager.addToEngine(item.text.toString(), 0, autoPin)
                    } else if (item.uri != null) {
                        EngineManager.addToEngine(item.uri.toString(), 1, autoPin)
                    }
                }
            }
        } catch (e: Exception) { }
        
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
            val timestamp = EngineManager.getTimestampFromEngine(i)
            val timeString = formatTimeAgo(timestamp)
            
            val btn = Button(this)
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(0, 0, 0, 15)
            btn.layoutParams = params
            btn.isAllCaps = false
            btn.setBackgroundColor(Color.parseColor("#FFFFFF"))
            btn.setTextColor(Color.parseColor("#000000"))
            
            val prefix = if (isPinned) "📌 " else "⏳ "
            val mainText = if (type == 0) (if (content.length > 80) content.substring(0, 80) + "..." else content) else "🖼️️ صورة / ملف"
            
            // دمج النص مع التاريخ
            btn.text = "prefix mainText\ntimeString"
            
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
