# Magnus ML - Document Forgery & Tamper Detection Microservice

A computer vision microservice for the Magnus Document Verification platform that exposes digital forgery, photoshopped grades, altered student names, cloned blocks, and tampered institutional seals using **Error Level Analysis (ELA)** and **Deep Convolutional Neural Networks (CNN)**.

---

## 🔬 How It Works

### 1. Error Level Analysis (ELA)
- Standard JPEG compression creates uniform 8x8 DCT quantization error levels across the image.
- When an attacker edits a document (e.g., photoshops a higher grade, alters a name, or pastes an institutional seal from another image), the modified pixels exhibit significantly different compression artifact rates than the original untouched regions.
- ELA re-saves the document at a controlled quality (90) and computes the pixel-by-pixel differential:
  $$\text{ELA}(x, y) = |\text{Original}(x, y) - \text{Compressed}_{90}(x, y)|$$
- High discrepancy regions reveal spliced and altered content.

### 2. Deep Learning Spatial Anomaly Detection (CNN / ResNet)
- A deep convolutional network with residual skip connections processes multi-channel ELA representations.
- Extracts spatial anomaly attention maps pinpointing high-frequency compression discontinuities.
- Computes calibrated **Tamper Risk Scores** (0.00 to 1.00) and classifies the document as:
  - `AUTHENTIC` (< 25%)
  - `LIKELY_AUTHENTIC` (25% - 50%)
  - `SUSPICIOUS_ALTERATION` (50% - 75%)
  - `HIGH_CONFIDENCE_FORGERY` (> 75%)

### 3. Thermal Forgery Heatmap & Bounding Box Extraction
- Fuses ELA variance with CNN spatial activations into a normalized anomaly field.
- Applies a Jet thermal colormap overlay:
  - 🔵 **Blue / Cyan**: Pristine untouched background
  - 🟡 **Yellow**: Minor text edge variance
  - 🔴 **Red Hotspots**: Altered text fields, pasted stamps, or spliced elements
- Runs morphological clustering and contour analysis to locate exact bounding boxes `(x, y, width, height)` of suspected tampering.

---

## 🚀 Running the ML Service

### Option A: Local Python Environment (PowerShell)
```powershell
cd magnus-ml
.\run_ml.ps1
```
Or manually:
```powershell
..\magnus-ml-venv\Scripts\python -m uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Option B: Docker Compose
```powershell
docker compose up magnus-ml -d
```

---

## 📡 API Endpoints

### 1. Health Check
`GET /health`
```json
{
  "status": "UP",
  "service": "magnus-ml",
  "engine": "PyTorch CNN / NumPy Convolutional Filter",
  "cuda_available": false
}
```

### 2. Analyze Document
`POST /api/v1/analyze`
- Content-Type: `multipart/form-data`
- Parameter: `file` (PDF, PNG, JPG, WEBP)

**Response:**
```json
{
  "is_tampered": true,
  "tamper_score": 0.82,
  "verdict": "HIGH_CONFIDENCE_FORGERY",
  "message": "High probability of image manipulation, splicing, or tampered credentials detected across 2 region(s).",
  "confidence": 0.82,
  "regions": [
    {
      "x": 234,
      "y": 512,
      "width": 120,
      "height": 38,
      "confidence": 0.86,
      "label": "Modified Text Field"
    }
  ],
  "metrics": {
    "mean_error": 14.2,
    "p95_error": 68.4,
    "anomaly_peak": 0.89,
    "tamper_hotspots_count": 2
  },
  "heatmap_image_base64": "data:image/jpeg;base64,...",
  "ela_image_base64": "data:image/jpeg;base64,..."
}
```
