# Fello Monday Morning

An explainable, multi-tenant account-risk queue for Customer Success.

## What this demonstrates

- 5,000 synthetic accounts and 200,000 events across Salesforce, Gmail, Zendesk, product usage, and Stripe.
- Messy data: missing values, source-specific timestamps, and events shuffled out of order.
- A logistic-regression model fitted on account-level features generated from those events.
- An unattended scheduled agent that recomputes scores and creates reviewable outreach recommendations.
- Tenant-aware APIs and an evidence view that traces every score to computed features and source events.

## Submission artifacts

- One-page brief: `outputs/one-page-solution-brief.pptx`
- Short walkthrough deck: `outputs/fello-solution-brief.pptx`
- Brief source: `docs/solution-brief.md`
- Walkthrough script: `docs/walkthrough.md`
- Public deployment handoff: `docs/deploy.md`

The complete handoff package is also grouped under `submission/` for easy review.

## Run

Requires JDK 17+.

```bash
javac -d out src/Main.java
java -cp out Main
```

Open http://localhost:8080.

Run the deterministic and live checks with `./scripts/verify.sh` while no other process is using the configured port. The walkthrough sequence is in `docs/walkthrough.md`.

## Public demo deployment

For a reviewer-accessible URL, deploy the repository as a Docker web service on Render or Railway. `Dockerfile` and `render.yaml` are included. The hosted service must bind `HOST=0.0.0.0` and the platform-provided `PORT`; local runs default to `127.0.0.1:8080`.

The demo is intentionally dependency-free so the reviewer can run it with only a JDK. On startup it creates the synthetic dataset, fits the model, and runs the agent. A background job reruns scoring every five minutes; the UI also exposes the latest run timestamp. The manual agent endpoint is `POST /api/agent/run`.

## Product decision

The first release ranks and explains accounts; it does not automatically send customer email. A red score is a prioritization signal, not proof of churn, and the source data can be stale or incomplete. The system creates an outreach recommendation for CSM approval and records that decision. A future release can add sending after consent, message policy, and delivery safeguards are defined.

## Model and score

The model predicts a synthetic 30-day risk event from a fixed feature contract: usage trend, active days, severe/unresolved support, payment failures, customer sentiment, renewal proximity, and missing-data indicators. It is trained with logistic regression using gradient descent on the generated account data. The displayed health score is the calibrated model probability mapped to 0–100. Green/yellow/red thresholds are policy (`<50`, `50–79`, `>=80`) and are separate from the model; the higher red threshold keeps the Monday queue focused on the highest-priority accounts.

Each account response includes model version, run timestamp, feature values, contributions (`feature × coefficient`), confidence based on data coverage, and representative source-event evidence.

## Deliberately skipped

Real integrations, email delivery, CRM write-back, SSO/RBAC, PII, and production-grade persistence. The demo uses generated data and in-memory state to keep the logic inspectable.

## What breaks first at 10x

The in-memory event store and single-process scheduler. At 50,000 accounts / 2M events, move raw events to a partitioned database or lakehouse, materialize features incrementally, train in a separate job, and use a durable queue plus leader election for the agent. Tenant authorization and model registry also need production enforcement before handling real customers. The demo now reports tenant-scoped event counts and uses POST for manual agent runs.

## Walkthrough path

1. Choose a tenant.
2. Open the highest-risk queue and select an account.
3. Show the evidence and model contributions.
4. Explain that the score came from a scheduled run, not a UI click.
5. Open the outreach recommendation and approve it without sending email.

