use actix_web::{web, App, HttpServer, HttpResponse};
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::sync::Mutex;
use chrono::Utc;

mod ml;
use ml::FraudModel;

struct AppState {
    claim_history: Mutex<HashMap<String, Vec<i64>>>,
    city_claims: Mutex<HashMap<String, Vec<(String, i64)>>>,
    model: FraudModel,
}

#[derive(Deserialize)]
struct FraudRequest {
    worker_id: String,
    city: String,
    event_type: String,
    policy_age_hours: f64,
    severity: f64,
}

#[derive(Serialize)]
struct FraudResponse {
    fraud_score: f64,
    flags: Vec<String>,
    decision: String,
}

fn score_rules(req: &FraudRequest, state: &AppState) -> (f64, Vec<String>) {
    let now = Utc::now().timestamp();
    let mut score = 0.0;
    let mut flags = Vec::new();

    if req.policy_age_hours < 2.0 {
        let penalty = (2.0 - req.policy_age_hours) / 2.0 * 0.5;
        score += penalty;
        flags.push(format!("Policy too new ({:.1}h)", req.policy_age_hours));
    }

    {
        let mut history = state.claim_history.lock().unwrap();
        let worker_claims = history.entry(req.worker_id.clone()).or_default();

        let cutoff = now - 30 * 24 * 3600;
        worker_claims.retain(|&t| t > cutoff);

        let count = worker_claims.len();

        if count >= 3 {
            score += 0.4;
            flags.push(format!("{} claims in 30 days", count));
        } else if count == 2 {
            score += 0.2;
            flags.push("Multiple recent claims".to_string());
        }

        worker_claims.push(now);
    }

    {
        let mut city_map = state.city_claims.lock().unwrap();
        let city_claims = city_map.entry(req.city.clone()).or_default();

        let cutoff = now - 600;
        city_claims.retain(|(_, t)| *t > cutoff);

        let unique: HashSet<&str> = city_claims
            .iter()
            .filter(|(wid, _)| wid != &req.worker_id)
            .map(|(wid, _)| wid.as_str())
            .collect();

        if unique.len() > 5 {
            score += 0.2;
            flags.push("Cluster activity detected".to_string());
        }

        city_claims.push((req.worker_id.clone(), now));
    }

    if req.severity < 0.2 {
        score += 0.3;
        flags.push("Suspiciously low severity".to_string());
    }

    (score.min(1.0), flags)
}

async fn health() -> HttpResponse {
    HttpResponse::Ok().json(serde_json::json!({
        "status": "ok",
        "service": "shieldnet-fraud"
    }))
}

async fn score_endpoint(
    req: web::Json<FraudRequest>,
    data: web::Data<AppState>,
) -> HttpResponse {

    let (rule_score, mut flags) = score_rules(&req, &data);

    let claim_count = {
        let history = data.claim_history.lock().unwrap();
        history.get(&req.worker_id).map(|v| v.len()).unwrap_or(0)
    };

    let cluster_size = {
        let city_map = data.city_claims.lock().unwrap();
        city_map.get(&req.city).map(|v| v.len()).unwrap_or(0)
    };

    let features = vec![
        req.policy_age_hours,
        req.severity,
        claim_count as f64,
        cluster_size as f64,
    ];

    let ml_score = data.model.predict(features);

    let final_score = (rule_score * 0.7) + (ml_score * 0.3);

    let decision = if final_score > 0.75 {
        flags.push("High fraud probability".to_string());
        "reject"
    } else if final_score > 0.45 {
        flags.push("Needs manual review".to_string());
        "review"
    } else {
        "approve"
    };

    println!(
        "📊 FRAUD → rule: {:.2}, ml: {:.2}, final: {:.2}, decision: {}",
        rule_score, ml_score, final_score, decision
    );

    HttpResponse::Ok().json(FraudResponse {
        fraud_score: final_score,
        flags,
        decision: decision.to_string(),
    })
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    let addr = "0.0.0.0:8080";

    println!("🚀 Fraud Service running at {}", addr);

    let state = web::Data::new(AppState {
        claim_history: Mutex::new(HashMap::new()),
        city_claims: Mutex::new(HashMap::new()),
        model: FraudModel::new(),
    });

    HttpServer::new(move || {
        App::new()
            .app_data(state.clone())
            .route("/health", web::get().to(health))
            .route("/score", web::post().to(score_endpoint))
    })
    .bind(addr)?
    .run()
    .await
}