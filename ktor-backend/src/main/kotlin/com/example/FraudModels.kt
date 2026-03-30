package com.example

import kotlinx.serialization.Serializable

@Serializable
data class FraudRequest(
    val worker_id: String,
    val city: String,
    val event_type: String,
    val policy_age_hours: Double,
    val severity: Double
)

@Serializable
data class FraudResponse(
    val fraud_score: Double,
    val flags: List<String>,
    val decision: String
)