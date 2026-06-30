# FocusGuard AI — Deploy to AWS Lambda + API Gateway

Your FastAPI backend runs as a serverless function on AWS Lambda.
API Gateway provides the public HTTPS URL.
No servers to manage. Scales to zero when not in use.

---

## What Gets Created

```
API Gateway (HTTPS URL)
    ↓
AWS Lambda (focusguard-api)
    ↓  reads/writes
DynamoDB (8 tables — already created)
    ↓  AI calls
Amazon Bedrock (Nova Lite)
    ↓  secrets
AWS Secrets Manager (credentials)
```

---

## Prerequisites

Install these on your Windows machine:

### 1. AWS SAM CLI
```powershell
# Download installer from:
# https://github.com/aws/aws-sam-cli/releases/latest
# → Download: AWS_SAM_CLI_64_PY3.msi
# Run installer, then verify:
sam --version
```

### 2. AWS CLI (already installed if you ran create_tables.py)
```powershell
aws --version
# If not installed: https://aws.amazon.com/cli/
```

### 3. Docker Desktop (SAM uses it to build Lambda packages)
```powershell
# Download: https://www.docker.com/products/docker-desktop/
# Install and start Docker Desktop
# Verify:
docker --version
```

---

## Step 1 — Store Secrets in AWS Secrets Manager

Run these commands in PowerShell from the `focusguard-backend` folder.
Replace the values with your actual credentials.

```powershell
# App secrets (JWT)
aws secretsmanager create-secret `
  --name "focusguard/app" `
  --region us-east-1 `
  --secret-string '{
    "jwt_secret": "YOUR_JWT_SECRET_HERE_run_python_c_import_secrets_print_secrets_token_hex_32"
  }'

# Twilio secrets
aws secretsmanager create-secret `
  --name "focusguard/twilio" `
  --region us-east-1 `
  --secret-string '{
    "account_sid":         "YOUR_TWILIO_ACCOUNT_SID",
    "auth_token":          "YOUR_TWILIO_AUTH_TOKEN",
    "phone_number":        "+1XXXXXXXXXX",
    "default_alert_phone": "+91XXXXXXXXXX"
  }'

# n8n secrets (update webhook_url after deploying to n8n cloud)
aws secretsmanager create-secret `
  --name "focusguard/n8n" `
  --region us-east-1 `
  --secret-string '{
    "webhook_url":    "http://localhost:5678/webhook/focusguard-alert",
    "webhook_secret": "aegisai-n8n-secret"
  }'
```

> After deploying to n8n cloud, update the webhook_url:
> ```powershell
> aws secretsmanager update-secret `
>   --name "focusguard/n8n" `
>   --region us-east-1 `
>   --secret-string '{
>     "webhook_url":    "https://YOUR-WORKSPACE.app.n8n.cloud/webhook/focusguard-alert",
>     "webhook_secret": "aegisai-n8n-secret"
>   }'
> ```

---

## Step 2 — Build the Lambda Package

```powershell
cd F:\Vibe2Ship\focusguard-backend

# SAM builds dependencies inside a Docker container matching Lambda's environment
sam build --use-container
```

This takes 2–5 minutes on first run (downloads the Lambda Docker image).
You'll see: `Build Succeeded`

---

## Step 3 — Deploy

```powershell
sam deploy --guided
```

Answer the prompts:

```
Stack Name [sam-app]: focusguard-prod
AWS Region [us-east-1]: us-east-1
Confirm changes before deploy [y/N]: N
Allow SAM CLI IAM role creation [Y/n]: Y
Disable rollback [y/N]: N
Save arguments to configuration file [Y/n]: Y
SAM configuration file [samconfig.toml]: samconfig.toml
SAM configuration environment [default]: default
```

Wait ~3 minutes. At the end you'll see:

```
CloudFormation outputs:
----------------------------------------------------------------------
Key    ApiUrl
Value  https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod
----------------------------------------------------------------------
```

**Copy that URL — this is your production backend URL.**

---

## Step 4 — Update Everything With the New URL

### Android app — `local.properties`
```
API_BASE_URL=https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod
```

### n8n workflow — FOCUSGUARD_BACKEND_URL variable
In n8n cloud: **Settings → Variables**
```
FOCUSGUARD_BACKEND_URL = https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod
```

### .env — API_PUBLIC_URL (for Twilio callbacks)
```
API_PUBLIC_URL=https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod
```

### Update the Twilio secret with the public URL
```powershell
# No Twilio secret needed for API_PUBLIC_URL — it's read from env in Lambda
# Lambda already gets it from the SAM template globals
```

---

## Step 5 — Test the Deployed API

```powershell
# Health check
Invoke-RestMethod -Uri "https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod/health"

# Expected:
# status  : ok
# service : focusguard-api
# phase   : 3
# version : 3.0.0
```

```powershell
# Register a test user
$body = @{
  name     = "Vikas"
  email    = "vikas@focusguard.ai"
  password = "Test@1234"
  phone    = "+918433654259"
} | ConvertTo-Json

$response = Invoke-RestMethod `
  -Method POST `
  -Uri "https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod/auth/register" `
  -ContentType "application/json" `
  -Body $body

$token = $response.access_token
Write-Host "Token: $token"
```

```powershell
# Create a task (Bedrock parses and scores it)
$taskBody = @{ raw_text = "Submit hackathon before Sunday 2 PM urgent" } | ConvertTo-Json

Invoke-RestMethod `
  -Method POST `
  -Uri "https://abc123xyz.execute-api.us-east-1.amazonaws.com/prod/tasks" `
  -ContentType "application/json" `
  -Headers @{ Authorization = "Bearer $token" } `
  -Body $taskBody
```

---

## Subsequent Deployments (After Code Changes)

```powershell
cd F:\Vibe2Ship\focusguard-backend
sam build --use-container
sam deploy   # uses samconfig.toml from first deploy — no prompts
```

---

## Monitoring

View Lambda logs in real time:
```powershell
sam logs --name focusguard-api --region us-east-1 --tail
```

Or in AWS Console:
1. Search **CloudWatch** → Log groups
2. Find `/aws/lambda/focusguard-api`

---

## Cost Estimate (AWS Lambda pricing)

| Resource | Free tier | Expected hackathon usage |
|---|---|---|
| Lambda requests | 1M requests/month free | ~1,000 requests |
| Lambda duration | 400,000 GB-seconds free | ~500 MB × 2s avg = 1,000 GB-s |
| API Gateway | 1M calls free first 12 months | ~1,000 calls |
| Secrets Manager | $0.40/secret/month | 3 secrets = $1.20 |
| **Total** | | **~$1.20/month** |

Lambda itself is free for hackathon usage. Only Secrets Manager has a small cost.

---

## Rollback If Something Goes Wrong

```powershell
# Delete the entire deployment (does NOT delete DynamoDB tables or Secrets)
aws cloudformation delete-stack --stack-name focusguard-prod --region us-east-1
```

DynamoDB tables and Secrets Manager secrets are NOT deleted — your data is safe.

---

## Architecture Summary

```
Android App (Kotlin)
    ↓ HTTPS
API Gateway  →  Lambda (focusguard-api)
                    ↓              ↓
              DynamoDB (8 tables)  Bedrock (Nova Lite)
                    ↓
              Secrets Manager (JWT, Twilio, n8n)

Voice Call Flow:
Lambda → n8n cloud webhook → Lambda /api/alerts/n8n-call → Twilio → User's phone
Twilio STT → Lambda /webhooks/twilio/response/{taskId} → DynamoDB updated
```
