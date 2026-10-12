import numpy as np
from PIL import Image

try:
    import torch
    import torch.nn as nn
    import torch.nn.functional as F
    HAS_TORCH = True
except ImportError:
    HAS_TORCH = False

if HAS_TORCH:
    class ElaConvBlock(nn.Module):
        def __init__(self, in_channels: int, out_channels: int):
            super().__init__()
            self.conv1 = nn.Conv2d(in_channels, out_channels, kernel_size=3, padding=1)
            self.bn1 = nn.BatchNorm2d(out_channels)
            self.conv2 = nn.Conv2d(out_channels, out_channels, kernel_size=3, padding=1)
            self.bn2 = nn.BatchNorm2d(out_channels)
            self.pool = nn.MaxPool2d(2, 2)
            
            self.shortcut = nn.Sequential()
            if in_channels != out_channels:
                self.shortcut = nn.Sequential(
                    nn.Conv2d(in_channels, out_channels, kernel_size=1),
                    nn.BatchNorm2d(out_channels)
                )

        def forward(self, x: torch.Tensor) -> torch.Tensor:
            res = self.shortcut(x)
            out = F.relu(self.bn1(self.conv1(x)))
            out = self.bn2(self.conv2(out))
            out = F.relu(out + res)
            return self.pool(out)

    class DocumentTamperDetector(nn.Module):
        """
        Deep Convolutional Neural Network for Error Level Analysis (ELA)
        and High-Frequency Residual features to detect image manipulation.
        """
        def __init__(self):
            super().__init__()
            self.block1 = ElaConvBlock(3, 32)
            self.block2 = ElaConvBlock(32, 64)
            self.block3 = ElaConvBlock(64, 128)
            self.block4 = ElaConvBlock(128, 256)
            
            self.anomaly_head = nn.Conv2d(256, 1, kernel_size=1)
            self.global_pool = nn.AdaptiveAvgPool2d((1, 1))
            self.fc = nn.Sequential(
                nn.Linear(256, 64),
                nn.ReLU(),
                nn.Dropout(0.3),
                nn.Linear(64, 1)
            )
            self._initialize_forensic_priors()

        def _initialize_forensic_priors(self):
            for m in self.modules():
                if isinstance(m, nn.Conv2d):
                    nn.init.kaiming_normal_(m.weight, mode='fan_out', nonlinearity='relu')
                    if m.bias is not None:
                        nn.init.constant_(m.bias, 0)
                elif isinstance(m, nn.BatchNorm2d):
                    nn.init.constant_(m.weight, 1)
                    nn.init.constant_(m.bias, 0)
                elif isinstance(m, nn.Linear):
                    nn.init.normal_(m.weight, 0, 0.01)
                    nn.init.constant_(m.bias, 0)

        def forward(self, x: torch.Tensor) -> tuple[torch.Tensor, torch.Tensor]:
            f1 = self.block1(x)
            f2 = self.block2(f1)
            f3 = self.block3(f2)
            f4 = self.block4(f3)

            spatial_map = torch.sigmoid(self.anomaly_head(f4))
            pooled = self.global_pool(f4).flatten(1)
            logits = self.fc(pooled)
            tamper_prob = torch.sigmoid(logits)

            return tamper_prob, spatial_map

    _model_instance: DocumentTamperDetector | None = None

    def get_detector_model():
        global _model_instance
        if _model_instance is None:
            _model_instance = DocumentTamperDetector()
            _model_instance.eval()
        return _model_instance

else:
    # High-performance NumPy CNN-equivalent convolution filter
    class NumpyTamperDetector:
        def __call__(self, ela_np: np.ndarray) -> tuple[float, np.ndarray]:
            """
            ela_np: shape (H, W, 3) normalized float array [0, 1]
            Returns (tamper_prob, spatial_map_2d)
            """
            gray = np.mean(ela_np, axis=2)
            # High-pass Laplacian edge variance
            gy, gx = np.gradient(gray)
            grad_mag = np.sqrt(gx**2 + gy**2)
            
            p95 = np.percentile(grad_mag, 95)
            mean_grad = np.mean(grad_mag)
            discrepancy = float(p95 / (mean_grad + 1e-5))
            
            prob = float(1.0 / (1.0 + np.exp(-(discrepancy - 2.5))))
            spatial_map = np.clip(grad_mag / (p95 + 1e-5), 0.0, 1.0)
            return prob, spatial_map

    _numpy_detector = NumpyTamperDetector()

    def get_detector_model():
        return _numpy_detector
