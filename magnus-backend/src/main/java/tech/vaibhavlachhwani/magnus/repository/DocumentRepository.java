package tech.vaibhavlachhwani.magnus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.vaibhavlachhwani.magnus.model.Document;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {
    Optional<Document> findFirstByHashValue(String hashValue);
}
