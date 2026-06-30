"""
Creates a deployment .zip ready to upload to AWS Lambda via the website.

Usage (from focusguard-backend folder):
    $env:PYTHONPATH="."; python scripts/create_deployment_zip.py

Output: focusguard_lambda.zip
"""

import os
import subprocess
import zipfile
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).parent.parent   # focusguard-backend/
DIST = ROOT / "lambda_build"
ZIP  = ROOT / "focusguard_lambda.zip"

# Folders/files to include
INCLUDE = ["app", "main.py", "lambda_handler.py", "requirements.txt"]

# Patterns to skip
SKIP = {"__pycache__", ".pyc", ".pyo", ".env", ".git", "lambda_build",
        "focusguard_lambda.zip", "scripts", "venv", ".venv"}

print("Step 1 — Installing dependencies into lambda_build/...")
if DIST.exists():
    shutil.rmtree(DIST)
DIST.mkdir()

subprocess.check_call([
    sys.executable, "-m", "pip", "install",
    "-r", str(ROOT / "requirements.txt"),
    "--target", str(DIST),
    "--quiet",
])

print("Step 2 — Copying source files...")
for item in INCLUDE:
    src = ROOT / item
    if src.is_dir():
        shutil.copytree(src, DIST / item, dirs_exist_ok=True)
    elif src.is_file():
        shutil.copy2(src, DIST / item)

print("Step 3 — Creating ZIP...")
if ZIP.exists():
    ZIP.unlink()

with zipfile.ZipFile(ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
    for file in DIST.rglob("*"):
        # Skip cache files and test directories
        parts = set(file.parts)
        if any(s in str(file) for s in SKIP):
            continue
        if file.is_file():
            zf.write(file, file.relative_to(DIST))

size_mb = ZIP.stat().st_size / (1024 * 1024)
print(f"\nDone!  focusguard_lambda.zip created — {size_mb:.1f} MB")
print(f"Path:  {ZIP}")
print("\nNext: Upload this zip to AWS Lambda (see DEPLOY_AWS_CONSOLE.md)")
