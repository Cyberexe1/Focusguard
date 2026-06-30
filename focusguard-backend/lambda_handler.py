"""
AWS Lambda entry point — wraps FastAPI with Mangum.
Mangum translates API Gateway events into ASGI requests.
"""
from mangum import Mangum
from main import app

# Mangum wraps the FastAPI ASGI app for Lambda
handler = Mangum(app, lifespan="off")
