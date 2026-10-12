Write-Host "Starting Magnus ML Forensic & Tamper Detection Service on port 8000..." -ForegroundColor Cyan
Set-Location -Path $PSScriptRoot
& "..\magnus-ml-venv\Scripts\python.exe" -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
