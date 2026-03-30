package com.example

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val name: String,
    val phone: String,
    val city: String,
    val createdAt: Long = System.currentTimeMillis()
)