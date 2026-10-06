package com.zayad.megaclipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AlertDialog
import android.widget.Button
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ClipboardKeyboard : InputMethodService() {

    private lateinit var clipboardManager: ClipboardManager

    private var rootKeyboard: LinearLayout? = null
    private var topToolbar: LinearLayout? = null
    private var layoutKeysContainer: LinearLayout? = null
    private var layoutClipboardContainer: LinearLayout? = null
    private var historyContainer: LinearLayout? = null
    private var btnToggleView: Button? = null

    private var dialogOverlay: LinearLayout? = null
    private var btnPin: Button? = null
    private var btnDelete: Button? = null
    private var btnCancel: Button? = null

    private var isClipboardMode = false
    private var currentThemeIndex = 0
    private var currentLayoutIndex = 0
    private var numberMode = false
    private var shiftMode = false
    private var currentSelectedIndex = -1

    private val prefsName = "MegaPrefs"

    private fun prefs() =
        getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    private fun isAutoPinEnabled(): Boolean =
        prefs().getBoolean("auto_pin", false)

    private fun formatTime(ts: Long): String {
        if (ts <= 0L) return ""
        return SimpleDateFormat("hh:mm a", Locale("ar"))
            .format(Date(ts * 1000L))
    }

    private val clipboardListener =
        ClipboardManager.OnPrimaryClipChangedListener {

            val clip = clipboardManager.primaryClip ?: return@OnPrimaryClipChangedListener
            if (clip.itemCount <= 0) return@OnPrimaryClipChangedListener

            val item = clip.getItemAt(0)

            if (item.text != null && item.text.isNotEmpty()) {
                EngineManager.addToEngine(
                    item.text.toString(),
                    0,
                    isAutoPinEnabled()
                )

                if (isClipboardMode) {
                    refreshHistoryView()
                }

            } else if (item.uri != null) {

                EngineManager.addToEngine(
                    item.uri.toString(),
                    1,
                    isAutoPinEnabled()
                )

                if (isClipboardMode) {
                    refreshHistoryView()
                }
            }
        }

    override fun onCreate() {
        super.onCreate()

        EngineManager.initEngine(
            applicationContext.filesDir.absolutePath
        )

        clipboardManager =
            getSystemService(Context.CLIPBOARD_SERVICE)
                    as ClipboardManager

        clipboardManager.addPrimaryClipChangedListener(
            clipboardListener
        )

        currentThemeIndex =
            prefs().getInt("theme_index", 0)

        currentLayoutIndex =
            prefs().getInt("keyboard_layout", 0)
    }

    override fun onDestroy() {

        if (::clipboardManager.isInitialized) {
            clipboardManager.removePrimaryClipChangedListener(
                clipboardListener
            )
        }

        super.onDestroy()
    }

    override fun onCreateInputView(): View {

        EngineManager.cleanupEngine()

        val view =
            layoutInflater.inflate(
                R.layout.keyboard_view,
                null
            )

        rootKeyboard =
            view.findViewById(R.id.root_keyboard_view)

        topToolbar =
            view.findViewById(R.id.top_toolbar)

        layoutKeysContainer =
            view.findViewById(R.id.layout_keys_container)

        layoutClipboardContainer =
            view.findViewById(R.id.layout_clipboard_container)

        historyContainer =
            view.findViewById(R.id.history_container)

        btnToggleView =
            view.findViewById(R.id.btn_toggle_view)

        view.findViewById<Button>(
            R.id.btn_settings_top
        ).setOnClickListener {
            openKeyboardSettings()
        }

        btnToggleView?.setOnClickListener {

            isClipboardMode = !isClipboardMode

            if (isClipboardMode) {

                btnToggleView?.text = "⌨️"

                layoutKeysContainer?.visibility =
                    View.GONE

                layoutClipboardContainer?.visibility =
                    View.VISIBLE

                refreshHistoryView()

            } else {

                btnToggleView?.text = "📋"

                layoutClipboardContainer?.visibility =
                    View.GONE

                layoutKeysContainer?.visibility =
                    View.VISIBLE
            }
        }

        view.findViewById<Button>(
            R.id.btn_theme
        ).setOnClickListener {

            currentThemeIndex =
                (currentThemeIndex + 1) % 3

            prefs().edit()
                .putInt(
                    "theme_index",
                    currentThemeIndex
                )
                .apply()

            applyTheme()
            generateKeyboardLayout()

            if (isClipboardMode) {
                refreshHistoryView()
            }
        }

        dialogOverlay =
            view.findViewById(R.id.dialog_overlay)

        btnPin =
            view.findViewById(R.id.dialog_btn_pin)

        btnDelete =
            view.findViewById(R.id.dialog_btn_delete)

        btnCancel =
            view.findViewById(R.id.dialog_btn_cancel)

        btnCancel?.setOnClickListener {
            dialogOverlay?.visibility =
                View.GONE
        }

        btnDelete?.setOnClickListener {

            if (currentSelectedIndex >= 0) {

                EngineManager.deleteItem(
                    currentSelectedIndex
                )

                currentSelectedIndex = -1

                dialogOverlay?.visibility =
                    View.GONE

                refreshHistoryView()
            }
        }

        btnPin?.setOnClickListener {

            if (currentSelectedIndex >= 0) {

                EngineManager.pinItem(
                    currentSelectedIndex
                )

                currentSelectedIndex = -1

                dialogOverlay?.visibility =
                    View.GONE

                refreshHistoryView()
            }
        }

        applyTheme()
        generateKeyboardLayout()

        return view
    }

    private fun openKeyboardSettings() {

        val popupLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(
                when (currentThemeIndex) {
                    1 -> Color.WHITE
                    else -> Color.parseColor("#252525")
                }
            )
        }

        val title = Button(this).apply {
            text = "⚙️ إعدادات التخطيط"
            isAllCaps = false
            setTextColor(
                if (currentThemeIndex == 1) Color.BLACK else Color.WHITE
            )
            setBackgroundColor(Color.TRANSPARENT)
            textSize = 18f
        }

        popupLayout.addView(
            title,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val names = listOf(
            "🇾🇪 عربي قياسي",
            "⚡ عربي سريع",
            "🇺🇸 English QWERTY"
        )

        names.forEachIndexed { index, name ->

            val button = Button(this).apply {
                text = if (index == currentLayoutIndex)
                    "✓ $name"
                else
                    name

                isAllCaps = false

                setTextColor(
                    if (currentThemeIndex == 1)
                        Color.BLACK
                    else
                        Color.WHITE
                )

                setBackgroundColor(
                    if (index == currentLayoutIndex)
                        Color.parseColor("#1565C0")
                    else
                        Color.TRANSPARENT
                )

                setOnClickListener {

                    currentLayoutIndex = index

                    prefs().edit()
                        .putInt(
                            "keyboard_layout",
                            currentLayoutIndex
                        )
                        .apply()

                    numberMode = false
                    shiftMode = false

                    generateKeyboardLayout()

                    popup.dismiss()
                }
            }

            popupLayout.addView(
                button,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val cancel = Button(this).apply {
            text = "إلغاء"
            isAllCaps = false

            setOnClickListener {
                popup.dismiss()
            }
        }

        popupLayout.addView(
            cancel,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val popup = android.widget.PopupWindow(
            popupLayout,
            (resources.displayMetrics.widthPixels * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        popup.setBackgroundDrawable(
            android.graphics.drawable.ColorDrawable(
                Color.TRANSPARENT
            )
        )

        popup.elevation = 12f

        popup.showAtLocation(
            rootKeyboard,
            android.view.Gravity.CENTER,
            0,
            0
        )
    }

    private fun cycleKeyboardLayout() {

        currentLayoutIndex =
            (currentLayoutIndex + 1) % 3

        prefs().edit()
            .putInt(
                "keyboard_layout",
                currentLayoutIndex
            )
            .apply()

        numberMode = false
        shiftMode = false

        generateKeyboardLayout()
    }

    private fun generateKeyboardLayout() {

        layoutKeysContainer?.removeAllViews()

        if (numberMode) {
            generateNumberLayout()
            return
        }

        val rows: List<List<String>> =
            when (currentLayoutIndex) {

                0 -> listOf(
                    listOf(
                        "ض","ص","ث","ق","ف","غ",
                        "ع","ه","خ","ح","ج","د"
                    ),
                    listOf(
                        "ش","س","ي","ب","ل","ا",
                        "ت","ن","م","ك","ط"
                    ),
                    listOf(
                        "⇧","ئ","ء","ؤ","ر","لا",
                        "ى","ة","و","ز","ظ","⌫"
                    ),
                    listOf(
                        "?123","🌐","☺","مسافة",".","↵"
                    )
                )

                1 -> listOf(
                    listOf(
                        "ق","و","ع","ر","ت","ي",
                        "ب","ل","ا","د","س","م"
                    ),
                    listOf(
                        "ن","ك","ط","ح","ض","ص",
                        "ث","خ","ج","ف","غ"
                    ),
                    listOf(
                        "⇧","ش","ه","ة","و","ز",
                        "ظ","ذ","ء","ئ","⌫"
                    ),
                    listOf(
                        "?123","🌐","☺","مسافة",".","↵"
                    )
                )

                else -> listOf(
                    listOf(
                        "q","w","e","r","t","y",
                        "u","i","o","p"
                    ),
                    listOf(
                        "a","s","d","f","g","h",
                        "j","k","l"
                    ),
                    listOf(
                        "⇧","z","x","c","v","b",
                        "n","m","⌫"
                    ),
                    listOf(
                        "?123","🌐","☺","مسافة",".","↵"
                    )
                )
            }

        rows.forEach { row ->

            val rowLayout =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1f
                        )
                }

            row.forEach { key ->

                createKeyButton(
                    rowLayout,
                    key
                )
            }

            layoutKeysContainer?.addView(
                rowLayout
            )
        }
    }

    private fun generateNumberLayout() {

        val rows = listOf(
            listOf(
                "1","2","3","4","5","6","7","8","9","0"
            ),
            listOf(
                "@","#","$","%","&","*","-","+","=","/"
            ),
            listOf(
                "(" ,")","[","]","{","}","_","\"","'","⌫"
            ),
            listOf(
                "ABC","🌐","☺","مسافة",".","↵"
            )
        )

        rows.forEach { row ->

            val rowLayout =
                LinearLayout(this).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            0,
                            1f
                        )
                }

            row.forEach { key ->
                createKeyButton(
                    rowLayout,
                    key
                )
            }

            layoutKeysContainer?.addView(
                rowLayout
            )
        }
    }

    private fun createKeyButton(
        rowLayout: LinearLayout,
        key: String
    ) {

        val bgColor =
            when (currentThemeIndex) {
                0 -> Color.parseColor("#333333")
                1 -> Color.WHITE
                else -> Color.parseColor("#1565C0")
            }

        val textColor =
            if (currentThemeIndex == 1)
                Color.BLACK
            else
                Color.WHITE

        val shownKey =
            displayKey(key)

        val btn =
            Button(this).apply {

                text = shownKey
                isAllCaps = false

                setBackgroundColor(
                    bgColor
                )

                setTextColor(
                    textColor
                )

                setPadding(
                    0,0,0,0
                )

                textSize = 17f
            }

        var weight = 1f

        when (key) {

            "مسافة" ->
                weight = 4f

            "↵",
            "⌫",
            "⇧",
            "?123",
            "ABC",
            "🌐",
            "☺" ->
                weight = 1.5f
        }

        val params =
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                weight
            )

        params.setMargins(
            3,3,3,3
        )

        btn.layoutParams = params

        btn.setOnClickListener {
            handleKey(key)
        }

        rowLayout.addView(btn)
    }

    private fun displayKey(key: String): String {

        if (
            currentLayoutIndex == 2 &&
            !numberMode &&
            shiftMode &&
            key.length == 1 &&
            key[0].isLetter()
        ) {
            return key.uppercase(Locale.US)
        }

        return key
    }

    private fun handleKey(key: String) {

        val connection =
            currentInputConnection
                ?: return

        when (key) {

            "⌫" -> {

                connection.sendKeyEvent(
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_DEL
                    )
                )

                connection.sendKeyEvent(
                    KeyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_DEL
                    )
                )
            }

            "↵" -> {

                connection.sendKeyEvent(
                    KeyEvent(
                        KeyEvent.ACTION_DOWN,
                        KeyEvent.KEYCODE_ENTER
                    )
                )

                connection.sendKeyEvent(
                    KeyEvent(
                        KeyEvent.ACTION_UP,
                        KeyEvent.KEYCODE_ENTER
                    )
                )
            }

            "مسافة" -> {
                connection.commitText(
                    " ",
                    1
                )
            }

            "⇧" -> {

                shiftMode =
                    !shiftMode

                generateKeyboardLayout()
            }

            "?123" -> {

                numberMode = true
                generateKeyboardLayout()
            }

            "ABC" -> {

                numberMode = false
                generateKeyboardLayout()
            }

            "🌐" -> {
                cycleKeyboardLayout()
            }

            "☺" -> {

                connection.commitText(
                    "😊",
                    1
                )
            }

            else -> {

                val text =
                    if (
                        currentLayoutIndex == 2 &&
                        shiftMode &&
                        key.length == 1
                    ) {
                        key.uppercase(Locale.US)
                    } else {
                        key
                    }

                connection.commitText(
                    text,
                    1
                )

                if (
                    currentLayoutIndex == 2 &&
                    shiftMode
                ) {
                    shiftMode = false
                    generateKeyboardLayout()
                }
            }
        }
    }

    private fun applyTheme() {

        val rootBg =
            when (currentThemeIndex) {

                0 ->
                    Color.parseColor("#1E1E1E")

                1 ->
                    Color.parseColor("#EAEAEA")

                else ->
                    Color.parseColor("#0D47A1")
            }

        val toolbarBg =
            when (currentThemeIndex) {

                0 ->
                    Color.parseColor("#121212")

                1 ->
                    Color.parseColor("#D6D6D6")

                else ->
                    Color.parseColor("#002171")
            }

        rootKeyboard?.setBackgroundColor(
            rootBg
        )

        topToolbar?.setBackgroundColor(
            toolbarBg
        )

        val iconColor =
            if (currentThemeIndex == 1)
                Color.BLACK
            else
                Color.WHITE

        for (
            i in 0 until
                (topToolbar?.childCount ?: 0)
        ) {

            val child =
                topToolbar?.getChildAt(i)

            if (child is Button) {
                child.setTextColor(
                    iconColor
                )
            }
        }
    }

    private fun refreshHistoryView() {

        if (!isClipboardMode) {
            return
        }

        Handler(
            Looper.getMainLooper()
        ).post {

            historyContainer?.removeAllViews()

            val count =
                EngineManager.getCountFromEngine()

            val btnBgColor =
                if (currentThemeIndex == 1)
                    Color.WHITE
                else
                    Color.parseColor("#333333")

            val btnTextColor =
                if (currentThemeIndex == 1)
                    Color.BLACK
                else
                    Color.WHITE

            for (i in 0 until count) {

                val content =
                    EngineManager.getDataFromEngine(i)

                val type =
                    EngineManager.getTypeFromEngine(i)

                val isPinned =
                    EngineManager.isItemPinned(i)

                val timestamp =
                    EngineManager.getTimestampFromEngine(i)

                val btn =
                    Button(this@ClipboardKeyboard)

                val params =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                params.setMargins(
                    0,0,0,10
                )

                btn.layoutParams = params

                btn.isAllCaps = false

                btn.setBackgroundColor(
                    btnBgColor
                )

                btn.setTextColor(
                    btnTextColor
                )

                val prefix =
                    if (isPinned)
                        "📌 "
                    else
                        "⏳ "

                val mainText =
                    if (type == 0) {

                        if (content.length > 80)
                            content.substring(
                                0,80
                            ) + "..."
                        else
                            content

                    } else {

                        "🖼️ صورة / ملف"
                    }

                val timeStr =
                    formatTime(timestamp)

                val timeAppend =
                    if (timeStr.isNotEmpty())
                        "  🕒 $timeStr"
                    else
                        ""

                btn.text =
                    prefix +
                    mainText +
                    timeAppend

                btn.setOnClickListener {

                    if (type == 0) {

                        currentInputConnection
                            ?.commitText(
                                content,
                                1
                            )

                    } else {

                        currentInputConnection
                            ?.commitText(
                                "🖼 $content",
                                1
                            )
                    }
                }

                btn.setOnLongClickListener {

                    currentSelectedIndex =
                        i

                    btnPin?.text =
                        if (isPinned)
                            "❌ إلغاء التثبيت"
                        else
                            "📌 تثبيت للأبد"

                    dialogOverlay?.visibility =
                        View.VISIBLE

                    true
                }

                historyContainer?.addView(
                    btn
                )
            }
        }
    }
}


