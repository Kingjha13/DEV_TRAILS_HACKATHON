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

    val port = System.getenv("PORT")?.toInt() ?: 8080

    embeddedServer(Netty, port = port, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {

    install(ContentNegotiation) {
        json()
    }

    val client = HttpClient(CIO)

    val fraudUrl = System.getenv("FRAUD_URL")
        ?: "https://shieldnet-fraud.onrender.com/score"

    routing {

        get("/") {
            call.respond(mapOf("message" to "Ktor backend running 🚀"))
        }

        post("/api/risk/score") {

            try {

                val request = call.receive<FraudRequest>()

                val response: HttpResponse = client.post(fraudUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                val responseBody = response.bodyAsText()

                call.respondText(
                    text = responseBody,
                    contentType = ContentType.Application.Json
                )

            } catch (e: Exception) {

                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to (e.message ?: "unknown error"))
                )
            }
        }

        post("/register") {

            try {
                val user = call.receive<User>()
                Database.users.insertOne(user)

                call.respond(mapOf("status" to "saved"))

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to e.message)
                )
            }
        }
    }
}