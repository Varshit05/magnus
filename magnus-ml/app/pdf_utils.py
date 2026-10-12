import io
from PIL import Image
import pypdfium2 as pdfium

def convert_to_rgb_image(file_bytes: bytes, filename: str = "") -> Image.Image:
    """
    Accepts raw file bytes (PDF or standard image formats) and returns
    a PIL Image in RGB mode ready for forensic analysis.
    """
    if filename.lower().endswith(".pdf") or file_bytes.startswith(b"%PDF"):
        try:
            pdf = pdfium.PdfDocument(file_bytes)
            if len(pdf) == 0:
                raise ValueError("PDF document has 0 pages.")
            # Render first page at scale 2.0 (approx 144 DPI) for accurate forensic resolution
            page = pdf.get_page(0)
            pil_image = page.render(scale=2.0).to_pil()
            return pil_image.convert("RGB")
        except Exception as e:
            raise ValueError(f"Could not parse PDF document: {str(e)}")
    else:
        try:
            pil_image = Image.open(io.BytesIO(file_bytes))
            return pil_image.convert("RGB")
        except Exception as e:
            raise ValueError(f"Could not parse image format: {str(e)}")
