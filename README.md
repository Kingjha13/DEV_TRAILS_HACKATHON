<div align="center">

<img src="https://img.shields.io/badge/ShieldNet-Parametric%20Income%20Protection-0f766e?style=for-the-badge&logoColor=white" />

# ShieldNet
### AI-Powered Parametric Income Insurance for India's Informal Workforce

*When the storm hits, your income shouldn't.*

[![Android](https://img.shields.io/badge/Platform-Android-3ddc84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7f52ff?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Node.js](https://img.shields.io/badge/Backend-Node.js%20%2B%20TypeScript-339933?style=flat-square&logo=nodedotjs&logoColor=white)](https://nodejs.org)
[![Python](https://img.shields.io/badge/ML-Python%20%2B%20XGBoost-3776ab?style=flat-square&logo=python&logoColor=white)](https://python.org)
[![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-4169e1?style=flat-square&logo=postgresql&logoColor=white)](https://postgresql.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)

</div>

---

## The Problem

India has over **50 million gig and informal workers** — delivery riders, auto drivers, daily wage laborers — whose income vanishes the moment conditions outside their control make working impossible. A single day of heavy rain, a sudden curfew, or a heatwave advisory can erase a week's worth of earnings.

Traditional insurance doesn't serve them:

- Requires formal employment proof
- Demands manual claim documentation
- Takes days or weeks to process payouts
- Charges premiums calculated for a salaried, predictable income

These workers need **sub-day, event-triggered, zero-friction income protection** — not a policy buried in paperwork.

**ShieldNet** is that protection.

---

## What ShieldNet Does

ShieldNet is a **parametric micro-insurance platform** where payouts are triggered automatically the moment a disruption event is confirmed — no claims form, no adjuster, no waiting.

```
Worker enrolled → disruption detected by APIs → thresholds crossed →
AI validates the event → loss calculated → money sent via UPI → done.
```

The worker never has to do anything after purchasing a weekly plan.

### Core Capabilities

| Capability | Description |
|---|---|
| **Hyper-local Risk Profiling** | ML model scores each worker's zone using weather history, flood frequency, AQI, and traffic congestion |
| **Dynamic Premium Pricing** | Weekly premiums adjust to the worker's actual risk level — not a flat industry-wide rate |
| **Automated Event Detection** | Continuous polling of weather, traffic, and disruption APIs; no human trigger needed |
| **Parametric Claim Engine** | Claim is auto-created the moment pre-defined thresholds are crossed |
| **Income Loss Estimation** | ML model calculates actual lost earnings using delivery activity and routing data |
| **AR Navigation Layer** | ARCore overlays flood zones, road closures, and safe routes directly onto live camera view |
| **Fraud Defense Stack** | Multi-signal anomaly detection prevents GPS spoofing, duplicate claims, and fraud rings |
| **Instant UPI Payout** | Validated claims are paid out directly to the worker's UPI handle within minutes |
| **SMS Notifications** | Worker is notified at every stage — policy activated, disruption detected, payout sent |

---

## Disruption Types Covered

ShieldNet monitors five categories of income-disrupting events:

```
┌─────────────────┬────────────────────────────────┬─────────────────────────┐
│ Event Type      │ Trigger Condition              │ Data Source             │
├─────────────────┼────────────────────────────────┼─────────────────────────┤
│ Heavy Rainfall  │ Precipitation ≥ 70 mm/6 hours  │ OpenWeatherMap          │
│ Extreme Heat    │ Temperature ≥ 44°C             │ OpenWeatherMap          │
│ Air Quality     │ AQI ≥ 300 (Hazardous)         │ AQICN                   │
│ Curfew / Strike │ Geo-tagged disruption event    │ News / Traffic APIs     │
│ Road Closure    │ Congestion index ≥ 2.5× base  │ TomTom Traffic Flow     │
└─────────────────┴────────────────────────────────┴─────────────────────────┘
```

---

## Architecture

### System Overview

```
┌──────────────────────────────────────────────────────────────┐
│                        ShieldNet Platform                     │
│                                                              │
│   Mobile App (Android — Kotlin + Java)                       │
│   ├── TomTom Map Dashboard                                   │
│   └── ARCore Navigation Layer                                │
│        │                                                     │
│        ▼                                                     │
│   API Gateway ──────────────────────────────────────────┐   │
│        │                                                 │   │
│   ┌────┴───────────────────────────────────────────┐    │   │
│   │              Core Microservices                │    │   │
│   │  Auth  │  User  │  Policy  │  Claims  │  Notif │    │   │
│   └────────────────────────────────────────────────┘    │   │
│        │                          │                      │   │
│   ┌────┴──────────┐    ┌──────────┴──────────────────┐  │   │
│   │  AI/ML Layer  │    │   Event Processing Layer     │  │   │
│   │  Risk Engine  │    │   Kafka Bus → Claims Engine  │  │   │
│   │  Loss Model   │    │   Trigger Monitor            │  │   │
│   │  Fraud Model  │    │                              │  │   │
│   └───────────────┘    └──────────────────────────────┘  │   │
│        │                          │                      │   │
│   ┌────▼──────────────────────────▼────────────────────┐ │   │
│   │           Data & Integration Layer                 │ │   │
│   │  PostgreSQL  │  Redis Cache  │  External APIs      │ │   │
│   └────────────────────────────────────────────────────┘ │   │
│                                                          │   │
│   ◄──────────── Payment Gateway (Razorpay/UPI) ─────────┘   │
└──────────────────────────────────────────────────────────────┘
```

### Automated Claim Trigger Flow

```
Weather / Traffic APIs
        │
        ▼
  Trigger Monitor
  (polls every 5 min)
        │
        ▼ Threshold breached?
  Disruption Event ──► Kafka Topic: disruption.events
        │
        ▼
  Claims Service
  ├── Find affected workers (location match)
  ├── Check active policy existence
  ├── Income Loss Estimation Model
  └── Fraud Detection Model
             │
      ┌──────┴──────┐
      ▼             ▼
  Low Risk      High Risk
  Auto-approve  Manual Review
      │
      ▼
  Razorpay / UPI Payout
      │
      ▼
  Twilio SMS → Worker Notified
```

### Premium Purchase Flow

```
Worker ──► Register (Name / Phone / City / Platform / Earnings)
  │
  ▼
Risk Engine ML
  ├── Input: Rainfall history, flood freq, AQI, traffic congestion
  └── Output: Risk Score (0.0–1.0)
                │
                ▼
         Risk Level        Weekly Premium    Coverage
         ─────────────     ──────────────    ─────────
         Low  (0–0.35)     ₹40/week          ₹1,500
         Med  (0.35–0.7)   ₹60/week          ₹2,000
         High (0.7–1.0)    ₹80/week          ₹3,000
                │
                ▼
  Worker selects plan → UPI / Wallet payment → Policy active (7 days)
```

---

## AR Navigation Layer

ShieldNet's AR layer is built using **Google ARCore** and **TomTom Map SDK**. When a worker opens the AR camera view, real-time spatial overlays appear directly on the live street ahead — no map reading required while riding.

### What the AR Layer Shows

| Overlay | Meaning |
|---|---|
| Red zone | Flood-prone road — avoid |
| Orange alert | High congestion or incident ahead |
| Green path | Safe alternate delivery route |

### How It Works

```
TomTom Real-Time Spatial Feed
        │
        ▼
  ARCore Screen-Space Renderer
  (per-frame recalculation — stable at riding speed)
        │
        ▼
  Overlays projected onto live camera feed
  ├── Flood zone boundary ahead → Red overlay
  ├── Road closure or congestion → Orange alert banner
  └── Safe route suggestion → Green path indicator
```

ARCore world anchors drift at two-wheeler speeds (30–40 km/h). ShieldNet uses **screen-space overlays** recalculated every frame from TomTom's live data feed — stable, accurate rendering at riding speed without anchor drift.

---

## AI / ML Models

ShieldNet uses three purpose-built models trained on Indian weather and urban mobility data.

### 1. Risk Prediction Model

Calculates a hyperlocal risk score for a worker's registered delivery zone before policy purchase.

**Stack:** Python, Pandas, NumPy, Scikit-learn, XGBoost

**Features:**
- Monthly rainfall totals (last 3 years, IMD data)
- Historical flood incidents per zone (NDMA dataset)
- Heatwave frequency (IMD seasonal bulletins)
- Average AQI (AQICN historical)
- Traffic congestion baseline (TomTom historical flow)

**Output:** Continuous risk score → mapped to premium tier

### 2. Income Loss Estimation Model

Quantifies how much income a worker actually lost during a disruption period, enabling proportionate payouts.

**Core Logic:**

```
Normal efficiency  = avg deliveries/hour × avg fare/delivery
                   = 3 deliveries/hr × ₹40 = ₹120/hr

Disrupted eff.     = TomTom real-time route delay ratio applied
                   = 1 delivery/hr × ₹40 = ₹40/hr

Loss per hour      = ₹120 - ₹40 = ₹80
Total loss (6hr)   = ₹80 × disrupted hours
```

**Additional signals from TomTom Routing API:**
- Route time inflation (normal vs actual)
- Delivery zone congestion index
- Distance traveled vs expected distance

### 3. Fraud Detection Model

Protects the payout engine using anomaly detection before any claim is approved.

**Algorithms:** Isolation Forest, One-Class SVM

**Detection Signals:**
- GPS coordinate consistency vs road network (TomTom Map Matching)
- Speed pattern analysis (unrealistic jumps flagged)
- Delivery activity drop correlation with claimed disruption
- Historical claim frequency per user
- Cluster analysis for coordinated fraud ring detection

**Risk-Tiered Response:**

```
Low fraud risk   → Instant auto-payout
Medium risk      → Secondary automated verification (location re-check, activity cross-ref)
High risk        → Held for manual review queue
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Mobile Frontend | Kotlin + Java (Android), MVVM Architecture |
| AR Navigation | Google ARCore, TomTom Map SDK |
| API Gateway | Node.js + TypeScript (Express) |
| Core Services | Node.js microservices (Auth, User, Policy, Claims, Notification) |
| Event Bus | Apache Kafka |
| ML Services | Python 3.11 (Flask), Pandas, NumPy, Scikit-learn, XGBoost |
| Primary Database | PostgreSQL 15 + Prisma ORM |
| Cloud DB | NeonDB (serverless Postgres) |
| Cache | Redis |
| Payments | Razorpay (premium collection + UPI payout) |
| Notifications | Twilio SMS |
| Weather | OpenWeatherMap API |
| Air Quality | AQICN (World Air Quality Index) |
| Traffic / Maps | TomTom Traffic API + Map SDK |
| Historical Weather | IMD Mausam Portal (model training data) |

---

## Project Structure

```
shieldnet/
├── android/                    # Android mobile app (Kotlin + Java)
│   ├── app/src/main/
│   │   ├── kotlin/com/shieldnet/
│   │   │   ├── ui/             # Activities, Fragments, ViewModels
│   │   │   ├── ar/             # ARCore navigation layer
│   │   │   ├── network/        # Retrofit API clients
│   │   │   └── model/          # Data models
│   │   └── res/                # Layouts, drawables, strings
│   └── build.gradle
│
├── backend/                    # Node.js + TypeScript services
│   ├── gateway/                # API Gateway (routing, auth middleware)
│   ├── services/
│   │   ├── auth/               # JWT-based authentication
│   │   ├── user/               # Worker profile management
│   │   ├── policy/             # Plan selection, activation
│   │   ├── claims/             # Claim creation, status, payout trigger
│   │   └── notification/       # Twilio SMS integration
│   ├── kafka/
│   │   ├── producers/          # Disruption event publishers
│   │   └── consumers/          # Claims processing consumers
│   └── prisma/
│       └── schema.prisma       # DB schema (Worker, Policy, Claim, Event)
│
├── ml/                         # Python ML services
│   ├── risk_engine/
│   │   ├── model.py            # XGBoost risk scoring model
│   │   ├── train.py            # Training pipeline
│   │   └── serve.py            # Flask inference API
│   ├── loss_estimation/
│   │   ├── model.py            # Income loss calculation model
│   │   └── serve.py            # Flask inference API
│   └── fraud_detection/
│       ├── model.py            # Isolation Forest + One-Class SVM
│       └── serve.py            # Flask inference API
│
├── trigger-monitor/            # Disruption event detection service
│   ├── weather_poller.ts       # OpenWeatherMap polling
│   ├── aqi_poller.ts           # AQICN polling
│   ├── traffic_poller.ts       # TomTom congestion monitor
│   └── event_publisher.ts     # Kafka event publisher
│
├── docker-compose.yml          # Local dev environment
└── README.md
```

---

## Database Schema (Key Entities)

```sql
CREATE TABLE workers (
  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name        TEXT NOT NULL,
  phone       TEXT UNIQUE NOT NULL,
  city        TEXT NOT NULL,
  platform    TEXT NOT NULL,
  weekly_avg  INTEGER NOT NULL,
  risk_score  FLOAT,
  upi_handle  TEXT,
  created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE policies (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  worker_id    UUID REFERENCES workers(id),
  plan_tier    TEXT NOT NULL,
  premium_inr  INTEGER NOT NULL,
  coverage_inr INTEGER NOT NULL,
  starts_at    TIMESTAMPTZ NOT NULL,
  expires_at   TIMESTAMPTZ NOT NULL,
  status       TEXT DEFAULT 'active'
);

CREATE TABLE disruption_events (
  id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_type   TEXT NOT NULL,
  city         TEXT NOT NULL,
  severity     FLOAT NOT NULL,
  detected_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE claims (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  worker_id       UUID REFERENCES workers(id),
  policy_id       UUID REFERENCES policies(id),
  event_id        UUID REFERENCES disruption_events(id),
  estimated_loss  INTEGER,
  approved_amount INTEGER,
  fraud_score     FLOAT,
  status          TEXT DEFAULT 'pending',
  payout_ref      TEXT,
  created_at      TIMESTAMPTZ DEFAULT NOW()
);
```

---

## External API Integration

### TomTom — Road Intelligence Layer

TomTom is the spatial backbone of ShieldNet — powering the AR navigation layer, delivery efficiency calculations, fraud GPS validation, and hyperlocal risk zone scoring.

| TomTom API | How ShieldNet Uses It |
|---|---|
| **Traffic Flow** | Detects real-time congestion index per delivery zone |
| **Incident Details** | Identifies road closures, accidents, blocked routes |
| **Routing** | Compares normal vs disrupted travel time to calculate efficiency drop |
| **Map Matching** | Validates worker GPS traces against actual road network (fraud prevention) |
| **Map SDK (Android)** | Renders risk heat maps — flood zones (red), disruption areas (orange), safe zones (green) |
| **Real-Time Spatial Feed** | Powers per-frame AR overlay recalculation at riding speed |

**Zone Risk Example:**

```json
{
  "coordinates": [19.0760, 72.8777],
  "zone": "Andheri East",
  "flood_risk": "high",
  "congestion_index": 8.7,
  "baseline_congestion": 3.2,
  "risk_multiplier": 1.42
}
```

### OpenWeatherMap

```
GET /data/2.5/weather?lat={lat}&lon={lon}&appid={key}

Monitored fields:
  rain.1h       → triggers if ≥ 70mm/6-hour window
  main.temp     → triggers if ≥ 317.15K (44°C)
  weather[0].id → storm/flood event codes
```

### Razorpay

- **Inbound:** Worker pays weekly premium via UPI / Wallet
- **Outbound:** Validated claims disbursed via Razorpay Payout API to worker UPI handles

---

## Fraud Defense

ShieldNet's fraud stack operates on the principle: **trust signals, not user claims.**

```
Layer 1 — GPS Integrity
  ├── Speed consistency check (no teleportation between pings)
  ├── Road network matching via TomTom Map Matching API
  └── Device integrity flag (rooted device / emulator detection)

Layer 2 — Activity Correlation
  ├── Delivery count drop must correlate with claimed disruption
  ├── Route distance traveled vs expected for active shift
  └── App-reported active hours vs movement data

Layer 3 — Cross-Signal Validation
  ├── Weather signal must exist in worker's city
  ├── Traffic congestion must reflect the disruption
  └── At least 2 of 3 signals required before claim is created

Layer 4 — Pattern Analysis
  ├── Claim frequency per worker (cooldown enforcement)
  ├── Isolation Forest on claim features (anomaly scoring)
  └── Cluster analysis — identical GPS patterns across users → fraud ring flag

Layer 5 — Policy Integrity
  ├── Policy must have been active BEFORE the disruption event
  ├── One claim per disruption event per worker (deduplication)
  └── Registered city must match claim event city
```

| Signal | Genuine Worker | Fraudster |
|---|---|---|
| GPS movement | Continuous, road-following | Static, jumping, straight-line |
| Speed pattern | Natural variance with stops | Unrealistic or zero movement |
| Delivery activity | Measurable drop during event | Activity unchanged or inconsistent |
| Location history | Consistent zone over weeks | Sudden city change for claim |
| Claim timing | Matches event window | Submitted before/after window |

---

## Getting Started

### Prerequisites

```bash
Node.js >= 18
Python >= 3.11
PostgreSQL >= 15
Apache Kafka (or use Docker Compose)
Android Studio (for mobile app)
ARCore supported Android device (API level 24+)
```

### Setup

```bash
git clone https://github.com/yourusername/shieldnet.git
cd shieldnet

docker-compose up -d

cd backend
npm install
cp .env.example .env
npx prisma migrate dev
npm run dev

cd ../ml
pip install -r requirements.txt
python risk_engine/serve.py &
python loss_estimation/serve.py &
python fraud_detection/serve.py &

cd ../trigger-monitor
npm install
npm run start
```

### Environment Variables

```env
DATABASE_URL=postgresql://user:pass@localhost:5432/shieldnet
REDIS_URL=redis://localhost:6379
KAFKA_BROKER=localhost:9092

OPENWEATHER_API_KEY=your_key
AQICN_API_KEY=your_key
TOMTOM_API_KEY=your_key
RAZORPAY_KEY_ID=your_key
RAZORPAY_KEY_SECRET=your_key
TWILIO_ACCOUNT_SID=your_sid
TWILIO_AUTH_TOKEN=your_token
TWILIO_FROM_NUMBER=+1xxxxxxxxxx

JWT_SECRET=your_secret
```

---

## How It Works — End to End

**Day 1: Ravi signs up**

Ravi, a Swiggy delivery partner in Pune, downloads ShieldNet. He enters his name, phone, city (Pune), platform, and average weekly earnings (₹7,000). The Risk Engine scores his delivery zone — congestion-prone, moderate flood history — and returns a **medium** risk score. ShieldNet recommends ₹60/week covering up to ₹2,000. Ravi pays via UPI. Policy is active.

**Day 4: Unseasonal heavy rain**

At 3 PM, the Trigger Monitor detects 78mm of rainfall in Pune in the past 6 hours — above the 70mm threshold. The event is published to Kafka. The Claims Service finds Ravi's active policy and calls the Income Loss Estimation Model. TomTom routing data shows 3× delivery time inflation. Based on Ravi's baseline of 3 deliveries/hour, the model estimates ₹720 income loss over his 6-hour shift.

Meanwhile, Ravi opens ShieldNet's AR camera view. Red overlays appear on the flooded roads ahead. A green alternate route is projected onto the dry street to his left. He navigates safely without checking any map.

Fraud Detection runs — GPS road-matching passes, activity data consistent with rain disruption, no red flags. Claim auto-approved. Razorpay disburses ₹720 to Ravi's UPI handle.

Ravi receives: *"ShieldNet: Claim approved. ₹720 sent to your UPI for today's rainfall disruption in Pune."*

Total time from threshold breach to payout: **under 4 minutes.**

---

## Parametric Trigger Thresholds

| Event | Trigger | Source |
|---|---|---|
| Heavy Rainfall | ≥ 70 mm/6-hour window | OpenWeatherMap |
| Extreme Heat | Temperature ≥ 44°C | OpenWeatherMap |
| Poor Air Quality | AQI ≥ 300 (Hazardous) | AQICN |
| Curfew / Strike | Geo-tagged disruption active | News + TomTom Incidents |
| Road Closure | Congestion index ≥ 2.5× baseline | TomTom Traffic Flow |

---

## Premium & Coverage Model

| Risk Level | Score Range | Weekly Premium | Coverage Cap |
|---|---|---|---|
| Low | 0.00 – 0.35 | ₹40 | ₹1,500 |
| Medium | 0.35 – 0.70 | ₹60 | ₹2,000 |
| High | 0.70 – 1.00 | ₹80 | ₹3,000 |

Risk scores are recalculated at each policy renewal. A worker who relocates to a lower-risk zone will see their premium drop at the next 7-day cycle.

---

## Why Parametric

Traditional insurance pays based on **what you prove you lost**. Parametric insurance pays based on **what the data says happened**.

For gig workers this distinction is everything — no receipts required, no adjuster visits, no denial based on claim interpretation, no weeks of waiting. The policy contract is simply: *"If rainfall in your city exceeds X on day Y, you receive Z."* The data decides, not a human claims handler.

---

## Roadmap

- [x] Risk scoring model (v1)
- [x] Parametric trigger engine
- [x] Automated claim creation
- [x] UPI payout via Razorpay
- [x] Basic fraud detection
- [x] ARCore navigation layer (v1 — screen-space overlays)
- [ ] AR turn-by-turn delivery routing with waypoints
- [ ] Auto-partner onboarding via Swiggy / Zomato SDK integration
- [ ] Fleet-level coverage for delivery companies
- [ ] IRDAI regulatory sandbox application
- [ ] Multi-language app (Hindi, Marathi, Tamil, Telugu)
- [ ] Cooperative pool model (community risk pooling between workers)
- [ ] Government API integration (NDMA flood alerts, IMD direct feed)

---

## Contributing

Pull requests are welcome. For major changes, open an issue first.

Please run `npm run lint` and `pytest` before submitting.

---

## License

MIT — see [LICENSE](LICENSE) for details.

---

<div align="center">

Built with care for the workers who deliver our meals in the rain.

</div>
