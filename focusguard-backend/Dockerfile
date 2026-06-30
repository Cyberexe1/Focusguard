FROM python:3.11-slim

WORKDIR /app

# Install dependencies
COPY requirements.cloudrun.txt .
RUN pip install --no-cache-dir -r requirements.cloudrun.txt

# Copy application source
COPY app/ ./app/
COPY main.py .

# Cloud Run listens on PORT env var (default 8080)
ENV PORT=8080

EXPOSE 8080

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]
