# Verification report

## Deterministic local verification

The application compiles and the smoke test passes:

```text
accounts=5000
events=200000
tenants=3
model=risk-logreg-v1
```

## Live Render verification

Verified from the deployed environment:

- `GET /api/health` returns HTTP 200.
- Health payload reports 5,000 accounts, 200,000 events, and `modelFitted: true`.
- Northstar summary returns 1,667 accounts and 66,680 tenant events.
- Account detail returns model version, features, contributions, confidence, timestamps, and source-event evidence.
- `POST /api/agent/run` returns `ok: true` and updates `lastAgentRun`.
- Wrong-tenant account access returns HTTP 404.

## Deliberate scope boundary

The demo does not send real email or connect to production Salesforce, Gmail, Zendesk, product analytics, or Stripe accounts. Outreach is a reviewable task because a probabilistic risk score should not directly send customer communication without consent and suppression policy.

