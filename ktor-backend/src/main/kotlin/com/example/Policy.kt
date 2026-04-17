package com.example

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Policy(
    val id: String,

    @SerialName("worker_id")
    val workerId: String,

    val premium: Int,
    val coverage: Int,
    val tier: String,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)