package com.pfe.DFinancialStatement.user.mapper;

import com.pfe.DFinancialStatement.user.dto.UserDTO;
import com.pfe.DFinancialStatement.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // Convertir User en UserDTO
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setEmail(user.getEmail());
        userDTO.setUsername(user.getUsername());
        userDTO.setRole(user.getRole());

        return userDTO;
    }

    // Convertir UserDTO en User
    public User toEntity(UserDTO userDTO) {
        if (userDTO == null) {
            return null;
        }

        User user = new User();
        user.setId(userDTO.getId());
        user.setEmail(userDTO.getEmail());
        user.setUsername(userDTO.getUsername());
        user.setRole(userDTO.getRole());

        return user;
    }
}
