package com.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

fun main() {

    val port = System.getenv("PORT")?.toInt() ?: 8081

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    // ✅ HTTP CLIENT
    val client = HttpClient(CIO)

    // ✅ FRAUD SERVICE URL
    val fraudUrl = System.getenv("FRAUD_URL")
        ?: "http://localhost:8080/score"

    routing {

        get("/") {
            call.respond(mapOf("message" to "Ktor backend running 🚀"))
        }

        post("/check-fraud") {

            val request = call.receive<FraudRequest>()

            val response: FraudResponse = client.post(fraudUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            // 🔥 SAVE TO MONGODB
            MongoService.saveClaim(request, response)

            call.respond(response)
        }

        post("/register") {

            val user = call.receive<User>()

            Database.users.insertOne(user)

            call.respond(mapOf("status" to "saved"))
        }
    }
}