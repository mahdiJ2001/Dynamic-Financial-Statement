package com.pfe.DFinancialStatement.user.repository;

import com.pfe.DFinancialStatement.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Trouver un utilisateur par son email
    Optional<User> findByEmail(String email);

    // Trouver un utilisateur par son username
    Optional<User> findByUsername(String username);
}
