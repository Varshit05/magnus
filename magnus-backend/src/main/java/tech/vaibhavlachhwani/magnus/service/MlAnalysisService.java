package tech.vaibhavlachhwani.magnus.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tech.vaibhavlachhwani.magnus.dto.TamperedRegionDTO;
import tech.vaibhavlachhwani.magnus.dto.VerificationResultDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MlAnalysisService {

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void enrichWithMlAnalysis(MultipartFile file, VerificationResultDTO.VerificationResultDTOBuilder builder) {
        try {
            String url = mlServiceUrl + "/api/v1/analyze";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource contentsAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.png";
                }
            };
            body.add("file", contentsAsResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());

                double tamperScore = root.path("tamper_score").asDouble(0.0);
                String verdict = root.path("verdict").asText("UNKNOWN");
                String mlMessage = root.path("message").asText("");
                String heatmapBase64 = root.path("heatmap_image_base64").asText("");
                String elaImageBase64 = root.path("ela_image_base64").asText("");

                List<TamperedRegionDTO> regions = new ArrayList<>();
                JsonNode regionsNode = root.path("regions");
                if (regionsNode.isArray()) {
                    for (JsonNode r : regionsNode) {
                        regions.add(TamperedRegionDTO.builder()
                                .x(r.path("x").asInt())
                                .y(r.path("y").asInt())
                                .width(r.path("width").asInt())
                                .height(r.path("height").asInt())
                                .confidence(r.path("confidence").asDouble())
                                .label(r.path("label").asText())
                                .build());
                    }
                }

                Map<String, Object> metrics = new HashMap<>();
                JsonNode metricsNode = root.path("metrics");
                if (metricsNode.isObject()) {
                    metricsNode.fields().forEachRemaining(entry -> {
                        metrics.put(entry.getKey(), entry.getValue().asText());
                    });
                }

                builder.mlAnalyzed(true)
                       .tamperScore(tamperScore)
                       .tamperVerdict(verdict)
                       .heatmapBase64(heatmapBase64)
                       .elaImageBase64(elaImageBase64)
                       .tamperedRegions(regions)
                       .metrics(metrics);

                log.info("ML forensic analysis complete: verdict={}, tamperScore={}, regionsCount={}",
                        verdict, tamperScore, regions.size());
            } else {
                builder.mlAnalyzed(false);
            }
        } catch (Exception e) {
            log.warn("ML forensic service unavailable or failed: {}", e.getMessage());
            builder.mlAnalyzed(false);
        }
    }
}
