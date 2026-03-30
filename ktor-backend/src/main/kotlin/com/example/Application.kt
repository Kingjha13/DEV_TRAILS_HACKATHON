package com.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

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

    routing {

        get("/") {
            call.respond(mapOf("message" to "Ktor backend running 🚀"))
        }

        post("/check-fraud") {

            val request = call.receive<FraudRequest>()

            val fraudClient = FraudClient()
            val response = fraudClient.checkFraud(request)

            Database.fraudLogs.insertOne(
                FraudLog(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = request.worker_id,
                    fraudScore = response.fraud_score,
                    decision = response.decision,
                    flags = response.flags
                )
            )

            call.respond(response)
        }

        post("/register") {

            val user = call.receive<User>()

            Database.users.insertOne(user)

            call.respond(mapOf("status" to "saved"))
        }
    }
}