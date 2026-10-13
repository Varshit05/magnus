package tech.vaibhavlachhwani.magnus.service;

import tech.vaibhavlachhwani.magnus.model.Role;

import tech.vaibhavlachhwani.magnus.model.User;
import tech.vaibhavlachhwani.magnus.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String username, String rawPassword, Set<Role> roles) {
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles(roles != null ? roles : Collections.singleton(Role.ROLE_USER))
                .build();
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
