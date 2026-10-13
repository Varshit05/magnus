package tech.vaibhavlachhwani.magnus.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import tech.vaibhavlachhwani.magnus.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);
}
