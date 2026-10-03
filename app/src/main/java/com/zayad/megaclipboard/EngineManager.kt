package com.zayad.megaclipboard

object EngineManager {
init { System.loadLibrary("megaclipboard") }
external fun initEngine(path: String)
external fun addToEngine(data: String, type: Int)
external fun getDataFromEngine(index: Int): String
external fun getTypeFromEngine(index: Int): Int
external fun getCountFromEngine(): Int
external fun cleanupEngine()
external fun deleteItem(index: Int)
external fun pinItem(index: Int)
external fun isItemPinned(index: Int): Boolean
}
