import io
import base64
import cv2
import numpy as np
import torch
from PIL import Image

def image_to_base64_data_uri(pil_image: Image.Image, format: str = "JPEG", quality: int = 85) -> str:
    """Encodes a PIL Image to base64 data URI string"""
    buffer = io.BytesIO()
    pil_image.save(buffer, format=format, quality=quality)
    encoded = base64.b64encode(buffer.getvalue()).decode("utf-8")
    mime = "image/jpeg" if format.upper() == "JPEG" else "image/png"
    return f"data:{mime};base64,{encoded}"

def generate_tamper_heatmap_and_regions(
    original_image: Image.Image,
    ela_image: Image.Image,
    diff_np: np.ndarray,
    spatial_map_tensor: torch.Tensor,
    tamper_prob_val: float
) -> tuple[Image.Image, Image.Image, list[dict], dict]:
    """
    Generates:
      1. Blended Heatmap Overlay image (Original + Jet Thermal Map)
      2. Enhanced ELA image
      3. List of detected suspicious bounding boxes/regions
      4. Summary metrics
    """
    orig_w, orig_h = original_image.size
    orig_np = np.array(original_image)
    orig_bgr = cv2.cvtColor(orig_np, cv2.COLOR_RGB2BGR)

    # 1. Process ELA diff map
    # Take max across RGB channels to get raw error intensity
    ela_intensity = np.max(diff_np, axis=2)
    # Smooth with Gaussian blur to extract macro compression differences
    ela_blurred = cv2.GaussianBlur(ela_intensity, (21, 21), 0)
    
    # Normalize ELA intensity
    ela_min, ela_max = np.min(ela_blurred), np.max(ela_blurred)
    if ela_max > ela_min:
        ela_norm = (ela_blurred - ela_min) / (ela_max - ela_min)
    else:
        ela_norm = np.zeros_like(ela_blurred)

    # 2. Process CNN spatial map
    if isinstance(spatial_map_tensor, np.ndarray):
        spatial_np = spatial_map_tensor
    else:
        spatial_np = spatial_map_tensor.squeeze().detach().cpu().numpy()
    spatial_resized = cv2.resize(spatial_np, (orig_w, orig_h), interpolation=cv2.INTER_CUBIC)
    spatial_resized = cv2.GaussianBlur(spatial_resized, (25, 25), 0)

    # 3. Fuse ELA discrepancies with CNN spatial anomaly attention
    fused_map = (0.55 * ela_norm + 0.45 * spatial_resized)
    fused_norm = np.clip(fused_map, 0.0, 1.0)
    heatmap_gray = (fused_norm * 255.0).astype(np.uint8)

    # 4. Colorize with JET thermal colormap
    heatmap_colored = cv2.applyColorMap(heatmap_gray, cv2.COLORMAP_JET)

    # 5. Blend overlay with original image
    alpha = 0.45
    blended_bgr = cv2.addWeighted(heatmap_colored, alpha, orig_bgr, 1 - alpha, 0)

    # 6. Extract high-discrepancy candidate regions (Bounding Boxes)
    # Threshold at top percentile anomaly
    threshold_val = int(max(140, np.percentile(heatmap_gray, 88)))
    _, thresh_mask = cv2.threshold(heatmap_gray, threshold_val, 255, cv2.THRESH_BINARY)
    
    # Morphological dilation to group nearby letters/words together into coherent blocks
    kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (15, 7))
    dilated_mask = cv2.dilate(thresh_mask, kernel, iterations=2)

    contours, _ = cv2.findContours(dilated_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    regions = []
    min_area = (orig_w * orig_h) * 0.0004  # Minimum 0.04% of page area
    max_area = (orig_w * orig_h) * 0.40    # Maximum 40% of page area

    # Annotate copy for visual overlay
    annotated_bgr = blended_bgr.copy()

    for cnt in contours:
        area = cv2.contourArea(cnt)
        if min_area < area < max_area:
            x, y, w, h = cv2.boundingRect(cnt)
            # Compute average anomaly score inside this bounding box
            roi = fused_norm[y:y+h, x:x+w]
            region_confidence = float(np.mean(roi)) if roi.size > 0 else float(tamper_prob_val)

            # Categorize label based on shape and aspect ratio
            aspect_ratio = w / float(h)
            if aspect_ratio > 3.0:
                label = "Modified Text Field"
            elif 0.8 <= aspect_ratio <= 1.5 and w > 40:
                label = "Altered Stamp / Logo"
            else:
                label = "Spliced / Edited Region"

            # Draw glowing red border on annotated overlay
            cv2.rectangle(annotated_bgr, (x, y), (x + w, y + h), (0, 0, 255), 2)
            # Add small label pill
            cv2.rectangle(annotated_bgr, (x, max(0, y - 20)), (x + min(180, w), y), (0, 0, 220), -1)
            cv2.putText(
                annotated_bgr,
                f"{label} ({int(region_confidence * 100)}%)",
                (x + 4, max(12, y - 5)),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.4,
                (255, 255, 255),
                1,
                cv2.LINE_AA
            )

            regions.append({
                "x": int(x),
                "y": int(y),
                "width": int(w),
                "height": int(h),
                "confidence": round(region_confidence, 2),
                "label": label
            })

    # Sort regions by confidence descending
    regions.sort(key=lambda r: r["confidence"], reverse=True)

    # Convert annotated image back to RGB PIL Image
    annotated_rgb = cv2.cvtColor(annotated_bgr, cv2.COLOR_BGR2RGB)
    annotated_pil = Image.fromarray(annotated_rgb)

    summary = {
        "anomaly_mean": round(float(np.mean(fused_norm)), 3),
        "anomaly_peak": round(float(np.max(fused_norm)), 3),
        "tamper_hotspots_count": len(regions)
    }

    return annotated_pil, ela_image, regions, summary
