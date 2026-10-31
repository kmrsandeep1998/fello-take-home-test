# Walkthrough script

## 1. Start with the decision

“This is a Monday queue, not an auto-email engine. The goal is to help a CSM decide who to call and why.”

## 2. Show the queue

Open the app and point out the tenant selector, summary counts, ranked accounts, health state, ARR, and recommended action.

## 3. Show one account

Select a red account. Walk through:

- risk score and confidence;
- computed feature values;
- model contributions;
- model version and computation timestamp;
- source event IDs from Zendesk, Product, and Stripe.

The important sentence is: “This is not an LLM explanation written after the fact. These values are the features and evidence used by the scoring run.”

## 4. Prove multi-tenancy

Switch Northstar to Acme and show that the queue changes. The API also rejects an account lookup under the wrong tenant with `404`.

## 5. Prove unattended work

Point to the last agent-run timestamp. Explain that startup runs scoring and a background scheduler repeats it every five minutes. The manual endpoint is `POST /api/agent/run` for demonstration and operations.

## 6. Explain the boundary

“I intentionally did not auto-send email. The model is probabilistic, missing data is real, and customer communication needs consent, suppression rules, and human review. The current automated output is a reviewable task.”

## 7. Close with scale

At 10×, move raw events to durable storage, materialize features incrementally, separate training from scoring, and use a durable queue plus leader election.

