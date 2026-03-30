package com.example

import kotlinx.serialization.Serializable

@Serializable
data class FraudLog(
    val id: String,
    val userId: String,
    val fraudScore: Double,
    val decision: String,
    val flags: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)