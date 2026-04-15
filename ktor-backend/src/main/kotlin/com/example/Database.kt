package com.example

import org.litote.kmongo.coroutine.*
import org.litote.kmongo.reactivestreams.KMongo

object Database {

    private val uri = System.getenv("MONGO_URI")
        ?: ""

    private val client = KMongo.createClient(uri).coroutine
    val database = client.getDatabase("shieldnet")

    val users = database.getCollection<User>()
    val policies = database.getCollection<Policy>()
    val fraudLogs = database.getCollection<FraudLog>()
    val claims = database.getCollection<ClaimResponse>()
}