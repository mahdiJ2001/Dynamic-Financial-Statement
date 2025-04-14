package com.pfe.DFinancialStatement.user.service;

import com.pfe.DFinancialStatement.user.entity.User;
import com.pfe.DFinancialStatement.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Créer un nouvel utilisateur
    public User createUser(User user) {
        return userRepository.save(user);
    }

    // Récupérer un utilisateur par son ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // Récupérer un utilisateur par son email
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Récupérer un utilisateur par son username
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // Mettre à jour un utilisateur
    public User updateUser(User user) {
        return userRepository.save(user);
    }

    // Supprimer un utilisateur par ID
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
