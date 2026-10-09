package com.ernest.geo.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class GeoAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: GeoAccessibilityService? = null
        
        fun getScreenText(): String {
            val root = instance?.rootInActiveWindow ?: return ""
            val sb = StringBuilder()
            extractText(root, sb)
            return sb.toString()
        }

        private fun extractText(node: AccessibilityNodeInfo?, sb: StringBuilder) {
            if (node == null) return
            if (node.text != null) {
                sb.append(node.text).append("\n")
            }
            for (i in 0 until node.childCount) {
                extractText(node.getChild(i), sb)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d("GEO_ACCESSIBILITY", "Service Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }
}