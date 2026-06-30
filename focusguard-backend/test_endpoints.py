import urllib.request, json, time

URL = "https://6f0tbzcsr6.execute-api.us-east-1.amazonaws.com"

print("=== HEALTH ===")
res = urllib.request.urlopen(URL + "/health", timeout=15)
print(res.status, res.read().decode())

print("\n=== REGISTER ===")
data = json.dumps({"name":"TestUser","email":"apicheck2@focusguard.ai","password":"Test@1234","phone":"+918433654259"}).encode()
token = ""
try:
    req = urllib.request.Request(URL + "/auth/register", data=data, headers={"Content-Type":"application/json"}, method="POST")
    res = urllib.request.urlopen(req, timeout=20)
    body = json.loads(res.read().decode())
    print(res.status, "OK - token:", body.get("access_token","")[:40]+"...")
    token = body.get("access_token","")
except Exception as e:
    print("FAILED:", e)

print("\n=== LOGIN ===")
data = json.dumps({"email":"apicheck2@focusguard.ai","password":"Test@1234"}).encode()
try:
    req = urllib.request.Request(URL + "/auth/login", data=data, headers={"Content-Type":"application/json"}, method="POST")
    res = urllib.request.urlopen(req, timeout=20)
    body = json.loads(res.read().decode())
    print(res.status, "OK - token:", body.get("access_token","")[:40]+"...")
    token = body.get("access_token", token)
except Exception as e:
    print("FAILED:", e)

if token:
    print("\n=== GET TASKS ===")
    try:
        req = urllib.request.Request(URL + "/tasks", headers={"Authorization": "Bearer " + token})
        res = urllib.request.urlopen(req, timeout=20)
        print(res.status, res.read().decode()[:200])
    except Exception as e:
        print("FAILED:", e)

    print("\n=== CREATE TASK ===")
    data = json.dumps({"raw_text":"Submit hackathon before Sunday 2PM urgent"}).encode()
    try:
        req = urllib.request.Request(URL + "/tasks", data=data, headers={"Content-Type":"application/json","Authorization":"Bearer "+token}, method="POST")
        res = urllib.request.urlopen(req, timeout=30)
        print(res.status, res.read().decode()[:300])
    except Exception as e:
        print("FAILED:", e)

    print("\n=== DASHBOARD ===")
    try:
        req = urllib.request.Request(URL + "/dashboard/daily", headers={"Authorization": "Bearer " + token})
        res = urllib.request.urlopen(req, timeout=20)
        print(res.status, res.read().decode()[:200])
    except Exception as e:
        print("FAILED:", e)
else:
    print("\nNo token — skipping auth-protected endpoints")
