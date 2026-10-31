# Fello Monday Morning — solution brief

## The real problem

CSMs do not need another dashboard. They need a trustworthy Monday queue that compresses five noisy systems into a small set of accounts worth acting on, with enough evidence to make a good call. The success measure is earlier, higher-quality intervention—not the number of red accounts or emails sent.

## What I would build first

An explainable, tenant-aware account-risk queue:

1. Generate/ingest source events and normalize them into account-level features.
2. Fit a simple, auditable model that predicts a 30-day risk event.
3. Recompute scores on a schedule and rank accounts by risk × renewal proximity × customer value.
4. Show the score, confidence, top contributing features, and source-event evidence.
5. Produce an outreach recommendation for CSM review, with an audit record.

## Where I disagree with the request

I would not send an email automatically whenever an account becomes red. A red score is probabilistic, can be driven by stale or incomplete data, and may require a sensitive human conversation. The first automated action should be a reviewable task/draft. Sending can follow once message policy, customer consent, suppression rules, and false-positive monitoring are agreed.

## Data and model choices

The first model is logistic regression because its coefficients and per-account contributions are easy to explain. Features cover product usage trend, support severity and resolution, payment failures, customer sentiment, renewal proximity, and missing-data coverage. Missingness is explicit and lowers confidence; it is not treated as healthy behavior. Health bands are a separate policy layer: green below 50, yellow 50–79, red 80 and above.

## Unattended work

A scheduled agent runs each morning, ingests new events, recomputes features, scores accounts, detects state changes, and creates reviewable recommendations. Every run is versioned. The UI links the score back to the feature snapshot and source events used.

## Business decisions still needed

- What counts as a risk event and what historical outcome labels exist?
- Which accounts/segments have different renewal motions?
- Who may view each tenant and approve outreach?
- What are the email consent, suppression, and escalation rules?
- Which metrics define success: retained ARR, CSM time saved, recall, precision, or lead time?

## Scope boundary

This first slice does not deliver real integrations, email sending, SSO, PII handling, or production persistence. It proves the decision loop end-to-end with realistic synthetic data and a model that can be inspected.

