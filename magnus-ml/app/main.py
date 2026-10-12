import logging
import numpy as np
from PIL import Image
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

try:
    import torch
    import torchvision.transforms as transforms
    HAS_TORCH = True
except ImportError:
    HAS_TORCH = False

from app.pdf_utils import convert_to_rgb_image
from app.ela import compute_ela
from app.model import get_detector_model
from app.heatmap import generate_tamper_heatmap_and_regions, image_to_base64_data_uri

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("magnus-ml")

app = FastAPI(
    title="Magnus ML - Forgery & Tamper Detection Service",
    description="Error Level Analysis (ELA) and Deep Learning Computer Vision Service for Document Integrity",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

if HAS_TORCH:
    transform = transforms.Compose([
        transforms.Resize((256, 256)),
        transforms.ToTensor(),
        transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
    ])

class TamperedRegion(BaseModel):
    x: int
    y: int
    width: int
    height: int
    confidence: float
    label: str

class AnalysisResponse(BaseModel):
    is_tampered: bool
    tamper_score: float
    verdict: str
    message: str
    confidence: float
    regions: list[TamperedRegion]
    metrics: dict
    heatmap_image_base64: str
    ela_image_base64: str

@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "magnus-ml",
        "engine": "PyTorch CNN" if HAS_TORCH else "NumPy Convolutional Filter",
        "cuda_available": bool(torch.cuda.is_available()) if HAS_TORCH else False
    }

@app.post("/api/v1/analyze", response_model=AnalysisResponse)
async def analyze_document(file: UploadFile = File(...)):
    try:
        contents = await file.read()
        if len(contents) == 0:
            raise HTTPException(status_code=400, detail="Empty file uploaded.")

        filename = file.filename or "uploaded_document"
        logger.info(f"Analyzing document: {filename} (size: {len(contents)} bytes)")

        # 1. Convert PDF or Image to RGB PIL Image
        try:
            image = convert_to_rgb_image(contents, filename)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"File could not be converted: {str(e)}")

        # 2. Compute Error Level Analysis (ELA)
        ela_image, diff_np, ela_metrics = compute_ela(image, quality=90, scale_factor=15.0)

        # 3. Model Inference via CNN
        model = get_detector_model()

        if HAS_TORCH:
            tensor_input = transform(ela_image).unsqueeze(0)  # Shape: (1, 3, 256, 256)
            with torch.no_grad():
                raw_tamper_prob, spatial_map = model(tensor_input)
                cnn_score = float(raw_tamper_prob.item())
        else:
            ela_np = np.array(ela_image, dtype=np.float32) / 255.0
            cnn_score, spatial_map = model(ela_np)

        # 4. Calibration: Combine CNN confidence with empirical ELA variance
        p95 = ela_metrics.get("p95_error", 0)
        mean_err = ela_metrics.get("mean_error", 1)
        discrepancy_ratio = p95 / max(1.0, mean_err)
        
        # Weighted tamper score
        ela_indicator = min(1.0, max(0.0, (discrepancy_ratio - 1.8) / 3.2))
        calibrated_score = round(float(0.55 * cnn_score + 0.45 * ela_indicator), 2)
        calibrated_score = max(0.02, min(0.98, calibrated_score))

        # 5. Generate Heatmap and Region Annotations
        heatmap_pil, ela_display_pil, regions, summary_metrics = generate_tamper_heatmap_and_regions(
            image,
            ela_image,
            diff_np,
            spatial_map,
            calibrated_score
        )

        # 6. Determine Classification Verdict
        is_tampered = calibrated_score >= 0.50
        if calibrated_score < 0.25:
            verdict = "AUTHENTIC"
            message = "Document exhibits uniform compression artifacts. No digital tampering or text alteration detected."
        elif calibrated_score < 0.50:
            verdict = "LIKELY_AUTHENTIC"
            message = "Minor localized compression variance detected, consistent with standard document rescanning."
        elif calibrated_score < 0.75:
            verdict = "SUSPICIOUS_ALTERATION"
            message = f"Suspicious localized compression discrepancies detected across {len(regions)} region(s). Possible text or grade alteration."
        else:
            verdict = "HIGH_CONFIDENCE_FORGERY"
            message = f"High probability of image manipulation, splicing, or tampered credentials detected across {len(regions)} region(s)."

        # 7. Convert images to Base64
        heatmap_b64 = image_to_base64_data_uri(heatmap_pil, format="JPEG", quality=85)
        ela_b64 = image_to_base64_data_uri(ela_display_pil, format="JPEG", quality=85)

        full_metrics = {
            **ela_metrics,
            **summary_metrics,
            "cnn_raw_prob": round(cnn_score, 3)
        }

        return AnalysisResponse(
            is_tampered=is_tampered,
            tamper_score=calibrated_score,
            verdict=verdict,
            message=message,
            confidence=round(max(calibrated_score, 1.0 - calibrated_score), 2),
            regions=regions,
            metrics=full_metrics,
            heatmap_image_base64=heatmap_b64,
            ela_image_base64=ela_b64
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error during analysis: {str(e)}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Internal ML analysis failure: {str(e)}")
