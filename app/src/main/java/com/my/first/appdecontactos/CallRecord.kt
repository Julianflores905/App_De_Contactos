package com.my.first.appdecontactos

data class CallRecord(
    val id: String = "",
    val phoneNumber: String = "",
    val contactName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
