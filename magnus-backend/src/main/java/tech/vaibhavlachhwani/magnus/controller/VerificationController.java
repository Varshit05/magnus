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

    public VerificationController(HashService hashService, VerificationService verificationService) {
        this.hashService = hashService;
        this.verificationService = verificationService;
    }

    @PostMapping()
    public ResponseEntity<VerificationResultDTO> verifyDocumentByHash(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String hash = hashService.generateHmac(file.getBytes());

        Optional<Document> doc = verificationService.verifyByHash(hash);

        if (doc.isPresent()) {
            return ResponseEntity.ok(VerificationResultDTO.builder()
                    .authentic(true)
                    .status("AUTHENTIC")
                    .documentId(doc.get().getId())
                    .message("The document is authentic and has not been forged.")
                    .build());
        } else {
            return ResponseEntity.ok(VerificationResultDTO.builder()
                    .authentic(false)
                    .status("NOT AUTHENTIC")
                    .message("The document could not be verified or has been forged.")
                    .build());
        }
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
