package com.example

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val name: String,
    val phone: String,
    val city: String,
    val platform: String,

    @SerialName("weekly_avg")
    val weeklyAvg: Int,

    @SerialName("upi_handle")
    val upiHandle: String
)