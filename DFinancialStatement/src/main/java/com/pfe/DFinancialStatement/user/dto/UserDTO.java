package com.pfe.DFinancialStatement.user.dto;

import com.pfe.DFinancialStatement.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String email;
    private String username;
    private Role role;
}
