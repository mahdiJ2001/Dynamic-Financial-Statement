package com.pfe.DFinancialStatement.activity.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ActivityLogDTO {
    private Long id;
    private String actionType;
    private String messageKey;
    private Map<String, String> params;
    private String timestamp;
    private UserDTO user;

    @Data
    public static class UserDTO {
        private Long id;
        private String username;
        private String email;
    }
}
