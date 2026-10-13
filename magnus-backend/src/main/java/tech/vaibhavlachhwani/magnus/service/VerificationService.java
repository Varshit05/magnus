package tech.vaibhavlachhwani.magnus.service;

import org.springframework.stereotype.Service;
import tech.vaibhavlachhwani.magnus.model.Document;
import tech.vaibhavlachhwani.magnus.repository.DocumentRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationService {
    private final DocumentRepository documentRepository;

    public VerificationService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Optional<Document> verifyByHash(String hash) {
        return documentRepository.findFirstByHashValue(hash);
    }

    public Optional<Document> verifyById(UUID id) {
        return documentRepository.findById(id);
    }
}
