package com.example

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*

fun main() {

    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json(
            kotlinx.serialization.json.Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            }
        )
    }

    val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(
                kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                }
            )
        }
    }

    val fraudUrl = System.getenv("FRAUD_URL")
        ?: "https://shieldnet-fraud.onrender.com/score"

    routing {

        get("/") {
            call.respond(mapOf("message" to "Ktor backend running 🚀"))
        }

        post("/api/risk/score") {

            try {

                val request = call.receive<FraudRequest>()

                val jsonBody = """
        {
          "worker_id": "${request.worker_id}",
          "city": "${request.city}",
          "event_type": "${request.event_type}",
          "policy_age_hours": ${request.policy_age_hours},
          "severity": ${request.severity}
        }
        """.trimIndent()

                val response: FraudResponse = client.post(fraudUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(jsonBody)
                }.body()

                call.respond(response)

            } catch (e: Exception) {

                e.printStackTrace()

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "unknown error"))
                )
            }
        }

        get("/test-risk") {

            val request = FraudRequest(
                worker_id = "123",
                city = "Mumbai",
                event_type = "rain",
                policy_age_hours = 1.0,
                severity = 0.8
            )

            val response: FraudResponse = client.post(fraudUrl) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()

            call.respond(response)
        }
        get("/test"){
            val sample=FraudRequest(worker_id="123",
                city="bihar",
                event_type="rain",
                policy_age_hours=1.0,
                severity=0.8)
            call.respond(sample)
        }

        post("/api/workers/register") {

            try {

                val user = call.receive<User>()

                val workerId = "worker_" + System.currentTimeMillis()

                val response = mapOf(
                    "id" to workerId,
                    "name" to user.name,
                    "phone" to user.phone,
                    "token" to "demo_token"
                )

                call.respond(response)

            } catch (e: Exception) {
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message)
                )
            }
        }
    }
}