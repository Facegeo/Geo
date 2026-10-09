package com.ernest.geo.util

import android.content.Context
import android.provider.ContactsContract

object ContactHelper {
    fun getNumberFromName(context: Context, name: String): String? {
        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        
        val cursor = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            
            while (it.moveToNext()) {
                val contactName = it.getString(nameIndex)
                if (contactName.equals(name, ignoreCase = true) || contactName.contains(name, ignoreCase = true)) {
                    return it.getString(numberIndex)
                }
            }
        }
        return null
    }
}