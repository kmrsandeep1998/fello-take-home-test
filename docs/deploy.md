# Public demo deployment

The app is ready to deploy as a single Docker web service.

## GitHub

From the repository root:

```bash
git init
git add .
git commit -m "Build explainable customer health queue"
git branch -M main
git remote add origin https://github.com/<your-user>/<your-repo>.git
git push -u origin main
```

## Render

1. Create a Render account and choose **New → Blueprint**.
2. Connect the GitHub repository.
3. Render reads `render.yaml` and uses the repository `Dockerfile`.
4. Confirm the service is on the `free` plan for the demo.
5. Deploy and wait for the health check to pass.
6. Open the generated `onrender.com` URL.

Verify the deployment:

```bash
curl -sS https://<service>.onrender.com/api/health
curl -sS https://<service>.onrender.com/api/summary?tenant=northstar
curl -sS https://<service>.onrender.com/api/accounts/acct-2647?tenant=northstar | jq .
```

Expected health response:

```json
{"ok":true,"accounts":5000,"events":200000,"modelFitted":true}
```

The free service may sleep after inactivity and the demo's in-memory dataset is regenerated after restarts. Mention this in the walkthrough; it is acceptable for a take-home demo, not for production persistence.

