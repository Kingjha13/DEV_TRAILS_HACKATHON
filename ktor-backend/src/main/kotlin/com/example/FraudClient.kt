package com.example

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*

class FraudClient {

    private val client = HttpClient(CIO)

    suspend fun checkFraud(request: FraudRequest): FraudResponse {

        val fraudUrl = System.getenv("FRAUD_URL")
            ?: "http://localhost:8080/score"

        return client.post(fraudUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }
}