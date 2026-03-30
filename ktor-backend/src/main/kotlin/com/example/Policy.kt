package com.example

import kotlinx.serialization.Serializable

@Serializable
data class Policy(
    val id: String,
    val userId: String,
    val premium: Int,
    val coverage: Int,
    val tier: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)