package com.example

import com.mongodb.client.MongoClients
import org.bson.Document

object MongoService {

    private val uri = System.getenv("MONGO_URI") ?: "mongodb://localhost:27017"

    private val client = MongoClients.create(uri)
    private val database = client.getDatabase("shieldnet")
    private val collection = database.getCollection("claims")

    fun saveClaim(request: FraudRequest, response: FraudResponse) {

        val doc = Document()
            .append("worker_id", request.worker_id)
            .append("city", request.city)
            .append("event_type", request.event_type)
            .append("policy_age_hours", request.policy_age_hours)
            .append("severity", request.severity)
            .append("fraud_score", response.fraud_score)
            .append("decision", response.decision)

        collection.insertOne(doc)
    }
}