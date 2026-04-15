<div align="center">

# 🛡️ ShieldNet

### Parametric Micro-Insurance for Gig Workers

*When a condition is met, the payout happens automatically. No claim. No paperwork. No waiting.*

[![Backend](https://img.shields.io/badge/Backend-Ktor%20%7C%20Kotlin-blue)](https://ktor.io)
[![Fraud Engine](https://img.shields.io/badge/Fraud%20Engine-Rust%20%7C%20actix--web-orange)](https://actix.rs)
[![Database](https://img.shields.io/badge/Database-MongoDB-green)](https://mongodb.com)
[![App](https://img.shields.io/badge/App-Android%20%7C%20Kotlin-brightgreen)](https://developer.android.com)

</div>

---

## The Problem

India has over 15 million gig workers — delivery riders, freelancers, and daily earners — who operate with no financial safety net. When it rains heavily, platforms go slow, earnings drop, and there is nothing to fall back on.

Traditional insurance does not work for them. It requires filing a claim, submitting documents, waiting for verification, and hoping for approval. For a worker who lost ₹400 in one afternoon, this process is pointless.

---

## What ShieldNet Does Differently

ShieldNet is a **parametric insurance platform**. A parametric system pays out based on a measurable external condition — not on a claim filed by the user.

Here is the difference:

| Traditional Insurance | ShieldNet |
|---|---|
| Worker files a claim | No action required from the worker |
| Manual document verification | Condition verified against live data |
| Payout in days or weeks | Payout triggered in under 90 seconds |
| Prone to fraud via false claims | Every trigger validated by Rust fraud engine |

The moment a trigger threshold is breached — for example, rain severity crossing 7.5 on a 0–10 index — ShieldNet initiates the evaluation and payout pipeline **automatically**.

---

## How It Works

```
External Condition Reaches Threshold
(e.g. rain severity > 7.5 / 10)
              │
              ▼
    Trigger Engine (Ktor backend)
    detects threshold breach for
    workers in the affected city
              │
              ▼
    Rust Fraud Microservice evaluates:
    ├── Policy age (was it bought just before the event?)
    ├── Claim frequency (≥ 3 claims in 30 days → flagged)
    ├── City cluster detection (> 5 workers claiming in 10 min → flagged)
    ├── Event severity (severity < 0.2 → rejected as sub-threshold)
    └── ML logistic regression across all 4 features (40% weight)
              │
         [fraud_score]
              │
       < 0.4 → APPROVE     (payout initiated)
    0.4–0.7 → REVIEW       (held for manual check)
       > 0.7 → REJECT      (blocked automatically)
              │
              ▼
    Worker receives push notification:
    "Rain payout of ₹500 credited to your wallet"
    (worker never opened the app to request this)
```

---

## Parametric Trigger Examples

| Trigger Condition | Threshold | Coverage Tier | Auto Payout |
|---|---|---|---|
| Rain severity index | > 7.5 / 10 | Basic (₹10,000) | ₹300–500 per event |
| Platform outage duration | > 2 hours | Standard (₹25,000) | Hourly income replacement |
| City traffic congestion index | > 85% | Premium (₹50,000) | Zone-based shift coverage |
| Extreme heat (°C) | > 42°C for 3+ hours | Any tier | Heat relief payout |

No worker files a claim for any of these. The condition itself is the proof.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Android App (Kotlin)                    │
│   OTP Login → Registration → Policy Purchase → Dashboard   │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS / REST
┌───────────────────────────▼─────────────────────────────────┐
│                     Ktor Backend (Kotlin)                   │
│                                                             │
│  /auth/send-otp       →  OTP generation & verification      │
│  /workers/register    →  Profile creation, risk onboarding  │
│  /policies/create     →  Policy binding (Basic/Std/Premium) │
│  /risk/analyze        →  Proxies event to Rust service      │
│  /status/triggers     →  Active condition broadcast         │
│  /claims/list         →  Payout history per worker          │
└──────┬────────────────────────┬────────────────────────────┘
       │                        │
       │ MongoDB Atlas           │ HTTP → /score
       │                        │
┌──────▼──────────┐   ┌─────────▼──────────────────────────┐
│   MongoDB       │   │   Rust Fraud Detection Service      │
│  - workers      │   │   (actix-web + smartcore ML)        │
│  - policies     │   │                                     │
│  - claims       │   │   Rule engine  (60% weight):        │
│  - trigger logs │   │   ├── Policy age penalty            │
└─────────────────┘   │   ├── 30-day claim frequency        │
                      │   ├── 10-min city cluster check     │
                      │   └── Severity floor gate           │
                      │                                     │
                      │   ML layer  (40% weight):           │
                      │   └── Logistic regression on        │
                      │       [policy_age, severity,        │
                      │        claim_count, cluster_size]   │
                      └────────────────────────────────────┘
```

---

## The Fraud Engine — Why Rust

Every parametric trigger event goes through the fraud service before any payout is released. This sits on the critical path: a slow evaluation holds up a real payout for a real worker.

The fraud service does two things simultaneously:

**1. Rule-based scoring** (60% of final score)
- `policy_age_hours < 2.0` → up to +0.5 penalty (bought policy right before the event)
- `claim_count_30d ≥ 3` → +0.4 penalty (anomalous frequency)
- `city_cluster > 5 workers in 10 minutes` → +0.2 penalty (coordinated fraud ring detected)
- `event_severity < 0.2` → +0.3 penalty (event didn't actually cross the claimable threshold)

**2. ML layer: logistic regression** (40% of final score)
- Features: `[policy_age_hours, severity, claim_count, cluster_size]`
- Trained via `smartcore` — the Rust-native ML library
- Runs entirely in-process, no external model server

**Final decision:**
```
final_score = (rule_score × 0.6) + (ml_score × 0.4)

< 0.40  →  approve   (payout released)
0.40–0.70  →  review  (flagged for manual check)
> 0.70  →  reject   (blocked, reason logged)
```

Rust gives us consistent sub-millisecond latency on this path — no GC pauses, no runtime overhead — so the evaluation never becomes the bottleneck even during a burst of simultaneous triggers across a city.

---

## Tech Stack

| Layer | Technology | Role |
|---|---|---|
| Mobile | Android / Kotlin | Onboarding, policy purchase, dashboard, alerts |
| Backend | Ktor / Kotlin | Auth, policy engine, trigger routing, payout ledger |
| Fraud | Rust / actix-web | Real-time fraud scoring on every parametric event |
| ML | smartcore (Rust) | Logistic regression for behavioral fraud detection |
| Database | MongoDB Atlas | Workers, policies, claim records, trigger logs |
| Deployment | Render | Backend: `ktor-backend-eda7.onrender.com` |

---

## User Flow

```
1. Worker downloads app → enters phone number
2. OTP sent and verified (no email, no documents)
3. Worker fills profile: name, city, platform, weekly earnings, UPI handle
4. Backend generates worker ID and stores profile in MongoDB
5. Worker selects a plan tier (Basic ₹99 / Standard ₹199 / Premium ₹399)
6. Policy is activated and stored (30-day coverage window)
7. [Background] Trigger engine monitors conditions for worker's city
8. Condition threshold crossed → fraud check → payout initiated
9. Worker receives notification — money already in wallet
```

---

## Demo Flow (Simulated Trigger)

Since live weather API integration is a production concern, the demo uses a simulated trigger call to show the full pipeline:

**Simulated input:**
```json
{
  "worker_id": "worker_demo_001",
  "city": "Chennai",
  "event_type": "rainfall",
  "policy_age_hours": 18.5,
  "severity": 0.82
}
```

**What happens:**
1. Ktor backend receives the trigger via `/risk/analyze`
2. Forwards to Rust fraud service at `/score`
3. Rust checks: age is fine, no recent claims, city cluster is 2 (safe), severity is high (0.82 > 0.2)
4. Rule score: ~0.0 | ML score: ~0.0 | `final_score: 0.08`
5. Decision: **APPROVE**
6. Backend marks claim as approved, initiates payout reference
7. Worker dashboard shows: *"Rain event detected in Chennai — ₹500 credited"*

---

## Real-World Integration Path

The trigger engine is architected to consume real external data:

- **Weather:** OpenWeatherMap API, IMD (India Met Department) webhooks
- **Platform outages:** Zomato / Swiggy status APIs
- **Traffic:** Google Maps Directions API congestion index
- **Geofencing:** Worker location matched to affected city zone before trigger applies

The switch from simulated to live triggers requires one integration layer — the rest of the pipeline, including fraud detection and payout logic, is already live.

---

## Why This Is Not Traditional Insurance

Traditional insurance → user reports an event → insurer investigates → insurer decides → payout (maybe).

ShieldNet → condition crosses threshold → system decides automatically → payout (deterministic).

The worker does not need to understand insurance. They do not need to file a claim, prove a loss, or understand policy language. They picked a plan, paid a small premium, and when something goes wrong, money appears in their UPI wallet.

That is the product.

---

## Impact

- A delivery rider in Mumbai earns ₹600 on a good day. One afternoon of heavy rain cuts that to zero. With ShieldNet, ₹500 lands in their account before the rain stops.
- A freelance courier during a Swiggy outage loses 3 hours of earnings. ShieldNet detects the outage and compensates automatically.
- Workers in a cluster fraud ring are blocked before any false payout is released — protecting the platform and honest workers alike.

---

## Links

- **GitHub:** https://github.com/Kingjha13/DEV_TRAILS_HACKATHON
- **Backend (live):** https://ktor-backend-eda7.onrender.com/health
- **Fraud Service (live):** https://shieldnet-fraud.onrender.com/health