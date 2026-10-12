package tech.vaibhavlachhwani.magnus.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tech.vaibhavlachhwani.magnus.crypto.HashService;
import tech.vaibhavlachhwani.magnus.dto.DocumentDTO;
import tech.vaibhavlachhwani.magnus.dto.VerificationResultDTO;
import tech.vaibhavlachhwani.magnus.model.Document;
import tech.vaibhavlachhwani.magnus.service.VerificationService;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/verify")
public class VerificationController {
    private final HashService hashService;
    private final VerificationService verificationService;
    private final tech.vaibhavlachhwani.magnus.service.MlAnalysisService mlAnalysisService;

    public VerificationController(
            HashService hashService,
            VerificationService verificationService,
            tech.vaibhavlachhwani.magnus.service.MlAnalysisService mlAnalysisService
    ) {
        this.hashService = hashService;
        this.verificationService = verificationService;
        this.mlAnalysisService = mlAnalysisService;
    }

    @PostMapping()
    public ResponseEntity<VerificationResultDTO> verifyDocumentByHash(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String hash = hashService.generateHmac(file.getBytes());

        Optional<Document> doc = verificationService.verifyByHash(hash);
        VerificationResultDTO.VerificationResultDTOBuilder builder = VerificationResultDTO.builder();

        // Run ML Forensic Analysis (Error Level Analysis + CNN Tamper Detection)
        mlAnalysisService.enrichWithMlAnalysis(file, builder);

        if (doc.isPresent()) {
            builder.authentic(true)
                    .status("AUTHENTIC")
                    .documentId(doc.get().getId())
                    .message("The document is authentic and has not been forged.");
            return ResponseEntity.ok(builder.build());
        } else {
            builder.authentic(false)
                    .status("NOT AUTHENTIC");
            
            VerificationResultDTO partial = builder.build();
            if (Boolean.TRUE.equals(partial.getMlAnalyzed()) && partial.getTamperScore() != null && partial.getTamperScore() >= 0.50) {
                builder.message("Cryptographic match failed. AI Forensic Analysis detected digital tampering (" 
                        + (int)(partial.getTamperScore() * 100) + "% confidence).");
            } else {
                builder.message("The document could not be verified or has been forged.");
            }
            return ResponseEntity.status(404).body(builder.build());
        }
    }

    @PostMapping("/forensic-analysis")
    public ResponseEntity<VerificationResultDTO> analyzeForensics(
            @RequestParam("file") MultipartFile file
    ) {
        VerificationResultDTO.VerificationResultDTOBuilder builder = VerificationResultDTO.builder();
        mlAnalysisService.enrichWithMlAnalysis(file, builder);
        VerificationResultDTO partial = builder.build();

        boolean isAuthentic = partial.getTamperScore() != null && partial.getTamperScore() < 0.50;
        builder.authentic(isAuthentic)
               .status(isAuthentic ? "AUTHENTIC" : "TAMPERED")
               .message("AI Forensic Analysis completed: " + (partial.getTamperVerdict() != null ? partial.getTamperVerdict() : "Processed"));

        return ResponseEntity.ok(builder.build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> verifyDocumentById(@PathVariable UUID id) {
        Optional<Document> doc = verificationService.verifyById(id);

        if (doc.isPresent()) {
            return ResponseEntity.ok(convertToDTO(doc.get()));
        } else {
            return ResponseEntity.status(404).body(VerificationResultDTO.builder()
                    .authentic(false)
                    .status("NOT FOUND")
                    .message("No document found with the provided ID.")
                    .build());
        }
    }

    private DocumentDTO convertToDTO(Document doc) {
        return DocumentDTO.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .hashValue(doc.getHashValue())
                .issueDate(doc.getIssueDate())
                .studentId(doc.getStudent() != null ? doc.getStudent().getId() : null)
                .build();
    }
}
