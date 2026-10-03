package com.zayad.megaclipboard

object EngineManager {
    init { System.loadLibrary("megaclipboard") }
    external fun initEngine(path: String)
    // لاحظ: أضفنا متغير isAutoPinned لمعرفة هل الحفظ الأبدي مفعل أم لا
    external fun addToEngine(data: String, type: Int, isAutoPinned: Boolean)
    external fun getDataFromEngine(index: Int): String
    external fun getTypeFromEngine(index: Int): Int
    external fun getCountFromEngine(): Int
    external fun cleanupEngine()
    external fun deleteItem(index: Int)
    external fun pinItem(index: Int)
    external fun isItemPinned(index: Int): Boolean
}
