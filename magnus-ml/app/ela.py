import io
import numpy as np
from PIL import Image, ImageChops, ImageEnhance

def compute_ela(image: Image.Image, quality: int = 90, scale_factor: float = 15.0) -> tuple[Image.Image, np.ndarray, dict]:
    """
    Performs Error Level Analysis (ELA) on a PIL RGB image.
    Returns:
      - ela_image: Enhanced PIL Image highlighting compression rate discrepancies.
      - raw_diff_np: Raw difference numpy array (H, W, 3) in float32.
      - metrics: Dictionary containing statistical metrics (mean error, peak error, variance).
    """
    # 1. Resave image in memory as JPEG at specific quality
    buffer = io.BytesIO()
    image.save(buffer, format="JPEG", quality=quality)
    buffer.seek(0)
    resaved = Image.open(buffer).convert("RGB")

    # 2. Compute absolute difference between original and resaved
    diff = ImageChops.difference(image, resaved)
    diff_np = np.array(diff, dtype=np.float32)

    # 3. Enhance diff for visual display
    # We dynamically calculate brightness multiplier based on peak difference
    extrema = diff.getextrema()
    max_diff = max([ex[1] for ex in extrema])
    if max_diff == 0:
        max_diff = 1
    
    # Dynamic scale with upper limit
    scale = min(255.0 / max_diff, scale_factor)
    enhancer = ImageEnhance.Brightness(diff)
    ela_image = enhancer.enhance(scale)

    # 4. Statistical metrics for anomaly calculation
    # High variance or localized spikes indicate digital tampering (copy-move, text alteration, pasted seals)
    mean_error = float(np.mean(diff_np))
    std_error = float(np.std(diff_np))
    max_error = float(np.max(diff_np))
    p95_error = float(np.percentile(diff_np, 95))

    metrics = {
        "mean_error": round(mean_error, 2),
        "std_error": round(std_error, 2),
        "max_error": round(max_error, 2),
        "p95_error": round(p95_error, 2),
        "scale_applied": round(scale, 2)
    }

    return ela_image, diff_np, metrics
