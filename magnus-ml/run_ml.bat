@echo off
echo Starting Magnus ML Forensic & Tamper Detection Service on port 8000...
cd /d "%~dp0"
..\magnus-ml-venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
