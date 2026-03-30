use actix_web::{web, App, HttpServer, HttpResponse, middleware};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Mutex;
mod ml;
use ml::FraudModel;

struct AppState {
    claim_history: Mutex<HashMap<String, Vec<i64>>>,
    city_claims: Mutex<HashMap<String, Vec<(String, i64)>>>,
    model: FraudModel,
}

#[derive(Deserialize)]
struct FraudRequest {
    worker_id:         String,
    city:              String,
    event_type:        String,
    policy_age_hours:  f64,
    severity:          f64,
}

#[derive(Serialize)]
struct FraudResponse {
    fraud_score:  f64,
    flags:        Vec<String>,
    decision:     String,
}

fn score_fraud(req: &FraudRequest, state: &AppState) -> (f64, Vec<String>) {
    let now = chrono::Utc::now().timestamp();
    let mut score: f64 = 0.0;
    let mut flags: Vec<String> = Vec::new();
    if req.policy_age_hours < 2.0 {
        let penalty = (2.0 - req.policy_age_hours) / 2.0 * 0.5;
        score += penalty;
        flags.push(format!(
            "Policy only {:.1}h old when claim triggered (threshold: 2h)",
            req.policy_age_hours
        ));
    }

    {
        let mut history = state.claim_history.lock().unwrap();
        let worker_claims = history.entry(req.worker_id.clone()).or_default();

        let cutoff_30d = now - 30 * 24 * 3600;
        worker_claims.retain(|&t| t > cutoff_30d);

        let recent_count = worker_claims.len();
        if recent_count >= 3 {
            score += 0.4;
            flags.push(format!("{} claims in last 30 days (threshold: 3)", recent_count));
        } else if recent_count == 2 {
            score += 0.15;
            flags.push(format!("{} claims in last 30 days — monitoring", recent_count));
        }

        worker_claims.push(now);
    }

    {
        let mut city_map = state.city_claims.lock().unwrap();
        let city_claims = city_map.entry(req.city.clone()).or_default();

        let cutoff_10min = now - 600;
        city_claims.retain(|(_, t)| *t > cutoff_10min);

        let unique_workers: std::collections::HashSet<&str> = city_claims
            .iter()
            .filter(|(wid, _)| wid != &req.worker_id)
            .map(|(wid, _)| wid.as_str())
            .collect();

        if unique_workers.len() > 5 {
            score += 0.2;
            flags.push(format!(
                "{} workers in {} claimed in last 10 min — cluster detected",
                unique_workers.len(),
                req.city
            ));
        }

        city_claims.push((req.worker_id.clone(), now));
    }

    if req.severity < 0.2 {
        score += 0.3;
        flags.push(format!(
            "Event severity {:.2} is below minimum claimable threshold (0.2)",
            req.severity
        ));
    }

    (score.min(1.0), flags)
}

async fn health() -> HttpResponse {
    HttpResponse::Ok().json(serde_json::json!({ "status": "ok", "service": "shieldnet-fraud" }))
}

async fn score_endpoint(
    req: web::Json<FraudRequest>,
    data: web::Data<AppState>,
) -> HttpResponse {

    let (rule_score, flags) = score_fraud(&req, &data);

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

    let final_score = (rule_score * 0.6) + (ml_score * 0.4);

    let decision = if final_score > 0.7 {
        "reject"
    } else if final_score > 0.4 {
        "review"
    } else {
        "approve"
    };

    HttpResponse::Ok().json(FraudResponse {
        fraud_score: final_score,
        flags,
        decision: decision.to_string(),
    })
}

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    let port = std::env::var("PORT").unwrap_or_else(|_| "8080".to_string());
    let addr = format!("0.0.0.0:{}", port);

    println!("ShieldNet Fraud Service running on {}", addr);

   let state = web::Data::new(AppState {
       claim_history: Mutex::new(HashMap::new()),
       city_claims: Mutex::new(HashMap::new()),
       model: FraudModel::new(),
   });

    HttpServer::new(move || {
        App::new()
            .app_data(state.clone())
            .route("/health", web::get().to(health))
            .route("/score",  web::post().to(score_endpoint))
    })
    .bind(&addr)?
    .run()
    .await
}
